use crate::pdf_analysis::models::*;
use crate::pdf_analysis::traits::*;
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
    pub fn extract_blocks_from_page(page: &dyn PdfPageTrait, page_num: i32) -> Vec<PdfBlock> {
        let mut blocks: Vec<PdfBlock> = Vec::new();
        let chars_collection = page.extract_chars();

        // 1. Sort characters by reading order (Top-to-Bottom, Left-to-Right)
        let mut sorted_chars = chars_collection;
        sorted_chars.sort_by(|a, b| {
            let ay = a.y;
            let by = b.y;
            let ax = a.x;
            let bx = b.x;
            
            by.partial_cmp(&ay).unwrap_or(std::cmp::Ordering::Equal)
                .then(ax.partial_cmp(&bx).unwrap_or(std::cmp::Ordering::Equal))
        });

        // 2. Pre-calculate metrics
        let (page_global_space_width, inferred_widths) = Self::calculate_page_metrics(&sorted_chars);

        // 3. Build lines and blocks
        let mut current_line: Option<PdfLine> = None;
        let mut last_char_baseline_y: f32 = 0.0;
        let mut last_char_right: f32 = 0.0;

        for (i, char_info) in sorted_chars.into_iter().enumerate() {
            let mut text = char_info.text;
            if text.contains('\r') || text.contains('\n') {
                text = text.replace('\r', "").replace('\n', "");
            }
            
            let baseline_y = char_info.y;
            let bounds = PdfRectValue {
                left: char_info.left,
                right: char_info.right,
                top: char_info.top,
                bottom: char_info.bottom,
            };
            
            let space_width = page_global_space_width;
            let advance_width = inferred_widths.get(i).cloned().unwrap_or_else(|| char_info.width);

            let skew = char_info.skew;

            let is_new_line = if let Some(ref _line) = current_line {
                (baseline_y - last_char_baseline_y).abs() > 1.0
            } else {
                true
            };

            if is_new_line {
                if let Some(line) = current_line.take() {
                    Self::add_line_to_blocks(&mut blocks, line);
                }
                
                let style = PdfStyle::new(char_info.font_name.clone(), char_info.font_size);
                current_line = Some(PdfLine::new(bounds, page_num, style, skew, baseline_y));
            }

            if let Some(ref mut line) = current_line {
                line.append_char_replica(&text, bounds, space_width, last_char_right);
                
                let chunk = TextChunk {
                    text: text.clone(),
                    x: char_info.left,
                    y: baseline_y,
                    width: advance_width,
                    font_name: char_info.font_name,
                    font_size: char_info.font_size,
                    single_space_width: space_width,
                    skew,
                };
                line.add_chunk(chunk);
            }

            last_char_baseline_y = baseline_y;
            last_char_right = char_info.right;
        }

        if let Some(line) = current_line {
            Self::add_line_to_blocks(&mut blocks, line);
        }

        blocks
    }

    fn calculate_page_metrics(sorted_chars: &[AnalyzableChar]) -> (f32, Vec<f32>) {
        let mut inferred_widths = vec![0.0; sorted_chars.len()];
        let mut total_width = 0.0;
        let mut char_count = 0;
        let mut space_char_width = 0.0;

        for i in 0..sorted_chars.len() {
            let curr = &sorted_chars[i];
            let mut width = curr.width;

            if i + 1 < sorted_chars.len() {
                let next = &sorted_chars[i + 1];
                let curr_y = curr.y;
                let next_y = next.y;
                
                if (curr_y - next_y).abs() < 1.0 {
                    let curr_x = curr.left;
                    let next_x = next.left;
                    
                    let advance = next_x - curr_x;
                    let font_size = curr.font_size;
                    if advance > 0.0 && advance < font_size * 1.5 {
                        width = advance;
                    }
                }
            }

            inferred_widths[i] = width;
            total_width += width;
            char_count += 1;
            
            if curr.text == " " {
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

        let v_gap = (last_line.y - next_line.y).abs();
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