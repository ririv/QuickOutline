use serde::{Deserialize, Serialize};
use std::path::Path;
use pdfium_render::prelude::*;
use lopdf::{Document, Object, Dictionary};
use anyhow::Result;

use tauri::{State};
use crate::pdf::manager::{PdfWorker, PdfRequest};
use crate::pdf::page_label::{PageLabelProcessor, PageLabel, PageLabelNumberingStyle};
use crate::pdf::merge::merge_pdfs;
use tokio::sync::oneshot;
use log::{info, warn, error};

#[derive(Debug, Serialize, Deserialize, Clone)]
#[serde(rename_all = "camelCase")]
pub struct TocLinkDto {
    pub toc_page_index: usize,
    pub x: f64,
    pub y: f64,
    pub width: f64,
    pub height: f64,
    pub target_page_index: i32,
    pub target_is_original_doc: bool,
}

#[derive(Debug, Serialize, Deserialize, Clone)]
#[serde(rename_all = "camelCase")]
pub struct TocConfig {
    pub toc_content: String,
    pub title: String,
    pub insert_pos: i32,
    pub toc_page_label: Option<PageLabel>,
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
    info!("Processing TOC generation via shared PdfWorker (ID Mapping Strategy)");
    let toc_pdf_path = config.toc_pdf_path.as_ref().ok_or("No TOC PDF path provided")?.clone();
    let final_dest = resolve_dest_path(&src_path, dest_path);
    let insert_pos = config.insert_pos as u16;

    // Step 1: Capture Original Page IDs (ID Mapping Strategy)
    let original_page_ids = {
        let doc = Document::load(&src_path).map_err(|e| format!("Failed to load source for ID mapping: {}", e))?;
        let pages = doc.get_pages();
        let mut ids = vec![];
        let mut sorted_keys: Vec<_> = pages.keys().cloned().collect();
        sorted_keys.sort();
        for key in sorted_keys {
            ids.push(pages[&key]);
        }
        ids
    };

    // Step 2: Merge TOC using Pdfium (Safe Rust) via helper
    let main_doc = merge_pdfs(pdfium, &src_path, &toc_pdf_path, insert_pos)
        .map_err(|e| format!("Pdfium Merge Error: {}", e))?;
    
    // Save to memory buffer
    let pdf_bytes = main_doc.save_to_bytes().map_err(|e| e.to_string())?;
    
    // Step 3: Inject Links using lopdf and the captured IDs
    let mut doc = Document::load_mem(&pdf_bytes).map_err(|e| e.to_string())?;
    
    if let Some(links) = config.links {
        if !links.is_empty() {
            add_links_to_lopdf_doc(&mut doc, links, config.insert_pos as usize, &original_page_ids)?;
        }
    }

        // Step 4: Correct Page Labels
        apply_toc_page_labels(
            &mut doc, 
            &src_path, 
            &toc_pdf_path, 
            config.insert_pos, 
            config.toc_page_label.as_ref()
        );
    
        doc.save(&final_dest).map_err(|e| e.to_string())?;
        info!("TOC generation complete: {}", final_dest);
    Ok(final_dest)
}

fn apply_toc_page_labels(
    doc: &mut Document,
    src_path: &str,
    toc_path: &str,
    insert_pos: i32,
    toc_label_opt: Option<&PageLabel>
) {
    if let Ok(src_doc) = Document::load(src_path) {
        if let Ok(rules) = PageLabelProcessor::get_page_label_rules_from_doc(&src_doc) {
            // Try to load TOC rules
            let (toc_len, toc_rules) = if let Ok(toc_doc) = Document::load(toc_path) {
                let len = toc_doc.get_pages().len() as i32;
                let r = PageLabelProcessor::get_page_label_rules_from_doc(&toc_doc).unwrap_or_default();
                (len, r)
            } else {
                (0, vec![])
            };

            if toc_len > 0 {
                // Optimization: If both docs have no PageLabel rules AND no label config requested, do nothing.
                if rules.is_empty() && toc_rules.is_empty() && toc_label_opt.is_none() {
                    return;
                }

                let new_rules = calculate_merged_rules(rules, insert_pos, toc_len, toc_rules, toc_label_opt);
                let _ = PageLabelProcessor::set_page_labels_in_doc(doc, new_rules);
            }
        }
    }
}

fn calculate_merged_rules(
    mut rules: Vec<PageLabel>, 
    insert_pos: i32, 
    toc_len: i32,
    mut toc_rules: Vec<PageLabel>,
    toc_label_opt: Option<&PageLabel>
) -> Vec<PageLabel> {
    let insert_idx_1based = insert_pos + 1;
    let resume_idx_1based = insert_idx_1based + toc_len;

    // 1. Identify the "Impact Rule"
    let mut impact_rule_idx = None;
    for (i, rule) in rules.iter().enumerate() {
        if rule.page_num <= insert_idx_1based {
            impact_rule_idx = Some(i);
        } else {
            break;
        }
    }

    // 2. Prepare Resume Rule
    let mut resume_rule = None;
    let exact_match = rules.iter().any(|r| r.page_num == insert_idx_1based);
    
    if !exact_match {
        let (style, prefix, start_num) = if let Some(idx) = impact_rule_idx {
            let r = &rules[idx];
            let offset = insert_pos - (r.page_num - 1);
            (r.numbering_style.clone(), r.label_prefix.clone(), r.first_page.unwrap_or(1) + offset)
        } else {
            (PageLabelNumberingStyle::DecimalArabicNumerals, None, insert_pos + 1)
        };

        resume_rule = Some(PageLabel {
            page_num: resume_idx_1based,
            numbering_style: style,
            label_prefix: prefix,
            first_page: Some(start_num),
        });
    }

    // 3. Shift existing rules
    for rule in &mut rules {
        if rule.page_num >= insert_idx_1based {
            rule.page_num += toc_len;
        }
    }

    // 4. Insert TOC rules
    if toc_rules.is_empty() {
        // Use requested label config or fallback to Decimal
        let mut new_rule = if let Some(l) = toc_label_opt {
            l.clone()
        } else {
            PageLabel {
                page_num: 1, // Placeholder
                numbering_style: PageLabelNumberingStyle::DecimalArabicNumerals,
                label_prefix: None,
                first_page: Some(1),
            }
        };
        
        // Ensure the rule starts at the correct position
        new_rule.page_num = insert_idx_1based;
        rules.push(new_rule);
    } else {
        // Shift and merge TOC rules
        for tr in &mut toc_rules {
            tr.page_num += insert_pos; 
        }
        rules.extend(toc_rules);
    }

    if let Some(rr) = resume_rule {
        rules.push(rr);
    }
    
    rules
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

fn add_links_to_lopdf_doc(
    doc: &mut Document, 
    links: Vec<TocLinkDto>, 
    insert_pos: usize, 
    original_page_ids: &[lopdf::ObjectId]
) -> Result<(), String> {
    let merged_pages_map = doc.get_pages(); 
    let mut merged_pages_list: Vec<lopdf::ObjectId> = vec![];
    let mut sorted_keys: Vec<_> = merged_pages_map.keys().cloned().collect();
    sorted_keys.sort();
    for key in sorted_keys {
        merged_pages_list.push(merged_pages_map[&key]);
    }
    
    for link in links {
        let toc_page_idx_in_merged = insert_pos + link.toc_page_index;
        if toc_page_idx_in_merged >= merged_pages_list.len() { continue; }
        let source_page_id = merged_pages_list[toc_page_idx_in_merged];
        
        // Convert 1-based page index to 0-based index
        // Note: The frontend is now expected to handle the 0-based/1-based logic and pass a clean 0-based index in `target_page_index`.
        // However, if we want to be safe or if the frontend logic varies, we should clarify.
        // Assuming frontend sends 0-based index now as per our discussion.
        let target_idx = link.target_page_index as usize;

        let target_page_id = if link.target_is_original_doc {
            // Mapping Strategy: Use original ID to find the page, regardless of where it moved
            if target_idx >= original_page_ids.len() { continue; }
            original_page_ids[target_idx]
        } else {
            // Absolute Index Strategy: Use the index directly on the merged document
            if target_idx >= merged_pages_list.len() { continue; }
            merged_pages_list[target_idx]
        };
        
        let page_height = {
            let page_dict = doc.get_object(source_page_id).map_err(|e| e.to_string())?.as_dict().map_err(|e| e.to_string())?;
            
            let box_array = page_dict.get(b"CropBox")
                .or_else(|_| page_dict.get(b"MediaBox"))
                .and_then(|o| o.as_array())
                .map(|a| a.iter().map(|n| n.as_float().unwrap_or(0.0) as f64).collect::<Vec<f64>>());
            
            if let Ok(ba) = box_array {
                if ba.len() >= 4 { ba[3] } else { 842.0 }
            } else {
                842.0 
            }
        };
        
        let rect = vec![
            Object::Real(link.x as f32), 
            Object::Real((page_height - (link.y + link.height)) as f32), 
            Object::Real((link.x + link.width) as f32), 
            Object::Real((page_height - link.y) as f32)
        ];
        
        let annotation = Dictionary::from_iter(vec![
            ("Type", Object::Name(b"Annot".to_vec())), 
            ("Subtype", Object::Name(b"Link".to_vec())), 
            ("Rect", Object::Array(rect)), 
            ("Border", Object::Array(vec![Object::Integer(0), Object::Integer(0), Object::Integer(0)])), 
            ("Dest", Object::Array(vec![Object::Reference(target_page_id), Object::Name(b"Fit".to_vec())]))
        ]);
        let ann_id = doc.add_object(annotation);
        
        if let Ok(page) = doc.get_object_mut(source_page_id).and_then(|o| o.as_dict_mut()) {
             if !page.has(b"Annots") { page.set("Annots", Object::Array(vec![])); }
             if let Ok(annots) = page.get_mut(b"Annots").and_then(|o| o.as_array_mut()) { 
                 annots.push(Object::Reference(ann_id)); 
             }
        }
    }
    Ok(())
}
