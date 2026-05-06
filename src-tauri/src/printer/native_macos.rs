use tauri::{Runtime, WebviewWindow};
use std::path::PathBuf;
use log::{info, warn, error};

use super::native::PageDimensions;

/// mm 转 points 的转换因子 (72 / 25.4)
const MM_TO_POINTS: f64 = 2.83465;
/// URL 内容加载等待时间（毫秒）
const URL_LOAD_WAIT_MS: u64 = 2000;
/// HTML 内容加载等待时间（毫秒）
const HTML_LOAD_WAIT_MS: u64 = 1000;

// ================= MAC OS NATIVE (WKPDFConfiguration - URL) =================
#[cfg(target_os = "macos")]
pub async fn print_native_with_url_mac_wkpdf<R: Runtime>(
    window: WebviewWindow<R>,
    url: String,
    output_path: PathBuf,
    dimensions: Option<PageDimensions>,
) -> Result<String, String> {
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
    let (ptr_tx, ptr_rx) = mpsc::channel::<Result<usize, String>>();
    
    // Step 1: Initialize, Attach, and Load
    let url_clone = url.clone();
    
    window.with_webview(move |webview| {
        unsafe {
            let mtm = MainThreadMarker::new_unchecked();
            
            let (w, h) = dimensions.map(|d| (d.width, d.height)).unwrap_or((210.0, 297.0));
            let w_pts = w * MM_TO_POINTS;
            let h_pts = h * MM_TO_POINTS;
            let frame = NSRect::new(NSPoint::new(0.0, 0.0), NSSize::new(w_pts, h_pts));
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
                warn!("无法找到 superview 来附加打印 webview");
            }
            
            // Load Request
            let url_ns = NSString::from_str(&url_clone);
            let ns_url = match NSURL::URLWithString(&url_ns) {
                Some(u) => u,
                None => {
                    // URL 解析失败，清理 webview 并返回错误
                    let _: () = msg_send![&*new_view, removeFromSuperview];
                    let _ = ptr_tx.send(Err(format!("无效的 URL: {}", url_clone)));
                    return;
                }
            };
            let request = NSURLRequest::requestWithURL(&ns_url);
            
            new_view.loadRequest(&request);
            
            // Pass pointer out
            let raw: *mut WKWebView = Retained::into_raw(new_view);
            let addr = raw as usize;
            let _ = ptr_tx.send(Ok(addr));
        }
    }).map_err(|e| e.to_string())?;

    // Step 2: Wait for content to load
    let addr = match ptr_rx.recv() {
        Ok(Ok(addr)) => addr,
        Ok(Err(e)) => return Err(e),
        Err(_) => return Err("创建 webview 失败".to_string()),
    };
    thread::sleep(Duration::from_millis(URL_LOAD_WAIT_MS));

    // Step 3: Print
    let (result_tx, result_rx) = mpsc::channel();
    let output_path_clone = output_path.clone();
    let path_str_clone = path_str.clone();

    window.with_webview(move |_webview| {
        unsafe {
            let mtm = MainThreadMarker::new_unchecked();
            
            let ptr = addr as *mut WKWebView;
            let target_webview: Retained<WKWebView> = match Retained::from_raw(ptr) {
                Some(w) => w,
                None => {
                    error!("无效的 webview 指针");
                    let _ = result_tx.send(Err("无效的 webview 指针".to_string()));
                    return;
                }
            };

            let pdf_config = WKPDFConfiguration::new(mtm);
            
            // 设置 PDF 页面矩形以控制分页
            let (w, h) = dimensions.map(|d| (d.width, d.height)).unwrap_or((210.0, 297.0));
            let w_pts = w * MM_TO_POINTS;
            let h_pts = h * MM_TO_POINTS;
            let pdf_rect = NSRect::new(NSPoint::new(0.0, 0.0), NSSize::new(w_pts, h_pts));
            pdf_config.setRect(pdf_rect);
            
            let webview_for_block = target_webview.clone();

            let completion_handler = RcBlock::new(move |pdf_data: *mut NSData, error: *mut NSError| {
                let _: () = msg_send![&*webview_for_block, removeFromSuperview];

                if !error.is_null() {
                    let error_obj: &NSError = &*error;
                    let desc = error_obj.localizedDescription();
                    let err_msg = format!("PDF 创建失败: {}", desc);
                    let _ = result_tx.send(Err(err_msg));
                    return;
                }

                if pdf_data.is_null() {
                    let _ = result_tx.send(Err("PDF 创建失败: 未返回数据".to_string()));
                    return;
                }

                let data: &NSData = &*pdf_data;
                let ptr: *const std::ffi::c_void = msg_send![data, bytes];
                let len: usize = msg_send![data, length];
                
                let ptr = ptr as *const u8;
                let data_slice = std::slice::from_raw_parts(ptr, len);

                match std::fs::write(&output_path_clone, data_slice) {
                    Ok(_) => {
                        info!("原生 PDF 已生成: {:?}", output_path_clone);
                        let _ = result_tx.send(Ok(path_str_clone.clone()));
                    },
                    Err(e) => {
                        let _ = result_tx.send(Err(e.to_string()));
                    }
                }
            });

            target_webview.createPDFWithConfiguration_completionHandler(Some(&pdf_config), &completion_handler);
        }
    }).map_err(|e| e.to_string())?;

    result_rx.recv().map_err(|e| e.to_string())?
}

// ================= MAC OS NATIVE (NSPrintOperation - URL) =================
#[cfg(target_os = "macos")]
pub async fn print_native_with_url_mac_print_operation<R: Runtime>(
    window: WebviewWindow<R>,
    url: String,
    output_path: PathBuf,
    dimensions: Option<PageDimensions>,
) -> Result<String, String> {
    use objc2_foundation::{NSString, NSRect, NSPoint, NSSize, NSURL, NSURLRequest};
    use objc2_web_kit::{WKWebView, WKWebViewConfiguration};
    use objc2_app_kit::{NSPrintInfo, NSPrintJobSavingURL, NSPrintOperation, NSPrintSaveJob, NSPrintingPaginationMode};
    use objc2::rc::Retained;
    use objc2::{MainThreadMarker, msg_send};
    use objc2::runtime::AnyObject;
    use std::sync::mpsc;
    use std::thread;
    use std::time::Duration;

    let path_str = output_path.to_string_lossy().to_string();
    let (ptr_tx, ptr_rx) = mpsc::channel::<Result<usize, String>>();

    let url_clone = url.clone();

    window.with_webview(move |webview| {
        unsafe {
            let mtm = MainThreadMarker::new_unchecked();

            let (w, h) = dimensions.map(|d| (d.width, d.height)).unwrap_or((210.0, 297.0));
            let w_pts = w * MM_TO_POINTS;
            let h_pts = h * MM_TO_POINTS;

            let frame = NSRect::new(NSPoint::new(0.0, 0.0), NSSize::new(w_pts, h_pts));
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
            let ns_url = match NSURL::URLWithString(&url_ns) {
                Some(u) => u,
                None => {
                    let _: () = msg_send![&*new_view, removeFromSuperview];
                    let _ = ptr_tx.send(Err(format!("无效的 URL: {}", url_clone)));
                    return;
                }
            };
            let request = NSURLRequest::requestWithURL(&ns_url);
            
            new_view.loadRequest(&request);

            let raw: *mut WKWebView = Retained::into_raw(new_view);
            let addr = raw as usize;
            let _ = ptr_tx.send(Ok(addr));
        }
    }).map_err(|e| e.to_string())?;

    let addr = match ptr_rx.recv() {
        Ok(Ok(addr)) => addr,
        Ok(Err(e)) => return Err(e),
        Err(_) => return Err("创建 webview 失败".to_string()),
    };
    thread::sleep(Duration::from_millis(URL_LOAD_WAIT_MS));

    let (result_tx, result_rx) = mpsc::channel();
    let output_path_clone = output_path.clone();
    let path_str_clone = path_str.clone();

    window.with_webview(move |_webview| {
        unsafe {
            let mtm = MainThreadMarker::new_unchecked();

            let ptr = addr as *mut WKWebView;
            let target_webview: Retained<WKWebView> = match Retained::from_raw(ptr) {
                Some(w) => w,
                None => {
                    error!("无效的 webview 指针");
                    let _ = result_tx.send(Err("无效的 webview 指针".to_string()));
                    return;
                }
            };

            let alloc_print_info = mtm.alloc::<NSPrintInfo>();
            let print_info: Retained<NSPrintInfo> = msg_send![alloc_print_info, init];

            print_info.setJobDisposition(NSPrintSaveJob);

            let dict = print_info.dictionary();
            let output_path_str = output_path_clone.to_string_lossy();
            let v_url = NSURL::fileURLWithPath(&NSString::from_str(&output_path_str));
            let _: () = msg_send![&dict, setObject: &*v_url, forKey: NSPrintJobSavingURL];

            print_info.setScalingFactor(1.0);
            
            let (w, h) = dimensions.map(|d| (d.width, d.height)).unwrap_or((210.0, 297.0));
            let w_pts = w * MM_TO_POINTS;
            let h_pts = h * MM_TO_POINTS;

            let paper_size = NSSize::new(w_pts, h_pts); 
            print_info.setPaperSize(paper_size);
            print_info.setLeftMargin(0.0);
            print_info.setRightMargin(0.0);
            print_info.setTopMargin(0.0);
            print_info.setBottomMargin(0.0);
            let _: () = msg_send![&print_info, setOrientation: 0isize];
            print_info.setVerticallyCentered(false);
            print_info.setHorizontallyCentered(false);
            print_info.setHorizontalPagination(NSPrintingPaginationMode::Automatic);
            print_info.setVerticalPagination(NSPrintingPaginationMode::Automatic);

            let print_op: Retained<NSPrintOperation> = target_webview.printOperationWithPrintInfo(&print_info);
            print_op.setShowsPrintPanel(false);
            print_op.setShowsProgressPanel(false);
            print_op.setCanSpawnSeparateThread(false);
            print_op.setJobTitle(Some(&NSString::from_str("QuickOutline PDF")));

            let success = print_op.runOperation();
            let result = if success && output_path_clone.exists() {
                Ok(path_str_clone.clone())
            } else if success {
                Err("NSPrintOperation completed but PDF file was not created".to_string())
            } else {
                Err("NSPrintOperation failed".to_string())
            };

            let _: () = msg_send![&target_webview, removeFromSuperview];
            let _ = result_tx.send(result);
        }
    }).map_err(|e| e.to_string())?;

    result_rx.recv().map_err(|e| e.to_string())?
}

// ================= MAC OS NATIVE (NSPrintOperation - HTML) =================
#[cfg(target_os = "macos")]
pub async fn print_native_with_html_mac_print_operation<R: Runtime>(
    window: WebviewWindow<R>,
    html: String,
    output_path: PathBuf,
    dimensions: Option<PageDimensions>,
) -> Result<String, String> {
    use objc2_foundation::{NSString, NSRect, NSPoint, NSSize, NSURL};
    use objc2_web_kit::{WKWebView, WKWebViewConfiguration};
    use objc2_app_kit::{NSPrintInfo, NSPrintJobSavingURL, NSPrintOperation, NSPrintSaveJob, NSPrintingPaginationMode};
    use objc2::rc::Retained;
    use objc2::{MainThreadMarker, msg_send};
    use objc2::runtime::AnyObject;
    use std::sync::mpsc;
    use std::thread;
    use std::time::Duration;

    let path_str = output_path.to_string_lossy().to_string();
    let (ptr_tx, ptr_rx) = mpsc::channel::<Result<usize, String>>();

    let html_clone = html.clone();

    window.with_webview(move |webview| {
        unsafe {
            let mtm = MainThreadMarker::new_unchecked();

            let (w, h) = dimensions.map(|d| (d.width, d.height)).unwrap_or((210.0, 297.0));
            let w_pts = w * MM_TO_POINTS;
            let h_pts = h * MM_TO_POINTS;

            let frame = NSRect::new(NSPoint::new(0.0, 0.0), NSSize::new(w_pts, h_pts));
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

            // Load HTML
            let html_ns = NSString::from_str(&html_clone);
            new_view.loadHTMLString_baseURL(&html_ns, None);

            let raw: *mut WKWebView = Retained::into_raw(new_view);
            let addr = raw as usize;
            let _ = ptr_tx.send(Ok(addr));
        }
    }).map_err(|e| e.to_string())?;

    let addr = match ptr_rx.recv() {
        Ok(Ok(addr)) => addr,
        Ok(Err(e)) => return Err(e),
        Err(_) => return Err("创建 webview 失败".to_string()),
    };
    thread::sleep(Duration::from_millis(HTML_LOAD_WAIT_MS));

    let (result_tx, result_rx) = mpsc::channel();
    let output_path_clone = output_path.clone();
    let path_str_clone = path_str.clone();

    window.with_webview(move |_webview| {
        unsafe {
            let mtm = MainThreadMarker::new_unchecked();

            let ptr = addr as *mut WKWebView;
            let target_webview: Retained<WKWebView> = match Retained::from_raw(ptr) {
                Some(w) => w,
                None => {
                    error!("无效的 webview 指针");
                    let _ = result_tx.send(Err("无效的 webview 指针".to_string()));
                    return;
                }
            };

            let alloc_print_info = mtm.alloc::<NSPrintInfo>();
            let print_info: Retained<NSPrintInfo> = msg_send![alloc_print_info, init];

            print_info.setJobDisposition(NSPrintSaveJob);

            let dict = print_info.dictionary();
            let output_path_str = output_path_clone.to_string_lossy();
            let v_url = NSURL::fileURLWithPath(&NSString::from_str(&output_path_str));
            let _: () = msg_send![&dict, setObject: &*v_url, forKey: NSPrintJobSavingURL];

            print_info.setScalingFactor(1.0);

            let (w, h) = dimensions.map(|d| (d.width, d.height)).unwrap_or((210.0, 297.0));
            let w_pts = w * MM_TO_POINTS;
            let h_pts = h * MM_TO_POINTS;

            let paper_size = NSSize::new(w_pts, h_pts); 
            print_info.setPaperSize(paper_size);
            print_info.setLeftMargin(0.0);
            print_info.setRightMargin(0.0);
            print_info.setTopMargin(0.0);
            print_info.setBottomMargin(0.0);
            let _: () = msg_send![&print_info, setOrientation: 0isize];
            print_info.setVerticallyCentered(false);
            print_info.setHorizontallyCentered(false);
            print_info.setHorizontalPagination(NSPrintingPaginationMode::Automatic);
            print_info.setVerticalPagination(NSPrintingPaginationMode::Automatic);

            let print_op: Retained<NSPrintOperation> = target_webview.printOperationWithPrintInfo(&print_info);
            print_op.setShowsPrintPanel(false);
            print_op.setShowsProgressPanel(false);
            print_op.setCanSpawnSeparateThread(false);
            print_op.setJobTitle(Some(&NSString::from_str("QuickOutline PDF")));

            let success = print_op.runOperation();
            let result = if success && output_path_clone.exists() {
                Ok(path_str_clone.clone())
            } else if success {
                Err("NSPrintOperation completed but PDF file was not created".to_string())
            } else {
                Err("NSPrintOperation failed".to_string())
            };

            let _: () = msg_send![&target_webview, removeFromSuperview];
            let _ = result_tx.send(result);
        }
    }).map_err(|e| e.to_string())?;

    result_rx.recv().map_err(|e| e.to_string())?
}

// ================= MAC OS NATIVE (WKPDFConfiguration - HTML) =================
#[cfg(target_os = "macos")]
pub async fn print_native_with_html_mac_wkpdf<R: Runtime>(
    window: WebviewWindow<R>,
    html: String,
    output_path: PathBuf,
    dimensions: Option<PageDimensions>,
) -> Result<String, String> {
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
    let (ptr_tx, ptr_rx) = mpsc::channel::<Result<usize, String>>();
    
    let html_clone = html.clone();
    
    window.with_webview(move |webview| {
        unsafe {
            let mtm = MainThreadMarker::new_unchecked();
            
            let (w, h) = dimensions.map(|d| (d.width, d.height)).unwrap_or((210.0, 297.0));
            let w_pts = w * MM_TO_POINTS;
            let h_pts = h * MM_TO_POINTS;

            let frame = NSRect::new(NSPoint::new(0.0, 0.0), NSSize::new(w_pts, h_pts));
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
                warn!("无法找到 superview 来附加打印 webview");
            }
            
            // Load HTML
            let html_ns = NSString::from_str(&html_clone);
            new_view.loadHTMLString_baseURL(&html_ns, None);
            
            let raw: *mut WKWebView = Retained::into_raw(new_view);
            let addr = raw as usize;
            let _ = ptr_tx.send(Ok(addr));
        }
    }).map_err(|e| e.to_string())?;

    let addr = match ptr_rx.recv() {
        Ok(Ok(addr)) => addr,
        Ok(Err(e)) => return Err(e),
        Err(_) => return Err("创建 webview 失败".to_string()),
    };
    thread::sleep(Duration::from_millis(HTML_LOAD_WAIT_MS));

    let (result_tx, result_rx) = mpsc::channel();
    let output_path_clone = output_path.clone();
    let path_str_clone = path_str.clone();

    window.with_webview(move |_webview| {
        unsafe {
            let mtm = MainThreadMarker::new_unchecked();
            
            let ptr = addr as *mut WKWebView;
            let target_webview: Retained<WKWebView> = match Retained::from_raw(ptr) {
                Some(w) => w,
                None => {
                    error!("无效的 webview 指针");
                    let _ = result_tx.send(Err("无效的 webview 指针".to_string()));
                    return;
                }
            };

            let pdf_config = WKPDFConfiguration::new(mtm);
            
            // 设置 PDF 页面矩形以控制分页
            let (w, h) = dimensions.map(|d| (d.width, d.height)).unwrap_or((210.0, 297.0));
            let w_pts = w * MM_TO_POINTS;
            let h_pts = h * MM_TO_POINTS;
            let pdf_rect = NSRect::new(NSPoint::new(0.0, 0.0), NSSize::new(w_pts, h_pts));
            pdf_config.setRect(pdf_rect);
            
            let webview_for_block = target_webview.clone();

            let completion_handler = RcBlock::new(move |pdf_data: *mut NSData, error: *mut NSError| {
                let _: () = msg_send![&*webview_for_block, removeFromSuperview];

                if !error.is_null() {
                    let error_obj: &NSError = &*error;
                    let desc = error_obj.localizedDescription();
                    let domain = error_obj.domain();
                    let code = error_obj.code();
                    let err_msg = format!("PDF 创建失败: {} (Domain: {}, Code: {})", desc, domain, code);
                    let _ = result_tx.send(Err(err_msg));
                    return;
                }

                if pdf_data.is_null() {
                    let _ = result_tx.send(Err("PDF 创建失败: 未返回数据".to_string()));
                    return;
                }

                let data: &NSData = &*pdf_data;
                let ptr: *const std::ffi::c_void = msg_send![data, bytes];
                let len: usize = msg_send![data, length];
                
                let ptr = ptr as *const u8;
                let data_slice = std::slice::from_raw_parts(ptr, len);

                match std::fs::write(&output_path_clone, data_slice) {
                    Ok(_) => {
                        info!("原生 PDF 已生成: {:?}", output_path_clone);
                        let _ = result_tx.send(Ok(path_str_clone.clone()));
                    },
                    Err(e) => {
                        let _ = result_tx.send(Err(e.to_string()));
                    }
                }
            });

            target_webview.createPDFWithConfiguration_completionHandler(Some(&pdf_config), &completion_handler);
        }
    }).map_err(|e| e.to_string())?;

    result_rx.recv().map_err(|e| e.to_string())?
}

// 非 macOS 平台的桩实现
#[cfg(not(target_os = "macos"))]
pub async fn print_native_with_url_mac_wkpdf<R: Runtime>(
    _window: WebviewWindow<R>,
    _url: String,
    _output_path: PathBuf,
    _dimensions: Option<PageDimensions>,
) -> Result<String, String> {
    Err("macOS 原生打印在非 macOS 平台上不可用".to_string())
}

#[cfg(not(target_os = "macos"))]
pub async fn print_native_with_url_mac_print_operation<R: Runtime>(
    _window: WebviewWindow<R>,
    _url: String,
    _output_path: PathBuf,
    _dimensions: Option<PageDimensions>,
) -> Result<String, String> {
    Err("macOS 原生打印在非 macOS 平台上不可用".to_string())
}

#[cfg(not(target_os = "macos"))]
pub async fn print_native_with_html_mac_print_operation<R: Runtime>(
    _window: WebviewWindow<R>,
    _html: String,
    _output_path: PathBuf,
    _dimensions: Option<PageDimensions>,
) -> Result<String, String> {
    Err("macOS 原生打印在非 macOS 平台上不可用".to_string())
}

#[cfg(not(target_os = "macos"))]
pub async fn print_native_with_html_mac_wkpdf<R: Runtime>(
    _window: WebviewWindow<R>,
    _html: String,
    _output_path: PathBuf,
    _dimensions: Option<PageDimensions>,
) -> Result<String, String> {
    Err("macOS 原生打印在非 macOS 平台上不可用".to_string())
}
