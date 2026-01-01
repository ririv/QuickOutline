use tauri::{AppHandle, Runtime, WebviewWindow, WebviewWindowBuilder, WebviewUrl};
use std::path::PathBuf;
use log::{info, warn, error};
use uuid::Uuid;

#[cfg(target_os = "linux")]
mod impl_linux {
    use super::*;
    use webkit2gtk::{PrintOperation, WebViewExt, PrintOperationExt};
    use gtk::{PrintSettings, PrintOperationAction};
    use gtk::prelude::*; 
    use gtk::glib;
    use std::sync::{Arc, Mutex};

    pub async fn execute<R: Runtime>(app: AppHandle<R>, html: String, output_path: PathBuf) -> Result<String, String> {
        let label = format!("print_hidden_{}", Uuid::new_v4());
        
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
            // In Tauri v2 Linux, webview is PlatformWebview. 
            // .inner() returns the webkit2gtk::WebView
            let webview = webview.inner();
            
            let tx_clone = tx.clone();
            let path = output_path_clone.clone();
            
            webview.connect_load_changed(move |wv, event| {
                if event == webkit2gtk::LoadEvent::Finished {
                    let op = PrintOperation::new(wv);
                    let settings = PrintSettings::new();
                    
                    let uri = format!("file://{}", path.to_string_lossy());
                    settings.set(gtk::PRINT_SETTINGS_OUTPUT_URI, Some(&uri));
                    settings.set(gtk::PRINT_SETTINGS_PRINTER, Some("Print to File"));
                    
                    op.set_print_settings(&settings);
                    // op.set_action(PrintOperationAction::Export); // Removed as it might not exist on WebKitPrintOperation
                    
                    let tx_inner = tx_clone.clone();
                    op.connect_finished(move |_| {
                        if let Ok(sender) = tx_inner.lock() {
                            let _ = sender.send(Ok(()));
                        }
                    });
                    
                    let tx_inner_err = tx_clone.clone();
                    op.connect_failed(move |_, error: &glib::Error| {
                         if let Ok(sender) = tx_inner_err.lock() {
                            let _ = sender.send(Err(error.to_string()));
                        }
                    });
                    
                    op.print(); // Use print() for silent printing
                }
            });
            
            if html_clone.starts_with("http://") || html_clone.starts_with("https://") || html_clone.starts_with("file://") {
                webview.load_uri(&html_clone);
            } else {
                webview.load_html(&html_clone, None);
            }
            
        }).map_err(|e| e.to_string())?;

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