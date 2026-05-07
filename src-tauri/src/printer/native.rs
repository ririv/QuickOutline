use tauri::{AppHandle, Manager, Runtime, WebviewWindow};
use std::path::PathBuf;
use std::fs;
use log::error;

#[cfg(target_os = "windows")]
pub use super::native_windows::print_native_windows;

#[cfg(target_os = "linux")]
pub use super::native_linux::print_native_linux;

#[cfg(target_os = "macos")]
pub use super::native_macos::{
    print_native_with_url_mac_wkpdf,
    print_native_with_html_mac_wkpdf,
};

#[derive(Debug, Clone, Copy, serde::Deserialize)]
pub struct PageDimensions {
    pub width: f64,
    pub height: f64,
}

#[tauri::command]
pub async fn print_to_pdf_with_html_string_native<R: Runtime>(
    app: AppHandle<R>,
    window: WebviewWindow<R>,
    html: String,
    filename: String,
    dimensions: Option<PageDimensions>,
) -> Result<String, String> {
    let output_path = app.path().app_data_dir()
        .map_err(|e| e.to_string())?
        .join(&filename);

    if let Some(parent) = output_path.parent() {
        fs::create_dir_all(parent).map_err(|e| e.to_string())?;
    }

    #[cfg(target_os = "macos")]
    {
        match print_native_with_html_mac_wkpdf(window.clone(), html.clone(), output_path.clone(), dimensions).await {
            Ok(path) => Ok(path),
            Err(e) => {
                error!("Native PDF generation (WKPDF) failed: {}", e);
                Err(e)
            }
        }
    }

    #[cfg(not(target_os = "macos"))]
    {
        let _ = (window, html, output_path, dimensions);
        Err("Native HTML printing not implemented for this platform.".to_string())
    }
}

pub async fn print_to_pdf_with_url_native<R: Runtime>(
    _app: AppHandle<R>,
    window: WebviewWindow<R>,
    url: String,
    output_path: PathBuf,
    dimensions: Option<PageDimensions>,
) -> Result<String, String> {
    if let Some(parent) = output_path.parent() {
        fs::create_dir_all(parent).map_err(|e| e.to_string())?;
    }

    #[cfg(target_os = "macos")]
    {
        match print_native_with_url_mac_wkpdf(window, url, output_path, dimensions).await {
            Ok(path) => Ok(path),
            Err(e) => {
                error!("Native PDF generation (WKPDF) failed: {}.", e);
                Err(e)
            }
        }
    }

    #[cfg(not(target_os = "macos"))]
    {
        let _ = (window, url, output_path, dimensions);
        Err("Native URL printing not implemented for this platform.".to_string())
    }
}
