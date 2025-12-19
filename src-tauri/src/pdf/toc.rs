use serde::{Deserialize, Serialize};
use std::path::Path;
use pdfium_render::prelude::*;
use lopdf::{Document, Object, Dictionary};
use anyhow::Result;

use tauri::{State};
use crate::pdf::manager::{PdfWorker, PdfRequest};
use tokio::sync::oneshot;

#[derive(Debug, Serialize, Deserialize, Clone)]
#[serde(rename_all = "camelCase")]
pub struct TocLinkDto {
    pub toc_page_index: usize,
    pub x: f64,
    pub y: f64,
    pub width: f64,
    pub height: f64,
    pub target_page_index: i32,
}

#[derive(Debug, Serialize, Deserialize, Clone)]
#[serde(rename_all = "camelCase")]
pub struct TocConfig {
    pub toc_content: String,
    pub title: String,
    pub insert_pos: i32,
    pub toc_pdf_path: Option<String>,
    pub links: Option<Vec<TocLinkDto>>,
}

pub fn resolve_dest_path(src_path: &str, dest_path: Option<String>) -> String {
    if let Some(path) = dest_path {
        if !path.trim().is_empty() {
            return path;
        }
    }
    let src = Path::new(src_path);
    let parent = src.parent().unwrap_or(Path::new(""));
    let file_stem = src.file_stem().and_then(|s| s.to_str()).unwrap_or("output");
    let ext = src.extension().and_then(|s| s.to_str()).unwrap_or("pdf");
    let mut candidate_name = format!("{}_new.{}", file_stem, ext);
    let mut candidate_path = parent.join(&candidate_name);
    if !candidate_path.exists() { return candidate_path.to_string_lossy().to_string(); }
    let mut counter = 1;
    while candidate_path.exists() {
        candidate_name = format!("{}_new_{}.{}", file_stem, counter, ext);
        candidate_path = parent.join(&candidate_name);
        counter += 1;
    }
    candidate_path.to_string_lossy().to_string()
}

pub fn process_toc_generation(
    pdfium: &Pdfium,
    src_path: String,
    config: TocConfig,
    dest_path: Option<String>
) -> Result<String, String> {
    println!("Processing TOC generation via shared PdfWorker");
    let toc_pdf_path = config.toc_pdf_path.as_ref().ok_or("No TOC PDF path provided")?.clone();
    let final_dest = resolve_dest_path(&src_path, dest_path);
    let insert_pos = config.insert_pos as u16;

    let main_doc = pdfium.load_pdf_from_file(&src_path, None).map_err(|e| e.to_string())?;
    let toc_doc = pdfium.load_pdf_from_file(&toc_pdf_path, None).map_err(|e| e.to_string())?;

    let toc_len = toc_doc.pages().len();
    if toc_len > 0 {
        // UNSAFE: Directly bypass borrow checker to call mutable methods on PdfPages.
        // main_doc.pages() returns &PdfPages. We cast this reference to a mutable pointer.
        let pages = main_doc.pages();
        unsafe {
            let pages_ptr = pages as *const PdfPages as *mut PdfPages;
            (*pages_ptr).copy_page_range_from_document(&toc_doc, 0..=(toc_len - 1), insert_pos)
                .map_err(|e| format!("Pdfium Import Error: {}", e))?;
        }
    }
    
    // Save to memory buffer to avoid multiple disk IOs
    let pdf_bytes = main_doc.save_to_bytes().map_err(|e| e.to_string())?;
    
    // Load into lopdf for annotation adding (much easier in lopdf)
    let mut doc = Document::load_mem(&pdf_bytes).map_err(|e| e.to_string())?;
    
    if let Some(links) = config.links {
        if !links.is_empty() {
            // Get TOC count from toc_doc (already open)
            let toc_count = toc_doc.pages().len() as usize;
            add_links_to_lopdf_doc(&mut doc, links, config.insert_pos as usize, toc_count)?;
        }
    }

    doc.save(&final_dest).map_err(|e| e.to_string())?;
    println!("TOC generation complete: {}", final_dest);
    Ok(final_dest)
}

#[tauri::command]
pub async fn generate_toc_page(
    pdf_worker: State<'_, PdfWorker>,
    src_path: String,
    config: TocConfig,
    dest_path: Option<String>
) -> Result<String, String> {
    let (tx, rx) = oneshot::channel();
    
    pdf_worker.0.send(PdfRequest::GenerateToc {
        src_path,
        config,
        dest_path,
        response_tx: tx,
    }).await.map_err(|e| format!("Failed to send request to PDF worker: {}", e))?;

    rx.await.map_err(|e| format!("Failed to receive response from PDF worker: {}", e))?
}

fn add_links_to_lopdf_doc(doc: &mut Document, links: Vec<TocLinkDto>, insert_pos: usize, toc_count: usize) -> Result<(), String> {
    let page_ids_map = doc.get_pages(); 
    let mut sorted_pages: Vec<lopdf::ObjectId> = vec![];
    for (_, object_id) in page_ids_map { sorted_pages.push(object_id); }
    
    for link in links {
        let toc_page_idx = insert_pos + link.toc_page_index;
        if toc_page_idx >= sorted_pages.len() { continue; }
        let page_id = sorted_pages[toc_page_idx];
        
        let mut target_idx = link.target_page_index as usize;
        if link.target_page_index >= 0 {
             if target_idx >= insert_pos { target_idx += toc_count; }
        } else { continue; }
        
        if target_idx >= sorted_pages.len() { continue; }
        let target_page_id = sorted_pages[target_idx];
        
        let page_height = {
            let page_dict = doc.get_object(page_id).map_err(|e| e.to_string())?.as_dict().map_err(|e| e.to_string())?;
            let media_box = page_dict.get(b"MediaBox").and_then(|o| o.as_array()).map(|a| a.iter().map(|n| n.as_float().unwrap_or(0.0) as f64).collect::<Vec<f64>>()).unwrap_or(vec![0.0, 0.0, 595.0, 842.0]);
            if media_box.len() >= 4 { media_box[3] } else { 842.0 }
        };
        
        let rect = vec![Object::Real(link.x as f32), Object::Real((page_height - (link.y + link.height)) as f32), Object::Real((link.x + link.width) as f32), Object::Real((page_height - link.y) as f32)];
        let annotation = Dictionary::from_iter(vec![("Type", Object::Name(b"Annot".to_vec())), ("Subtype", Object::Name(b"Link".to_vec())), ("Rect", Object::Array(rect)), ("Border", Object::Array(vec![Object::Integer(0), Object::Integer(0), Object::Integer(0)])), ("Dest", Object::Array(vec![Object::Reference(target_page_id), Object::Name(b"Fit".to_vec())]))]);
        let ann_id = doc.add_object(annotation);
        
        if let Ok(page) = doc.get_object_mut(page_id).and_then(|o| o.as_dict_mut()) {
             if !page.has(b"Annots") { page.set("Annots", Object::Array(vec![])); }
             if let Ok(annots) = page.get_mut(b"Annots").and_then(|o| o.as_array_mut()) { annots.push(Object::Reference(ann_id)); }
        }
    }
    Ok(())
}
