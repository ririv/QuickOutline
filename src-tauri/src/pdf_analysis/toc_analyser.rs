use super::models::{PdfBlock, PdfStyle};
use std::collections::HashMap;
use regex::Regex;
use once_cell::sync::Lazy;

use log::{info, debug};

#[allow(clippy::expect_used)]
static TOC_DOT_PATTERN: Lazy<Regex> = Lazy::new(|| Regex::new(r".*([.]\s*|\s{2,}){4,}\s*\d+\s*$").expect("Invalid TOC_DOT_PATTERN"));
#[allow(clippy::expect_used)]
static TOC_NUMERIC_END_PATTERN: Lazy<Regex> = Lazy::new(|| Regex::new(r"^(.*[^0-9])\s+([0-9]+)\s*$").expect("Invalid TOC_NUMERIC_END_PATTERN"));

pub struct TocAnalyser;

impl TocAnalyser {
    pub fn find_dominant_style(all_blocks: &Vec<PdfBlock>) -> PdfStyle {
        if all_blocks.is_empty() { return PdfStyle::new("Default".to_string(), 10.0); }
        let mut counts = HashMap::new();
        for block in all_blocks {
            let style = block.get_primary_style();
            *counts.entry(style).or_insert(0) += 1;
        }
        counts.into_iter().max_by_key(|&(_, count)| count).map(|(style, _)| style.clone())
            .unwrap_or_else(|| PdfStyle::new("Default".to_string(), 10.0))
    }

    pub fn find_toc_blocks_in_page(page_blocks: &mut Vec<PdfBlock>, dominant_style: &PdfStyle) -> Vec<PdfBlock> {
        let mut candidates = Vec::new();
        for block in page_blocks {
            if Self::is_toc_like_block(block, dominant_style) {
                candidates.push(block.clone());
            }
        }
        // Candidates threshold: minimum 3 blocks per page
        if candidates.len() >= 3 { candidates } else { Vec::new() }
    }

    fn is_toc_like_block(block: &mut PdfBlock, dominant_style: &PdfStyle) -> bool {
        let text_raw = block.get_text_plain().to_string();
        let text = text_raw.trim();
        
        // 100% Replica: use char count for length checks
        let char_count = text.chars().count();
        if char_count > 150 || char_count < 3 { return false; }
        
        if TOC_DOT_PATTERN.is_match(text) { return true; }
        
        if TOC_NUMERIC_END_PATTERN.is_match(text) {
            let style = block.get_primary_style();
            let is_abnormal = style.get_size() > dominant_style.get_size() + 0.5 && char_count < 80;
            return is_abnormal && !text.ends_with('.') && !text.ends_with('。');
        }
        false
    }
}
