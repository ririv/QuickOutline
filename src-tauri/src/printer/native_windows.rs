use tauri::{AppHandle, Runtime, WebviewWindow, WebviewWindowBuilder, WebviewUrl};
use std::path::PathBuf;
use log::{info, warn, error};
use uuid::Uuid;

#[cfg(target_os = "windows")]
mod impl_win {
    use super::*;
    use windows_implement::implement;
    use windows_core::{Interface, HRESULT, PCWSTR, BOOL};
    
    use webview2_com::Microsoft::Web::WebView2::Win32::{
        ICoreWebView2, ICoreWebView2_7, ICoreWebView2PrintToPdfCompletedHandler, 
        ICoreWebView2PrintToPdfCompletedHandler_Impl, ICoreWebView2Controller,
    };
    use std::sync::mpsc::Sender;
    use std::os::windows::ffi::OsStrExt;

    #[implement(ICoreWebView2PrintToPdfCompletedHandler)]
    pub struct PrintCallback {
        pub tx: Sender<Result<(), String>>,
    }

    impl ICoreWebView2PrintToPdfCompletedHandler_Impl for PrintCallback_Impl {
        fn Invoke(&self, error_code: HRESULT, is_successful: BOOL) -> windows_core::Result<()> {
            if error_code.is_ok() && is_successful.as_bool() {
                let _ = self.tx.send(Ok(()));
            } else {
                let _ = self.tx.send(Err(format!("WebView2 PrintToPdf failed. HRESULT: 0x{:X}, Success: {:?}", error_code.0, is_successful)));
            }
            Ok(())
        }
    }

    pub async fn execute<R: Runtime>(app: AppHandle<R>, html: String, output_path: PathBuf) -> Result<String, String> {
        let label = format!("print_hidden_{}", Uuid::new_v4());
        
        let hidden_window = WebviewWindowBuilder::new(
            &app,
            &label,
            WebviewUrl::App("about:blank".into())
        )
        .visible(false)
        .skip_taskbar(true)
        .build()
        .map_err(|e| e.to_string())?;

        let (tx, rx) = std::sync::mpsc::channel();
        let output_path_clone = output_path.clone();
        let html_clone = html.clone();

        let (init_tx, init_rx) = std::sync::mpsc::channel();

        hidden_window.with_webview(move |webview| {
            unsafe {
                // Use safe casting instead of raw transmute where possible, or stick to raw cast for private fields
                let controller_ptr = &webview as *const _ as *const ICoreWebView2Controller;
                let controller = &*controller_ptr;
                
                let core_webview: ICoreWebView2 = match controller.CoreWebView2() {
                    Ok(c) => c,
                    Err(e) => {
                        let _ = init_tx.send(Err(format!("Failed to get CoreWebView2: {}", e)));
                        return;
                    }
                };

                let is_url = html_clone.starts_with("http://") || html_clone.starts_with("https://") || html_clone.starts_with("file://");
                let content_wide: Vec<u16> = html_clone.encode_utf16().chain(std::iter::once(0)).collect();
                
                let nav_result = if is_url {
                    core_webview.Navigate(PCWSTR(content_wide.as_ptr()))
                } else {
                    core_webview.NavigateToString(PCWSTR(content_wide.as_ptr()))
                };

                if let Err(e) = nav_result {
                     let _ = init_tx.send(Err(format!("Navigation failed: {}", e)));
                     return;
                }
                
                let core_webview7: ICoreWebView2_7 = match core_webview.cast() {
                    Ok(c) => c,
                    Err(e) => {
                        let _ = init_tx.send(Err(format!("Failed to cast to ICoreWebView2_7: {}", e)));
                        return;
                    }
                };

                let callback = PrintCallback { tx };
                let handler: ICoreWebView2PrintToPdfCompletedHandler = callback.into();

                let path_wide: Vec<u16> = output_path_clone.as_os_str().encode_wide().chain(std::iter::once(0)).collect();
                
                if let Err(e) = core_webview7.PrintToPdf(PCWSTR(path_wide.as_ptr()), None, &handler) {
                     let _ = init_tx.send(Err(format!("PrintToPdf call failed: {}", e)));
                     return;
                }

                let _ = init_tx.send(Ok(()));
            }
        }).map_err(|e| e.to_string())?;

        if let Ok(Err(e)) = init_rx.recv() {
            let _ = hidden_window.close();
            return Err(e);
        }

        let result = match rx.recv_timeout(std::time::Duration::from_secs(30)) {
            Ok(Ok(())) => Ok(output_path.to_string_lossy().to_string()),
            Ok(Err(e)) => Err(e),
            Err(_) => Err("PrintToPdf timed out".to_string()),
        };

        let _ = hidden_window.close();
        result
    }
}

#[cfg(target_os = "windows")]
pub async fn print_native_windows<R: Runtime>(app: AppHandle<R>, _window: WebviewWindow<R>, html: String, output_path: PathBuf) -> Result<String, String> {
    impl_win::execute(app, html, output_path).await
}

#[cfg(not(target_os = "windows"))]
pub async fn print_native_windows<R: Runtime>(_app: AppHandle<R>, _window: WebviewWindow<R>, _html: String, _output_path: PathBuf) -> Result<String, String> {
    unimplemented!("Windows native print called on non-Windows platform.");
}
