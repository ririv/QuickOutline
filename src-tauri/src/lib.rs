mod java_sidecar;
mod printer_native;
mod printer_headless;
mod printer_headless_chrome;
mod printer;
mod static_server;
mod pdf_handler;

use std::sync::Mutex;
use tauri::{AppHandle, Manager, Runtime};
use std::fs;
use std::path::{Path, PathBuf};
use tauri_plugin_cli::CliExt;

// Helper function to recursively copy a directory
fn copy_dir_all(src: impl AsRef<Path>, dst: impl AsRef<Path>) -> std::io::Result<()> {
    fs::create_dir_all(&dst)?;
    for entry in fs::read_dir(src)? {
        let entry = entry?;
        let ty = entry.file_type()?;
        let src_path = entry.path();
        let dst_path = dst.as_ref().join(entry.file_name());
        if ty.is_dir() {
            copy_dir_all(src_path, dst_path)?;
        } else {
            fs::copy(src_path, dst_path)?;
        }
    }
    Ok(())
}

// Helper function to set up print workspace, including copying fonts
fn setup_print_workspace<R: Runtime>(app_handle: &AppHandle<R>) -> Result<PathBuf, String> {
    let app_data_dir = app_handle.path().app_data_dir()
        .map_err(|e| format!("Failed to get app data dir: {}", e))?;
    let workspace = app_data_dir.join("print_workspace");
    let fonts_dst = workspace.join("fonts");

    // Only copy if fonts haven't been copied yet
    if !fonts_dst.exists() {
        if let Ok(resource_dir) = app_handle.path().resource_dir() {
            let fonts_src = resource_dir.join("resources").join("fonts"); // Assumes 'fonts/' in resources
            if fonts_src.exists() {
                copy_dir_all(&fonts_src, &fonts_dst)
                    .map_err(|e| format!("Rust Setup: Failed to copy fonts: {}", e))?;
                println!("Rust Setup: Copied fonts to {:?}", fonts_dst);
            } else {
                println!("Rust Setup: Font resources not found at {:?}", fonts_src);
            }
        } else {
            println!("Rust Setup: Failed to get resource dir.");
        }
    } else {
        println!("Rust Setup: Fonts already exist in print workspace.");
    }

    // Clean up old temporary print files
    if let Ok(entries) = fs::read_dir(&workspace) {
        for entry in entries.flatten() {
            let path = entry.path();
            if path.is_file() {
                if let Some(filename) = path.file_name().and_then(|s| s.to_str()) {
                    if (filename.starts_with("print_job_") || filename.starts_with("print_fallback_")) 
                       && filename.ends_with(".html") { // Corrected to Rust's ends_with
                        if let Err(e) = fs::remove_file(&path) {
                            eprintln!("Rust Setup: Failed to delete old print job {:?}: {}", path, e);
                        } else {
                            println!("Rust Setup: Cleaned up old print job: {:?}", filename);
                        }
                    }
                }
            }
        }
    }

    Ok(workspace)
}

#[tauri::command]
fn greet(name: &str) -> String {
    format!("Hello, {}! You've been greeted from Rust!", name)
}

#[tauri::command]
async fn get_print_workspace_path<R: Runtime>(app: AppHandle<R>) -> Result<String, String> {
    let app_data_dir = app.path().app_data_dir()
        .map_err(|e| format!("Failed to get app data dir: {}", e))?;
    let workspace_path = app_data_dir.join("print_workspace");
    Ok(workspace_path.to_string_lossy().to_string())
}

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    tauri::Builder::default()
        .plugin(tauri_plugin_dialog::init())
        .plugin(tauri_plugin_shell::init())
        .plugin(tauri_plugin_opener::init())
        .plugin(tauri_plugin_cli::init())
        .manage(java_sidecar::JavaState {
            port: Mutex::new(None),
        })
        .manage(static_server::LocalServerState::new())
        .setup(move |app| {
            // Setup print workspace on app startup
            let workspace_path = setup_print_workspace(app.handle()).expect("Failed to setup print workspace.");

            // Start local static server
            static_server::start_server(app.handle().clone(), workspace_path);

            let mut custom_port: Option<u16> = None;
            let mut use_external_sidecar = false;

            // Use the CLI plugin to parse arguments
            match app.cli().matches() {
                Ok(matches) => {
                    if let Some(arg_data) = matches.args.get("port") {
                        // Check if value is number or string and parse
                        if let Some(port_val) = arg_data.value.as_u64() {
                            custom_port = Some(port_val as u16);
                            println!("Rust (CLI Plugin): Custom port {} parsed.", port_val);
                        } else if let Some(port_str) = arg_data.value.as_str() {
                            if let Ok(p) = port_str.parse::<u16>() {
                                custom_port = Some(p);
                                println!("Rust (CLI Plugin): Custom port {} parsed from string.", p);
                            }
                        }
                    }
                    if let Some(arg_data) = matches.args.get("external-sidecar") {
                         use_external_sidecar = arg_data.value.as_bool().unwrap_or(false);
                         if use_external_sidecar {
                             println!("Rust (CLI Plugin): External sidecar mode enabled.");
                         }
                    }
                }
                Err(e) => {
                    eprintln!("Rust: Failed to match CLI args: {}", e);
                }
            }

            if use_external_sidecar {
                if let Some(port) = custom_port {
                    java_sidecar::connect_external(app.handle(), port);
                } else {
                    eprintln!("Rust Error: --external-sidecar requires --port to be specified.");
                }
            } else {
                // 2. 调用解耦后的启动逻辑，并传入 custom_port
                java_sidecar::start(app.handle(), custom_port);
            }

            // (可选) 调试时自动打开控制台
            #[cfg(debug_assertions)]
            {
                if let Some(window) = app.get_webview_window("main") {
                    window.open_devtools();
                }
            }

            Ok(())
        })
        .invoke_handler(tauri::generate_handler![greet, java_sidecar::get_java_port, printer::print_to_pdf, pdf_handler::merge_pdfs])
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}
