use tauri::{AppHandle, Manager, Runtime, WebviewWindow};
use std::fs;
use std::path::PathBuf;
use serde::Deserialize;

use crate::printer_native;
use crate::printer_headless;
use crate::printer_headless_chrome;
use crate::static_server::LocalServerState; // Import LocalServerState

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

    // Try to get local server port and workspace path
    let mut local_url: Option<String> = None;
    let mut temp_file_to_clean: Option<PathBuf> = None; // Track temp file for cleanup
    
    if let Some(html_content) = &html {
        let state = app.state::<LocalServerState>();
        let port_guard = state.port.lock().unwrap();
        let workspace_guard = state.workspace.lock().unwrap();
        
        if let Some(port) = *port_guard {
             // Save HTML to workspace
             let temp_filename = format!("print_job_{}.html", uuid::Uuid::new_v4());
             let temp_file_path = workspace_guard.join(&temp_filename);
             
             if let Ok(_) = fs::write(&temp_file_path, html_content) {
                 local_url = Some(format!("http://127.0.0.1:{}/{}", port, temp_filename));
                 temp_file_to_clean = Some(temp_file_path); // Store path for cleanup
                 println!("Saved HTML to workspace: {:?} -> URL: {:?}", temp_file_to_clean, local_url);
             }
        }
    }

    // Determine target URL: Provided URL > Local Server URL
    let target_url = url.or(local_url);

    let result = if let Some(url_str) = target_url {
        // URL-based printing (Remote or Localhost)
        match print_mode {
            PrintMode::HeadlessChrome => {
                printer_headless_chrome::print_to_pdf_with_url(url_str, output_path)
                    .await
                    .map_err(|e| e.to_string())
            },
            PrintMode::Headless => {
                let res = {
                    #[cfg(target_os = "macos")]
                    {
                        printer_headless::print_with_url_mac(&app, url_str, output_path, browser_path, force_dl).await
                    }
                    #[cfg(target_os = "windows")]
                    {
                        printer_headless::print_with_url_windows(url_str, output_path).await
                    }
                    #[cfg(target_os = "linux")]
                    {
                        printer_headless::print_with_url_linux(url_str, output_path).await
                    }
                    #[cfg(not(any(target_os = "macos", target_os = "windows", target_os = "linux")))]
                    {
                        Err("Headless printing not implemented for this OS.".to_string())
                    }
                };
                res
            },
            _ => { // Native Mode
                let res = {
                     #[cfg(target_os = "macos")]
                     {
                        printer_native::print_to_pdf_with_url_native(app, window, url_str, filename).await
                     }
                     #[cfg(not(target_os = "macos"))]
                     {
                        Err("Native URL printing is only implemented for macOS currently.".to_string())
                     }
                };
                res
            }
        }
    } else if let Some(html_str) = html {
        // Fallback: If server URL generation failed, use a temporary file with file:// protocol
        // This restores the legacy behavior (which may have permission issues) as a last resort.
        let temp_dir = std::env::temp_dir();
        let temp_filename = format!("print_fallback_{}.html", uuid::Uuid::new_v4());
        let temp_path = temp_dir.join(&temp_filename);
        
        let res = if let Ok(_) = fs::write(&temp_path, &html_str) {
             let file_url = format!("file://{}", temp_path.to_string_lossy());
             
             match print_mode {
                PrintMode::HeadlessChrome => {
                    printer_headless_chrome::print_to_pdf_with_url(file_url, output_path)
                        .await
                        .map_err(|e| e.to_string())
                },
                PrintMode::Headless => {
                    let res_inner = {
                        #[cfg(target_os = "macos")]
                        {
                            printer_headless::print_with_url_mac(&app, file_url, output_path, browser_path, force_dl).await
                        }
                        #[cfg(target_os = "windows")]
                        {
                            printer_headless::print_with_url_windows(file_url, output_path).await
                        }
                        #[cfg(target_os = "linux")]
                        {
                            printer_headless::print_with_url_linux(file_url, output_path).await
                        }
                        #[cfg(not(any(target_os = "macos", target_os = "windows", target_os = "linux")))]
                        {
                            Err("Headless printing not implemented for this OS.".to_string())
                        }
                    };
                    res_inner
                },
                PrintMode::Native => {
                    let res_inner = {
                         #[cfg(target_os = "macos")]
                         {
                            printer_native::print_native_with_html_mac_wkpdf(window, html_str, output_path).await
                         }
                         #[cfg(target_os = "windows")]
                         {
                            printer_native::print_native_windows(html_str, output_path).await
                         }
                         #[cfg(target_os = "linux")]
                         {
                            printer_native::print_native_linux(html_str, output_path).await
                         }
                         #[cfg(not(any(target_os = "macos", target_os = "windows", target_os = "linux")))]
                         {
                            Err("Native printing not implemented for this OS.".to_string())
                         }
                    };
                    res_inner
                }
             }
        } else {
            Err("Failed to save temporary HTML file for fallback printing.".to_string())
        };
        res // Return the result of the print operation
    } else {
        Err("Neither 'html' nor 'url' parameters were provided.".to_string())
    };
    result // Return the final result
}
