use pdfium_render::prelude::*;
use crate::pdf_analysis::processor::PdfProcessor;
use crate::pdf_analysis::toc_analyser::TocAnalyser;
use crate::pdf_analysis::models::PdfBlock;
use anyhow::{Result, format_err};
use std::collections::BTreeMap;
use rayon::prelude::*; // Phase 2/3 are CPU-bound and safe for Rayon
use log::{info, debug};

pub struct TocExtractor;

impl TocExtractor {
    pub fn extract_toc(pdfium: &Pdfium, path: &str) -> Result<Vec<String>> {
        info!("[TocExtractor] Starting extraction for: {}", path);

        // 1. Get page count using the passed Pdfium instance
        // No mutex needed if we are running serially (or if we trust the singleton in serial context)
        // But let's use the global getter to be consistent with mod.rs changes
        let global_pdfium = crate::pdf::get_pdfium().map_err(|e| format_err!("Global Pdfium not ready: {}", e))?;
        
        let num_pages = {
            let _guard = crate::pdf::PDF_LOAD_MUTEX.lock().unwrap();
            let doc = global_pdfium.load_pdf_from_file(path, None)
                .map_err(|e| format_err!("Failed to load PDF: {}", e))?;
            doc.pages().len()
        };
        info!("[TocExtractor] Total pages: {}", num_pages);

        if num_pages == 0 {
            return Ok(Vec::new());
        }

        // 2. Strategy: Serial Extraction (Plan S)
        // Due to persistent stability issues with multi-threaded PDFium usage (Deadlocks on dlopen, Segfaults on shared handles),
        // we fallback to serial extraction to guarantee stability. Rust's single-thread performance is still very good.
        // We still iterate by chunks to keep the logic structure similar and allow future parallelization attempts.
        let chunk_size = 50; // Reasonable batch size
        let chunks: Vec<(u16, u16)> = (0..num_pages)
            .step_by(chunk_size)
            .map(|start| {
                let end = std::cmp::min(start + chunk_size as u16, num_pages);
                (start, end)
            })
            .collect();

        info!("[TocExtractor] Processing {} chunks serially...", chunks.len());
        
        let mut all_blocks = Vec::new();

        // Single Pdfium instance, single thread, serial execution. Safe.
        // We still use the Mutex to be good citizens in the process.
        {
            // We can hold the doc open for the whole loop if we want, or open/close per chunk.
            // Opening once is faster.
            let _guard = crate::pdf::PDF_LOAD_MUTEX.lock().unwrap();
            let doc = global_pdfium.load_pdf_from_file(path, None)
                .map_err(|e| format_err!("Failed to load PDF loop: {}", e))?;
            
            for (start, end) in chunks {
                debug!("[TocExtractor] Processing chunk {}..{}", start, end);
                for i in start..end {
                    if let Ok(page) = doc.pages().get(i) {
                        let page_num = i as i32 + 1;
                        let page_blocks = PdfProcessor::extract_blocks_from_page(&page, page_num);
                        all_blocks.extend(page_blocks);
                    }
                }
            }
        } // Doc closed, mutex released

        info!("[TocExtractor] Total raw blocks extracted: {}", all_blocks.len());

        if all_blocks.is_empty() {
            return Ok(Vec::new());
        }

        // 3. Phase 2: Global Style Analysis (Safe for Rayon or Serial)
        let dominant_style = TocAnalyser::find_dominant_style(&all_blocks);
        info!("[TocExtractor] Dominant style: {:?} (Size: {})", dominant_style.font_name, dominant_style.get_size());

        // 4. Phase 3: Parallel Analysis per Page (Pure CPU-bound, perfectly safe for Rayon)
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