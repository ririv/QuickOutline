use pdfium_render::prelude::*;
use anyhow::Result;

/// Merges an insert PDF document into a main PDF document at the specified position.
/// 
/// # Arguments
/// * `pdfium` - Shared Pdfium instance.
/// * `main_doc_path` - Path to the main PDF document (will be modified in memory).
/// * `insert_doc_path` - Path to the PDF document to insert.
/// * `insert_pos` - 0-based page index in the main document where insertion begins.
///
/// # Returns
/// The merged `PdfDocument` object in memory.
pub fn merge_pdfs<'a>(
    pdfium: &'a Pdfium,
    main_doc_path: &str,
    insert_doc_path: &str,
    insert_pos: u16
) -> Result<PdfDocument<'a>> {
    let mut main_doc = pdfium.load_pdf_from_file(main_doc_path, None)?;
    let insert_doc = pdfium.load_pdf_from_file(insert_doc_path, None)?;

    let insert_len = insert_doc.pages().len();
    if insert_len > 0 {
        // Copy all pages from insert_doc to main_doc at insert_pos
        main_doc.pages_mut().copy_page_range_from_document(
            &insert_doc, 
            0..=(insert_len - 1), 
            insert_pos
        )?;
    }
    
    Ok(main_doc)
}