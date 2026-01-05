use crate::pdf_analysis::processor::PdfProcessor;
use crate::pdf_analysis::toc_analyser::TocAnalyser;
use crate::pdf_analysis::models::PdfBlock;
use crate::pdf_analysis::traits::PdfDocumentTrait;
use anyhow::Result;
use std::collections::BTreeMap;
use rayon::prelude::*;
use log::{info, debug};

pub struct TocExtractor;

impl TocExtractor {
    pub fn extract_toc<D: PdfDocumentTrait + ?Sized>(doc: &D) -> Result<Vec<String>> {
        info!("[TocExtractor] Starting extraction (Abstract Document)");
        
        let num_pages = doc.page_count();
        info!("[TocExtractor] Total pages: {}", num_pages);

        if num_pages == 0 {
            return Ok(Vec::new());
        }

        let chunk_size = 50;
        let chunks: Vec<(u16, u16)> = (0..num_pages)
            .step_by(chunk_size)
            .map(|start| {
                let end = std::cmp::min(start + chunk_size as u16, num_pages);
                (start, end)
            })
            .collect();

        info!("[TocExtractor] Processing {} chunks serially...", chunks.len());
        
        let mut all_blocks = Vec::new();

        {
            for (start, end) in chunks {
                debug!("[TocExtractor] Processing chunk {}..{}", start, end);
                for i in start..end {
                    if let Ok(page) = doc.get_page(i) {
                        let page_num = i as i32 + 1;
                        let page_blocks = PdfProcessor::extract_blocks_from_page(page.as_ref(), page_num);
                        all_blocks.extend(page_blocks);
                    }
                }
            }
        } 

        info!("[TocExtractor] Total raw blocks extracted: {}", all_blocks.len());

        if all_blocks.is_empty() {
            return Ok(Vec::new());
        }

        let dominant_style = TocAnalyser::find_dominant_style(&all_blocks);
        info!("[TocExtractor] Dominant style: {:?} (Size: {})", dominant_style.font_name, dominant_style.get_size());

        let mut blocks_by_page: BTreeMap<i32, Vec<PdfBlock>> = BTreeMap::new();
        for block in all_blocks {
            blocks_by_page.entry(block.get_page_num()).or_default().push(block);
        }

        let pages_blocks: Vec<Vec<PdfBlock>> = blocks_by_page.into_values().collect();

        let toc_results: Vec<Vec<String>> = pages_blocks.into_par_iter()
            .map(|mut page_blocks| {
                let filtered = TocAnalyser::find_toc_blocks_in_page(&mut page_blocks, &dominant_style);
                filtered.into_iter()
                    .map(|b| b.get_text_reconstructed())
                    .collect()
            })
            .collect();

        let toc_lines: Vec<String> = toc_results.into_iter().flatten().collect();
        info!("[TocExtractor] Final TOC lines: {}", toc_lines.len());

        Ok(toc_lines)
    }
}