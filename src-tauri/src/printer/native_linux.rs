//! Linux 原生 PDF 打印，使用 webkit2gtk 的 PrintOperation。
//!
//! 本模块利用 WebKitGTK 的 PrintOperation 功能，
//! 在 Linux 上实现原生 PDF 生成。

use std::path::PathBuf;
use tauri::{AppHandle, Runtime, WebviewWindow};

use super::native::PageDimensions;

/// 打印超时时间（秒）
const PRINT_TIMEOUT_SECS: u64 = 30;

/// 在 Linux 上使用 WebKitGTK 将 HTML 或 URL 内容打印为 PDF。
///
/// # 参数
/// * `_app` - Tauri 应用句柄（未使用，保留以保持 API 一致性）
/// * `window` - 用于打印的 WebviewWindow
/// * `content` - 要打印的 HTML 字符串或 URL
/// * `output_path` - PDF 保存路径
/// * `_dimensions` - 页面尺寸（传递给打印设置）
///
/// # 平台
/// 此函数仅在 Linux（GTK）上有效。在其他平台上返回错误。
#[cfg(target_os = "linux")]
pub async fn print_native_linux<R: Runtime>(
    _app: AppHandle<R>,
    window: WebviewWindow<R>,
    content: String,
    output_path: PathBuf,
    _dimensions: Option<PageDimensions>,
) -> Result<String, String> {
    use gtk::prelude::*;
    use std::sync::atomic::{AtomicBool, Ordering};
    use std::sync::mpsc;
    use std::sync::{Arc, Mutex};
    use std::time::Duration;
    use webkit2gtk::{PrintOperationExt, WebViewExt};

    let (result_tx, result_rx) = mpsc::channel();
    let result_tx = Arc::new(Mutex::new(result_tx));
    let output_path_clone = output_path.clone();
    let content_clone = content.clone();
    // 防止多次触发打印的原子标志
    let printed = Arc::new(AtomicBool::new(false));

    window
        .with_webview(move |webview| {
            let webview = webview.inner();

            let tx = result_tx.clone();
            let path = output_path_clone.clone();
            let printed_flag = printed.clone();

            // 设置页面加载完成回调
            webview.connect_load_changed(move |wv, event| {
                // 只在首次 Finished 事件时执行打印
                if event == webkit2gtk::LoadEvent::Finished
                    && !printed_flag.swap(true, Ordering::SeqCst)
                {
                    // 创建打印操作
                    let op = webkit2gtk::PrintOperation::new(wv);
                    let settings = gtk::PrintSettings::new();

                    // 配置 PDF 输出
                    let uri = format!("file://{}", path.to_string_lossy());
                    settings.set(gtk::PRINT_SETTINGS_OUTPUT_URI, Some(&uri));
                    settings.set(gtk::PRINT_SETTINGS_PRINTER, Some("Print to File"));
                    settings.set(gtk::PRINT_SETTINGS_OUTPUT_FILE_FORMAT, Some("pdf"));

                    op.set_print_settings(&settings);

                    // 处理完成事件
                    let tx_success = tx.clone();
                    op.connect_finished(move |_| {
                        if let Ok(sender) = tx_success.lock() {
                            let _ = sender.send(Ok(()));
                        }
                    });

                    // 处理失败事件
                    let tx_error = tx.clone();
                    op.connect_failed(move |_, error| {
                        if let Ok(sender) = tx_error.lock() {
                            let _ = sender.send(Err(error.to_string()));
                        }
                    });

                    // 执行打印（静默模式）
                    op.print();
                }
            });

            // 加载内容
            let is_url = content_clone.starts_with("http://")
                || content_clone.starts_with("https://")
                || content_clone.starts_with("file://");

            if is_url {
                webview.load_uri(&content_clone);
            } else {
                webview.load_html(&content_clone, None);
            }
        })
        .map_err(|e| e.to_string())?;

    // 等待结果，带超时
    match result_rx.recv_timeout(Duration::from_secs(PRINT_TIMEOUT_SECS)) {
        Ok(Ok(())) => Ok(output_path.to_string_lossy().to_string()),
        Ok(Err(e)) => Err(e),
        Err(_) => Err(format!("PrintToPdf 超时（{}秒）", PRINT_TIMEOUT_SECS)),
    }
}

#[cfg(not(target_os = "linux"))]
pub async fn print_native_linux<R: Runtime>(
    _app: AppHandle<R>,
    _window: WebviewWindow<R>,
    _content: String,
    _output_path: PathBuf,
    _dimensions: Option<PageDimensions>,
) -> Result<String, String> {
    Err("Linux 原生打印仅在 Linux 上可用。".to_string())
}
