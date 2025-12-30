use std::process::Command;
use std::sync::Mutex;
use tauri::{AppHandle, Runtime, Emitter};
use tempfile::Builder;
use std::io::{Write, Seek, SeekFrom};
use notify::{Watcher, RecursiveMode, Event, Config};
use tokio::sync::oneshot;

pub struct ExternalEditorState {
    pub temp_file: Mutex<Option<tempfile::NamedTempFile>>,
    pub abort_handle: Mutex<Option<oneshot::Sender<()>>>,
}

impl ExternalEditorState {
    pub fn new() -> Self {
        Self {
            temp_file: Mutex::new(None),
            abort_handle: Mutex::new(None),
        }
    }
}

pub struct ExternalEditor;

impl ExternalEditor {
    pub fn open<R: Runtime>(
        app: AppHandle<R>,
        content: String,
        line: u32,
        col: u32,
        state: tauri::State<'_, ExternalEditorState>,
    ) -> Result<(), String> {
        // 1. Cancel existing watcher task if any
        if let Some(old_abort) = state.abort_handle.lock().unwrap().take() {
            let _ = old_abort.send(());
        }

        let mut temp_file_guard = state.temp_file.lock().unwrap();
        
        // 2. Create temp file with .txt suffix for proper editor support
        if temp_file_guard.is_none() {
            let file = Builder::new()
                .prefix("contents_")
                .suffix(".txt")
                .tempfile()
                .map_err(|e| e.to_string())?;
            *temp_file_guard = Some(file);
        }
        
        let temp_file = temp_file_guard.as_mut().unwrap();
        
        // 3. Setup file content
        temp_file.as_file_mut().set_len(0).map_err(|e| e.to_string())?;
        temp_file.as_file_mut().seek(SeekFrom::Start(0)).map_err(|e| e.to_string())?;
        temp_file.as_file_mut().write_all(content.as_bytes()).map_err(|e| e.to_string())?;
        temp_file.as_file_mut().flush().map_err(|e| e.to_string())?;

        let file_path = temp_file.path().to_path_buf();
        let app_clone = app.clone();
        
        // 4. Watcher cancellation channel
        let (stop_tx, mut stop_rx) = oneshot::channel::<()>();
        *state.abort_handle.lock().unwrap() = Some(stop_tx);

        // 5. Async File Watcher
        tokio::spawn(async move {
            let (mut watcher, mut rx) = Self::async_watcher().unwrap();
            let parent = file_path.parent().unwrap();

            if let Err(e) = watcher.watch(parent, RecursiveMode::NonRecursive) {
                eprintln!("External Editor: Watcher failed: {:?}", e);
                return;
            }

            let mut last_content_hash = Self::calculate_hash(&content);

            loop {
                tokio::select! {
                    _ = &mut stop_rx => break,
                    res = rx.recv() => {
                        match res {
                            Some(Ok(event)) => {
                                if event.paths.contains(&file_path) && event.kind.is_modify() {
                                    // Debounce for atomic save stability
                                    tokio::time::sleep(std::time::Duration::from_millis(50)).await;
                                    if let Ok(new_content) = std::fs::read_to_string(&file_path) {
                                        let new_hash = Self::calculate_hash(&new_content);
                                        if new_hash != last_content_hash {
                                            last_content_hash = new_hash;
                                            let _ = app_clone.emit("external-editor-sync", new_content);
                                        }
                                    }
                                }
                            }
                            _ => break,
                        }
                    }
                }
            }
        });

        // 6. Editor Execution
        let goto_arg = format!("{}:{}:{}", temp_file.path().to_string_lossy(), line, col);
        let file_path_for_final_sync = temp_file.path().to_path_buf();
        
        drop(temp_file_guard);

        app.emit("external-editor-start", ()).unwrap();

        let app_clone_for_exit = app.clone();
        std::thread::spawn(move || {
            let executables = if cfg!(windows) { vec!["code.cmd", "code-insiders.cmd"] } else { vec!["code", "code-insiders"] };
            let mut success = false;

            for exe in executables {
                let mut command = Command::new(exe);
                command.args(&["-n", "-w", "-g", &goto_arg]);
                
                match command.status() {
                    Ok(status) => {
                        if status.success() {
                            success = true;
                            break;
                        }
                    }
                    Err(_) => continue,
                }
            }

            if !success {
                let _ = app_clone_for_exit.emit("external-editor-error", "VS Code (code) not found in PATH.");
            }
            
            // 7. Final Sync on Close
            if let Ok(final_content) = std::fs::read_to_string(&file_path_for_final_sync) {
                let _ = app_clone_for_exit.emit("external-editor-sync", final_content);
            }

            let _ = app_clone_for_exit.emit("external-editor-end", ());
        });

        Ok(())
    }

    fn async_watcher() -> notify::Result<(notify::RecommendedWatcher, tokio::sync::mpsc::Receiver<notify::Result<Event>>)> {
        let (tx, rx) = tokio::sync::mpsc::channel(1);
        let watcher = notify::RecommendedWatcher::new(move |res| {
            let _ = tx.blocking_send(res);
        }, Config::default())?;
        Ok((watcher, rx))
    }

    fn calculate_hash(t: &str) -> u64 {
        use std::collections::hash_map::DefaultHasher;
        use std::hash::{Hash, Hasher};
        let mut s = DefaultHasher::new();
        t.hash(&mut s);
        s.finish()
    }
}

#[tauri::command]
pub async fn open_external_editor<R: Runtime>(
    app: AppHandle<R>,
    content: String,
    line: u32,
    col: u32,
    state: tauri::State<'_, ExternalEditorState>,
) -> Result<(), String> {
    ExternalEditor::open(app, content, line, col, state)
}
