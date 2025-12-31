pub mod merge;
pub mod render;
pub mod manager;
pub mod toc;
pub mod numbering;
pub mod page_label;

use pdfium_render::prelude::*;
use anyhow::{Result, format_err};

pub fn init_pdfium() -> Result<Pdfium> {
    Pdfium::bind_to_library(Pdfium::pdfium_platform_library_name_at_path("./"))
        .or_else(|_| Pdfium::bind_to_library(Pdfium::pdfium_platform_library_name_at_path("src-tauri/")))
        .or_else(|_| Pdfium::bind_to_library(Pdfium::pdfium_platform_library_name_at_path("../src-tauri/")))
        .or_else(|_| Pdfium::bind_to_system_library())
        .map(Pdfium::new)
        .map_err(|e| format_err!("Failed to bind Pdfium: {}", e))
}