use tauri::State;
use crate::pdf::manager::{PdfWorker, LoadMode};

#[tauri::command]
pub async fn load_pdf_document(
    state: State<'_, PdfWorker>,
    path: String,
    mode: Option<LoadMode>
) -> Result<(), String> {
    let load_mode = mode.unwrap_or(LoadMode::DirectFile);
    state.call(move |worker| {
        worker.load_document(path, load_mode)
    }).await.map_err(|e| e.to_string())?
            .map_err(|e| e.to_string())
}

#[tauri::command]
pub async fn close_pdf_document(
    state: State<'_, PdfWorker>,
    path: String
) -> Result<(), String> {
    state.call(move |worker| {
        worker.close_document(&path);
    }).await.map_err(|e| e.to_string())
}
