use pdfium_render::prelude::*;
use crate::pdf_analysis::models::*;
use crate::pdf_analysis::text_metrics::TextMetrics;
use regex::Regex;
use std::sync::LazyLock;

static NUMBERING_REGEX: LazyLock<Regex> = LazyLock::new(|| {
    Regex::new(r"^\s*([\d.]+|[A-Za-z][.]|[IVXLCDM]+[.)]).*\s*$")
        .expect("Invalid NUMBERING_REGEX pattern")
});
static PUNCTUATION_REGEX: LazyLock<Regex> = LazyLock::new(|| {
    Regex::new(r"[.!?:：。！？．]$")
        .expect("Invalid PUNCTUATION_REGEX pattern")
});

pub struct PdfProcessor;

impl PdfProcessor {
    pub fn extract_blocks_from_page(page: &PdfPage, page_num: i32) -> Vec<PdfBlock> {
        let mut blocks: Vec<PdfBlock> = Vec::new();
        let page_text = match page.text() {
            Ok(t) => t,
            Err(_) => return blocks,
        };

        // 100% Replica: Sort by Baseline Y (origin_y) desc, then X asc
        let chars_collection = page_text.chars();
        let mut sorted_chars: Vec<_> = chars_collection.iter().collect();
        sorted_chars.sort_by(|a, b| {
            let ay = a.origin_y().map(|p| p.value).unwrap_or(0.0);
            let by = b.origin_y().map(|p| p.value).unwrap_or(0.0);
            let ax = a.origin_x().map(|p| p.value).unwrap_or(0.0);
            let bx = b.origin_x().map(|p| p.value).unwrap_or(0.0);
            
            by.partial_cmp(&ay).unwrap_or(std::cmp::Ordering::Equal)
                .then(ax.partial_cmp(&bx).unwrap_or(std::cmp::Ordering::Equal))
        });

        let mut current_line: Option<PdfLine> = None;
        let mut last_char_baseline_y: f32 = 0.0;
        let mut last_char_right: f32 = 0.0;

        for char_info in sorted_chars {
            let text = char_info.unicode_string().unwrap_or_default();
            let bounds = char_info.tight_bounds()
                .or_else(|_| char_info.loose_bounds())
                .unwrap_or_else(|_| PdfRect::new(PdfPoints::ZERO, PdfPoints::ZERO, PdfPoints::ZERO, PdfPoints::ZERO));
            
            let baseline_y = char_info.origin_y().map(|p| p.value).unwrap_or(bounds.bottom.value);
            let space_width = TextMetrics::calculate_visual_space_width(&char_info);

            // Calculate skew for every char to store in chunk
            let skew = if let Ok(m) = char_info.matrix() {
                let (a, b, c, d) = (m.a(), m.b(), m.c(), m.d());
                if a != 0.0 && d != 0.0 {
                    (b / a).powi(2) + (c / d).powi(2)
                } else { 0.0 }
            } else { 0.0 };

            // 100% Replica: Line break detection based on Baseline Y deviation (> 1.0pt)
            let is_new_line = if let Some(ref _line) = current_line {
                (baseline_y - last_char_baseline_y).abs() > 1.0
            } else {
                true
            };

            if is_new_line {
                if let Some(line) = current_line.take() {
                    Self::add_line_to_blocks(&mut blocks, line);
                }
                
                let style = PdfStyle::new(char_info.font_name(), char_info.scaled_font_size().value);
                // 100% Replica: pass baseline_y (origin_y) as line's Y
                current_line = Some(PdfLine::new(bounds, page_num, style, skew as f64, baseline_y));
            }

            if let Some(ref mut line) = current_line {
                line.append_char_replica(&text, bounds, space_width, last_char_right);
                
                let chunk = TextChunk {
                    text: text.clone(),
                    x: bounds.left.value,
                    y: baseline_y,
                    width: bounds.right.value - bounds.left.value,
                    font_name: char_info.font_name(),
                    font_size: char_info.scaled_font_size().value,
                    single_space_width: space_width,
                    skew: skew as f64,
                };
                line.add_chunk(chunk);
            }

            last_char_baseline_y = baseline_y;
            last_char_right = bounds.right.value;
        }

        if let Some(line) = current_line {
            Self::add_line_to_blocks(&mut blocks, line);
        }

        blocks
    }

    fn add_line_to_blocks(blocks: &mut Vec<PdfBlock>, line: PdfLine) {
        if line.text.trim().is_empty() { return; }

        if let Some(last_block) = blocks.last_mut() {
            if Self::should_merge_lines(last_block, &line) {
                last_block.merge_line(line);
                return;
            }
        }
        blocks.push(PdfBlock::from_line(line));
    }

    fn should_merge_lines(block: &PdfBlock, next_line: &PdfLine) -> bool {
        let last_line = match block.lines.last() {
            Some(l) => l,
            None => return true,
        };

        if last_line.page_num != next_line.page_num { return false; }

        // 100% Replica: Vertical distance between baselines (Java uses getY())
        let v_gap = (last_line.y - next_line.y).abs();
        
        // Optimization: Use average font size instead of primary style size for better robustness
        if v_gap > last_line.avg_font_size * 1.8 { return false; }

        if last_line.style != next_line.style { return false; }

        if (last_line.bounds.left - next_line.bounds.left).abs() > 5.0 { return false; }

        let prev_text = last_line.text.trim();
        if prev_text.is_empty() { return true; }

        if PUNCTUATION_REGEX.is_match(prev_text) { return false; }

        let next_text = next_line.text.trim();
        if NUMBERING_REGEX.is_match(next_text) { return false; }

        let is_lowercase_continuation = next_text.chars().next().map_or(false, |c| c.is_lowercase());
        if is_lowercase_continuation {
            return true;
        }

        prev_text.chars().count() <= 60
    }
}