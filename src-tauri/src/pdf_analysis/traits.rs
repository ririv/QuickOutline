use anyhow::Result;
use crate::pdf_analysis::models::AnalyzableChar;

pub trait PdfPageTrait {
    fn extract_chars(&self) -> Vec<AnalyzableChar>;
}

pub trait PdfDocumentTrait {
    fn page_count(&self) -> u16;
    fn get_page<'a>(&'a self, index: u16) -> Result<Box<dyn PdfPageTrait + 'a>>;
}