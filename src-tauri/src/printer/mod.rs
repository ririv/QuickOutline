use log::{info, warn};
use serde::Deserialize;
use std::fs;
use std::path::PathBuf;
use tauri::{AppHandle, Manager, Runtime, WebviewWindow};

pub mod headless;
pub mod headless_chrome;
pub mod native;
pub mod native_linux;
pub mod native_macos;
pub mod native_windows;

use crate::printer::native::PageDimensions;
use crate::static_server::LocalServerState;

#[derive(Debug, Deserialize)]
#[serde(rename_all = "snake_case")]
pub enum PrintMode {
    Native,
    Headless,
    HeadlessChrome,
}

#[tauri::command]
#[allow(clippy::too_many_arguments)]
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
    dimensions: Option<PageDimensions>,
) -> Result<String, String> {
    // Use app_data_dir / pdf_workspace
    let app_data_dir = app.path().app_data_dir().map_err(|e| e.to_string())?;
    let pdf_workspace = app_data_dir.join("pdf_workspace");

    // Ensure the directory exists
    fs::create_dir_all(&pdf_workspace).map_err(|e| e.to_string())?;

    let output_path = pdf_workspace.join(&filename);

    let print_mode = mode.unwrap_or(PrintMode::Native);
    let force_dl = force_download.unwrap_or(false);

    info!(
        "打印请求: Mode={:?}, Output={:?}, URL={:?}, HTML len={:?}, Dimensions={:?}",
        print_mode,
        output_path,
        url,
        html.as_ref().map(|s| s.len()),
        dimensions
    );

    // Try to get local server port and workspace path
    let mut local_url: Option<String> = None;
    let mut temp_file_to_clean: Option<PathBuf> = None; // 跟踪临时文件以便清理

    if let Some(html_content) = &html {
        let state = app.state::<LocalServerState>();
        if let (Ok(port_guard), Ok(workspace_guard)) = (state.port.lock(), state.workspace.lock())
            && let Some(port) = *port_guard
        {
            // Save HTML to workspace
            let temp_filename = format!("print_job_{}.html", uuid::Uuid::new_v4());
            let temp_file_path = workspace_guard.join(&temp_filename);

            if fs::write(&temp_file_path, html_content).is_ok() {
                local_url = Some(format!("http://127.0.0.1:{}/{}", port, temp_filename));
                temp_file_to_clean = Some(temp_file_path.clone());
                info!(
                    "已保存 HTML 到工作区: {:?} -> URL: {:?}",
                    temp_file_path, local_url
                );
            }
        }
    }

    // Determine target URL: Provided URL > Local Server URL
    let target_url = url.or(local_url);

    // 辅助函数：清理临时文件
    let cleanup_temp_file = |path: Option<PathBuf>| {
        if let Some(p) = path
            && let Err(e) = fs::remove_file(&p)
        {
            warn!("清理临时文件失败 {:?}: {}", p, e);
        }
    };

    if let Some(url_str) = target_url {
        // URL-based printing (Remote or Localhost)
        let result = match print_mode {
            PrintMode::HeadlessChrome => {
                headless_chrome::print_to_pdf_with_url(url_str, output_path)
                    .await
                    .map_err(|e| e.to_string())
            }
            PrintMode::Headless => {
                #[cfg(target_os = "macos")]
                {
                    headless::print_with_url_mac(&app, url_str, output_path, browser_path, force_dl)
                        .await
                }
                #[cfg(target_os = "windows")]
                {
                    headless::print_with_url_windows(url_str, output_path).await
                }
                #[cfg(target_os = "linux")]
                {
                    headless::print_with_url_linux(url_str, output_path).await
                }
                #[cfg(not(any(target_os = "macos", target_os = "windows", target_os = "linux")))]
                {
                    Err("此操作系统不支持 Headless 打印".to_string())
                }
            }
            _ => {
                // Native Mode

                {
                    #[cfg(target_os = "macos")]
                    {
                        native::print_to_pdf_with_url_native(
                            app,
                            window,
                            url_str,
                            output_path.clone(),
                            dimensions,
                        )
                        .await
                    }
                    #[cfg(target_os = "windows")]
                    {
                        native::print_native_windows(
                            app.clone(),
                            window.clone(),
                            url_str,
                            output_path,
                            dimensions,
                        )
                        .await
                    }
                    #[cfg(target_os = "linux")]
                    {
                        native::print_native_linux(
                            app.clone(),
                            window.clone(),
                            url_str,
                            output_path,
                            dimensions,
                        )
                        .await
                    }
                    #[cfg(not(any(
                        target_os = "macos",
                        target_os = "windows",
                        target_os = "linux"
                    )))]
                    {
                        Err("原生 URL 打印仅支持特定平台".to_string())
                    }
                }
            }
        };
        cleanup_temp_file(temp_file_to_clean);
        result
    } else if let Some(html_str) = html {
        // Fallback: If server URL generation failed, use a temporary file with file:// protocol
        // This restores the legacy behavior (which may have permission issues) as a last resort.
        let temp_dir = std::env::temp_dir();
        let temp_filename = format!("print_fallback_{}.html", uuid::Uuid::new_v4());
        let temp_path = temp_dir.join(&temp_filename);

        if fs::write(&temp_path, &html_str).is_ok() {
            let file_url = format!("file://{}", temp_path.to_string_lossy());
            let temp_path_clone = temp_path.clone();

            let result = match print_mode {
                PrintMode::HeadlessChrome => {
                    headless_chrome::print_to_pdf_with_url(file_url, output_path)
                        .await
                        .map_err(|e| e.to_string())
                }
                PrintMode::Headless => {
                    #[cfg(target_os = "macos")]
                    {
                        headless::print_with_url_mac(
                            &app,
                            file_url,
                            output_path,
                            browser_path,
                            force_dl,
                        )
                        .await
                    }
                    #[cfg(target_os = "windows")]
                    {
                        headless::print_with_url_windows(file_url, output_path).await
                    }
                    #[cfg(target_os = "linux")]
                    {
                        headless::print_with_url_linux(file_url, output_path).await
                    }
                    #[cfg(not(any(
                        target_os = "macos",
                        target_os = "windows",
                        target_os = "linux"
                    )))]
                    {
                        Err("此操作系统不支持 Headless 打印".to_string())
                    }
                }
                PrintMode::Native => {
                    #[cfg(target_os = "macos")]
                    {
                        native::print_native_with_html_mac_wkpdf(
                            window,
                            html_str,
                            output_path,
                            dimensions,
                        )
                        .await
                    }
                    #[cfg(target_os = "windows")]
                    {
                        native::print_native_windows(
                            app.clone(),
                            window.clone(),
                            html_str,
                            output_path,
                            dimensions,
                        )
                        .await
                    }
                    #[cfg(target_os = "linux")]
                    {
                        native::print_native_linux(
                            app.clone(),
                            window.clone(),
                            html_str,
                            output_path,
                            dimensions,
                        )
                        .await
                    }
                    #[cfg(not(any(
                        target_os = "macos",
                        target_os = "windows",
                        target_os = "linux"
                    )))]
                    {
                        Err("此操作系统不支持原生打印".to_string())
                    }
                }
            };
            // 清理临时文件
            cleanup_temp_file(Some(temp_path_clone));
            result
        } else {
            Err("保存临时 HTML 文件失败".to_string())
        }
    } else {
        Err("未提供 'html' 或 'url' 参数".to_string())
    }
}
