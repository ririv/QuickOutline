use pdfium_render::prelude::*;
use crate::pdf_analysis::models::*;
use crate::pdf_analysis::text_metrics::TextMetrics;
use regex::Regex;
use std::sync::LazyLock;

#[allow(clippy::expect_used)]
static NUMBERING_REGEX: LazyLock<Regex> = LazyLock::new(|| {
    Regex::new(r"^\s*([\d.]+|[A-Za-z][.]|[IVXLCDM]+[.)]).*\s*$")
        .expect("Invalid NUMBERING_REGEX pattern")
});
#[allow(clippy::expect_used)]
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

        // 1. Sort characters by reading order (Top-to-Bottom, Left-to-Right)
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

        // 2. Pre-calculate metrics: inferred advance widths and page-global space width
        let (page_global_space_width, inferred_widths) = Self::calculate_page_metrics(&sorted_chars);

        // 3. Build lines and blocks
        let mut current_line: Option<PdfLine> = None;
        let mut last_char_baseline_y: f32 = 0.0;
        let mut last_char_right: f32 = 0.0;

        for (i, char_info) in sorted_chars.into_iter().enumerate() {
            let mut text = char_info.unicode_string().unwrap_or_default();
            // Sanitize text: remove control characters that might cause double newlines
            if text.contains('\r') || text.contains('\n') {
                text = text.replace('\r', "").replace('\n', "");
            }
            
            // Use loose_bounds for layout consistency
            let bounds = char_info.loose_bounds()
                .or_else(|_| char_info.tight_bounds())
                .unwrap_or_else(|_| PdfRect::new(PdfPoints::ZERO, PdfPoints::ZERO, PdfPoints::ZERO, PdfPoints::ZERO));
            
            let baseline_y = char_info.origin_y().map(|p| p.value).unwrap_or(bounds.bottom.value);
            
            let space_width = page_global_space_width;
            let advance_width = inferred_widths.get(i).cloned().unwrap_or_else(|| bounds.width().value);

            let skew = if let Ok(m) = char_info.matrix() {
                let (a, b, c, d) = (m.a(), m.b(), m.c(), m.d());
                if a != 0.0 && d != 0.0 { (b / a).powi(2) + (c / d).powi(2) } else { 0.0 }
            } else { 0.0 };

            // Line break detection based on Baseline Y deviation (> 1.0pt)
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
                // Use baseline_y (origin_y) as the primary vertical coordinate for the line
                current_line = Some(PdfLine::new(bounds, page_num, style, skew as f64, baseline_y));
            }

            if let Some(ref mut line) = current_line {
                line.append_char_replica(&text, bounds, space_width, last_char_right);
                
                let chunk = TextChunk {
                    text: text.clone(),
                    x: bounds.left.value,
                    y: baseline_y,
                    width: advance_width, // Use inferred advance width from layout context
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

    /// Calculates page-level metrics:
    /// 1. `page_global_space_width`: Heuristic based on actual space glyphs or average character width.
    /// 2. `inferred_widths`: A vector of inferred advance widths for each character based on positioning.
    fn calculate_page_metrics(sorted_chars: &[PdfPageTextChar]) -> (f32, Vec<f32>) {
        let mut inferred_widths = vec![0.0; sorted_chars.len()];
        let mut total_width = 0.0;
        let mut char_count = 0;
        let mut space_char_width = 0.0;

        for i in 0..sorted_chars.len() {
            let curr = &sorted_chars[i];
            let curr_rect = curr.loose_bounds().or_else(|_| curr.tight_bounds()).unwrap_or(PdfRect::new(PdfPoints::ZERO, PdfPoints::ZERO, PdfPoints::ZERO, PdfPoints::ZERO));
            
            let mut width = curr_rect.width().value;

            // Try to infer advance width by measuring distance to the next character in the same line
            if i + 1 < sorted_chars.len() {
                let next = &sorted_chars[i + 1];
                let curr_y = curr.origin_y().map(|p| p.value).unwrap_or(0.0);
                let next_y = next.origin_y().map(|p| p.value).unwrap_or(0.0);
                
                if (curr_y - next_y).abs() < 1.0 {
                    let curr_x = curr.loose_bounds().map(|r| r.left.value).unwrap_or(0.0);
                    let next_x = next.loose_bounds().map(|r| r.left.value).unwrap_or(0.0);
                    
                    let advance = next_x - curr_x;
                    let font_size = curr.scaled_font_size().value;
                    // Ensure the advance is positive and within a reasonable range for a single character
                    if advance > 0.0 && advance < font_size * 1.5 {
                        width = advance;
                    }
                }
            }

            inferred_widths[i] = width;
            total_width += width;
            char_count += 1;
            
            if curr.unicode_string().as_deref() == Some(" ") {
                space_char_width = width;
            }
        }

        let avg_width = if char_count > 0 { total_width / char_count as f32 } else { 0.0 };
        let global_space_width = if space_char_width > 0.0 { space_char_width } else { avg_width };
        
        (global_space_width, inferred_widths)
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

        // Use vertical distance between baselines for precise line spacing measurement
        let v_gap = (last_line.y - next_line.y).abs();
        
        // Adaptive threshold using average font size for improved robustness
        if v_gap > last_line.avg_font_size * 1.8 { return false; }

        if last_line.style != next_line.style { return false; }

        if (last_line.bounds.left - next_line.bounds.left).abs() > 5.0 { return false; }

        let prev_text = last_line.text.trim();
        if prev_text.is_empty() { return true; }

        // Block splitting based on sentence-ending punctuation
        if PUNCTUATION_REGEX.is_match(prev_text) { return false; }

        let next_text = next_line.text.trim();
        // Prevent merging if the next line starts with a clear numbering pattern
        if NUMBERING_REGEX.is_match(next_text) { return false; }

        let is_lowercase_continuation = next_text.chars().next().map_or(false, |c| c.is_lowercase());
        if is_lowercase_continuation {
            return true;
        }

        // Conservative merge for short lines that don't end with punctuation
        prev_text.chars().count() <= 60
    }
}