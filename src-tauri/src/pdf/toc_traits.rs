use anyhow::Result;
use crate::pdf::toc::{TocLinkDto};
use crate::pdf::page_label::PageLabel;

/// 抽象 PDF 合并引擎 (由 pdfium_render 实现)
pub trait TocMerger {
    fn merge_toc_pdf(&self, main_path: &str, memory_ptr: Option<*mut [u8]>, is_memory_mode: bool, toc_pdf_path: &str, insert_pos: u16) -> Result<Vec<u8>>;
}

/// 抽象 PDF 编辑引擎 (由 lopdf 实现)
pub trait TocEditor {
    fn capture_page_identifiers(&mut self) -> Result<Vec<String>>;
    fn inject_links(&mut self, pdf_bytes: &[u8], links: Vec<TocLinkDto>, insert_pos: usize, original_page_ids: &[String]) -> Result<Vec<u8>>;
    fn apply_page_labels(&mut self, pdf_bytes: &[u8], toc_pdf_path: &str, insert_pos: i32, toc_label: Option<&PageLabel>) -> Result<Vec<u8>>;
}