use serde::{Deserialize, Serialize};
use std::path::Path;
use anyhow::Result;
use tauri::{State};
use crate::pdf::manager::{PdfWorker};
use crate::pdf::page_label::{PageLabel, PageLabelNumberingStyle};
use log::info;
use crate::pdf::toc_traits::{TocMerger, TocEditor};

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

pub fn process_toc_generation<M: TocMerger, E: TocEditor>(
    merger: &M,
    editor: &mut E,
    config: TocConfig,
    src_path: &str,
    is_memory_mode: bool,
    memory_ptr: Option<*mut [u8]>,
    dest_path: Option<String>
) -> Result<String, String> {
    info!("Processing TOC generation (Coordinated Flow)");
    let toc_pdf_path = config.toc_pdf_path.as_ref().ok_or("No TOC PDF path provided")?.clone();
    let final_dest = resolve_dest_path(src_path, dest_path);
    let insert_pos = config.insert_pos as u16;

    // 1. Capture Page Identifiers (lopdf)
    let original_page_ids = editor.capture_page_identifiers().map_err(|e| e.to_string())?;

    // 2. Merge (pdfium)
    let merged_bytes = merger.merge_toc_pdf(src_path, memory_ptr, is_memory_mode, &toc_pdf_path, insert_pos).map_err(|e| e.to_string())?;
    
    // 3. Links (lopdf)
    let mut final_bytes = merged_bytes;
    if let Some(links) = config.links {
        if !links.is_empty() {
            final_bytes = editor.inject_links(&final_bytes, links, config.insert_pos as usize, &original_page_ids).map_err(|e| e.to_string())?;
        }
    }

    // 4. Page Labels (lopdf)
    final_bytes = editor.apply_page_labels(&final_bytes, &toc_pdf_path, config.insert_pos, config.toc_page_label.as_ref()).map_err(|e| e.to_string())?;
    
    // 5. Save
    std::fs::write(&final_dest, final_bytes).map_err(|e| e.to_string())?;

    info!("TOC generation complete: {}", final_dest);
    Ok(final_dest)
}

pub fn calculate_merged_rules(
    mut rules: Vec<PageLabel>, 
    insert_pos: i32, 
    toc_len: i32,
    mut toc_rules: Vec<PageLabel>,
    toc_label_opt: Option<&PageLabel>
) -> Vec<PageLabel> {
    let insert_idx_1based = insert_pos + 1;
    let resume_idx_1based = insert_idx_1based + toc_len;

    let mut impact_rule_idx = None;
    for (i, rule) in rules.iter().enumerate() {
        if rule.page_index <= insert_idx_1based {
            impact_rule_idx = Some(i);
        } else {
            break;
        }
    }

    let mut resume_rule = None;
    let exact_match = rules.iter().any(|r| r.page_index == insert_idx_1based);
    
    if !exact_match {
        let (style, prefix, start_num) = if let Some(idx) = impact_rule_idx {
            let r = &rules[idx];
            let offset = insert_pos - (r.page_index - 1);
            (r.numbering_style.clone(), r.label_prefix.clone(), r.start_value.unwrap_or(1) + offset)
        } else {
            (PageLabelNumberingStyle::DecimalArabicNumerals, None, insert_pos + 1)
        };

        resume_rule = Some(PageLabel {
            page_index: resume_idx_1based,
            numbering_style: style,
            label_prefix: prefix,
            start_value: Some(start_num),
        });
    }

    for rule in &mut rules {
        if rule.page_index >= insert_idx_1based {
            rule.page_index += toc_len;
        }
    }

    if toc_rules.is_empty() {
        let mut new_rule = if let Some(l) = toc_label_opt {
            l.clone()
        } else {
            PageLabel {
                page_index: 1,
                numbering_style: PageLabelNumberingStyle::DecimalArabicNumerals,
                label_prefix: None,
                start_value: Some(1),
            }
        };
        
        new_rule.page_index = insert_idx_1based;
        rules.push(new_rule);
    } else {
        for tr in &mut toc_rules {
            tr.page_index += insert_pos;
        }
        rules.extend(toc_rules);
    }

    if let Some(rr) = resume_rule {
        rules.push(rr);
    }
    
    rules.sort_by_key(|r| r.page_index);
    rules
}

#[tauri::command]
pub async fn generate_toc_page(
    pdf_worker: State<'_, PdfWorker>,
    src_path: String,
    config: TocConfig,
    dest_path: Option<String>
) -> Result<String, String> {
    pdf_worker.call(move |state| -> Result<String, String> {
        let pdfium = state.pdfium;
        let src_path_clone = src_path.clone();
        
        match state.get_session_mut(&src_path) {
            Ok(session) => {
                let is_mem = session.mode == crate::pdf::manager::LoadMode::MemoryBuffer;
                let ptr = session.memory_ptr;
                
                let merger = crate::pdf::pdfium_render::toc_merger_adapter::PdfiumTocAdapter::new(pdfium);
                let mut editor = crate::pdf::lopdf::toc_editor_adapter::LopdfTocAdapter::new(session);
                
                process_toc_generation(&merger, &mut editor, config, &src_path_clone, is_mem, ptr, dest_path)
            },
            Err(e) => Err(e.to_string())
        }
    }).await.map_err(|e| e.to_string())?
}