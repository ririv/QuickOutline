use tauri::{AppHandle, Manager, Runtime, WebviewWindow};
use std::path::PathBuf;
use std::fs;
use serde::Deserialize;

use crate::printer_native;
use crate::printer_headless;
use crate::printer_headless_chrome;

#[derive(Debug, Deserialize)]
#[serde(rename_all = "snake_case")]
pub enum PrintMode {
    Native,
    Headless,
    HeadlessChrome,
}

#[tauri::command]
pub async fn print_to_pdf<R: Runtime>(
    app: AppHandle<R>,
    window: WebviewWindow<R>,
    html: Option<String>,
    url: Option<String>,
    filename: String,
    // Options
    mode: Option<PrintMode>,
    browser_path: Option<String>,
    force_download: Option<bool>,
) -> Result<String, String> {
    let output_path = app.path().app_data_dir()
        .map_err(|e| e.to_string())?
        .join(&filename);

    if let Some(parent) = output_path.parent() {
        fs::create_dir_all(parent).map_err(|e| e.to_string())?;
    }

    let print_mode = mode.unwrap_or(PrintMode::Native);
    let force_dl = force_download.unwrap_or(false);

    println!("Print Request: Mode={:?}, Output={:?}, URL={:?}, HTML len={:?}", 
             print_mode, output_path, url, html.as_ref().map(|s| s.len()));

    if let Some(url_str) = url {
        // URL-based printing
        match print_mode {
            PrintMode::Headless => {
                #[cfg(target_os = "macos")]
                return printer_headless::print_with_url_mac(&app, url_str, output_path, browser_path, force_dl).await;
                
                #[cfg(target_os = "windows")]
                return printer_headless::print_with_url_windows(url_str, output_path).await;

                #[cfg(target_os = "linux")]
                return printer_headless::print_with_url_linux(url_str, output_path).await;
            },
            _ => return Err(format!("Printing with URL is currently only supported in 'Headless' mode. Selected: {:?}", print_mode))
        }
    } else if let Some(html_str) = html {
        // HTML String-based printing
        match print_mode {
            PrintMode::HeadlessChrome => {
                // New Rust-native Headless Chrome implementation
                return printer_headless_chrome::print_to_pdf_with_html_string(html_str, output_path)
                    .await
                    .map_err(|e| e.to_string());
            },
            PrintMode::Headless => {
                // Legacy/Custom Command-based Headless implementation
                #[cfg(target_os = "macos")]
                return printer_headless::print_with_html_mac(&app, html_str, output_path, browser_path, force_dl).await;
                
                #[cfg(target_os = "windows")]
                return printer_headless::print_with_html_windows(html_str, output_path).await;

                #[cfg(target_os = "linux")]
                return printer_headless::print_with_html_linux(html_str, output_path).await;
            },
            PrintMode::Native => {
                // Native Webview Printing (WKWebView on Mac)
                #[cfg(target_os = "macos")]
                return printer_native::print_native_with_html_mac_wkpdf(window, html_str, output_path).await;

                #[cfg(target_os = "windows")]
                return printer_native::print_native_windows(html_str, output_path).await;

                #[cfg(target_os = "linux")]
                return printer_native::print_native_linux(html_str, output_path).await;
            }
        }
    } else {
        Err("Neither 'html' nor 'url' parameters were provided.".to_string())
    }
}
