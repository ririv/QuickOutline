use pdfium_render::prelude::*;
use crate::pdf_analysis::traits::*;
use crate::pdf_analysis::models::AnalyzableChar;
use anyhow::Result;

pub struct PdfiumPageAdapter<'a>(pub PdfPage<'a>);

impl<'a> PdfPageTrait for PdfiumPageAdapter<'a> {
    fn get_analyzable_chars(&self) -> Vec<AnalyzableChar> {
        if let Ok(text) = self.0.text() {
            text.chars().iter()
                .map(|c| {
                    let bounds = c.loose_bounds()
                        .or_else(|_| c.tight_bounds())
                        .unwrap_or_else(|_| PdfRect::new(PdfPoints::ZERO, PdfPoints::ZERO, PdfPoints::ZERO, PdfPoints::ZERO));
                    
                    let skew = if let Ok(m) = c.matrix() {
                        let (a, b, c, d) = (m.a(), m.b(), m.c(), m.d());
                        if a != 0.0 && d != 0.0 { ((b / a).powi(2) + (c / d).powi(2)) as f64 } else { 0.0 }
                    } else { 0.0 };

                    AnalyzableChar {
                        text: c.unicode_string().unwrap_or_default(),
                        x: c.origin_x().map(|p| p.value).unwrap_or(0.0),
                        y: c.origin_y().map(|p| p.value).unwrap_or(0.0),
                        width: bounds.width().value,
                        height: bounds.height().value,
                        left: bounds.left().value,
                        right: bounds.right().value,
                        top: bounds.top().value,
                        bottom: bounds.bottom().value,
                        font_name: c.font_name(),
                        font_size: c.scaled_font_size().value,
                        skew,
                    }
                })
                .collect()
        } else {
            Vec::new()
        }
    }
}

pub struct PdfiumDocumentAdapter<'a>(pub &'a PdfDocument<'a>);

impl<'a> PdfDocumentTrait for PdfiumDocumentAdapter<'a> {
    fn page_count(&self) -> u16 {
        self.0.pages().len()
    }
    fn get_page<'b>(&'b self, index: u16) -> Result<Box<dyn PdfPageTrait + 'b>> {
        let page = self.0.pages().get(index)?;
        Ok(Box::new(PdfiumPageAdapter(page)))
    }
}