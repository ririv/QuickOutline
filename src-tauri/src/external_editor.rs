use std::process::{Command, Child};
use std::sync::Mutex;
use tauri::{AppHandle, Runtime, Emitter, Manager};
use tempfile::Builder;
use std::io::{Write, Seek, SeekFrom};
use notify::{Watcher, RecursiveMode, Event, Config};
use tokio::sync::oneshot;
use std::path::Path;

pub struct ExternalEditorState {
    pub temp_file: Mutex<Option<tempfile::NamedTempFile>>,
    pub abort_handle: Mutex<Option<oneshot::Sender<()>>>,
    /// Tracks the active editor process to kill it on app exit.
    pub active_child: Mutex<Option<Child>>,
}

impl ExternalEditorState {
    pub fn new() -> Self {
        Self {
            temp_file: Mutex::new(None),
            abort_handle: Mutex::new(None),
            active_child: Mutex::new(None),
        }
    }
}

// --- Editor Backend Abstraction ---

pub trait EditorBackend: Send + Sync {
    fn is_available(&self) -> bool;
    fn build_command(&self, file_path: &Path, line: u32, col: u32) -> Command;
    fn id(&self) -> &'static str;
}

struct VscodeBackend {
    executable: &'static str,
}

impl EditorBackend for VscodeBackend {
    fn is_available(&self) -> bool {
        let exe = if cfg!(windows) { format!("{}.cmd", self.executable) } else { self.executable.to_string() };
        Command::new(exe)
            .arg("--version")
            .stdout(std::process::Stdio::null())
            .stderr(std::process::Stdio::null())
            .status()
            .is_ok()
    }

    fn build_command(&self, file_path: &Path, line: u32, col: u32) -> Command {
        let exe = if cfg!(windows) { format!("{}.cmd", self.executable) } else { self.executable.to_string() };
        let mut cmd = Command::new(exe);
        let goto_arg = format!("{}:{}:{}", file_path.to_string_lossy(), line, col);
        cmd.args(&["-n", "-w", "-g", &goto_arg]);
        cmd
    }

    fn id(&self) -> &'static str {
        self.executable
    }
}

struct ZedBackend;

impl EditorBackend for ZedBackend {
    fn is_available(&self) -> bool {
        Command::new("zed")
            .arg("--version")
            .stdout(std::process::Stdio::null())
            .stderr(std::process::Stdio::null())
            .status()
            .is_ok()
    }

    fn build_command(&self, file_path: &Path, line: u32, col: u32) -> Command {
        let mut cmd = Command::new("zed");
        let arg = format!("{}:{}:{}", file_path.to_string_lossy(), line, col);
        cmd.arg("--wait").arg(arg);
        cmd
    }

    fn id(&self) -> &'static str {
        "zed"
    }
}

// --- Core Implementation ---

pub struct ExternalEditor;

impl ExternalEditor {
    pub fn open<R: Runtime>(
        app: AppHandle<R>,
        content: String,
        line: u32,
        col: u32,
        editor_id: Option<String>,
        state: tauri::State<'_, ExternalEditorState>,
    ) -> Result<(), String> {
        // 1. Cancel existing watcher
        if let Ok(mut handle) = state.abort_handle.lock() {
            if let Some(old_abort) = handle.take() {
                let _ = old_abort.send(());
            }
        }

        // 2. Kill existing child process if any
        if let Ok(mut child_guard) = state.active_child.lock() {
            if let Some(mut child) = child_guard.take() {
                let _ = child.kill();
            }
        }

        let mut temp_file_guard = state.temp_file.lock().map_err(|e| e.to_string())?;
        
        // 3. Setup temp file
        if temp_file_guard.is_none() {
            let file = Builder::new()
                .prefix("contents_")
                .suffix(".txt")
                .tempfile()
                .map_err(|e| e.to_string())?;
            *temp_file_guard = Some(file);
        }
        
        let temp_file = temp_file_guard.as_mut().ok_or("Failed to access temp file")?;
        let file_handle = temp_file.as_file_mut();
        
        file_handle.set_len(0).map_err(|e| e.to_string())?;
        file_handle.seek(SeekFrom::Start(0)).map_err(|e| e.to_string())?;
        file_handle.write_all(content.as_bytes()).map_err(|e| e.to_string())?;
        file_handle.flush().map_err(|e| e.to_string())?;

        let file_path = temp_file.path().to_path_buf();
        let app_clone = app.clone();
        let (stop_tx, mut stop_rx) = oneshot::channel::<()>();
        
        if let Ok(mut handle) = state.abort_handle.lock() {
            *handle = Some(stop_tx);
        }

        // 4. Watcher
        let file_path_for_watcher = file_path.clone();
        tokio::spawn(async move {
            let watcher_result = Self::async_watcher();
            if let Ok((mut watcher, mut rx)) = watcher_result {
                if let Some(parent) = file_path_for_watcher.parent() {
                    let _ = watcher.watch(parent, RecursiveMode::NonRecursive);
                    let mut last_content_hash = Self::calculate_hash(&content);
                    loop {
                        tokio::select! {
                            _ = &mut stop_rx => break,
                            res = rx.recv() => {
                                match res {
                                    Some(Ok(event)) => {
                                        if event.paths.contains(&file_path_for_watcher) && event.kind.is_modify() {
                                            tokio::time::sleep(std::time::Duration::from_millis(50)).await;
                                            if let Ok(new_content) = std::fs::read_to_string(&file_path_for_watcher) {
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
                }
            }
        });

        // 5. Editor Discovery
        let backends: Vec<Box<dyn EditorBackend>> = vec![
            Box::new(VscodeBackend { executable: "code" }),
            Box::new(VscodeBackend { executable: "code-insiders" }),
            Box::new(ZedBackend),
        ];

        let selected_backend = if let Some(ref id) = editor_id {
            if id != "auto" {
                backends.into_iter().find(|b| b.id() == id)
            } else {
                backends.into_iter().find(|b| b.is_available())
            }
        } else {
            backends.into_iter().find(|b| b.is_available())
        };
        
        let file_path_for_final = file_path.clone();
        let app_clone_final = app.clone();
        
        drop(temp_file_guard);

        if let Some(backend) = selected_backend {
            if !backend.is_available() {
                return Err(format!("Selected editor '{}' is not available in PATH.", backend.id()));
            }

            let mut command = backend.build_command(&file_path, line, col);
            let child = command.spawn().map_err(|e| format!("Failed to spawn editor: {}", e))?;
            let _ = app.emit("external-editor-start", ());

            // Store the handle
            if let Ok(mut child_guard) = state.active_child.lock() {
                *child_guard = Some(child);
            }

            std::thread::spawn(move || {
                loop {
                    std::thread::sleep(std::time::Duration::from_millis(500));
                    if let Some(s) = app_clone_final.try_state::<ExternalEditorState>() {
                        let mut guard = s.active_child.lock().unwrap();
                        if let Some(ref mut child) = *guard {
                            // Specify ExitStatus as expected type
                            match child.try_wait() {
                                Ok(Some(_status)) => { 
                                    *guard = None;
                                    break;
                                }
                                Ok(None) => continue, // Still running
                                Err(_) => { *guard = None; break; }
                            }
                        } else {
                            break; 
                        }
                    } else {
                        break;
                    }
                }

                if let Ok(final_content) = std::fs::read_to_string(&file_path_for_final) {
                    let _ = app_clone_final.emit("external-editor-sync", final_content);
                }
                let _ = app_clone_final.emit("external-editor-end", ());
            });
            Ok(())
        } else {
            Err("No supported editor found.".to_string())
        }
    }

    fn async_watcher() -> notify::Result<(notify::RecommendedWatcher, tokio::sync::mpsc::Receiver<notify::Result<Event>>)> {
        let (tx, rx) = tokio::sync::mpsc::channel(1);
        let watcher = notify::RecommendedWatcher::new(move |res| { let _ = tx.blocking_send(res); }, Config::default())?;
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
    editor_id: Option<String>,
    state: tauri::State<'_, ExternalEditorState>,
) -> Result<(), String> {
    ExternalEditor::open(app, content, line, col, editor_id, state)
}