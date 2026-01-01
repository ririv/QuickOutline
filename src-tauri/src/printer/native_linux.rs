use tauri::{AppHandle, Runtime, WebviewWindow, WebviewWindowBuilder, WebviewUrl};
use std::path::PathBuf;
use log::{info, warn, error};
use uuid::Uuid;

#[cfg(target_os = "linux")]
mod impl_linux {
    use super::*;
    use webkit2gtk::{PrintOperation, PrintOperationAction, PrintSettings, WebViewExt};
    use gtk::prelude::*; 
    use std::sync::{Arc, Mutex};

    pub async fn execute<R: Runtime>(app: AppHandle<R>, html: String, output_path: PathBuf) -> Result<String, String> {
        let label = format!("print_hidden_{}", uuid::Uuid::new_v4());
        
        // Linux hidden window might need to be visible for WebKit to render?
        // WebKitGTK sometimes optimizes away non-visible rendering.
        // Let's try visible=false first.
        let hidden_window = WebviewWindowBuilder::new(
            &app,
            &label,
            WebviewUrl::App("about:blank".into())
        )
        .visible(false) 
        .build()
        .map_err(|e| e.to_string())?;

        let (tx, rx) = std::sync::mpsc::channel();
        let tx = Arc::new(Mutex::new(tx));
        let output_path_clone = output_path.clone();
        let html_clone = html.clone();

        hidden_window.with_webview(move |webview| {
            // Tauri passes `webkit2gtk::WebView` directly on Linux
            // No need to unsafe cast if the type matches, but `with_webview` signature is generic F: FnOnce(T).
            // T is `webkit2gtk::WebView`.
            
            // Wait, tauri::WebviewWindow::with_webview<F>(&self, f: F) 
            // In Tauri v2, for Linux, the argument IS `webkit2gtk::WebView`.
            // So we can use its methods directly.
            
            let webview = webview.clone(); // It's likely a reference or cloneable object
            
            // Setup Load Handler
            let tx_clone = tx.clone();
            let path = output_path_clone.clone();
            
            // We use a shared signal handler id to disconnect later if needed, 
            // or just rely on window closing.
            
            let load_handler_id = webview.connect_load_changed(move |wv, event| {
                if event == webkit2gtk::LoadEvent::Finished {
                    // Start Print
                    let op = PrintOperation::new(wv);
                    let settings = PrintSettings::new();
                    
                    let uri = format!("file://{}", path.to_string_lossy());
                    settings.set(gtk::PRINT_SETTINGS_OUTPUT_URI, Some(&uri));
                    settings.set(gtk::PRINT_SETTINGS_PRINTER, Some("Print to File"));
                    
                    op.set_print_settings(&settings);
                    op.set_action(PrintOperationAction::Export);
                    
                    let tx_inner = tx_clone.clone();
                    op.connect_finished(move |_, _| {
                        if let Ok(sender) = tx_inner.lock() {
                            let _ = sender.send(Ok(()));
                        }
                    });
                    
                    let tx_inner_err = tx_clone.clone();
                    op.connect_failed(move |_, error| {
                         if let Ok(sender) = tx_inner_err.lock() {
                            let _ = sender.send(Err(error.to_string()));
                        }
                    });
                    
                    // run_dialog is blocking? No, it's async-like in GTK loop.
                    // But for Export action, it might not show dialog.
                    // However, run_dialog requires a parent window. None is fine.
                    // IMPORTANT: Export action usually writes file directly.
                    let _ = op.run_dialog(None::<&gtk::Window>);
                }
            });
            
            // Trigger Load
            if html_clone.starts_with("http://") || html_clone.starts_with("https://") || html_clone.starts_with("file://") {
                webview.load_uri(&html_clone);
            } else {
                webview.load_html(&html_clone, None);
            }
            
        }).map_err(|e| e.to_string())?;

        // Wait
        let result = match rx.recv_timeout(std::time::Duration::from_secs(30)) {
            Ok(Ok(())) => Ok(output_path.to_string_lossy().to_string()),
            Ok(Err(e)) => Err(e),
            Err(_) => Err("PrintToPdf timed out".to_string()),
        };
        
        let _ = hidden_window.close();
        result
    }
}

#[cfg(target_os = "linux")]
pub async fn print_native_linux<R: Runtime>(app: AppHandle<R>, _window: WebviewWindow<R>, html: String, output_path: PathBuf) -> Result<String, String> {
    impl_linux::execute(app, html, output_path).await
}

#[cfg(not(target_os = "linux"))]
pub async fn print_native_linux<R: Runtime>(_app: AppHandle<R>, _window: WebviewWindow<R>, _html: String, _output_path: PathBuf) -> Result<String, String> {
    unimplemented!("Linux native print called on non-Linux platform.");
}
