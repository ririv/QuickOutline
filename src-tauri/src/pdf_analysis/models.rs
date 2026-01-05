use std::collections::HashMap;
use once_cell::sync::Lazy;
use regex::Regex;
use serde::{Serialize, Deserialize};

#[allow(clippy::expect_used)]
static MATH_SYMBOL_REGEX: Lazy<Regex> = Lazy::new(|| Regex::new(r"\p{Sm}").expect("Invalid Regex"));
#[allow(clippy::expect_used)]
static TITLE_CASE_REGEX: Lazy<Regex> = Lazy::new(|| Regex::new(r"\p{Lt}").expect("Invalid Regex"));

#[derive(Debug, Clone, Copy, Serialize, Deserialize)]
pub struct PdfRectValue {
    pub left: f32,
    pub top: f32,
    pub right: f32,
    pub bottom: f32,
}

impl PdfRectValue {
    pub fn width(&self) -> f32 { (self.right - self.left).abs() }
    pub fn height(&self) -> f32 { (self.top - self.bottom).abs() }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct AnalyzableChar {
    pub text: String,
    pub x: f32,
    pub y: f32,
    pub width: f32,
    pub height: f32,
    pub left: f32,
    pub right: f32,
    pub top: f32,
    pub bottom: f32,
    pub font_name: String,
    pub font_size: f32,
    pub skew: f64,
}

pub struct DocumentStats {
    pub dominant_text_style: PdfStyle,
    pub median_line_height: f64,
    pub median_char_density: f64,
    pub total_pages: i32,
    pub dominant_heading_char_pattern_type: i32,
    pub line_hash_frequencies: HashMap<i32, i32>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct TextChunk {
    pub text: String,
    pub x: f32,
    pub y: f32,
    pub width: f32,
    pub font_name: String,
    pub font_size: f32,
    pub single_space_width: f32,
    pub skew: f64,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct CharacterPattern {
    pub counts: [i32; 12],
    pub total: i32,
}

impl CharacterPattern {
    pub fn new(text: &str) -> Self {
        let mut counts = [0; 12];
        let mut total = 0;
        for c in text.chars() {
            let char_type = Self::get_char_type(c);
            counts[char_type as usize] += 1;
            total += 1;
        }
        Self { counts, total }
    }

    fn get_char_type(c: char) -> i32 {
        if c.is_lowercase() { return 3; }
        
        // 100% Replica: Java treats UpperCase (Lu) and TitleCase (Lt) as type 2
        if c.is_uppercase() || TITLE_CASE_REGEX.is_match(&c.to_string()) { 
            return 2; 
        }
        
        if c.is_numeric() { return 1; }
        if c.is_whitespace() { return 10; }
        if c == '.' || c == '?' || c == '!' { return 6; }
        if c == '-' || c == '_' || c == '–' || c == '—' { return 7; }
        if c == '(' || c == '[' || c == '{' || c == ')' || c == ']' || c == '}' { return 8; }
        
        // 100% Replica: Java uses Character.getType(c) == Character.MATH_SYMBOL
        if MATH_SYMBOL_REGEX.is_match(&c.to_string()) { return 9; }
        
        if c.is_control() { return 0; }
        8 
    }

    pub fn get_pattern_type(&self) -> i32 {
        if self.total == 0 { return 0; }
        let mut scores = [0.0f64; 11];
        let c = self.counts;

        // 100% Replica of the scoring formula
        scores[2] = c[2] as f64 * 10.0;
        scores[0] = (c[0] + c[2]) as f64;
        scores[3] = c[3] as f64 - (c[4] as f64 * 3.0) - (c[5] as f64 * 3.0) 
                    - (c[6] as f64 * 3.0) - (c[7] as f64 * 3.0) 
                    - (c[8] as f64 * 3.0) - (c[9] as f64 * 10.0);
        scores[4] = c[4] as f64;
        scores[5] = c[5] as f64 - (c[6] as f64 * 10.0) - (c[7] as f64 * 10.0);
        scores[6] = c[6] as f64;
        scores[7] = c[7] as f64;
        scores[8] = c[8] as f64;
        scores[9] = c[9] as f64;
        scores[10] = c[10] as f64;

        let mut max_score = -f64::INFINITY;
        let mut best_type = 0;
        for (i, &score) in scores.iter().enumerate() {
            if score > max_score {
                max_score = score;
                best_type = i as i32;
            }
        }
        best_type
    }
}

#[derive(Debug, Clone, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub struct PdfStyle {
    pub font_name: String,
    pub font_size_scaled: i32, 
}

impl PdfStyle {
    pub fn new(name: String, size: f32) -> Self {
        Self {
            font_name: name,
            font_size_scaled: (size * 10.0).round() as i32,
        }
    }
    pub fn get_size(&self) -> f32 { self.font_size_scaled as f32 / 10.0 }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PdfLine {
    pub text: String,
    pub bounds: PdfRectValue,
    pub avg_font_size: f32,
    pub page_num: i32,
    pub style: PdfStyle,
    pub skew: f64,
    pub chunks: Vec<TextChunk>,
    pub y: f32,
}

impl PdfLine {
    pub fn new(bounds: PdfRectValue, page_num: i32, style: PdfStyle, skew: f64, y: f32) -> Self {
        let font_size = style.get_size();
        Self {
            text: String::new(),
            bounds,
            avg_font_size: font_size,
            page_num,
            style,
            skew,
            chunks: Vec::new(),
            y,
        }
    }

    pub fn append_char_replica(&mut self, text: &str, bounds: PdfRectValue, space_width: f32, last_right: f32) {
        if text.trim().is_empty() { return; }

        let h_gap = bounds.left - last_right;
        
        if h_gap > space_width * 5.0 {
            self.text.push_str("     ");
        } else if h_gap > space_width * 0.3 && !self.text.is_empty() {
            self.text.push(' ');
        }

        let char_fs = (bounds.top - bounds.bottom).abs();
        if self.text.is_empty() {
            self.avg_font_size = char_fs;
        } else {
            let n = self.text.chars().count() as f32;
            self.avg_font_size = (self.avg_font_size * n + char_fs) / (n + 1.0);
        }

        self.text.push_str(text);
        self.extend_bounds(bounds);
    }

    pub fn add_chunk(&mut self, chunk: TextChunk) {
        self.chunks.push(chunk);
    }

    pub fn extend_bounds(&mut self, other: PdfRectValue) {
        if other.left < self.bounds.left { self.bounds.left = other.left; }
        if other.right > self.bounds.right { self.bounds.right = other.right; }
        if other.top > self.bounds.top { self.bounds.top = other.top; }
        if other.bottom < self.bounds.bottom { self.bounds.bottom = other.bottom; }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PdfBlock {
    pub lines: Vec<PdfLine>,
    pub cached_text: Option<String>,
    pub char_pattern: Option<CharacterPattern>,
}

impl PdfBlock {
    pub fn from_line(line: PdfLine) -> Self {
        Self {
            lines: vec![line],
            cached_text: None,
            char_pattern: None,
        }
    }

    pub fn merge_line(&mut self, line: PdfLine) {
        self.lines.push(line);
        self.cached_text = None;
        self.char_pattern = None;
    }

    pub fn get_text_plain(&mut self) -> &str {
        if self.cached_text.is_none() {
            let text = self.lines.iter()
                .map(|l| l.text.as_str())
                .collect::<Vec<_>>()
                .join(" ");
            self.cached_text = Some(text);
        }
        self.cached_text.as_deref().unwrap_or_default()
    }

    pub fn get_text_reconstructed(&self) -> String {
        let mut text_builder = String::new();
        
        for (line_idx, line) in self.lines.iter().enumerate() {
            if line.chunks.is_empty() {
                text_builder.push_str(&line.text);
            } else {
                let chunks = &line.chunks;
                let first_chunk = &chunks[0];
                text_builder.push_str(&first_chunk.text);

                for i in 1..chunks.len() {
                    let prev = &chunks[i - 1];
                    let curr = &chunks[i];

                    let mut space_width = prev.single_space_width;
                    if space_width <= 0.0 {
                        space_width = prev.font_size * 0.25;
                    }

                    let gap = curr.x - (prev.x + prev.width);

                    if gap > space_width * 6.0 {
                        text_builder.push_str("     ");
                    } else if gap > space_width * 0.5 {
                        text_builder.push_str(" ");
                    }
                    text_builder.push_str(&curr.text);
                }
            }

            if line_idx < self.lines.len() - 1 {
                text_builder.push('\n');
            }
        }
        
        text_builder.replace("\r\n", "\n").replace('\r', "\n")
    }

    pub fn get_char_pattern(&mut self) -> &CharacterPattern {
        if self.char_pattern.is_none() {
            let text = self.get_text_plain().to_string();
            self.char_pattern = Some(CharacterPattern::new(&text));
        }
        self.char_pattern.as_ref().unwrap_or_else(|| {
            static EMPTY_PATTERN: CharacterPattern = CharacterPattern { counts: [0; 12], total: 0 };
            &EMPTY_PATTERN
        })
    }

    pub fn get_primary_style(&self) -> &PdfStyle {
        static DEFAULT_STYLE: PdfStyle = PdfStyle {
            font_name: String::new(),
            font_size_scaled: 100,
        };
        self.lines.first().map(|l| &l.style).unwrap_or(&DEFAULT_STYLE)
    }

    pub fn get_page_num(&self) -> i32 {
        self.lines.first().map(|l| l.page_num).unwrap_or(0)
    }

    pub fn is_bold(&self) -> bool {
        !self.lines.is_empty() && self.get_primary_style().font_name.to_lowercase().contains("bold")
    }

    pub fn get_x(&self) -> f32 {
        self.lines.first().map(|l| l.bounds.left).unwrap_or(0.0)
    }

    pub fn get_y(&self) -> f32 {
        self.lines.first().map(|l| l.y).unwrap_or(0.0)
    }

    pub fn get_width(&self) -> f32 {
        self.lines.iter().map(|l| l.bounds.right - l.bounds.left).fold(0.0, f32::max)
    }

    pub fn get_height(&self) -> f32 {
        if self.lines.is_empty() { return 0.0; }
        let top = self.lines[0].y;
        if let Some(last) = self.lines.last() {
            let bottom = last.y - last.style.get_size();
            top - bottom
        } else {
            0.0
        }
    }

    pub fn get_skew(&self) -> f64 {
        if self.lines.is_empty() { return 0.0; }
        self.lines.iter().map(|l| l.skew).sum::<f64>() / self.lines.len() as f64
    }
}