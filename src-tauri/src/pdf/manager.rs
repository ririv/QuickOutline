use pdfium_render::prelude::*;
use anyhow::{Result, format_err};
use tokio::sync::{mpsc, oneshot};
use std::thread;
use image::ImageFormat;
use std::io::Cursor;
use log::{info, error};
use std::collections::HashMap;
use serde::{Deserialize, Serialize};
use tauri::State;

use crate::pdf::toc::{TocConfig, process_toc_generation};

#[derive(Debug, Clone, Copy, PartialEq, Serialize, Deserialize)]
pub enum LoadMode {
    DirectFile,
    MemoryBuffer,
}

pub struct PdfSession {
    pub mode: LoadMode,
    pub path: String,
    // Wrapped in Option to control drop order (drop doc before memory)
    pub pdfium_doc: Option<PdfDocument<'static>>,
    // For MemoryBuffer mode, we own the leaked memory
    pub memory_ptr: Option<*mut [u8]>,
}

impl PdfSession {
    /// Helper to get a lopdf Document based on the current mode.
    /// This bridges the gap between Pdfium's state and lopdf's needs.
    pub fn load_lopdf_doc(&self) -> Result<lopdf::Document> {
        match self.mode {
            LoadMode::DirectFile => {
                lopdf::Document::load(&self.path)
                    .map_err(|e| format_err!("Lopdf load failed: {}", e))
            },
            LoadMode::MemoryBuffer => {
                if let Some(ptr) = self.memory_ptr {
                    let slice = unsafe { &*ptr };
                    lopdf::Document::load_mem(slice)
                        .map_err(|e| format_err!("Lopdf load mem failed: {}", e))
                } else {
                    Err(format_err!("Memory mode but no memory pointer found"))
                }
            }
        }
    }
}

impl Drop for PdfSession {
    fn drop(&mut self) {
        // 1. Drop the Pdfium Document first to release references to the memory
        self.pdfium_doc = None;
        
        // 2. If we own memory (MemoryBuffer mode), reclaim and drop it
        if let Some(ptr) = self.memory_ptr {
            unsafe {
                // Reconstruct Box to drop it
                let _ = Box::from_raw(ptr);
            }
            info!("[PdfSession] Freed memory buffer for: {}", self.path);
        }
    }
}

// Request types for the PDF Worker
pub enum PdfRequest {
    LoadDocument {
        path: String,
        mode: LoadMode,
        response_tx: oneshot::Sender<Result<()>>,
    },
    CloseDocument {
        path: String,
        response_tx: oneshot::Sender<()>,
    },
    RenderPage {
        path: String,
        page_index: u16,
        scale: f32,
        response_tx: oneshot::Sender<Result<Vec<u8>>>,
    },
    GetPageCount {
        path: String,
        response_tx: oneshot::Sender<Result<u16>>,
    },
    GenerateToc {
        src_path: String,
        config: TocConfig,
        dest_path: Option<String>,
        response_tx: oneshot::Sender<Result<String, String>>,
    },
    ExtractToc {
        path: String,
        response_tx: oneshot::Sender<Result<Vec<String>>>,
    },
}

// The Sender type to be stored in Tauri State
pub struct PdfWorker(pub mpsc::Sender<PdfRequest>);

// Internal state of the PDF worker thread
struct PdfWorkerInternalState {
    pdfium: &'static Pdfium,
    // Store active sessions mapped by file path
    sessions: HashMap<String, PdfSession>,
}

impl PdfWorkerInternalState {
    fn new() -> Result<Self> {
        info!("[PDF Worker] Init. CWD: {:?}", std::env::current_dir());
        
        let pdfium = crate::pdf::get_pdfium()
            .map_err(|e| format_err!("[PDF Worker] {}", e))?;
        
        Ok(Self {
            pdfium,
            sessions: HashMap::new(),
        })
    }

    /// Retrieves an active session or auto-loads in DirectFile mode if missing.
    fn get_or_load(&mut self, path: &str) -> Result<&PdfSession> {
        if !self.sessions.contains_key(path) {
            info!("[PDF Worker] Auto-loading (DirectFile) for: {}", path);
            self.load_document(path.to_string(), LoadMode::DirectFile)?;
        }
        self.sessions.get(path).ok_or_else(|| format_err!("Session not found after load"))
    }

    fn load_document(&mut self, path: String, mode: LoadMode) -> Result<()> {
        // If already loaded, close it first (simplified logic: overwrite)
        if self.sessions.contains_key(&path) {
            info!("[PDF Worker] Reloading existing session: {}", path);
            self.sessions.remove(&path);
        }

        let session = match mode {
            LoadMode::DirectFile => {
                let doc = self.pdfium.load_pdf_from_file(&path, None)
                    .map_err(|e| format_err!("Pdfium load file failed: {}", e))?;
                PdfSession {
                    mode,
                    path: path.clone(),
                    pdfium_doc: Some(doc),
                    memory_ptr: None,
                }
            },
            LoadMode::MemoryBuffer => {
                let data = std::fs::read(&path)
                    .map_err(|e| format_err!("Read file failed: {}", e))?;
                
                // Leak memory to get a static reference for Pdfium
                let boxed_slice = data.into_boxed_slice();
                let leaked_ref = Box::leak(boxed_slice);
                let ptr = leaked_ref as *mut [u8];

                // Load from byte slice
                let doc = self.pdfium.load_pdf_from_byte_slice(leaked_ref, None)
                    .map_err(|e| format_err!("Pdfium load memory failed: {}", e))?;

                PdfSession {
                    mode,
                    path: path.clone(),
                    pdfium_doc: Some(doc),
                    memory_ptr: Some(ptr),
                }
            }
        };

        self.sessions.insert(path, session);
        Ok(())
    }

    fn close_document(&mut self, path: &str) {
        if self.sessions.remove(path).is_some() {
            info!("[PDF Worker] Closed session: {}", path);
        }
    }

    // This method handles the rendering using the session
    fn process_render_request(&mut self, path: String, page_index: u16, scale: f32) -> Result<Vec<u8>> {
        let session = self.get_or_load(&path)?;
        let doc = session.pdfium_doc.as_ref().expect("Session missing document");

        let page = doc.pages().get(page_index)?;
        
        // Calculate dimensions
        let width = (page.width().value * scale) as i32;
        let height = (page.height().value * scale) as i32;

        let bitmap = page.render(width, height, None)?;
        
        // Zero-copy conversion using `image` feature logic
        let dynamic_image = bitmap.as_image(); 

        let mut png_data = Vec::new();
        let mut cursor = Cursor::new(&mut png_data);
        
        dynamic_image.write_to(&mut cursor, ImageFormat::Png)?;

        Ok(png_data)
    }

    fn process_get_page_count(&mut self, path: String) -> Result<u16> {
        let session = self.get_or_load(&path)?;
        let doc = session.pdfium_doc.as_ref().expect("Session missing document");
        Ok(doc.pages().len())
    }
}


pub fn init_pdf_worker() -> PdfWorker {
    let (tx, mut rx) = mpsc::channel::<PdfRequest>(32);

    thread::spawn(move || {
        let mut worker_state = match PdfWorkerInternalState::new() {
            Ok(state) => state,
            Err(e) => {
                error!("{}", e);
                return;
            }
        };

        while let Some(request) = rx.blocking_recv() {
            match request {
                PdfRequest::LoadDocument { path, mode, response_tx } => {
                    let result = worker_state.load_document(path, mode);
                    let _ = response_tx.send(result);
                },
                PdfRequest::CloseDocument { path, response_tx } => {
                    worker_state.close_document(&path);
                    let _ = response_tx.send(());
                },
                PdfRequest::RenderPage { path, page_index, scale, response_tx } => {
                    let result = worker_state.process_render_request(path, page_index, scale);
                    let _ = response_tx.send(result);
                },
                PdfRequest::GetPageCount { path, response_tx } => {
                    let result = worker_state.process_get_page_count(path);
                    let _ = response_tx.send(result);
                },
                PdfRequest::GenerateToc { src_path, config, dest_path, response_tx } => {
                    let result = match worker_state.get_or_load(&src_path) {
                        Ok(session) => process_toc_generation(session, config, dest_path),
                        Err(e) => Err(e.to_string()),
                    };
                    let _ = response_tx.send(result);
                },
                PdfRequest::ExtractToc { path, response_tx } => {
                    let result = match worker_state.get_or_load(&path) {
                         Ok(session) => {
                             let doc = session.pdfium_doc.as_ref().expect("Session missing document");
                             crate::pdf_analysis::TocExtractor::extract_toc(doc)
                                .map_err(|e| format_err!("{}", e))
                         },
                         Err(e) => Err(e),
                    };
                    let _ = response_tx.send(result);
                }
            }
        }
    });

    PdfWorker(tx)
}

#[tauri::command]
pub async fn load_pdf_document(
    state: State<'_, PdfWorker>,
    path: String,
    mode: Option<LoadMode>
) -> Result<(), String> {
    let (tx, rx) = oneshot::channel();
    let load_mode = mode.unwrap_or(LoadMode::DirectFile);

    state.0.send(PdfRequest::LoadDocument {
        path,
        mode: load_mode,
        response_tx: tx,
    }).await.map_err(|e| format!("Failed to send request: {}", e))?;

    rx.await
        .map_err(|_| "Worker dropped channel".to_string())?
        .map_err(|e| e.to_string())
}

#[tauri::command]
pub async fn close_pdf_document(
    state: State<'_, PdfWorker>,
    path: String
) -> Result<(), String> {
    let (tx, rx) = oneshot::channel();

    state.0.send(PdfRequest::CloseDocument {
        path,
        response_tx: tx,
    }).await.map_err(|e| format!("Failed to send request: {}", e))?;

    rx.await.map_err(|_| "Worker dropped channel".to_string())
}