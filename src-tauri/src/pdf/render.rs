use pdfium_render::prelude::*;
use anyhow::{Result, format_err};
use tokio::task;
use image::{ImageFormat, RgbaImage};
use std::io::Cursor;

/// Helper function to render a specific page to a PNG byte vector
fn perform_pdf_render(
    path: &str,
    page_index: u16,
    scale: f32,
) -> Result<Vec<u8>> {
    // Initialize Pdfium
    // Note: In a production app, you might want to initialize Pdfium once globally or using a Singleton,
    // but binding per-request is safer for now if we don't have a shared state setup for it.
    let pdfium = Pdfium::new(
        Pdfium::bind_to_library(Pdfium::pdfium_platform_library_name_at_path("./"))
            .or_else(|_| Pdfium::bind_to_system_library())
            .map_err(|e| format_err!("Failed to bind pdfium: {}", e))?,
    );

    let document = pdfium.load_pdf_from_file(path, None)?;

    // Pdfium pages are 0-indexed
    let page = document.pages().get(page_index)?;

    // Calculate target dimensions
    // .width() and .height() return PdfPoints
    let width = (page.width().value * scale) as i32;
    let height = (page.height().value * scale) as i32;

    // Render options
    // PdfRenderConfig::new() ... 
    // For now we use basic render which returns a PdfBitmap
    let bitmap = page.render(width, height, None)?;

    // Pdfium bitmaps are usually BGRA (Blue-Green-Red-Alpha) on little-endian systems
    // We need to convert to RGBA for the `image` crate.
    let bytes = bitmap.as_bytes();
    let mut rgba_bytes = Vec::with_capacity(bytes.len());

    // Basic loop to swap B and R. 
    // Ideally check bitmap.format() but BGRA is standard for Pdfium.
    for chunk in bytes.chunks(4) {
        if chunk.len() == 4 {
            rgba_bytes.push(chunk[2]); // R
            rgba_bytes.push(chunk[1]); // G
            rgba_bytes.push(chunk[0]); // B
            rgba_bytes.push(chunk[3]); // A
        }
    }

    let img_buffer = RgbaImage::from_raw(width as u32, height as u32, rgba_bytes)
        .ok_or_else(|| format_err!("Failed to create image buffer"))?;

    let mut png_data = Vec::new();
    let mut cursor = Cursor::new(&mut png_data);
    
    // Write as PNG
    img_buffer.write_to(&mut cursor, ImageFormat::Png)?;

    Ok(png_data)
}

#[tauri::command]
pub async fn render_pdf_page(
    path: String,
    page_index: u16,
    scale: f32
) -> Result<Vec<u8>, String> {
    task::spawn_blocking(move || {
        perform_pdf_render(&path, page_index, scale)
    })
    .await
    .map_err(|e| e.to_string())?
    .map_err(|e| e.to_string())
}
