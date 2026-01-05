use tauri::{State, ipc::Response};
use anyhow::{Result, format_err};
use crate::pdf::manager::{PdfWorker, PdfWorkerInternalState};
use image::ImageFormat;
use std::io::Cursor;
use log::info;

pub fn internal_render_page(state: &mut PdfWorkerInternalState, path: String, page_index: u16, scale: f32) -> Result<Vec<u8>> {
    let session = state.get_session_mut(&path)?;
    
    // Check LRU Cache
    let scale_key = format!("{:.2}", scale);
    if let Some(cached) = session.render_cache.get(&(page_index, scale_key.clone())) {
        info!("[PdfRender] Cache hit for page {} scale {}", page_index, scale);
        return Ok(cached.clone());
    }

    let doc = session.pdfium_doc.as_ref().ok_or_else(|| format_err!("Session missing document"))?;
    let page = doc.pages().get(page_index)?;
    let width = (page.width().value * scale) as i32;
    let height = (page.height().value * scale) as i32;
    let bitmap = page.render(width, height, None)?;
    let dynamic_image = bitmap.as_image(); 
    let mut png_data = Vec::new();
    let mut cursor = Cursor::new(&mut png_data);
    dynamic_image.write_to(&mut cursor, ImageFormat::Png)?;
    
    // Update LRU Cache
    session.render_cache.put((page_index, scale_key), png_data.clone());
    
    Ok(png_data)
}

pub fn internal_get_page_count(state: &mut PdfWorkerInternalState, path: String) -> Result<u16> {
    let session = state.get_session(&path)?;
    let doc = session.pdfium_doc.as_ref().ok_or_else(|| format_err!("Session missing document"))?;
    Ok(doc.pages().len())
}

#[tauri::command]
pub async fn render_pdf_page(
    state: State<'_, PdfWorker>,
    path: String,
    page_index: u16,
    scale: f32
) -> Result<Response, String> {
    state.call(move |worker| {
        internal_render_page(worker, path, page_index, scale)
    }).await.map_err(|e| e.to_string())?
            .map_err(|e| e.to_string())
            .map(Response::new)
}

#[tauri::command]
pub async fn get_pdf_page_count(
    state: State<'_, PdfWorker>,
    path: String,
) -> Result<u16, String> {
    state.call(move |worker| {
        internal_get_page_count(worker, path)
    }).await.map_err(|e| e.to_string())?
            .map_err(|e| e.to_string())
}