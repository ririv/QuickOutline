use log::{error, info, warn};
use std::path::PathBuf;
use tauri::{Runtime, WebviewWindow};

use super::native::PageDimensions;

/// mm 转 points 的转换因子 (72 / 25.4)
const MM_TO_POINTS: f64 = 2.83465;
/// CSS absolute units use 96 px per inch.
const MM_TO_CSS_PX: f64 = 96.0 / 25.4;
/// URL 内容加载等待时间（毫秒）
const URL_LOAD_WAIT_MS: u64 = 2000;
/// HTML 内容加载等待时间（毫秒）
const HTML_LOAD_WAIT_MS: u64 = 1000;
const PDF_READY_TIMEOUT_MS: u64 = 10_000;
const PDF_READY_POLL_MS: u64 = 250;

#[cfg(target_os = "macos")]
#[derive(Debug, Clone, Copy)]
struct WkPdfPageRect {
    x: f64,
    y: f64,
    width: f64,
    height: f64,
}

#[cfg(target_os = "macos")]
#[derive(Debug, Clone)]
struct WkPdfPageMetrics {
    source_x: f64,
    source_y: f64,
    source_width: f64,
    source_page_height: f64,
    source_total_height: f64,
    output_width: f64,
    output_height: f64,
    page_count: usize,
    page_rects: Vec<WkPdfPageRect>,
}

#[cfg(target_os = "macos")]
#[derive(Debug, serde::Deserialize)]
struct JsPageRect {
    x: f64,
    y: f64,
    width: f64,
    height: f64,
}

#[cfg(target_os = "macos")]
#[derive(Debug, serde::Deserialize)]
struct JsPrintMetrics {
    ready: bool,
    x: f64,
    y: f64,
    width: f64,
    #[serde(rename = "pageHeight")]
    page_height: f64,
    height: f64,
    #[serde(rename = "pageCount")]
    page_count: usize,
    #[serde(default)]
    pages: Vec<JsPageRect>,
}

fn page_dimensions_mm(dimensions: Option<PageDimensions>) -> (f64, f64) {
    dimensions
        .map(|d| (d.width, d.height))
        .unwrap_or((210.0, 297.0))
}

fn page_dimensions_points(dimensions: Option<PageDimensions>) -> (f64, f64) {
    let (w, h) = page_dimensions_mm(dimensions);
    (w * MM_TO_POINTS, h * MM_TO_POINTS)
}

fn page_dimensions_css_px(dimensions: Option<PageDimensions>) -> (f64, f64) {
    let (w, h) = page_dimensions_mm(dimensions);
    (w * MM_TO_CSS_PX, h * MM_TO_CSS_PX)
}

#[cfg(target_os = "macos")]
fn number(value: f64) -> lopdf::Object {
    lopdf::Object::Real(value as f32)
}

#[cfg(target_os = "macos")]
fn page_box(width: f64, height: f64) -> lopdf::Object {
    lopdf::Object::Array(vec![
        number(0.0),
        number(0.0),
        number(width),
        number(height),
    ])
}

#[cfg(target_os = "macos")]
fn pdf_box_size(page: &lopdf::Dictionary) -> Option<(f64, f64)> {
    page.get(b"CropBox")
        .or_else(|_| page.get(b"MediaBox"))
        .ok()
        .and_then(|object| object.as_array().ok())
        .and_then(|values| {
            if values.len() < 4 {
                return None;
            }
            let x0 = values[0].as_float().ok().map(|v| v as f64)?;
            let y0 = values[1].as_float().ok().map(|v| v as f64)?;
            let x1 = values[2].as_float().ok().map(|v| v as f64)?;
            let y1 = values[3].as_float().ok().map(|v| v as f64)?;
            Some(((x1 - x0).abs(), (y1 - y0).abs()))
        })
}

#[cfg(target_os = "macos")]
fn build_wkpdf_page_metrics(
    js: &JsPrintMetrics,
    dimensions: Option<PageDimensions>,
) -> WkPdfPageMetrics {
    let (fallback_width, fallback_page_height) = page_dimensions_css_px(dimensions);
    let (output_width, output_height) = page_dimensions_points(dimensions);
    let page_rects = if js.pages.is_empty() {
        vec![WkPdfPageRect {
            x: js.x.max(0.0),
            y: js.y.max(0.0),
            width: fallback_width,
            height: fallback_page_height,
        }]
    } else {
        js.pages
            .iter()
            .map(|page| WkPdfPageRect {
                x: page.x.max(0.0),
                y: page.y.max(0.0),
                width: page.width,
                height: page.height,
            })
            .collect()
    };
    let source_x = js.x.max(0.0);
    let source_y = js.y.max(0.0);
    let source_width = if js.width > 1.0 {
        js.width
    } else {
        fallback_width
    };
    let source_page_height = if js.page_height > 1.0 {
        js.page_height
    } else {
        fallback_page_height
    };
    let page_count = if !page_rects.is_empty() {
        page_rects.len()
    } else if js.page_count > 0 {
        js.page_count
    } else {
        (js.height.max(source_page_height) / source_page_height)
            .ceil()
            .max(1.0) as usize
    };
    let source_total_height = if js.page_count > 0 {
        source_page_height * page_count as f64
    } else {
        js.height.max(source_page_height)
    };

    WkPdfPageMetrics {
        source_x,
        source_y,
        source_width,
        source_page_height,
        source_total_height,
        output_width,
        output_height,
        page_count,
        page_rects,
    }
}

#[cfg(target_os = "macos")]
fn split_wkpdf_long_page(path: &PathBuf, metrics: WkPdfPageMetrics) -> Result<(), String> {
    use lopdf::{Dictionary, Document, Object, Stream};

    let mut doc = Document::load(path).map_err(|e| format!("读取 WKPDF 输出失败: {}", e))?;
    let pages = doc.get_pages();
    if pages.is_empty() {
        return Err("WKPDF 输出中没有页面".to_string());
    }

    let original_page_id = *pages
        .values()
        .next()
        .ok_or_else(|| "WKPDF 输出中没有页面".to_string())?;
    let original_content = doc
        .get_page_content(original_page_id)
        .map_err(|e| format!("读取 WKPDF 页面内容失败: {}", e))?;
    let original_page = doc
        .get_dictionary(original_page_id)
        .map_err(|e| format!("读取 WKPDF 页面字典失败: {}", e))?
        .clone();
    let resources = original_page.get(b"Resources").ok().cloned().or_else(|| {
        doc.get_page_resources(original_page_id)
            .ok()
            .and_then(|(_, ids)| ids.first().copied().map(Object::Reference))
    });
    let pages_id = doc
        .catalog()
        .and_then(|catalog| catalog.get(b"Pages"))
        .and_then(Object::as_reference)
        .map_err(|e| format!("读取 WKPDF Pages 节点失败: {}", e))?;

    let (source_width, source_total_height) =
        pdf_box_size(&original_page).unwrap_or((metrics.source_width, metrics.source_total_height));
    let css_to_pdf_scale = source_total_height / metrics.source_total_height;
    let source_page_height = metrics.source_page_height * css_to_pdf_scale;
    let source_top_offset =
        ((source_total_height - source_page_height * metrics.page_count as f64) / 2.0).max(0.0);
    let scale_x = metrics.output_width / source_width;
    let scale_y = metrics.output_height / source_page_height;
    let scale = scale_x.min(scale_y);
    let media_box = page_box(metrics.output_width, metrics.output_height);
    let mut kids = Vec::with_capacity(metrics.page_count);

    for page_index in 0..metrics.page_count {
        let source_y_min =
            source_top_offset + source_page_height * (metrics.page_count - page_index - 1) as f64;
        let translate_y = -source_y_min * scale;
        let mut content =
            format!("q\n{scale:.8} 0 0 {scale:.8} 0 {translate_y:.8} cm\n").into_bytes();
        content.extend_from_slice(&original_content);
        content.extend_from_slice(b"\nQ\n");

        let content_id = doc.add_object(Stream::new(Dictionary::new(), content));
        let mut page = Dictionary::new();
        page.set("Type", Object::Name(b"Page".to_vec()));
        page.set("Parent", Object::Reference(pages_id));
        page.set("Contents", Object::Reference(content_id));
        page.set("MediaBox", media_box.clone());
        page.set("CropBox", media_box.clone());
        if let Some(resources) = resources.clone() {
            page.set("Resources", resources);
        }

        let page_id = doc.add_object(page);
        kids.push(Object::Reference(page_id));
    }

    let pages_dict = doc
        .get_object_mut(pages_id)
        .and_then(Object::as_dict_mut)
        .map_err(|e| format!("更新 WKPDF Pages 节点失败: {}", e))?;
    pages_dict.set("Kids", Object::Array(kids));
    pages_dict.set("Count", Object::Integer(metrics.page_count as i64));
    pages_dict.set("MediaBox", media_box);

    doc.prune_objects();
    doc.compress();
    doc.save(path)
        .map_err(|e| format!("保存分页后的 PDF 失败: {}", e))?;
    Ok(())
}

#[cfg(target_os = "macos")]
fn wait_for_wkpdf_metrics<R: Runtime>(
    window: &WebviewWindow<R>,
    webview_addr: usize,
    dimensions: Option<PageDimensions>,
) -> Result<WkPdfPageMetrics, String> {
    use block2::RcBlock;
    use objc2::msg_send;
    use objc2::runtime::AnyObject;
    use objc2_foundation::{NSError, NSString};
    use objc2_web_kit::WKWebView;
    use std::sync::mpsc;
    use std::thread;
    use std::time::{Duration, Instant};

    let started = Instant::now();

    loop {
        let (tx, rx) = mpsc::channel::<Result<String, String>>();
        window.with_webview(move |_webview| {
            unsafe {
                let target_webview = &*(webview_addr as *mut WKWebView);
                let script = NSString::from_str(r#"
                    (function() {
                        const pageElements = Array.from(document.querySelectorAll('.pagedjs_page'));
                        const page = pageElements[0] || null;
                        const pageRect = page ? page.getBoundingClientRect() : null;
                        const pages = pageElements.map((el) => {
                            const rect = el.getBoundingClientRect();
                            return { x: rect.left, y: rect.top, width: rect.width, height: rect.height };
                        });
                        const pageCount = pageElements.length;
                        const hasPaged = !!document.querySelector('script[src*="paged"]') || pageCount > 0;
                        const ready = !hasPaged || document.body.classList.contains('pagedjs_ready');
                        const doc = document.documentElement;
                        const body = document.body;
                        const width = pageRect ? pageRect.width : Math.max(
                            doc ? doc.scrollWidth : 0,
                            body ? body.scrollWidth : 0,
                            window.innerWidth || 0
                        );
                        const pageHeight = pageRect ? pageRect.height : Math.max(window.innerHeight || 0, 1);
                        const height = Math.max(
                            pageRect ? pageRect.height * pageCount : 0,
                            doc ? doc.scrollHeight : 0,
                            body ? body.scrollHeight : 0,
                            pageHeight
                        );
                        return JSON.stringify({
                            ready,
                            x: pageRect ? pageRect.left : 0,
                            y: pageRect ? pageRect.top : 0,
                            width,
                            pageHeight,
                            height,
                            pageCount,
                            pages
                        });
                    })()
                "#);
                let completion = RcBlock::new(move |value: *mut AnyObject, error: *mut NSError| {
                    if !error.is_null() {
                        let error_obj: &NSError = &*error;
                        let _ = tx.send(Err(format!("读取页面分页信息失败: {}", error_obj.localizedDescription())));
                        return;
                    }
                    if value.is_null() {
                        let _ = tx.send(Err("读取页面分页信息失败: 未返回数据".to_string()));
                        return;
                    }
                    let description: objc2::rc::Retained<NSString> = msg_send![&*value, description];
                    let _ = tx.send(Ok(description.to_string()));
                });
                target_webview.evaluateJavaScript_completionHandler(&script, Some(&completion));
            }
        }).map_err(|e| e.to_string())?;

        let raw = rx.recv().map_err(|e| e.to_string())??;
        let metrics: JsPrintMetrics = serde_json::from_str(&raw)
            .map_err(|e| format!("解析页面分页信息失败: {}; raw={}", e, raw))?;
        if metrics.ready {
            return Ok(build_wkpdf_page_metrics(&metrics, dimensions));
        }

        if started.elapsed() >= Duration::from_millis(PDF_READY_TIMEOUT_MS) {
            warn!("等待 Paged.js 分页完成超时，使用最后一次页面尺寸信息继续生成 PDF");
            return Ok(build_wkpdf_page_metrics(&metrics, dimensions));
        }
        thread::sleep(Duration::from_millis(PDF_READY_POLL_MS));
    }
}

#[cfg(target_os = "macos")]
fn create_wkpdf_for_rect<R: Runtime>(
    window: &WebviewWindow<R>,
    webview_addr: usize,
    frame_width: f64,
    frame_height: f64,
    rect: WkPdfPageRect,
) -> Result<Vec<u8>, String> {
    use block2::RcBlock;
    use objc2::msg_send;
    use objc2_foundation::{NSData, NSError, NSPoint, NSRect, NSSize};
    use objc2_web_kit::{WKPDFConfiguration, WKWebView};
    use std::sync::mpsc;

    let (tx, rx) = mpsc::channel();
    window
        .with_webview(move |_webview| unsafe {
            let mtm = objc2::MainThreadMarker::new_unchecked();
            let target_webview = &*(webview_addr as *mut WKWebView);
            let frame_rect = NSRect::new(
                NSPoint::new(0.0, 0.0),
                NSSize::new(frame_width, frame_height),
            );
            let _: () = msg_send![target_webview, setFrame: frame_rect];

            let pdf_config = WKPDFConfiguration::new(mtm);
            pdf_config.setRect(NSRect::new(
                NSPoint::new(rect.x, rect.y),
                NSSize::new(rect.width, rect.height),
            ));

            let completion_handler =
                RcBlock::new(move |pdf_data: *mut NSData, error: *mut NSError| {
                    if !error.is_null() {
                        let error_obj: &NSError = &*error;
                        let desc = error_obj.localizedDescription();
                        let _ = tx.send(Err(format!("PDF 创建失败: {}", desc)));
                        return;
                    }

                    if pdf_data.is_null() {
                        let _ = tx.send(Err("PDF 创建失败: 未返回数据".to_string()));
                        return;
                    }

                    let data: &NSData = &*pdf_data;
                    let ptr: *const std::ffi::c_void = msg_send![data, bytes];
                    let len: usize = msg_send![data, length];
                    let bytes = std::slice::from_raw_parts(ptr as *const u8, len).to_vec();
                    let _ = tx.send(Ok(bytes));
                });

            target_webview.createPDFWithConfiguration_completionHandler(
                Some(&pdf_config),
                &completion_handler,
            );
        })
        .map_err(|e| e.to_string())?;

    rx.recv().map_err(|e| e.to_string())?
}

#[cfg(target_os = "macos")]
fn normalize_pdf_page_sizes(
    path: &PathBuf,
    target_width: f64,
    target_height: f64,
) -> Result<(), String> {
    use lopdf::{Dictionary, Document, Object, Stream};

    let mut doc = Document::load(path).map_err(|e| format!("读取合并 PDF 失败: {}", e))?;
    let page_ids: Vec<_> = doc.get_pages().values().copied().collect();
    let target_box = page_box(target_width, target_height);

    for page_id in page_ids {
        let (source_width, source_height) = doc
            .get_dictionary(page_id)
            .ok()
            .and_then(pdf_box_size)
            .unwrap_or((target_width, target_height));
        if source_width <= 0.0 || source_height <= 0.0 {
            return Err("PDF 页面尺寸无效，无法归一化".to_string());
        }

        let scale_x = target_width / source_width;
        let scale_y = target_height / source_height;
        let original_content = doc
            .get_page_content(page_id)
            .map_err(|e| format!("读取 PDF 页面内容失败: {}", e))?;
        let mut content = format!("q\n{scale_x:.8} 0 0 {scale_y:.8} 0 0 cm\n").into_bytes();
        content.extend_from_slice(&original_content);
        content.extend_from_slice(b"\nQ\n");
        let content_id = doc.add_object(Stream::new(Dictionary::new(), content));

        let page = doc
            .get_object_mut(page_id)
            .and_then(Object::as_dict_mut)
            .map_err(|e| format!("更新 PDF 页面尺寸失败: {}", e))?;
        page.set("Contents", Object::Reference(content_id));
        page.set("MediaBox", target_box.clone());
        page.set("CropBox", target_box.clone());
    }

    doc.prune_objects();
    doc.compress();
    doc.save(path)
        .map_err(|e| format!("保存归一化后的 PDF 失败: {}", e))?;
    Ok(())
}

#[cfg(target_os = "macos")]
fn merge_wkpdf_pages(
    page_bytes: &[Vec<u8>],
    output_path: &PathBuf,
    target_width: f64,
    target_height: f64,
) -> Result<(), String> {
    let pdfium = crate::pdf::pdfium_render::get_pdfium().map_err(|e| e.to_string())?;
    let mut merged = pdfium.create_new_pdf().map_err(|e| format!("{:?}", e))?;

    for bytes in page_bytes {
        let page_doc = pdfium
            .load_pdf_from_byte_slice(bytes, None)
            .map_err(|e| format!("{:?}", e))?;
        let page_count = page_doc.pages().len();
        if page_count == 0 {
            continue;
        }
        let dest_index = merged.pages().len();
        merged
            .pages_mut()
            .copy_page_range_from_document(&page_doc, 0..=(page_count - 1), dest_index)
            .map_err(|e| format!("{:?}", e))?;
    }

    let bytes = merged.save_to_bytes().map_err(|e| format!("{:?}", e))?;
    std::fs::write(output_path, bytes).map_err(|e| e.to_string())?;
    normalize_pdf_page_sizes(output_path, target_width, target_height)
}

// ================= MAC OS NATIVE (WKPDFConfiguration - URL) =================
#[cfg(target_os = "macos")]
pub async fn print_native_with_url_mac_wkpdf<R: Runtime>(
    window: WebviewWindow<R>,
    url: String,
    output_path: PathBuf,
    dimensions: Option<PageDimensions>,
) -> Result<String, String> {
    use objc2::rc::Retained;
    use objc2::runtime::AnyObject;
    use objc2::{MainThreadMarker, msg_send};
    use objc2_foundation::{NSPoint, NSRect, NSSize, NSString, NSURL, NSURLRequest};
    use objc2_web_kit::{WKWebView, WKWebViewConfiguration};
    use std::sync::mpsc;
    use std::thread;
    use std::time::Duration;

    let path_str = output_path.to_string_lossy().to_string();
    let (ptr_tx, ptr_rx) = mpsc::channel::<Result<usize, String>>();

    // Step 1: Initialize, Attach, and Load
    let url_clone = url.clone();

    window
        .with_webview(move |webview| {
            unsafe {
                let mtm = MainThreadMarker::new_unchecked();

                let (w_px, h_px) = page_dimensions_css_px(dimensions);
                let frame = NSRect::new(NSPoint::new(0.0, 0.0), NSSize::new(w_px, h_px));
                let config = WKWebViewConfiguration::new(mtm);

                let alloc_view = mtm.alloc::<WKWebView>();
                let new_view: Retained<WKWebView> =
                    msg_send![alloc_view, initWithFrame: frame, configuration: &*config];

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
        })
        .map_err(|e| e.to_string())?;

    // Step 2: Wait for content to load
    let addr = match ptr_rx.recv() {
        Ok(Ok(addr)) => addr,
        Ok(Err(e)) => return Err(e),
        Err(_) => return Err("创建 webview 失败".to_string()),
    };
    thread::sleep(Duration::from_millis(URL_LOAD_WAIT_MS));
    let metrics = wait_for_wkpdf_metrics(&window, addr, dimensions)?;

    // Step 3: Export each Paged.js page as an independent WKPDF page.
    let frame_width = metrics
        .page_rects
        .iter()
        .map(|rect| rect.x + rect.width)
        .fold(metrics.source_x + metrics.source_width, f64::max);
    let frame_height = metrics
        .page_rects
        .iter()
        .map(|rect| rect.y + rect.height)
        .fold(metrics.source_y + metrics.source_total_height, f64::max);
    let mut page_bytes = Vec::with_capacity(metrics.page_rects.len());
    for rect in metrics.page_rects.iter().copied() {
        page_bytes.push(create_wkpdf_for_rect(
            &window,
            addr,
            frame_width,
            frame_height,
            rect,
        )?);
    }
    merge_wkpdf_pages(
        &page_bytes,
        &output_path,
        metrics.output_width,
        metrics.output_height,
    )?;

    // Cleanup retained hidden webview.
    window
        .with_webview(move |_webview| unsafe {
            let ptr = addr as *mut WKWebView;
            let target_webview: Retained<WKWebView> = match Retained::from_raw(ptr) {
                Some(w) => w,
                None => {
                    error!("无效的 webview 指针");
                    return;
                }
            };
            let _: () = msg_send![&target_webview, removeFromSuperview];
        })
        .map_err(|e| e.to_string())?;

    info!("原生 PDF 已生成: {:?}", output_path);
    Ok(path_str)
}

// ================= MAC OS NATIVE (NSPrintOperation - URL) =================
#[cfg(target_os = "macos")]
pub async fn print_native_with_url_mac_print_operation<R: Runtime>(
    window: WebviewWindow<R>,
    url: String,
    output_path: PathBuf,
    dimensions: Option<PageDimensions>,
) -> Result<String, String> {
    use objc2::rc::Retained;
    use objc2::runtime::AnyObject;
    use objc2::{MainThreadMarker, msg_send};
    use objc2_app_kit::{
        NSPrintInfo, NSPrintJobSavingURL, NSPrintOperation, NSPrintSaveJob,
        NSPrintingPaginationMode,
    };
    use objc2_foundation::{NSPoint, NSRect, NSSize, NSString, NSURL, NSURLRequest};
    use objc2_web_kit::{WKWebView, WKWebViewConfiguration};
    use std::sync::mpsc;
    use std::thread;
    use std::time::Duration;

    let path_str = output_path.to_string_lossy().to_string();
    let (ptr_tx, ptr_rx) = mpsc::channel::<Result<usize, String>>();

    let url_clone = url.clone();

    window
        .with_webview(move |webview| {
            unsafe {
                let mtm = MainThreadMarker::new_unchecked();

                let (w_pts, h_pts) = page_dimensions_points(dimensions);
                let frame = NSRect::new(NSPoint::new(0.0, 0.0), NSSize::new(w_pts, h_pts));
                let config = WKWebViewConfiguration::new(mtm);

                let alloc_view = mtm.alloc::<WKWebView>();
                let new_view: Retained<WKWebView> =
                    msg_send![alloc_view, initWithFrame: frame, configuration: &*config];

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
        })
        .map_err(|e| e.to_string())?;

    let addr = match ptr_rx.recv() {
        Ok(Ok(addr)) => addr,
        Ok(Err(e)) => return Err(e),
        Err(_) => return Err("创建 webview 失败".to_string()),
    };
    thread::sleep(Duration::from_millis(URL_LOAD_WAIT_MS));

    let (result_tx, result_rx) = mpsc::channel();
    let output_path_clone = output_path.clone();
    let path_str_clone = path_str.clone();

    window
        .with_webview(move |_webview| unsafe {
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

            let (w, h) = dimensions
                .map(|d| (d.width, d.height))
                .unwrap_or((210.0, 297.0));
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

            let print_op: Retained<NSPrintOperation> =
                target_webview.printOperationWithPrintInfo(&print_info);
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
        })
        .map_err(|e| e.to_string())?;

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
    use objc2::rc::Retained;
    use objc2::runtime::AnyObject;
    use objc2::{MainThreadMarker, msg_send};
    use objc2_app_kit::{
        NSPrintInfo, NSPrintJobSavingURL, NSPrintOperation, NSPrintSaveJob,
        NSPrintingPaginationMode,
    };
    use objc2_foundation::{NSPoint, NSRect, NSSize, NSString, NSURL};
    use objc2_web_kit::{WKWebView, WKWebViewConfiguration};
    use std::sync::mpsc;
    use std::thread;
    use std::time::Duration;

    let path_str = output_path.to_string_lossy().to_string();
    let (ptr_tx, ptr_rx) = mpsc::channel::<Result<usize, String>>();

    let html_clone = html.clone();

    window
        .with_webview(move |webview| {
            unsafe {
                let mtm = MainThreadMarker::new_unchecked();

                let (w, h) = dimensions
                    .map(|d| (d.width, d.height))
                    .unwrap_or((210.0, 297.0));
                let w_pts = w * MM_TO_POINTS;
                let h_pts = h * MM_TO_POINTS;

                let frame = NSRect::new(NSPoint::new(0.0, 0.0), NSSize::new(w_pts, h_pts));
                let config = WKWebViewConfiguration::new(mtm);

                let alloc_view = mtm.alloc::<WKWebView>();
                let new_view: Retained<WKWebView> =
                    msg_send![alloc_view, initWithFrame: frame, configuration: &*config];

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
        })
        .map_err(|e| e.to_string())?;

    let addr = match ptr_rx.recv() {
        Ok(Ok(addr)) => addr,
        Ok(Err(e)) => return Err(e),
        Err(_) => return Err("创建 webview 失败".to_string()),
    };
    thread::sleep(Duration::from_millis(HTML_LOAD_WAIT_MS));

    let (result_tx, result_rx) = mpsc::channel();
    let output_path_clone = output_path.clone();
    let path_str_clone = path_str.clone();

    window
        .with_webview(move |_webview| unsafe {
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

            let (w, h) = dimensions
                .map(|d| (d.width, d.height))
                .unwrap_or((210.0, 297.0));
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

            let print_op: Retained<NSPrintOperation> =
                target_webview.printOperationWithPrintInfo(&print_info);
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
        })
        .map_err(|e| e.to_string())?;

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
    use objc2::rc::Retained;
    use objc2::runtime::AnyObject;
    use objc2::{MainThreadMarker, msg_send};
    use objc2_foundation::{NSPoint, NSRect, NSSize, NSString};
    use objc2_web_kit::{WKWebView, WKWebViewConfiguration};
    use std::sync::mpsc;
    use std::thread;
    use std::time::Duration;

    let path_str = output_path.to_string_lossy().to_string();
    let (ptr_tx, ptr_rx) = mpsc::channel::<Result<usize, String>>();

    let html_clone = html.clone();

    window
        .with_webview(move |webview| {
            unsafe {
                let mtm = MainThreadMarker::new_unchecked();

                let (w_px, h_px) = page_dimensions_css_px(dimensions);
                let frame = NSRect::new(NSPoint::new(0.0, 0.0), NSSize::new(w_px, h_px));
                let config = WKWebViewConfiguration::new(mtm);

                let alloc_view = mtm.alloc::<WKWebView>();
                let new_view: Retained<WKWebView> =
                    msg_send![alloc_view, initWithFrame: frame, configuration: &*config];

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
        })
        .map_err(|e| e.to_string())?;

    let addr = match ptr_rx.recv() {
        Ok(Ok(addr)) => addr,
        Ok(Err(e)) => return Err(e),
        Err(_) => return Err("创建 webview 失败".to_string()),
    };
    thread::sleep(Duration::from_millis(HTML_LOAD_WAIT_MS));
    let metrics = wait_for_wkpdf_metrics(&window, addr, dimensions)?;

    let frame_width = metrics
        .page_rects
        .iter()
        .map(|rect| rect.x + rect.width)
        .fold(metrics.source_x + metrics.source_width, f64::max);
    let frame_height = metrics
        .page_rects
        .iter()
        .map(|rect| rect.y + rect.height)
        .fold(metrics.source_y + metrics.source_total_height, f64::max);
    let mut page_bytes = Vec::with_capacity(metrics.page_rects.len());
    for rect in metrics.page_rects.iter().copied() {
        page_bytes.push(create_wkpdf_for_rect(
            &window,
            addr,
            frame_width,
            frame_height,
            rect,
        )?);
    }
    merge_wkpdf_pages(
        &page_bytes,
        &output_path,
        metrics.output_width,
        metrics.output_height,
    )?;

    window
        .with_webview(move |_webview| unsafe {
            let ptr = addr as *mut WKWebView;
            let target_webview: Retained<WKWebView> = match Retained::from_raw(ptr) {
                Some(w) => w,
                None => {
                    error!("无效的 webview 指针");
                    return;
                }
            };
            let _: () = msg_send![&target_webview, removeFromSuperview];
        })
        .map_err(|e| e.to_string())?;

    info!("原生 PDF 已生成: {:?}", output_path);
    Ok(path_str)
}

#[cfg(all(test, target_os = "macos"))]
mod tests {
    use super::*;
    use lopdf::{Document, Object, Stream, dictionary};

    fn numeric_value(object: &Object) -> f64 {
        object
            .as_float()
            .map(|v| v as f64)
            .or_else(|_| object.as_i64().map(|v| v as f64))
            .expect("box value should be numeric")
    }

    #[test]
    fn split_wkpdf_long_page_creates_target_sized_pages() {
        let temp_dir = tempfile::tempdir().expect("temp dir");
        let pdf_path = temp_dir.path().join("long.pdf");
        let mut doc = Document::with_version("1.5");
        let pages_id = doc.new_object_id();
        let content_id = doc.add_object(Stream::new(
            dictionary! {},
            b"q\n0 0 200 900 re\nS\nQ\n".to_vec(),
        ));
        let page_id = doc.add_object(dictionary! {
            "Type" => "Page",
            "Parent" => pages_id,
            "Contents" => content_id,
            "MediaBox" => vec![0.into(), 0.into(), 200.into(), 900.into()],
        });
        doc.objects.insert(
            pages_id,
            Object::Dictionary(dictionary! {
                "Type" => "Pages",
                "Kids" => vec![page_id.into()],
                "Count" => 1,
                "MediaBox" => vec![0.into(), 0.into(), 200.into(), 900.into()],
            }),
        );
        let catalog_id = doc.add_object(dictionary! {
            "Type" => "Catalog",
            "Pages" => pages_id,
        });
        doc.trailer.set("Root", catalog_id);
        doc.save(&pdf_path).expect("save long pdf");

        split_wkpdf_long_page(
            &pdf_path,
            WkPdfPageMetrics {
                source_width: 200.0,
                source_x: 0.0,
                source_y: 0.0,
                source_page_height: 300.0,
                source_total_height: 900.0,
                output_width: 100.0,
                output_height: 150.0,
                page_count: 3,
                page_rects: vec![],
            },
        )
        .expect("split pdf");

        let split_doc = Document::load(&pdf_path).expect("load split pdf");
        let pages = split_doc.get_pages();
        assert_eq!(pages.len(), 3);
        for page_id in pages.values() {
            let page = split_doc.get_dictionary(*page_id).expect("page dictionary");
            let media_box = page
                .get(b"MediaBox")
                .expect("media box")
                .as_array()
                .expect("media box array");
            assert_eq!(numeric_value(&media_box[2]), 100.0);
            assert_eq!(numeric_value(&media_box[3]), 150.0);
        }
    }

    #[test]
    fn split_wkpdf_long_page_starts_first_slice_without_vertical_offset() {
        let temp_dir = tempfile::tempdir().expect("temp dir");
        let pdf_path = temp_dir.path().join("long.pdf");
        let mut doc = Document::with_version("1.5");
        let pages_id = doc.new_object_id();
        let content_id = doc.add_object(Stream::new(dictionary! {}, b"q\nQ\n".to_vec()));
        let page_id = doc.add_object(dictionary! {
            "Type" => "Page",
            "Parent" => pages_id,
            "Contents" => content_id,
            "MediaBox" => vec![0.into(), 0.into(), 200.into(), 900.into()],
        });
        doc.objects.insert(
            pages_id,
            Object::Dictionary(dictionary! {
                "Type" => "Pages",
                "Kids" => vec![page_id.into()],
                "Count" => 1,
                "MediaBox" => vec![0.into(), 0.into(), 200.into(), 900.into()],
            }),
        );
        let catalog_id = doc.add_object(dictionary! {
            "Type" => "Catalog",
            "Pages" => pages_id,
        });
        doc.trailer.set("Root", catalog_id);
        doc.save(&pdf_path).expect("save long pdf");

        split_wkpdf_long_page(
            &pdf_path,
            WkPdfPageMetrics {
                source_width: 200.0,
                source_x: 0.0,
                source_y: 0.0,
                source_page_height: 300.0,
                source_total_height: 900.0,
                output_width: 100.0,
                output_height: 150.0,
                page_count: 3,
                page_rects: vec![],
            },
        )
        .expect("split pdf");

        let split_doc = Document::load(&pdf_path).expect("load split pdf");
        let first_page_id = *split_doc
            .get_pages()
            .values()
            .next()
            .expect("first page id");
        let first_content = String::from_utf8(
            split_doc
                .get_page_content(first_page_id)
                .expect("first page content"),
        )
        .expect("first page content utf8");

        assert!(
            first_content.contains("0 0 cm") || first_content.contains("0.00000000 cm"),
            "first slice must not be shifted down: {first_content}"
        );
    }
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
