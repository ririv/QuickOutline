use anyhow::{Result, format_err};
use crate::pdf::manager::{PdfWorkerInternalState};

pub fn internal_extract_toc(state: &mut PdfWorkerInternalState, path: String) -> Result<Vec<String>> {
    let session = state.get_session(&path)?;
    let doc = session.pdfium_doc.as_ref().ok_or_else(|| format_err!("Session missing document"))?;
    
    // Safety: We transmute the lifetime to 'static to satisfy the Trait expectations.
    // This is safe because the adapter is stack-allocated and doesn't escape this call.
    let static_doc: &pdfium_render::prelude::PdfDocument<'static> = unsafe { 
        std::mem::transmute(doc) 
    };

    let adapter = crate::pdf::pdfium_render::analysis_adapter::PdfiumDocumentAdapter(static_doc);
    crate::pdf_analysis::TocExtractor::extract_toc(&adapter)
}
