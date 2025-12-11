use tauri::{AppHandle, Manager, Runtime, WebviewWindow}; // Import WebviewWindow
use std::path::PathBuf;
use std::fs;

#[tauri::command]
pub async fn print_to_pdf<R: Runtime>(
    app: AppHandle<R>,
    window: WebviewWindow<R>, // Use WebviewWindow
    html: String,
    filename: String,
) -> Result<String, String> {
    let output_path = app.path().app_data_dir()
        .map_err(|e| e.to_string())?
        .join(&filename);

    if let Some(parent) = output_path.parent() {
        fs::create_dir_all(parent).map_err(|e| e.to_string())?;
    }

    // Platform specific implementations
    #[cfg(target_os = "windows")]
    {
        // TODO: Implement Windows Native PrintToPdf using WebView2 API
        Err("Windows Native PrintToPdf not implemented yet.".to_string())
    }

    #[cfg(target_os = "linux")]
    {
        // TODO: Implement Linux Native PrintToPdf using WebKitGTK PrintOperation API
        Err("Linux Native PrintToPdf not implemented yet.".to_string())
    }

    #[cfg(target_os = "macos")]
    {
        match print_native_mac(window.clone(), html.clone(), output_path.clone()).await {
            Ok(path) => Ok(path),
            Err(e) => {
                println!("Native print failed: {}", e);
                Err(e)
            }
        }
    }
}

// ================= MAC OS NATIVE =================
#[cfg(target_os = "macos")]
async fn print_native_mac<R: Runtime>(window: WebviewWindow<R>, _html: String, output_path: PathBuf) -> Result<String, String> {

    use objc2_foundation::{NSData, NSError};
    use objc2_web_kit::{WKPDFConfiguration, WKWebView};
    use block2::RcBlock;
    use std::sync::mpsc;

    let path_str = output_path.to_string_lossy().to_string();
    let (tx, rx) = mpsc::channel();
    let output_path_clone = output_path.clone();

    // Use with_webview which is available on WebviewWindow
    window.with_webview(move |webview| {
        unsafe {
            // Tauri's webview.inner() returns the platform handle. On macOS it's the WKWebView pointer.
            // We need to cast it to the correct type for objc2.
            // The type `tauri::webview::Webview` has `inner()` method.
            let wk_webview_ptr = webview.inner() as *mut WKWebView;
            let wk_webview: &WKWebView = &*wk_webview_ptr;

            // Create Configuration
            // WKPDFConfiguration::new() should be available if objc2-web-kit maps it properly.
            // If not, we use msg_send!. But the error log showed the struct exists.
            let config = WKPDFConfiguration::new();

            // Define Completion Block
            let completion_handler = RcBlock::new(move |pdf_data: *mut NSData, error: *mut NSError| {
                if !error.is_null() {
                    let _ = tx.send(Err("PDF creation failed: NSError occurred".to_string()));
                    return;
                }

                if pdf_data.is_null() {
                    let _ = tx.send(Err("PDF creation failed: No data returned".to_string()));
                    return;
                }

                // NSData processing
                let data: &NSData = &*pdf_data;
                // .bytes() in objc2-foundation 0.2 likely returns &[u8] or similar safe wrapper?
                // The error said "casting `&[u8]` as `*const u8` is invalid", which implies `data.bytes()` returned `&[u8]`.
                // So we can just use it directly or get pointer.
                // Wait, if it returns &[u8], we don't need `length`, we can just write it!
                let data_slice = data.bytes();

                match std::fs::write(&output_path_clone, data_slice) {
                    Ok(_) => {
                        println!("Native PDF generated at: {:?}", output_path_clone);
                        let _ = tx.send(Ok(path_str.clone()));
                    },
                    Err(e) => {
                        let _ = tx.send(Err(e.to_string()));
                    }
                }
            });

            // Call createPDFWithConfiguration
            // Explicitly dereference RcBlock to Block. The method expects &Block, not Option<&Block>.
            wk_webview.createPDFWithConfiguration_completionHandler(Some(&config), &*completion_handler);
        }
    }).map_err(|e| e.to_string())?;

    rx.recv().map_err(|e| e.to_string())?
}

#[cfg(not(target_os = "windows"))]
async fn print_windows(_html: String, _output_path: PathBuf) -> Result<String, String> {
    unimplemented!("Windows native print is not implemented yet.");
}

// ================= LINUX (Placeholder) =================
#[cfg(not(target_os = "linux"))]
async fn print_linux(_html: String, _output_path: PathBuf) -> Result<String, String> {
    unimplemented!("Linux native print is not implemented yet.");
}