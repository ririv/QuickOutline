mod java_sidecar;
mod printer_native;
mod printer_headless;
mod printer_headless_chrome;
mod printer;
mod static_server;
mod pdf;
mod pdf_outline;

use std::sync::Mutex;
use tauri::{AppHandle, Manager, Runtime};
use std::fs;
use std::path::{Path, PathBuf};
use tauri_plugin_cli::CliExt;

// Helper function to set up print workspace (now only creates directory and cleans old files)
fn setup_print_workspace<R: Runtime>(app_handle: &AppHandle<R>) -> Result<PathBuf, String> {
    let app_data_dir = app_handle.path().app_data_dir()
        .map_err(|e| format!("Failed to get app data dir: {}", e))?;
    let workspace = app_data_dir.join("print_workspace");
    
    // Create workspace directory if not exists
    fs::create_dir_all(&workspace).map_err(|e| format!("Failed to create workspace: {}", e))?;

    // Clean up old temporary print files
    if let Ok(entries) = fs::read_dir(&workspace) {
        for entry in entries.flatten() {
            let path = entry.path();
            if path.is_file() {
                if let Some(filename) = path.file_name().and_then(|s| s.to_str()) {
                    if (filename.starts_with("print_job_") || filename.starts_with("print_fallback_")) 
                       && filename.ends_with(".html") {
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

// Helper function to clean up pdf workspace (remove toc_*.pdf files)
fn cleanup_pdf_workspace<R: Runtime>(app_handle: &AppHandle<R>) -> Result<(), String> {
    let app_data_dir = app_handle.path().app_data_dir()
        .map_err(|e| format!("Failed to get app data dir: {}", e))?;
    let pdf_workspace = app_data_dir.join("pdf_workspace");

    if pdf_workspace.exists() {
        if let Ok(entries) = fs::read_dir(&pdf_workspace) {
            for entry in entries.flatten() {
                let path = entry.path();
                if path.is_file() {
                    if let Some(filename) = path.file_name().and_then(|s| s.to_str()) {
                        if filename.starts_with("toc_") && filename.ends_with(".pdf") {
                            if let Err(e) = fs::remove_file(&path) {
                                eprintln!("Rust Setup: Failed to delete PDF temp file {:?}: {}", path, e);
                            } else {
                                println!("Rust Setup: Cleaned up PDF temp file: {:?}", filename);
                            }
                        }
                    }
                }
            }
        }
    }
    Ok(())
}

use crate::pdf_outline::model::{Bookmark, ViewScaleType};

fn resolve_dest_path(src_path: &str, dest_path: Option<String>) -> String {
    if let Some(path) = dest_path {
        if !path.trim().is_empty() {
            return path;
        }
    }
    
    let src = Path::new(src_path);
    let parent = src.parent().unwrap_or(Path::new(""));
    let file_stem = src.file_stem().and_then(|s| s.to_str()).unwrap_or("output");
    let ext = src.extension().and_then(|s| s.to_str()).unwrap_or("pdf");

    let mut candidate_name = format!("{}_new.{}", file_stem, ext);
    let mut candidate_path = parent.join(&candidate_name);
    
    if !candidate_path.exists() {
        return candidate_path.to_string_lossy().to_string();
    }

    let mut counter = 1;
    while candidate_path.exists() {
        candidate_name = format!("{}_new_{}.{}", file_stem, counter, ext);
        candidate_path = parent.join(&candidate_name);
        counter += 1;
    }

    candidate_path.to_string_lossy().to_string()
}

#[tauri::command]
async fn get_outline_as_bookmark(path: String, offset: i32) -> Result<Bookmark, String> {
    pdf_outline::processor::get_outline(&path, offset).map_err(|e| e.to_string())
}

#[tauri::command]
async fn save_outline(
    src_path: String, 
    bookmark_root: Bookmark, 
    dest_path: Option<String>, 
    offset: i32, 
    view_mode: Option<ViewScaleType>
) -> Result<String, String> {
    let actual_dest = resolve_dest_path(&src_path, dest_path);
    let scale = view_mode.unwrap_or(ViewScaleType::None);
    
    pdf_outline::processor::set_outline(&src_path, &actual_dest, bookmark_root, offset, scale)
        .map_err(|e| e.to_string())?;
        
    Ok(actual_dest)
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
            // Initialize PDF Worker and manage state
            let pdf_worker = pdf::manager::init_pdf_worker();
            app.manage(pdf_worker);

            // Setup print workspace on app startup
            let workspace_path = setup_print_workspace(app.handle()).expect("Failed to setup print workspace.");

            // Clean up PDF workspace
            if let Err(e) = cleanup_pdf_workspace(app.handle()) {
                eprintln!("Rust Setup: Failed to cleanup PDF workspace: {}", e);
            }

            // Start local static server
            // Pass the resources path ("resources" subfolder inside the resource dir)
            let resources_path = app.handle().path().resource_dir().ok().map(|p| p.join("resources"));
            static_server::start_server(app.handle().clone(), workspace_path, resources_path);

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
        .invoke_handler(tauri::generate_handler![
            greet, 
            java_sidecar::get_java_port, 
            printer::print_to_pdf, 
            pdf::merge::merge_pdfs, 
            pdf::render::render_pdf_page, 
            pdf::render::get_pdf_page_count,
            get_outline_as_bookmark,
            save_outline
        ])
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}
