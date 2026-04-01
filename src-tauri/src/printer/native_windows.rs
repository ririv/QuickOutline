//! Windows 原生 PDF 打印，使用 WebView2 的 PrintToPdf API。
//!
//! 本模块利用 WebView2 运行时内置的 PrintToPdf 功能，
//! 在 Windows 上实现原生 PDF 生成。

use tauri::{AppHandle, Runtime, WebviewWindow};
use std::path::PathBuf;

use super::native::PageDimensions;

/// 内容加载等待时间（毫秒）
const CONTENT_LOAD_WAIT_MS: u64 = 1500;
/// 打印超时时间（秒）
const PRINT_TIMEOUT_SECS: u64 = 30;

/// 在 Windows 上使用 WebView2 将 HTML 或 URL 内容打印为 PDF。
///
/// # 参数
/// * `_app` - Tauri 应用句柄（未使用，保留以保持 API 一致性）
/// * `window` - 用于打印的 WebviewWindow
/// * `content` - 要打印的 HTML 字符串或 URL
/// * `output_path` - PDF 保存路径
/// * `_dimensions` - 页面尺寸（WebView2 基础 API 暂不完全支持）
///
/// # 平台
/// 此函数仅在 Windows 上有效。在其他平台上返回错误。
#[cfg(target_os = "windows")]
pub async fn print_native_windows<R: Runtime>(
    _app: AppHandle<R>,
    window: WebviewWindow<R>,
    content: String,
    output_path: PathBuf,
    _dimensions: Option<PageDimensions>,
) -> Result<String, String> {
    use webview2_com::Microsoft::Web::WebView2::Win32::{
        ICoreWebView2_7, ICoreWebView2PrintToPdfCompletedHandler,
        ICoreWebView2PrintToPdfCompletedHandler_Impl,
    };
    use windows_core::{Interface, HRESULT, PCWSTR, BOOL, implement};
    use std::sync::mpsc;
    use std::os::windows::ffi::OsStrExt;
    use std::time::Duration;
    use std::thread;

    // PrintToPdf 完成回调处理器
    #[implement(ICoreWebView2PrintToPdfCompletedHandler)]
    struct PrintCallback {
        tx: mpsc::Sender<Result<(), String>>,
    }

    impl ICoreWebView2PrintToPdfCompletedHandler_Impl for PrintCallback_Impl {
        fn Invoke(&self, error_code: HRESULT, is_successful: BOOL) -> windows_core::Result<()> {
            let result = if error_code.is_ok() && is_successful.as_bool() {
                Ok(())
            } else {
                Err(format!(
                    "WebView2 PrintToPdf 失败。HRESULT: 0x{:X}, Success: {:?}",
                    error_code.0, is_successful
                ))
            };
            let _ = self.tx.send(result);
            Ok(())
        }
    }

    let (result_tx, result_rx) = mpsc::channel();
    let output_path_clone = output_path.clone();
    let content_clone = content.clone();

    // 加载内容并打印
    window.with_webview(move |webview| {
        unsafe {
            // 获取 WebView2 接口
            let webview2: ICoreWebView2_7 = match webview.controller().CoreWebView2() {
                Ok(wv) => match wv.cast() {
                    Ok(wv7) => wv7,
                    Err(e) => {
                        let _ = result_tx.send(Err(format!("转换到 ICoreWebView2_7 失败: {}", e)));
                        return;
                    }
                },
                Err(e) => {
                    let _ = result_tx.send(Err(format!("获取 CoreWebView2 失败: {}", e)));
                    return;
                }
            };

            // 导航到内容
            let is_url = content_clone.starts_with("http://") 
                || content_clone.starts_with("https://") 
                || content_clone.starts_with("file://");
            
            let content_wide: Vec<u16> = content_clone.encode_utf16().chain(std::iter::once(0)).collect();
            
            let nav_result = if is_url {
                webview2.Navigate(PCWSTR(content_wide.as_ptr()))
            } else {
                webview2.NavigateToString(PCWSTR(content_wide.as_ptr()))
            };

            if let Err(e) = nav_result {
                let _ = result_tx.send(Err(format!("导航失败: {}", e)));
                return;
            }

            // 等待内容加载（简单延时方式）
            // TODO: 使用 NavigationCompleted 事件实现更可靠的处理
            thread::sleep(Duration::from_millis(CONTENT_LOAD_WAIT_MS));

            // 创建打印回调
            let callback = PrintCallback { tx: result_tx };
            let handler: ICoreWebView2PrintToPdfCompletedHandler = callback.into();

            // 转换路径为宽字符串
            let path_wide: Vec<u16> = output_path_clone
                .as_os_str()
                .encode_wide()
                .chain(std::iter::once(0))
                .collect();

            // 执行 PrintToPdf
            if let Err(e) = webview2.PrintToPdf(PCWSTR(path_wide.as_ptr()), None, &handler) {
                // 注意：result_tx 已移入回调，此处无法发送错误
                // 回调会处理错误
                log::error!("PrintToPdf 调用失败: {}", e);
            }
        }
    }).map_err(|e| e.to_string())?;

    // 等待结果，带超时
    match result_rx.recv_timeout(Duration::from_secs(PRINT_TIMEOUT_SECS)) {
        Ok(Ok(())) => Ok(output_path.to_string_lossy().to_string()),
        Ok(Err(e)) => Err(e),
        Err(_) => Err(format!("PrintToPdf 超时（{}秒）", PRINT_TIMEOUT_SECS)),
    }
}

#[cfg(not(target_os = "windows"))]
pub async fn print_native_windows<R: Runtime>(
    _app: AppHandle<R>,
    _window: WebviewWindow<R>,
    _content: String,
    _output_path: PathBuf,
    _dimensions: Option<PageDimensions>,
) -> Result<String, String> {
    Err("Windows 原生打印仅在 Windows 上可用。".to_string())
}
