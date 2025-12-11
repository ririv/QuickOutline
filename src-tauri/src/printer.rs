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
async fn print_native_mac<R: Runtime>(window: WebviewWindow<R>, html: String, output_path: PathBuf) -> Result<String, String> {

    use objc2_foundation::{NSData, NSError, NSString, NSRect, NSPoint, NSSize};
    use objc2_web_kit::{WKPDFConfiguration, WKWebView, WKWebViewConfiguration};
    use block2::RcBlock;
    use std::sync::mpsc;
    use objc2::rc::Retained;
    use objc2::{MainThreadMarker, msg_send};
    use objc2::runtime::AnyObject;
    use std::thread;
    use std::time::Duration;

    let path_str = output_path.to_string_lossy().to_string();
    let (ptr_tx, ptr_rx) = mpsc::channel();
    
    // Step 1: Initialize, Attach, and Load
    let html_clone = html.clone();
    
    window.with_webview(move |webview| {
        unsafe {
            let mtm = MainThreadMarker::new_unchecked();
            
            let frame = NSRect::new(NSPoint::new(0.0, 0.0), NSSize::new(595.0, 842.0));
            let config = WKWebViewConfiguration::new(mtm);
            
            // Fix: Use msg_send! for init
            let alloc_view = mtm.alloc::<WKWebView>();
            let new_view: Retained<WKWebView> = msg_send![alloc_view, initWithFrame: frame, configuration: &*config];
            
            // Attach (Hidden)
            let current_wv_ptr = webview.inner() as *mut AnyObject;
            let current_wv = &*current_wv_ptr;
            let superview: Option<Retained<AnyObject>> = msg_send![current_wv, superview];
            
            if let Some(sv) = superview {
                let _: () = msg_send![&sv, addSubview: &*new_view];
                let _: () = msg_send![&*new_view, setAlphaValue: 0.0f64];
            } else {
                println!("Warning: Could not find superview to attach print webview.");
            }
            
            // Load
            let html_ns = NSString::from_str(&html_clone);
            new_view.loadHTMLString_baseURL(&html_ns, None);
            
            // Pass pointer out
            // Fix: into_raw returns *mut T
            let raw: *mut WKWebView = Retained::into_raw(new_view);
            let addr = raw as usize;
            let _ = ptr_tx.send(addr);
        }
    }).map_err(|e| e.to_string())?;

    // Step 2: Wait for content to load
    let addr = ptr_rx.recv().map_err(|_| "Failed to create webview".to_string())?;
    // Sleep 1s to allow rendering
    thread::sleep(Duration::from_millis(1000));

    // Step 3: Print
    let (result_tx, result_rx) = mpsc::channel();
    let output_path_clone = output_path.clone();
    let path_str_clone = path_str.clone();

    window.with_webview(move |_webview| {
        unsafe {
            let mtm = MainThreadMarker::new_unchecked();
            
            // Reconstruct Retained
            // Fix: Pass raw ptr and explicit type
            let ptr = addr as *mut WKWebView;
            let target_webview: Retained<WKWebView> = Retained::from_raw(ptr).expect("Invalid webview pointer");

            let pdf_config = WKPDFConfiguration::new(mtm);
            let webview_for_block = target_webview.clone();

            let completion_handler = RcBlock::new(move |pdf_data: *mut NSData, error: *mut NSError| {
                // Remove from superview
                let _: () = msg_send![&*webview_for_block, removeFromSuperview];
                // webview_for_block is kept alive by the closure capture and dropped when the block is released

                if !error.is_null() {
                    let error_obj: &NSError = unsafe { &*error };
                    let desc = error_obj.localizedDescription();
                    let domain = error_obj.domain();
                    let code = error_obj.code();
                    let err_msg = format!("PDF creation failed: {} (Domain: {}, Code: {})", desc, domain, code);
                    let _ = result_tx.send(Err(err_msg));
                    return;
                }

                if pdf_data.is_null() {
                    let _ = result_tx.send(Err("PDF creation failed: No data returned".to_string()));
                    return;
                }

                // NSData processing
                let data: &NSData = &*pdf_data;
                // Fix: Use msg_send for bytes and length
                let ptr: *const std::ffi::c_void = msg_send![data, bytes];
                let len: usize = msg_send![data, length];
                
                let ptr = ptr as *const u8;
                let data_slice = std::slice::from_raw_parts(ptr, len);

                match std::fs::write(&output_path_clone, data_slice) {
                    Ok(_) => {
                        println!("Native PDF generated at: {:?}", output_path_clone);
                        let _ = result_tx.send(Ok(path_str_clone.clone()));
                    },
                    Err(e) => {
                        let _ = result_tx.send(Err(e.to_string()));
                    }
                }
            });

            target_webview.createPDFWithConfiguration_completionHandler(Some(&pdf_config), &*completion_handler);
        }
    }).map_err(|e| e.to_string())?;

    result_rx.recv().map_err(|e| e.to_string())?
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