pub mod render;
pub mod merge;
pub mod adapter;
pub mod toc_merger_adapter;

use pdfium_render::prelude::*;
use anyhow::{Result, format_err};
use std::sync::Mutex;
use once_cell::sync::Lazy;
use log::{info, error};

// Wrapper to allow Pdfium to be stored in a static global.
struct GlobalPdfium(Pdfium);
unsafe impl Send for GlobalPdfium {}
unsafe impl Sync for GlobalPdfium {}

// Global singleton to avoid repeated dlopen calls which cause deadlocks.
static PDFIUM_SINGLETON: Lazy<Result<GlobalPdfium, String>> = Lazy::new(|| {
    info!("[PdfInit] Initializing Global Pdfium Singleton...");
    let result = Pdfium::bind_to_library(Pdfium::pdfium_platform_library_name_at_path("./"))
        .or_else(|_| Pdfium::bind_to_library(Pdfium::pdfium_platform_library_name_at_path("src-tauri/")))
        .or_else(|_| Pdfium::bind_to_library(Pdfium::pdfium_platform_library_name_at_path("src-tauri/libs/")))
        .or_else(|_| Pdfium::bind_to_library(Pdfium::pdfium_platform_library_name_at_path("libs/")))
        .or_else(|_| Pdfium::bind_to_library(Pdfium::pdfium_platform_library_name_at_path("../src-tauri/")))
        .or_else(|_| Pdfium::bind_to_library(Pdfium::pdfium_platform_library_name_at_path("../src-tauri/libs/")))
        .or_else(|_| Pdfium::bind_to_system_library())
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

pub fn get_pdfium() -> Result<&'static Pdfium> {
    match &*PDFIUM_SINGLETON {
        Ok(global) => Ok(&global.0),
        Err(e) => Err(format_err!("Pdfium init failed: {}", e)),
    }
}
