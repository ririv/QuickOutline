use tauri::{AppHandle, Manager, Runtime, WebviewWindow};
use std::path::PathBuf;
use std::fs;

use crate::printer_native;
use crate::printer_headless;

#[tauri::command]
pub async fn print_to_pdf<R: Runtime>(
    app: AppHandle<R>,
    window: WebviewWindow<R>,
    html: String,
    filename: String,
    // Options for printing mode
    use_headless: Option<bool>,
    browser_path: Option<String>,
    force_download: Option<bool>,
) -> Result<String, String> {
    let output_path = app.path().app_data_dir()
        .map_err(|e| e.to_string())?
        .join(&filename);

    if let Some(parent) = output_path.parent() {
        fs::create_dir_all(parent).map_err(|e| e.to_string())?;
    }

    // Default to native if not specified
    let headless = use_headless.unwrap_or(false);
    let force_dl = force_download.unwrap_or(false);

    if headless {
        #[cfg(target_os = "macos")]
        return printer_headless::print_mac(&app, html, output_path, browser_path, force_dl).await;
        
        #[cfg(target_os = "windows")]
        return printer_headless::print_windows(html, output_path).await;

        #[cfg(target_os = "linux")]
        return printer_headless::print_linux(html, output_path).await;
    } else {
        // Native Printing
        #[cfg(target_os = "macos")]
        return printer_native::print_native_mac_wkpdf(window, html, output_path).await;

        #[cfg(target_os = "windows")]
        return printer_native::print_native_windows(html, output_path).await;

        #[cfg(target_os = "linux")]
        return printer_native::print_native_linux(html, output_path).await;
    }
}
