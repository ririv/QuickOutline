use pdfium_render::prelude::*;
use anyhow::Result;

/// Merges an insert PDF document into a main PDF document at the specified position.
/// 
/// # Arguments
/// * `main_doc` - Mutable reference to the main PDF document.
/// * `insert_doc` - Reference to the PDF document to insert.
/// * `insert_pos` - 0-based page index in the main document where insertion begins.
///
/// # Returns
/// Result indicating success.
pub fn merge_pdfs<'a>(
    main_doc: &mut PdfDocument<'a>,
    insert_doc: &PdfDocument<'a>,
    insert_pos: u16
) -> Result<()> {
    let insert_len = insert_doc.pages().len();
    if insert_len > 0 {
        // Copy all pages from insert_doc to main_doc at insert_pos
        main_doc.pages_mut().copy_page_range_from_document(
            insert_doc, 
            0..=(insert_len - 1), 
            insert_pos
        )?;
    }
    
    Ok(())
}