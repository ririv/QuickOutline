use pdfium_render::prelude::*;
use anyhow::{Result, format_err};
use tokio::task;
use image::ImageFormat;
use std::io::Cursor;
use std::fs::File;
use std::io::Write;
use tauri::ipc::Response;

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

    // Use the `image` feature of pdfium-render to get a DynamicImage directly
    let dynamic_image = bitmap.as_image(); 

    let mut png_data = Vec::new();
    let mut cursor = Cursor::new(&mut png_data);
    
    // Write as PNG
    dynamic_image.write_to(&mut cursor, ImageFormat::Png)?;

    // DEBUG: Write to file for inspection
    // let debug_path = format!("/tmp/debug_page_{}.png", page_index);
    // if let Ok(mut file) = File::create(&debug_path) {
    //     if let Err(e) = file.write_all(&png_data) {
    //          eprintln!("DEBUG: Failed to write debug file: {}", e);
    //     } else {
    //          println!("DEBUG: Wrote page {} to {}", page_index, debug_path);
    //     }
    // } else {
    //     eprintln!("DEBUG: Failed to create debug file at {}", debug_path);
    // }

    Ok(png_data)
}

#[tauri::command]
pub async fn render_pdf_page(
    path: String,
    page_index: u16,
    scale: f32
) -> Result<Response, String> {
    let png_data = task::spawn_blocking(move || {
        perform_pdf_render(&path, page_index, scale)
    })
    .await
    .map_err(|e| e.to_string())?
    .map_err(|e| e.to_string())?;

    Ok(Response::new(png_data))
}
