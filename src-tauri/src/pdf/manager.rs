use pdfium_render::prelude::{PdfDocument, Pdfium};
use anyhow::{Result, format_err};
use tokio::sync::{mpsc, oneshot};
use std::thread;
use log::{info, error, debug};
use std::collections::HashMap;
use serde::{Deserialize, Serialize};
use lru::LruCache;
use std::num::NonZeroUsize;

// SAFETY: 50 is a compile-time constant, non-zero
const CACHE_SIZE: NonZeroUsize = NonZeroUsize::new(50).unwrap();

#[derive(Debug, Clone, Copy, PartialEq, Serialize, Deserialize)]
pub enum LoadMode {
    DirectFile,
    MemoryBuffer,
}

pub struct PdfSession {
    pub mode: LoadMode,
    pub path: String,
    pub pdfium_doc: Option<PdfDocument<'static>>,
    pub lopdf_doc: Option<lopdf::Document>,
    pub memory_ptr: Option<*mut [u8]>,
    pub render_cache: LruCache<(u16, String), Vec<u8>>,
}

impl PdfSession {
    pub fn get_lopdf_doc_mut(&mut self) -> Result<&mut lopdf::Document> {
        if self.lopdf_doc.is_none() {
            let start = std::time::Instant::now();
            let doc = match self.mode {
                LoadMode::DirectFile => {
                    lopdf::Document::load(&self.path)
                        .map_err(|e| format_err!("Lopdf load failed: {}", e))?
                },
                LoadMode::MemoryBuffer => {
                    if let Some(ptr) = self.memory_ptr {
                        let slice = unsafe { &*ptr };
                        lopdf::Document::load_mem(slice)
                            .map_err(|e| format_err!("Lopdf load mem failed: {}", e))?
                    } else {
                        return Err(format_err!("Memory mode but no memory pointer found"));
                    }
                }
            };
            debug!("[PdfSession] First-time lopdf::Document::load took {:?}", start.elapsed());
            self.lopdf_doc = Some(doc);
        }
        self.lopdf_doc.as_mut().ok_or_else(|| format_err!("lopdf_doc is None after loading"))
    }

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
        self.lopdf_doc = None;
        if let Some(ptr) = self.memory_ptr {
            unsafe { let _ = Box::from_raw(ptr); }
            info!("[PdfSession] Freed memory buffer for: {}", self.path);
        }
    }
}

// Job Type Definition
type Job = Box<dyn FnOnce(&mut PdfWorkerInternalState) + Send>;

// The Sender type to be stored in Tauri State
#[derive(Clone)]
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
        let pdfium = crate::pdf::pdfium_render::get_pdfium()
            .map_err(|e| format_err!("[PDF Worker] {}", e))?;
        Ok(Self {
            pdfium,
            sessions: HashMap::new(),
        })
    }

    pub fn get_session(&mut self, path: &str) -> Result<&PdfSession> {
        self.sessions.get(path).ok_or_else(|| format_err!("Session not found for: {}. Please call load_document first.", path))
    }

    // Helper to get mutable session
    pub fn get_session_mut(&mut self, path: &str) -> Result<&mut PdfSession> {
        self.sessions.get_mut(path).ok_or_else(|| format_err!("Session not found for: {}. Please call load_document first.", path))
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
                    lopdf_doc: None,
                    memory_ptr: None,
                    render_cache: LruCache::new(CACHE_SIZE),
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
                    lopdf_doc: None,
                    memory_ptr: Some(ptr),
                    render_cache: LruCache::new(CACHE_SIZE),
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

#[cfg(test)]
mod tests {
    use super::*;
    use std::{fs, path::Path};

    const PDFIUM_SMOKE_ENV: &str = "QO_PDFIUM_SMOKE";

    fn minimal_pdf_bytes() -> Vec<u8> {
        let objects = [
            "<< /Type /Catalog /Pages 2 0 R >>",
            "<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
            "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 72 72] >>",
        ];
        let mut pdf = b"%PDF-1.4\n".to_vec();
        let mut offsets = Vec::with_capacity(objects.len());

        for (index, object) in objects.iter().enumerate() {
            offsets.push(pdf.len());
            pdf.extend_from_slice(format!("{} 0 obj\n{}\nendobj\n", index + 1, object).as_bytes());
        }

        let xref_offset = pdf.len();
        pdf.extend_from_slice(format!("xref\n0 {}\n0000000000 65535 f \n", objects.len() + 1).as_bytes());
        for offset in offsets {
            pdf.extend_from_slice(format!("{offset:010} 00000 n \n").as_bytes());
        }
        pdf.extend_from_slice(
            format!(
                "trailer\n<< /Root 1 0 R /Size {} >>\nstartxref\n{}\n%%EOF\n",
                objects.len() + 1,
                xref_offset
            )
            .as_bytes(),
        );
        pdf
    }

    #[test]
    fn loads_pdf_with_configured_pdfium_resource_path() -> Result<()> {
        if std::env::var_os(PDFIUM_SMOKE_ENV).is_none() {
            eprintln!("Skipping PDFium smoke test. Set {PDFIUM_SMOKE_ENV}=1 to run it.");
            return Ok(());
        }

        let manifest_dir = Path::new(env!("CARGO_MANIFEST_DIR"));
        let pdfium_lib_dir = manifest_dir.join("libs");
        let pdfium_library = Pdfium::pdfium_platform_library_name_at_path(&pdfium_lib_dir);
        if !pdfium_library.exists() {
            return Err(format_err!(
                "Missing PDFium test library at {:?}",
                pdfium_library
            ));
        }

        crate::pdf::pdfium_render::configure_pdfium_search_paths(vec![pdfium_lib_dir]);

        let temp_dir = tempfile::tempdir()
            .map_err(|e| format_err!("Failed to create temp dir for PDFium smoke test: {}", e))?;
        let pdf_path = temp_dir.path().join("minimal.pdf");
        fs::write(&pdf_path, minimal_pdf_bytes())
            .map_err(|e| format_err!("Failed to write minimal PDF: {}", e))?;

        let worker = init_pdf_worker();
        let runtime = tokio::runtime::Runtime::new()
            .map_err(|e| format_err!("Failed to create Tokio runtime: {}", e))?;

        runtime.block_on(async {
            let path = pdf_path.to_string_lossy().to_string();
            let load_path = path.clone();
            worker
                .call(move |state| state.load_document(load_path, LoadMode::MemoryBuffer))
                .await??;
            worker
                .call(move |state| state.close_document(&path))
                .await?;
            Ok::<(), anyhow::Error>(())
        })?;

        Ok(())
    }
}
