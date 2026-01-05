use anyhow::{Result, format_err};
use crate::pdf::toc_traits::TocMerger;
use crate::pdf::pdfium_render::merge::merge_pdfs;
use pdfium_render::prelude::Pdfium;

pub struct PdfiumTocAdapter {
    pub pdfium: &'static Pdfium,
}

impl PdfiumTocAdapter {
    pub fn new(pdfium: &'static Pdfium) -> Self {
        Self { pdfium }
    }
}

impl TocMerger for PdfiumTocAdapter {
    fn merge_toc_pdf(&self, main_path: &str, memory_ptr: Option<*mut [u8]>, is_memory_mode: bool, toc_pdf_path: &str, insert_pos: u16) -> Result<Vec<u8>> {
        let mut main_doc = if is_memory_mode {
            if let Some(ptr) = memory_ptr {
                let slice = unsafe { &*ptr };
                self.pdfium.load_pdf_from_byte_slice(slice, None).map_err(|e| format_err!("{:?}", e))?
            } else {
                return Err(format_err!("Memory mode but no bytes"));
            }
        } else {
            self.pdfium.load_pdf_from_file(main_path, None).map_err(|e| format_err!("{:?}", e))?
        };
        
        let toc_doc = self.pdfium.load_pdf_from_file(toc_pdf_path, None).map_err(|e| format_err!("{:?}", e))?;
        merge_pdfs(&mut main_doc, &toc_doc, insert_pos).map_err(|e| format_err!("Merge Error: {}", e))?;
        
        main_doc.save_to_bytes().map_err(|e| format_err!("{:?}", e))
    }
}
