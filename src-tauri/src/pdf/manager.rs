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

// Re-export for use in closures
pub use crate::pdf::toc::{TocConfig, process_toc_generation};
pub use crate::pdf_outline::model::{Bookmark, ViewScaleType};

#[derive(Debug, Clone, Copy, PartialEq, Serialize, Deserialize)]
pub enum LoadMode {
    DirectFile,
    MemoryBuffer,
}

pub struct PdfSession {
    pub mode: LoadMode,
    pub path: String,
    pub pdfium_doc: Option<PdfDocument<'static>>,
    pub memory_ptr: Option<*mut [u8]>,
}

impl PdfSession {
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
        self.pdfium_doc = None;
        if let Some(ptr) = self.memory_ptr {
            unsafe { let _ = Box::from_raw(ptr); }
            info!("[PdfSession] Freed memory buffer for: {}", self.path);
        }
    }
}

// Job Type Definition
type Job = Box<dyn FnOnce(&mut PdfWorkerInternalState) + Send>;

// The Sender type to be stored in Tauri State
pub struct PdfWorker(pub mpsc::Sender<Job>);

impl PdfWorker {
    /// Executes a closure on the dedicated PDF worker thread.
    /// Returns the result of the closure.
    pub async fn call<F, R>(&self, f: F) -> Result<R>
    where
        F: FnOnce(&mut PdfWorkerInternalState) -> R + Send + 'static,
        R: Send + 'static,
    {
        let (tx, rx) = oneshot::channel();
        
        let job = Box::new(move |state: &mut PdfWorkerInternalState| {
            let result = f(state);
            // Ignore error if receiver dropped
            let _ = tx.send(result);
        });

        self.0.send(job).await.map_err(|_| format_err!("PDF Worker is closed"))?;

        rx.await.map_err(|_| format_err!("PDF Worker dropped the response channel"))
    }
}

// Internal state of the PDF worker thread
pub struct PdfWorkerInternalState {
    pub pdfium: &'static Pdfium, // Made pub for closures
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

    pub fn get_or_load(&mut self, path: &str) -> Result<&PdfSession> {
        if !self.sessions.contains_key(path) {
            info!("[PDF Worker] Auto-loading (DirectFile) for: {}", path);
            self.load_document(path.to_string(), LoadMode::DirectFile)?;
        }
        self.sessions.get(path).ok_or_else(|| format_err!("Session not found after load"))
    }

    pub fn load_document(&mut self, path: String, mode: LoadMode) -> Result<()> {
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
                let boxed_slice = data.into_boxed_slice();
                let leaked_ref = Box::leak(boxed_slice);
                let ptr = leaked_ref as *mut [u8];
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

    pub fn close_document(&mut self, path: &str) {
        if self.sessions.remove(path).is_some() {
            info!("[PDF Worker] Closed session: {}", path);
        }
    }

    // Helper methods (exposed for closures)
    pub fn process_render_request(&mut self, path: String, page_index: u16, scale: f32) -> Result<Vec<u8>> {
        let session = self.get_or_load(&path)?;
        let doc = session.pdfium_doc.as_ref().ok_or_else(|| format_err!("Session missing document"))?;
        let page = doc.pages().get(page_index)?;
        let width = (page.width().value * scale) as i32;
        let height = (page.height().value * scale) as i32;
        let bitmap = page.render(width, height, None)?;
        let dynamic_image = bitmap.as_image(); 
        let mut png_data = Vec::new();
        let mut cursor = Cursor::new(&mut png_data);
        dynamic_image.write_to(&mut cursor, ImageFormat::Png)?;
        Ok(png_data)
    }

    pub fn process_get_page_count(&mut self, path: String) -> Result<u16> {
        let session = self.get_or_load(&path)?;
        let doc = session.pdfium_doc.as_ref().ok_or_else(|| format_err!("Session missing document"))?;
        Ok(doc.pages().len())
    }
}

pub fn init_pdf_worker() -> PdfWorker {
    let (tx, mut rx) = mpsc::channel::<Job>(32);

    thread::spawn(move || {
        let mut worker_state = match PdfWorkerInternalState::new() {
            Ok(state) => state,
            Err(e) => {
                error!("{}", e);
                return;
            }
        };

        while let Some(job) = rx.blocking_recv() {
            // Execute the job
            job(&mut worker_state);
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
    let load_mode = mode.unwrap_or(LoadMode::DirectFile);
    state.call(move |worker| {
        worker.load_document(path, load_mode)
    }).await.map_err(|e| e.to_string())?
            .map_err(|e| e.to_string())
}

#[tauri::command]
pub async fn close_pdf_document(
    state: State<'_, PdfWorker>,
    path: String
) -> Result<(), String> {
    state.call(move |worker| {
        worker.close_document(&path);
    }).await.map_err(|e| e.to_string())
}
