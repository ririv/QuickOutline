use pdfium_render::prelude::*;
use crate::pdf_analysis::processor::PdfProcessor;
use crate::pdf_analysis::toc_analyser::TocAnalyser;
use crate::pdf_analysis::models::PdfBlock;
use anyhow::{Result, format_err};
use std::collections::BTreeMap;
use rayon::prelude::*;

pub struct TocExtractor;

impl TocExtractor {
    pub fn extract_toc(pdfium: &Pdfium, path: &str) -> Result<Vec<String>> {
        // 1. Get page count using the passed (already initialized) Pdfium instance
        let num_pages = {
            let doc = pdfium.load_pdf_from_file(path, None)
                .map_err(|e| format_err!("Failed to load PDF: {}", e))?;
            doc.pages().len()
        };

        if num_pages == 0 {
            return Ok(Vec::new());
        }

        // 2. Strategy: Chunk-based Parallelism with Thread-Local Pdfium
        // We create a new Pdfium binding PER THREAD/CHUNK to ensure safety.
        // Replicates Java's "new PdfDocument per thread" logic but optimized with chunks.
        let num_threads = rayon::current_num_threads().max(1);
        let chunk_size = (num_pages as usize + num_threads - 1) / num_threads;

        let chunks: Vec<(u16, u16)> = (0..num_pages)
            .step_by(chunk_size)
            .map(|start| {
                let end = std::cmp::min(start + chunk_size as u16, num_pages);
                (start, end)
            })
            .collect();

        let all_blocks: Vec<PdfBlock> = chunks.into_par_iter()
            .map(|(start, end)| {
                // Initialize thread-local Pdfium using shared utility
                let local_pdfium = match crate::pdf::init_pdfium() {
                    Ok(p) => p,
                    Err(_) => return Vec::new(),
                };

                // Open document
                match local_pdfium.load_pdf_from_file(path, None) {
                    Ok(doc) => {
                        let mut chunk_blocks = Vec::new();
                        for i in start..end {
                            if let Ok(page) = doc.pages().get(i) {
                                let page_num = i as i32 + 1;
                                let blocks = PdfProcessor::extract_blocks_from_page(&page, page_num);
                                chunk_blocks.extend(blocks);
                            }
                        }
                        chunk_blocks
                    },
                    Err(_) => Vec::new(),
                }
            })
            .flatten()
            .collect();

        if all_blocks.is_empty() {
            return Ok(Vec::new());
        }

        // 3. Phase 2: Global Style Analysis
        let dominant_style = TocAnalyser::find_dominant_style(&all_blocks);

        // 4. Phase 3: Parallel Analysis per Page
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

        Ok(toc_lines)
    }
}