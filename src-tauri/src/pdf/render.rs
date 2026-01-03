use tauri::{State, ipc::Response};
use anyhow::Result;
use crate::pdf::manager::PdfWorker;

#[tauri::command]
pub async fn render_pdf_page(
    state: State<'_, PdfWorker>,
    path: String,
    page_index: u16,
    scale: f32
) -> Result<Response, String> {
    let png_data = state.call(move |worker| {
        worker.process_render_request(path, page_index, scale)
    }).await.map_err(|e| e.to_string())?
            .map_err(|e| e.to_string())?;

    Ok(Response::new(png_data))
}

#[tauri::command]
pub async fn get_pdf_page_count(
    state: State<'_, PdfWorker>,
    path: String,
) -> Result<u16, String> {
    state.call(move |worker| {
        worker.process_get_page_count(path)
    }).await.map_err(|e| e.to_string())?
            .map_err(|e| e.to_string())
}
