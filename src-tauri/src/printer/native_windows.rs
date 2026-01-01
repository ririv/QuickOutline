use std::path::PathBuf;
use tauri::{AppHandle, Runtime, WebviewWindow};

#[cfg(target_os = "windows")]
mod impl_win {
    use super::*;
    use std::os::windows::ffi::OsStrExt;
    use std::sync::mpsc::Sender;
    use webview2_com::Microsoft::Web::WebView2::Win32::{
        ICoreWebView2, ICoreWebView2Controller, ICoreWebView2PrintToPdfCompletedHandler,
        ICoreWebView2PrintToPdfCompletedHandler_Impl, ICoreWebView2_7,
    };
    use windows::core::{implement, IUnknown, Interface, HRESULT, PCWSTR};
    use windows::Win32::Foundation::BOOL;

    #[implement(ICoreWebView2PrintToPdfCompletedHandler)]
    pub struct PrintCallback {
        pub tx: Sender<Result<(), String>>,
    }

    impl ICoreWebView2PrintToPdfCompletedHandler_Impl for PrintCallback {
        fn Invoke(&self, error_code: HRESULT, is_successful: BOOL) -> windows::core::Result<()> {
            if error_code.is_ok() && is_successful.as_bool() {
                let _ = self.tx.send(Ok(()));
            } else {
                let _ = self.tx.send(Err(format!("WebView2 PrintToPdf failed. HRESULT: 0x{:X}, Success: {:?}", error_code.0, is_successful)));
            }
            Ok(())
        }
    }

    pub async fn execute<R: Runtime>(app: AppHandle<R>, html: String, output_path: PathBuf) -> Result<String, String> {
        let label = format!("print_hidden_{}", uuid::Uuid::new_v4());
        
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

        // Use a flag to track if we successfully initiated the print
        let (init_tx, init_rx) = std::sync::mpsc::channel();

        hidden_window.with_webview(move |webview| {
            unsafe {
                // 1. Get CoreWebView2
                // In Tauri v2 Windows, the `webview` argument is `ICoreWebView2Controller`
                let controller: ICoreWebView2Controller = std::mem::transmute(webview);
                let core_webview: ICoreWebView2 = match controller.CoreWebView2() {
                    Ok(c) => c,
                    Err(e) => {
                        let _ = init_tx.send(Err(format!("Failed to get CoreWebView2: {}", e)));
                        return;
                    }
                };

                // 2. Navigate
                let is_url = html_clone.starts_with("http://") || html_clone.starts_with("https://") || html_clone.starts_with("file://");
                let content_wide: Vec<u16> = html_clone.encode_utf16().chain(std::iter::once(0)).collect();
                
                let nav_result = if is_url {
                    core_webview.Navigate(PCWSTR::from_raw(content_wide.as_ptr()))
                } else {
                    core_webview.NavigateToString(PCWSTR::from_raw(content_wide.as_ptr()))
                };

                if let Err(e) = nav_result {
                     let _ = init_tx.send(Err(format!("Navigation failed: {}", e)));
                     return;
                }
                
                // 4. Cast to ICoreWebView2_7
                let core_webview7: ICoreWebView2_7 = match core_webview.cast() {
                    Ok(c) => c,
                    Err(e) => {
                        let _ = init_tx.send(Err(format!("Failed to cast to ICoreWebView2_7: {}", e)));
                        return;
                    }
                };

                // 6. Setup Callback
                let callback = PrintCallback { tx };
                let handler: ICoreWebView2PrintToPdfCompletedHandler = callback.into();

                let path_wide: Vec<u16> = output_path_clone.as_os_str().encode_wide().chain(std::iter::once(0)).collect();
                
                if let Err(e) = core_webview7.PrintToPdf(PCWSTR::from_raw(path_wide.as_ptr()), None, &handler) {
                     let _ = init_tx.send(Err(format!("PrintToPdf call failed: {}", e)));
                     return;
                }

                let _ = init_tx.send(Ok(()));
            }
        }).map_err(|e| e.to_string())?; // with_webview error

        // Check if initialization failed
        if let Ok(Err(e)) = init_rx.recv() {
            let _ = hidden_window.close();
            return Err(e);
        }

        // Wait for Print Completion
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
