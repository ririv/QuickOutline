pub mod merge;
pub mod render;
pub mod manager;
pub mod toc;
pub mod numbering;
pub mod page_label;

use pdfium_render::prelude::*;
use anyhow::{Result, format_err};
use std::sync::Mutex;
use once_cell::sync::Lazy;

// Wrapper to allow Pdfium to be stored in a static global.
struct GlobalPdfium(Pdfium);
unsafe impl Send for GlobalPdfium {}
unsafe impl Sync for GlobalPdfium {}

// Global singleton to avoid repeated dlopen calls which cause deadlocks.
static PDFIUM_SINGLETON: Lazy<Result<GlobalPdfium, String>> = Lazy::new(|| {
    println!("[PdfInit] Initializing Global Pdfium Singleton...");
    let result = Pdfium::bind_to_library(Pdfium::pdfium_platform_library_name_at_path("./"))
        .or_else(|_| Pdfium::bind_to_library(Pdfium::pdfium_platform_library_name_at_path("src-tauri/")))
        .or_else(|_| Pdfium::bind_to_library(Pdfium::pdfium_platform_library_name_at_path("../src-tauri/")))
        .or_else(|_| Pdfium::bind_to_system_library())
        .map(Pdfium::new)
        .map(GlobalPdfium)
        .map_err(|e| e.to_string());
    
    println!("[PdfInit] Global Init Result: {}", if result.is_ok() { "OK" } else { "ERR" });
    result
});

// Mutex to serialize document loading.
// While Pdfium claims thread safety for loading different docs, we experienced crashes.
// Serializing the `load_pdf` step is a safe compromise.
pub static PDF_LOAD_MUTEX: Lazy<Mutex<()>> = Lazy::new(|| Mutex::new(()));

pub fn get_pdfium() -> Result<&'static Pdfium> {
    match &*PDFIUM_SINGLETON {
        Ok(global) => Ok(&global.0),
        Err(e) => Err(format_err!("Pdfium init failed: {}", e)),
    }
}

// Deprecated: Alias to get_pdfium for compatibility or force-update consumers
pub fn init_pdfium() -> Result<Pdfium> {
    Err(format_err!("Use get_pdfium() instead."))
}