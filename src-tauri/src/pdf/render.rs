use tauri::{State, ipc::Response};
use anyhow::Result;
use tokio::sync::oneshot;
use crate::pdf::manager::{PdfWorker, PdfRequest};

#[tauri::command]
pub async fn render_pdf_page(
    state: State<'_, PdfWorker>,
    path: String,
    page_index: u16,
    scale: f32
) -> Result<Response, String> {
    // Create a channel to receive the response from the worker
    let (tx, rx) = oneshot::channel();

    // Send request to worker
    state.0.send(PdfRequest::RenderPage {
        path,
        page_index,
        scale,
        response_tx: tx,
    }).await.map_err(|e| format!("Failed to send request to PDF worker: {}", e))?;

    // Await response
    let png_data = rx.await
        .map_err(|_| "PDF worker dropped the response channel".to_string())?
        .map_err(|e| e.to_string())?;

    Ok(Response::new(png_data))
}

#[tauri::command]
pub async fn get_pdf_page_count(
    state: State<'_, PdfWorker>,
    path: String,
) -> Result<u16, String> {
    let (tx, rx) = oneshot::channel();

    state.0.send(PdfRequest::GetPageCount {
        path,
        response_tx: tx,
    }).await.map_err(|e| format!("Failed to send request to PDF worker: {}", e))?;

    let count = rx.await
        .map_err(|_| "PDF worker dropped the response channel".to_string())?
        .map_err(|e| e.to_string())?;

    Ok(count)
}
