// Prevents additional console window on Windows in release, DO NOT REMOVE!!
#![cfg_attr(not(debug_assertions), windows_subsystem = "windows")]
#![deny(clippy::unwrap_used, clippy::expect_used)]

mod java_sidecar;
mod printer;
mod static_server;
mod pdf;
mod pdf_outline;
mod external_editor;
mod pdf_analysis;

use std::sync::Mutex;
use tauri::{AppHandle, Manager, Runtime};
use std::fs;
use std::path::{Path, PathBuf};
use tauri_plugin_cli::CliExt;
use log::{info, warn, error};

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
                            warn!("Rust Setup: Failed to delete old print job {:?}: {}", path, e);
                        } else {
                            info!("Rust Setup: Cleaned up old print job: {:?}", filename);
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
                                warn!("Rust Setup: Failed to delete PDF temp file {:?}: {}", path, e);
                            } else {
                                info!("Rust Setup: Cleaned up PDF temp file: {:?}", filename);
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
use crate::pdf::page_label::{PageLabel, PageLabelProcessor};

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
async fn get_outline_as_bookmark(
    state: tauri::State<'_, pdf::manager::PdfWorker>, 
    path: String, 
    offset: i32
) -> Result<Bookmark, String> {
    state.call(move |worker| -> Result<Bookmark, String> {
        let session = worker.get_or_load(&path).map_err(|e| e.to_string())?;
        let doc = session.load_lopdf_doc().map_err(|e| e.to_string())?;
        crate::pdf_outline::processor::get_outline(&doc, offset).map_err(|e| e.to_string())
    }).await.map_err(|e| e.to_string())?
}

#[tauri::command]
async fn save_outline(
    state: tauri::State<'_, pdf::manager::PdfWorker>,
    src_path: String, 
    bookmark_root: Bookmark, 
    dest_path: Option<String>, 
    offset: i32, 
    view_mode: Option<ViewScaleType>
) -> Result<String, String> {
    let actual_dest = resolve_dest_path(&src_path, dest_path);
    let scale = view_mode.unwrap_or(ViewScaleType::None);
    
    let dest_path_clone = actual_dest.clone();

    state.call(move |worker| -> Result<(), String> {
        let session = worker.get_or_load(&src_path).map_err(|e| e.to_string())?;
        let mut doc = session.load_lopdf_doc().map_err(|e| e.to_string())?;
        crate::pdf_outline::processor::set_outline_in_doc(&mut doc, bookmark_root, offset, scale)
            .map_err(|e| format!("Failed to set outline: {}", e))?;
        doc.save(&dest_path_clone).map(|_| ()).map_err(|e| format!("Failed to save PDF: {}", e))
    }).await.map_err(|e| e.to_string())??;
    
    Ok(actual_dest)
}

#[tauri::command]
async fn set_page_labels(
    state: tauri::State<'_, pdf::manager::PdfWorker>,
    src_path: String,
    rules: Vec<PageLabel>,
    dest_path: Option<String>
) -> Result<String, String> {
    let actual_dest = resolve_dest_path(&src_path, dest_path);
    let dest_clone = actual_dest.clone();

    state.call(move |worker| -> Result<(), String> {
        let session = worker.get_or_load(&src_path).map_err(|e| e.to_string())?;
        let mut doc = session.load_lopdf_doc().map_err(|e| e.to_string())?;
        PageLabelProcessor::set_page_labels_in_doc(&mut doc, rules)
            .map_err(|e| e.to_string())?;
        doc.save(&dest_clone).map(|_| ()).map_err(|e| e.to_string())
    }).await.map_err(|e| e.to_string())??;

    Ok(actual_dest)
}

#[tauri::command]
async fn get_page_labels(state: tauri::State<'_, pdf::manager::PdfWorker>, path: String) -> Result<Vec<String>, String> {
    state.call(move |worker| -> Result<Vec<String>, String> {
        let session = worker.get_or_load(&path).map_err(|e| e.to_string())?;
        let doc = session.load_lopdf_doc().map_err(|e| e.to_string())?;
        PageLabelProcessor::get_page_labels_from_doc(&doc).map_err(|e| e.to_string())
    }).await.map_err(|e| e.to_string())?
}

#[tauri::command]
async fn get_page_label_rules(state: tauri::State<'_, pdf::manager::PdfWorker>, path: String) -> Result<Vec<PageLabel>, String> {
    state.call(move |worker| -> Result<Vec<PageLabel>, String> {
        let session = worker.get_or_load(&path).map_err(|e| e.to_string())?;
        let doc = session.load_lopdf_doc().map_err(|e| e.to_string())?;
        PageLabelProcessor::get_page_label_rules_from_doc(&doc).map_err(|e| e.to_string())
    }).await.map_err(|e| e.to_string())?
}

#[tauri::command]
async fn simulate_page_labels(rules: Vec<PageLabel>, total_pages: u32) -> Result<Vec<String>, String> {
    Ok(PageLabelProcessor::simulate_page_labels(rules, total_pages))
}

#[tauri::command]
async fn get_static_server_port<R: Runtime>(app: AppHandle<R>) -> Result<u16, String> {
    if let Some(state) = app.try_state::<static_server::LocalServerState>() {
        state.port.lock()
            .map_err(|e| format!("Lock poisoned: {}", e))?
            .ok_or("Server not started yet".to_string())
    } else {
        Err("Failed to access LocalServerState".to_string())
    }
}

#[tauri::command]
async fn extract_toc(state: tauri::State<'_, pdf::manager::PdfWorker>, path: String) -> Result<Vec<String>, String> {
    state.call(move |worker| -> Result<Vec<String>, String> {
        let session = worker.get_or_load(&path).map_err(|e| e.to_string())?;
        let doc = session.pdfium_doc.as_ref().ok_or_else(|| "Session missing document".to_string())?;
        pdf_analysis::TocExtractor::extract_toc(doc).map_err(|e| e.to_string())
    }).await.map_err(|e| e.to_string())?
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
        .plugin(tauri_plugin_log::Builder::default()
            .level(log::LevelFilter::Warn) // Global default: only Warn/Error
            .level_for("quickoutline_lib", log::LevelFilter::Debug) // Our lib: Debug
            .level_for("quickoutline", log::LevelFilter::Debug) // Our bin: Debug
            .build())
        .plugin(tauri_plugin_dialog::init())
        .plugin(tauri_plugin_shell::init())
        .plugin(tauri_plugin_fs::init())
        .plugin(tauri_plugin_opener::init())
        .plugin(tauri_plugin_cli::init())
        .manage(external_editor::ExternalEditorState::new())
        .manage(static_server::LocalServerState::new())
        .setup(move |app| {
            // Initialize PDF Worker and manage state
            let pdf_worker = pdf::manager::init_pdf_worker();
            app.manage(pdf_worker);

            // Setup print workspace on app startup
            let workspace_path = setup_print_workspace(app.handle())?;

            // Clean up PDF workspace
            if let Err(e) = cleanup_pdf_workspace(app.handle()) {
                warn!("Rust Setup: Failed to cleanup PDF workspace: {}", e);
            }

            // Start local static server
            // Pass the resources path ("resources" subfolder inside the resource dir)
            let resources_path = app.handle().path().resource_dir().ok().map(|p| p.join("resources"));
            static_server::start_server(app.handle().clone(), workspace_path, resources_path);

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
            printer::print_to_pdf, 
            pdf::commands::load_pdf_document,
            pdf::commands::close_pdf_document,
            pdf::render::render_pdf_page, 
            pdf::render::get_pdf_page_count,
            get_outline_as_bookmark,
            save_outline,
            set_page_labels,
            get_page_labels,
            get_page_label_rules,
            simulate_page_labels,
            get_static_server_port,
            pdf::toc::generate_toc_page,
            extract_toc,
            external_editor::open_external_editor
        ])
        .build(tauri::generate_context!())
        .map(|app| {
            app.run(|app_handle, event| {
                if let tauri::RunEvent::Exit = event {
                    // Cleanup: Kill active external editor process
                    if let Some(state) = app_handle.try_state::<external_editor::ExternalEditorState>() {
                        if let Ok(mut child_guard) = state.active_child.lock() {
                            if let Some(mut child) = child_guard.take() {
                                let _ = child.kill();
                                info!("Rust Exit Hook: Killed active external editor.");
                            }
                        }
                    }
                }
            });
        })
        .unwrap_or_else(|e| {
            error!("error while building tauri application: {}", e);
        });
}
