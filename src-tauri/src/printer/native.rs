use tauri::{AppHandle, Manager, Runtime, WebviewWindow}; // Import WebviewWindow
use std::path::PathBuf;
use std::fs;
use log::{info, warn, error};

#[tauri::command]
pub async fn print_to_pdf_with_html_string_native<R: Runtime>(
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
        // Switch to NSPrintOperation based printing for better consistency
        // No fallback as requested
        match print_native_with_html_mac_op(window.clone(), html.clone(), output_path.clone()).await {
            Ok(path) => Ok(path),
            Err(e) => {
                error!("Native print (OP) failed: {}", e);
                Err(e)
            }
        }
    }
}

pub async fn print_to_pdf_with_url_native<R: Runtime>(
    app: AppHandle<R>,
    window: WebviewWindow<R>,
    url: String,
    output_path: PathBuf,
) -> Result<String, String> {
    if let Some(parent) = output_path.parent() {
        fs::create_dir_all(parent).map_err(|e| e.to_string())?;
    }

    #[cfg(target_os = "macos")]
    {
        // Use WKPDFConfiguration (modern) as default for URL printing
        match print_native_with_url_mac_wkpdf(window, url, output_path).await {
             Ok(path) => Ok(path),
             Err(e) => {
                 error!("Native print (WKPDF) failed: {}. No fallback configured.", e);
                 Err(e)
             }
        }
    }
    
    #[cfg(not(target_os = "macos"))]
    {
        Err("Native URL printing not implemented for this platform.".to_string())
    }
}

// ================= MAC OS NATIVE (WKPDFConfiguration - URL) =================
#[cfg(target_os = "macos")]
pub async fn print_native_with_url_mac_wkpdf<R: Runtime>(window: WebviewWindow<R>, url: String, output_path: PathBuf) -> Result<String, String> {

    use objc2_foundation::{NSData, NSError, NSString, NSRect, NSPoint, NSSize, NSURL, NSURLRequest};
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
    let url_clone = url.clone();
    
    window.with_webview(move |webview| {
        unsafe {
            let mtm = MainThreadMarker::new_unchecked();
            
            let frame = NSRect::new(NSPoint::new(0.0, 0.0), NSSize::new(595.0, 842.0));
            let config = WKWebViewConfiguration::new(mtm);
            
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
                warn!("Warning: Could not find superview to attach print webview.");
            }
            
            // Load Request

            // Load Request
            let url_ns = NSString::from_str(&url_clone);
            let ns_url = NSURL::URLWithString(&url_ns).expect("Invalid URL");
            let request = NSURLRequest::requestWithURL(&ns_url);
            
            new_view.loadRequest(&request);
            
            // Pass pointer out
            let raw: *mut WKWebView = Retained::into_raw(new_view);
            let addr = raw as usize;
            let _ = ptr_tx.send(addr);
        }
    }).map_err(|e| e.to_string())?;

    // Step 2: Wait for content to load
    let addr = ptr_rx.recv().map_err(|_| "Failed to create webview".to_string())?;
    // Sleep to allow rendering (TODO: implement navigation delegate for robustness)
    thread::sleep(Duration::from_millis(2000));

    // Step 3: Print
    let (result_tx, result_rx) = mpsc::channel();
    let output_path_clone = output_path.clone();
    let path_str_clone = path_str.clone();

    window.with_webview(move |_webview| {
        unsafe {
            let mtm = MainThreadMarker::new_unchecked();
            
            // Reconstruct Retained
            let ptr = addr as *mut WKWebView;
            let target_webview: Retained<WKWebView> = Retained::from_raw(ptr).expect("Invalid webview pointer");

            let pdf_config = WKPDFConfiguration::new(mtm);
            let webview_for_block = target_webview.clone();

            let completion_handler = RcBlock::new(move |pdf_data: *mut NSData, error: *mut NSError| {
                // Remove from superview
                let _: () = msg_send![&*webview_for_block, removeFromSuperview];

                if !error.is_null() {
                    let error_obj: &NSError = &*error;
                    let desc = error_obj.localizedDescription();
                    let err_msg = format!("PDF creation failed: {}", desc);
                    let _ = result_tx.send(Err(err_msg));
                    return;
                }

                if pdf_data.is_null() {
                    let _ = result_tx.send(Err("PDF creation failed: No data returned".to_string()));
                    return;
                }

                // NSData processing
                let data: &NSData = &*pdf_data;
                let ptr: *const std::ffi::c_void = msg_send![data, bytes];
                let len: usize = msg_send![data, length];
                
                let ptr = ptr as *const u8;
                let data_slice = std::slice::from_raw_parts(ptr, len);

                match std::fs::write(&output_path_clone, data_slice) {
                    Ok(_) => {
                        info!("Native PDF generated at: {:?}", output_path_clone);
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

// ================= MAC OS NATIVE (NSPrintOperation - URL) =================
#[cfg(target_os = "macos")]
pub async fn print_native_with_url_mac_op<R: Runtime>(window: WebviewWindow<R>, url: String, output_path: PathBuf) -> Result<String, String> {
    use objc2_foundation::{NSString, NSRect, NSPoint, NSSize, NSURL, NSURLRequest};
    use objc2_web_kit::{WKWebView, WKWebViewConfiguration};
    use objc2_app_kit::{NSPrintInfo, NSPrintOperation, NSPrinter};
    use objc2::rc::Retained;
    use objc2::{MainThreadMarker, msg_send};
    use objc2::runtime::{AnyObject, AnyClass};
    use std::sync::mpsc;
    use std::thread;
    use std::time::Duration;
    use std::ffi::{c_void, CStr};

    let path_str = output_path.to_string_lossy().to_string();
    let (ptr_tx, ptr_rx) = mpsc::channel();

    // Step 1: Initialize, Attach, and Load
    let url_clone = url.clone();

    window.with_webview(move |webview| {
        unsafe {
            let mtm = MainThreadMarker::new_unchecked();

            let frame = NSRect::new(NSPoint::new(0.0, 0.0), NSSize::new(595.0, 842.0));
            let config = WKWebViewConfiguration::new(mtm);

            let alloc_view = mtm.alloc::<WKWebView>();
            let new_view: Retained<WKWebView> = msg_send![alloc_view, initWithFrame: frame, configuration: &*config];

            // Attach (Hidden)
            let current_wv_ptr = webview.inner() as *mut AnyObject;
            let current_wv = &*current_wv_ptr;
            let superview: Option<Retained<AnyObject>> = msg_send![current_wv, superview];

            if let Some(sv) = superview {
                let _: () = msg_send![&sv, addSubview: &*new_view];
                let _: () = msg_send![&*new_view, setAlphaValue: 0.0f64];
            }

            // Load Request
            let url_ns = NSString::from_str(&url_clone);
            // Create NSURL
            let ns_url = NSURL::URLWithString(&url_ns).expect("Invalid URL");
            // Create NSURLRequest
            let request = NSURLRequest::requestWithURL(&ns_url);
            
            new_view.loadRequest(&request);

            // Pass pointer out
            let raw: *mut WKWebView = Retained::into_raw(new_view);
            let addr = raw as usize;
            let _ = ptr_tx.send(addr);
        }
    }).map_err(|e| e.to_string())?;

    // Step 2: Wait for content to load
    let addr = ptr_rx.recv().map_err(|_| "Failed to create webview".to_string())?;
    // Increase wait time slightly for network request (even local)
    thread::sleep(Duration::from_millis(2000));

    // Step 3: Print
    let (result_tx, result_rx) = mpsc::channel();
    let output_path_clone = output_path.clone();
    let path_str_clone = path_str.clone();

    window.with_webview(move |_webview| {
        unsafe {
            let mtm = MainThreadMarker::new_unchecked();

            let ptr = addr as *mut WKWebView;
            let target_webview: Retained<WKWebView> = Retained::from_raw(ptr).expect("Invalid webview pointer");

            // ========== Create Independent NSPrintInfo ==========
            let alloc_print_info = mtm.alloc::<NSPrintInfo>();
            let print_info: Retained<NSPrintInfo> = msg_send![alloc_print_info, init];

            let dict = print_info.dictionary();

            // Set Disposition: Save
            let k_disposition = NSString::from_str("NSPrintJobDisposition");
            let v_save = NSString::from_str("NSPrintSaveJob");
            let _: () = msg_send![&dict, setObject: &*v_save, forKey: &*k_disposition];

            // Set Save Path
            let k_url = NSString::from_str("NSPrintJobSavingURL");
            let output_path_str = output_path_clone.to_string_lossy();
            let v_url = NSURL::fileURLWithPath(&NSString::from_str(&output_path_str));
            let _: () = msg_send![&dict, setObject: &*v_url, forKey: &*k_url];

            // Set Generic Printer
            let k_printer_name = NSString::from_str("Generic");
            let cls_name = CStr::from_bytes_with_nul(b"NSPrinter\0").unwrap();
            let printer_class = AnyClass::get(cls_name).expect("NSPrinter class not found");
            let printer: Option<Retained<NSPrinter>> = msg_send![printer_class, printerWithName: &*k_printer_name];
            
            if let Some(p) = printer {
                 let _: () = msg_send![&print_info, setPrinter: &*p];
            }

            print_info.setScalingFactor(1.0);
            let paper_size = NSSize::new(595.0, 842.0); 
            print_info.setPaperSize(paper_size);
            print_info.setLeftMargin(0.0);
            print_info.setRightMargin(0.0);
            print_info.setTopMargin(0.0);
            print_info.setBottomMargin(0.0);
            let _: () = msg_send![&print_info, setOrientation: 0isize];
            print_info.setVerticallyCentered(false);
            print_info.setHorizontallyCentered(false);
            // ========== End NSPrintInfo ==========

            let print_op: Retained<NSPrintOperation> = target_webview.printOperationWithPrintInfo(&print_info);
            print_op.setShowsPrintPanel(false);
            print_op.setShowsProgressPanel(false);

            let ns_window: Option<Retained<AnyObject>> = msg_send![&*target_webview, window];

            let result = if let Some(win) = ns_window {
                let null_ptr: *mut c_void = std::ptr::null_mut();
                let _: () = msg_send![
                    &print_op,
                    runOperationModalForWindow: &*win,
                    delegate: null_ptr,
                    didRunSelector: null_ptr,
                    contextInfo: null_ptr
                ];

                if output_path_clone.exists() {
                    Ok(path_str_clone.clone())
                } else {
                    Err("Native print operation finished but file not found.".to_string())
                }
            } else {
                let success: bool = print_op.runOperation();
                if success && output_path_clone.exists() {
                    Ok(path_str_clone.clone())
                } else {
                    Err("NSPrintOperation failed or no window context.".to_string())
                }
            };

            let _: () = msg_send![&target_webview, removeFromSuperview];
            let _ = result_tx.send(result);
        }
    }).map_err(|e| e.to_string())?;

    result_rx.recv().map_err(|e| e.to_string())?
}

// ================= MAC OS NATIVE (NSPrintOperation) =================
#[cfg(target_os = "macos")]
pub async fn print_native_with_html_mac_op<R: Runtime>(window: WebviewWindow<R>, html: String, output_path: PathBuf) -> Result<String, String> {
    use objc2_foundation::{NSString, NSRect, NSPoint, NSSize, NSURL};
    use objc2_web_kit::{WKWebView, WKWebViewConfiguration};
    use objc2_app_kit::{NSPrintInfo, NSPrintOperation};
    use objc2::rc::Retained;
    use objc2::{MainThreadMarker, msg_send};
    use objc2::runtime::AnyObject;
    use std::sync::mpsc;
    use std::thread;
    use std::time::Duration;
    use std::ffi::c_void;

    let path_str = output_path.to_string_lossy().to_string();
    let (ptr_tx, ptr_rx) = mpsc::channel();

    // Step 1: Initialize, Attach, and Load
    let html_clone = html.clone();

    window.with_webview(move |webview| {
        unsafe {
            let mtm = MainThreadMarker::new_unchecked();

            let frame = NSRect::new(NSPoint::new(0.0, 0.0), NSSize::new(595.0, 842.0));
            let config = WKWebViewConfiguration::new(mtm);

            let alloc_view = mtm.alloc::<WKWebView>();
            let new_view: Retained<WKWebView> = msg_send![alloc_view, initWithFrame: frame, configuration: &*config];

            // Attach (Hidden)
            let current_wv_ptr = webview.inner() as *mut AnyObject;
            let current_wv = &*current_wv_ptr;
            let superview: Option<Retained<AnyObject>> = msg_send![current_wv, superview];

            if let Some(sv) = superview {
                let _: () = msg_send![&sv, addSubview: &*new_view];
                let _: () = msg_send![&*new_view, setAlphaValue: 0.0f64];
            }

            // Load
            let html_ns = NSString::from_str(&html_clone);
            new_view.loadHTMLString_baseURL(&html_ns, None);

            // Pass pointer out
            let raw: *mut WKWebView = Retained::into_raw(new_view);
            let addr = raw as usize;
            let _ = ptr_tx.send(addr);
        }
    }).map_err(|e| e.to_string())?;

    // Step 2: Wait for content to load
    let addr = ptr_rx.recv().map_err(|_| "Failed to create webview".to_string())?;
    thread::sleep(Duration::from_millis(1000));

    // Step 3: Print
    let (result_tx, result_rx) = mpsc::channel();
    let output_path_clone = output_path.clone();
    let path_str_clone = path_str.clone();

    window.with_webview(move |_webview| {
        unsafe {
            let mtm = MainThreadMarker::new_unchecked();

            let ptr = addr as *mut WKWebView;
            let target_webview: Retained<WKWebView> = Retained::from_raw(ptr).expect("Invalid webview pointer");

            // ========== 关键修改开始：创建并配置独立的 NSPrintInfo ==========
            // 创建全新的 NSPrintInfo 实例，而不是使用共享实例
            let alloc_print_info = mtm.alloc::<NSPrintInfo>();
            let print_info: Retained<NSPrintInfo> = msg_send![alloc_print_info, init];

            // 获取其字典并进行配置
            let dict = print_info.dictionary();

            // 设置作业处置方式为"保存"
            let k_disposition = NSString::from_str("NSPrintJobDisposition");
            let v_save = NSString::from_str("NSPrintSaveJob");
            let _: () = msg_send![&dict, setObject: &*v_save, forKey: &*k_disposition];

            // 设置保存路径 - 确保路径是完整的文件路径
            let k_url = NSString::from_str("NSPrintJobSavingURL");
            let output_path_str = output_path_clone.to_string_lossy();
            let v_url = NSURL::fileURLWithPath(&NSString::from_str(&output_path_str));
            let _: () = msg_send![&dict, setObject: &*v_url, forKey: &*k_url];

            // 尝试绕过无默认打印机错误：指定一个通用的虚拟打印机名称
            let k_printer_name = NSString::from_str("Generic");
            
            // 引入 NSPrinter 和 AnyClass
            use objc2_app_kit::NSPrinter;
            use objc2::runtime::AnyClass;
            use std::ffi::CStr;
            
            // 获取 NSPrinter 类
            let cls_name = CStr::from_bytes_with_nul(b"NSPrinter\0").unwrap();
            let printer_class = AnyClass::get(cls_name).expect("NSPrinter class not found");
            
            // 尝试创建 NSPrinter 对象
            // 注意：printerWithName 返回 Option<Retained<NSPrinter>>
            let printer: Option<Retained<NSPrinter>> = msg_send![printer_class, printerWithName: &*k_printer_name];
            
            if let Some(p) = printer {
                 let _: () = msg_send![&print_info, setPrinter: &*p];
            } else {
                 // 如果找不到 "Generic"，尝试其他名称或者忽略
                 warn!("Warning: Could not create 'Generic' printer.");
            }

            // 显式设置缩放比例，防止查询默认缩放
            print_info.setScalingFactor(1.0);

            // 关键修复：手动填充缺失的默认值，防止系统查询默认打印机
            // Set A4 Paper Size (595 x 842 points)
            let paper_size = NSSize::new(595.0, 842.0); 
            print_info.setPaperSize(paper_size);
            
            // Set Margins to 0
            print_info.setLeftMargin(0.0);
            print_info.setRightMargin(0.0);
            print_info.setTopMargin(0.0);
            print_info.setBottomMargin(0.0);
            
            // Set Orientation (Portrait)
            // NSPrintingOrientation: 0 = Portrait, 1 = Landscape
            let _: () = msg_send![&print_info, setOrientation: 0isize]; // Use 0 for Portrait

            // 可选：设置其他打印参数
            print_info.setVerticallyCentered(false);
            print_info.setHorizontallyCentered(false);
            // ========== 关键修改结束 ==========

            // 使用配置好的独立 print_info 创建打印操作
            let print_op: Retained<NSPrintOperation> = target_webview.printOperationWithPrintInfo(&print_info);

            // 配置为静默打印（无对话框）
            print_op.setShowsPrintPanel(false);
            print_op.setShowsProgressPanel(false);

            // 获取窗口（WebView应已附加到窗口）
            let ns_window: Option<Retained<AnyObject>> = msg_send![&*target_webview, window];

            let result = if let Some(win) = ns_window {
                // 使用 runOperationModalForWindow 方法（更可靠）
                let null_ptr: *mut c_void = std::ptr::null_mut();
                let _: () = msg_send![
                    &print_op,
                    runOperationModalForWindow: &*win,
                    delegate: null_ptr,
                    didRunSelector: null_ptr,
                    contextInfo: null_ptr
                ];

                // 检查文件是否已创建
                if output_path_clone.exists() {
                    Ok(path_str_clone.clone())
                } else {
                    Err("打印操作完成但未创建PDF文件".to_string())
                }
            } else {
                // 备选方案：直接运行操作（可能不够可靠）
                let success: bool = print_op.runOperation();
                if success && output_path_clone.exists() {
                    Ok(path_str_clone.clone())
                } else {
                    Err("NSPrintOperation 运行失败或无窗口".to_string())
                }
            };

            // 清理：从父视图中移除并释放WebView
            let _: () = msg_send![&target_webview, removeFromSuperview];
            // Retained 对象在作用域结束时自动释放

            let _ = result_tx.send(result);
        }
    }).map_err(|e| e.to_string())?;

    result_rx.recv().map_err(|e| e.to_string())?
}


// ================= MAC OS NATIVE (WKPDFConfiguration - LEGACY) =================
#[cfg(target_os = "macos")]
pub async fn print_native_with_html_mac_wkpdf<R: Runtime>(window: WebviewWindow<R>, html: String, output_path: PathBuf) -> Result<String, String> {

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
                warn!("Warning: Could not find superview to attach print webview.");
            }
            
            // Load Request

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
                    let error_obj: &NSError = &*error;
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
                        info!("Native PDF generated at: {:?}", output_path_clone);
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