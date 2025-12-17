use pdfium_render::prelude::*;
use anyhow::{Result, format_err}; // 导入 format_err!
use tokio::task; // 需要tokio来spawn_blocking
use std::ops::RangeInclusive; // RangeInclusive trait for 1..=X syntax

// Helper function for the actual PDF insertion logic
fn perform_pdf_insertion(
    original_path: &str,
    toc_path: &str,
    output_path: &str,
    insert_index: u16, // 0-based index
) -> Result<()> { // 这里的 Result 是 anyhow::Result<()>
    // 1. 创建第一个 Pdfium 实例用于加载 original_doc
    let pdfium_for_original = Pdfium::new(
        Pdfium::bind_to_library(Pdfium::pdfium_platform_library_name_at_path("./"))
            .or_else(|_| Pdfium::bind_to_system_library())
            .map_err(|e| format_err!("Failed to bind pdfium_for_original: {}", e))?,
    );
    let original_doc = pdfium_for_original.load_pdf_from_file(original_path, None)?;

    // 2. 创建第二个 Pdfium 实例用于加载 toc_doc
    let pdfium_for_toc = Pdfium::new(
        Pdfium::bind_to_library(Pdfium::pdfium_platform_library_name_at_path("./"))
            .or_else(|_| Pdfium::bind_to_system_library())
            .map_err(|e| format_err!("Failed to bind pdfium_for_toc: {}", e))?,
    );
    let toc_doc = pdfium_for_toc.load_pdf_from_file(toc_path, None)?;

    // 3. 创建第三个 Pdfium 实例用于创建和修改 new_doc
    let mut pdfium_for_new = Pdfium::new(
        Pdfium::bind_to_library(Pdfium::pdfium_platform_library_name_at_path("./"))
            .or_else(|_| Pdfium::bind_to_system_library())
            .map_err(|e| format_err!("Failed to bind pdfium_for_new: {}", e))?,
    );
    let mut new_doc = pdfium_for_new.create_new_pdf()?;

    // 1. Copy pages before insertion point
    if insert_index > 0 {
        let range = 1..=insert_index; 
        // TODO: Fix E0596 borrowing error
        // new_doc.pages().copy_page_range_from_document(&original_doc, range, 0)?;
        println!("TODO: Copy pages 1 to {} from original_doc", insert_index);
    }

    // 2. Copy TOC pages
    let toc_count = toc_doc.pages().len();
    if toc_count > 0 {
        let range = 1..=toc_count;
        // TODO: Fix E0596 borrowing error
        // new_doc.pages().copy_page_range_from_document(&toc_doc, range, insert_index)?;
        println!("TODO: Copy TOC pages 1 to {}", toc_count);
    }

    // 3. Copy pages after insertion point
    let original_count = original_doc.pages().len();
    if insert_index < original_count {
        let range = (insert_index + 1)..=original_count;
        // TODO: Fix E0596 borrowing error
        // new_doc.pages().copy_page_range_from_document(&original_doc, range, insert_index + toc_count)?;
        println!("TODO: Copy pages {} to {} from original_doc", insert_index + 1, original_count);
    }

    new_doc.save_to_file(output_path)?;
    
    Ok(())
}


// Tauri Command Wrapper
#[tauri::command]
pub async fn merge_pdfs(
    original_path: String,
    toc_path: String,
    output_path: String,
    insert_index: u16
) -> Result<(), String> {
    // 在一个阻塞线程中执行 PDF 操作，避免 Pdfium 跨越 await 点
    task::spawn_blocking(move || {
        perform_pdf_insertion(
            &original_path,
            &toc_path,
            &output_path,
            insert_index
        )
    }).await // 等待阻塞任务完成
    .map_err(|e| e.to_string())? // 处理 spawn_blocking 自身的错误
    .map_err(|e| e.to_string()) // 处理 perform_pdf_insertion 返回的 Result
}
