use pdfium_render::prelude::*;
use anyhow::{Result, format_err};
use tokio::sync::{mpsc, oneshot};
use std::thread;
use image::ImageFormat;
use std::io::Cursor;

use crate::pdf::toc::{TocConfig, process_toc_generation};

// Request types for the PDF Worker
pub enum PdfRequest {
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
    pdfium: Pdfium,
    // Store the path of the currently loaded document, if any.
    // The PdfDocument itself cannot be directly cached here due to lifetime issues with Pdfium.
    // Instead, we will reload the PdfDocument from the Pdfium instance when needed.
    // This is still an optimization as Pdfium itself is initialized only once.
    current_file_path: Option<String>,
}

impl PdfWorkerInternalState {
    fn new() -> Result<Self> {
        println!("[PDF Worker] Init. CWD: {:?}", std::env::current_dir());
        
        let pdfium = crate::pdf::init_pdfium()
            .map_err(|e| format_err!("[PDF Worker] {}", e))?;
        
        Ok(Self {
            pdfium,
            current_file_path: None,
        })
    }

    // This method handles the rendering and document caching
    fn process_render_request(&mut self, path: String, page_index: u16, scale: f32) -> Result<Vec<u8>> {
        // If the path has changed, update the current_file_path
        if self.current_file_path.as_ref().map_or(true, |p| *p != path) {
            self.current_file_path = Some(path.clone());
            // println!("[PDF Worker] Updated current file path to: {}", path);
        }

        // Always load the document from Pdfium using the current_file_path.
        let current_path = self.current_file_path.as_ref().ok_or_else(|| format_err!("No PDF file path set in worker state"))?;

        let doc = self.pdfium.load_pdf_from_file(current_path, None)
            .map_err(|e| format_err!("Failed to load PDF: {}", e))?;
        
        // Render logic (reused from previous implementation)
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
        // Similar caching logic as render
        if self.current_file_path.as_ref().map_or(true, |p| *p != path) {
            self.current_file_path = Some(path.clone());
        }

        let current_path = self.current_file_path.as_ref().ok_or_else(|| format_err!("No PDF file path set in worker state"))?;
        let doc = self.pdfium.load_pdf_from_file(current_path, None)
            .map_err(|e| format_err!("Failed to load PDF: {}", e))?;
        
        Ok(doc.pages().len())
    }
}


pub fn init_pdf_worker() -> PdfWorker {
    let (tx, mut rx) = mpsc::channel::<PdfRequest>(32);

    thread::spawn(move || {
        let mut worker_state = match PdfWorkerInternalState::new() {
            Ok(state) => state,
            Err(e) => {
                eprintln!("{}", e);
                return;
            }
        };

        while let Some(request) = rx.blocking_recv() {
            match request {
                PdfRequest::RenderPage { path, page_index, scale, response_tx } => {
                    let result = worker_state.process_render_request(path, page_index, scale);
                    let _ = response_tx.send(result);
                },
                PdfRequest::GetPageCount { path, response_tx } => {
                    let result = worker_state.process_get_page_count(path);
                    let _ = response_tx.send(result);
                },
                PdfRequest::GenerateToc { src_path, config, dest_path, response_tx } => {
                    let result = process_toc_generation(&worker_state.pdfium, src_path, config, dest_path);
                    let _ = response_tx.send(result);
                },
                PdfRequest::ExtractToc { path, response_tx } => {
                    let result = crate::pdf_analysis::TocExtractor::extract_toc(&worker_state.pdfium, &path);
                    let _ = response_tx.send(result);
                }
            }
        }
    });

    PdfWorker(tx)
}
