pub mod render;
pub mod merge;
pub mod analysis_adapter;
pub mod toc_merger_adapter;
pub mod extractor;

use pdfium_render::prelude::*;
use anyhow::{Result, format_err};
use std::path::PathBuf;
use std::sync::Mutex;
use once_cell::sync::{Lazy, OnceCell};
use log::{debug, info, warn, error};

// Wrapper to allow Pdfium to be stored in a static global.
struct GlobalPdfium(Pdfium);
unsafe impl Send for GlobalPdfium {}
unsafe impl Sync for GlobalPdfium {}

// Global singleton to avoid repeated dlopen calls which cause deadlocks.
static PDFIUM_SEARCH_PATHS: OnceCell<Vec<PathBuf>> = OnceCell::new();

static PDFIUM_SINGLETON: Lazy<Result<GlobalPdfium, String>> = Lazy::new(|| {
    info!("[PdfInit] Initializing Global Pdfium Singleton...");
    let configured_bindings = PDFIUM_SEARCH_PATHS
        .get()
        .into_iter()
        .flatten()
        .find_map(|path| {
            let library_path = Pdfium::pdfium_platform_library_name_at_path(path);
            Pdfium::bind_to_library(&library_path)
                .inspect_err(|e| debug!("[PdfInit] Failed to bind {:?}: {}", library_path, e))
                .ok()
        });

    let result = configured_bindings
        .map(Ok)
        .unwrap_or_else(|| {
            Pdfium::bind_to_library(Pdfium::pdfium_platform_library_name_at_path("./"))
                .or_else(|_| Pdfium::bind_to_library(Pdfium::pdfium_platform_library_name_at_path("src-tauri/")))
                .or_else(|_| Pdfium::bind_to_library(Pdfium::pdfium_platform_library_name_at_path("src-tauri/libs/")))
                .or_else(|_| Pdfium::bind_to_library(Pdfium::pdfium_platform_library_name_at_path("libs/")))
                .or_else(|_| Pdfium::bind_to_library(Pdfium::pdfium_platform_library_name_at_path("../src-tauri/")))
                .or_else(|_| Pdfium::bind_to_library(Pdfium::pdfium_platform_library_name_at_path("../src-tauri/libs/")))
                .or_else(|_| Pdfium::bind_to_system_library())
        })
        .map(Pdfium::new)
        .map(GlobalPdfium)
        .map_err(|e| e.to_string());
    
    match &result {
        Ok(_) => info!("[PdfInit] Global Init Result: OK"),
        Err(e) => error!("[PdfInit] Global Init Result: ERR - {}", e),
    }
    result
});

// Mutex to serialize document loading.
pub static PDF_LOAD_MUTEX: Lazy<Mutex<()>> = Lazy::new(|| Mutex::new(()));

pub fn configure_pdfium_search_paths(paths: Vec<PathBuf>) {
    if PDFIUM_SEARCH_PATHS.set(paths).is_err() {
        warn!("[PdfInit] PDFium search paths were already configured");
    }
}

pub fn get_pdfium() -> Result<&'static Pdfium> {
    match &*PDFIUM_SINGLETON {
        Ok(global) => Ok(&global.0),
        Err(e) => Err(format_err!("Pdfium init failed: {}", e)),
    }
}
