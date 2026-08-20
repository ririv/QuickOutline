use std::path::{Path, PathBuf};

use anyhow::{Context, Result, anyhow};
use lopdf::Document;

use crate::pdf::lopdf::outline_adapter::LopdfOutlineAdapter;
use crate::pdf_outline::model::{Bookmark, OutlineDocument, ViewScaleType};
use crate::pdf_outline::processor::PdfOutlineProcessor;

/// PDF outline 读写逻辑，供 GUI commands 和后续 CLI flow 复用。
pub fn resolve_dest_path(src_path: &Path, dest_path: Option<&Path>) -> PathBuf {
    if let Some(path) = dest_path
        && !path.as_os_str().is_empty()
    {
        return path.to_path_buf();
    }

    let parent = src_path.parent().unwrap_or_else(|| Path::new(""));
    let file_stem = src_path
        .file_stem()
        .and_then(|value| value.to_str())
        .unwrap_or("output");
    let ext = src_path
        .extension()
        .and_then(|value| value.to_str())
        .unwrap_or("pdf");

    let mut candidate_name = format!("{}_new.{}", file_stem, ext);
    let mut candidate_path = parent.join(&candidate_name);

    if !candidate_path.exists() {
        return candidate_path;
    }

    let mut counter = 1;
    while candidate_path.exists() {
        candidate_name = format!("{}_new_{}.{}", file_stem, counter, ext);
        candidate_path = parent.join(&candidate_name);
        counter += 1;
    }

    candidate_path
}

pub fn resolve_dest_path_string(src_path: &str, dest_path: Option<&str>) -> String {
    let dest = dest_path
        .map(str::trim)
        .filter(|value| !value.is_empty())
        .map(Path::new);

    resolve_dest_path(Path::new(src_path), dest)
        .to_string_lossy()
        .to_string()
}

pub fn resolve_outline_text_path(src_path: &Path, dest_path: Option<&Path>) -> PathBuf {
    if let Some(path) = dest_path
        && !path.as_os_str().is_empty()
    {
        return path.to_path_buf();
    }

    let parent = src_path.parent().unwrap_or_else(|| Path::new(""));
    let file_stem = src_path
        .file_stem()
        .and_then(|value| value.to_str())
        .unwrap_or("outline");

    let mut candidate_name = format!("{}_outline.txt", file_stem);
    let mut candidate_path = parent.join(&candidate_name);

    if !candidate_path.exists() {
        return candidate_path;
    }

    let mut counter = 1;
    while candidate_path.exists() {
        candidate_name = format!("{}_outline_{}.txt", file_stem, counter);
        candidate_path = parent.join(&candidate_name);
        counter += 1;
    }

    candidate_path
}

pub fn resolve_outline_text_path_string(src_path: &str, dest_path: Option<&str>) -> String {
    let dest = dest_path
        .map(str::trim)
        .filter(|value| !value.is_empty())
        .map(Path::new);

    resolve_outline_text_path(Path::new(src_path), dest)
        .to_string_lossy()
        .to_string()
}

pub fn get_outline_from_path(src_path: &Path, offset: i32) -> Result<Bookmark> {
    let mut doc = Document::load(src_path)
        .map_err(|err| anyhow!("Failed to load PDF {}: {err}", src_path.display()))?;
    get_outline_from_document(&mut doc, offset)
}

/// Reads the PDF outline directly from an in-memory PDF.
pub fn get_outline_from_bytes(src: &[u8], offset: i32) -> Result<OutlineDocument> {
    let mut doc = Document::load_mem(src).map_err(|err| anyhow!("Failed to load PDF: {err}"))?;
    if doc.is_encrypted() {
        return Err(anyhow!("Encrypted PDF is not supported"));
    }
    let page_count = u32::try_from(doc.get_pages().len()).context("PDF page count exceeds u32")?;
    let outline = get_outline_from_document(&mut doc, offset)?;

    Ok(OutlineDocument {
        page_count,
        outline,
    })
}

pub fn get_outline_from_document(doc: &mut Document, offset: i32) -> Result<Bookmark> {
    let adapter = LopdfOutlineAdapter::new(doc);
    PdfOutlineProcessor::get_outline(&adapter, offset)
}

pub fn set_outline_from_path(
    src_path: &Path,
    bookmark_root: Bookmark,
    dest_path: Option<&Path>,
    offset: i32,
    scale: ViewScaleType,
) -> Result<PathBuf> {
    let actual_dest = resolve_dest_path(src_path, dest_path);
    let mut doc = Document::load(src_path)
        .map_err(|err| anyhow!("Failed to load PDF {}: {err}", src_path.display()))?;

    set_outline_on_document(&mut doc, bookmark_root, offset, scale)?;
    doc.save(&actual_dest)
        .map(|_| ())
        .map_err(|err| anyhow!("Failed to save PDF {}: {err}", actual_dest.display()))?;

    Ok(actual_dest)
}

pub fn set_outline_on_document(
    doc: &mut Document,
    bookmark_root: Bookmark,
    offset: i32,
    scale: ViewScaleType,
) -> Result<()> {
    let mut adapter = LopdfOutlineAdapter::new(doc);
    PdfOutlineProcessor::set_outline(&mut adapter, bookmark_root, offset, scale)
}

/// Writes an outline to an in-memory PDF and returns the complete new PDF.
pub fn set_outline_on_bytes(
    src: &[u8],
    bookmark_root: Bookmark,
    offset: i32,
    scale: ViewScaleType,
) -> Result<Vec<u8>> {
    let mut doc = Document::load_mem(src).map_err(|err| anyhow!("Failed to load PDF: {err}"))?;
    if doc.is_encrypted() {
        return Err(anyhow!("Encrypted PDF is not supported"));
    }
    set_outline_on_document(&mut doc, bookmark_root, offset, scale)?;

    let mut output = Vec::new();
    doc.save_to(&mut output)
        .map_err(|err| anyhow!("Failed to serialize PDF: {err}"))?;
    Ok(output)
}

#[cfg(test)]
mod tests {
    use lopdf::{Document, Object, dictionary};

    use super::{get_outline_from_bytes, set_outline_on_bytes};
    use crate::pdf_outline::model::{Bookmark, ViewScaleType};

    fn one_page_pdf() -> Vec<u8> {
        let mut doc = Document::with_version("1.5");
        let pages_id = doc.new_object_id();
        let page_id = doc.add_object(dictionary! {
            "Type" => "Page",
            "Parent" => pages_id,
            "MediaBox" => vec![0.into(), 0.into(), 595.into(), 842.into()],
        });
        let catalog_id = doc.add_object(dictionary! {
            "Type" => "Catalog",
            "Pages" => pages_id,
        });
        doc.objects.insert(
            pages_id,
            Object::Dictionary(dictionary! {
                "Type" => "Pages",
                "Kids" => vec![page_id.into()],
                "Count" => 1,
            }),
        );
        doc.trailer.set("Root", catalog_id);

        let mut bytes = Vec::new();
        doc.save_to(&mut bytes).unwrap_or_else(|err| {
            panic!("测试 PDF 序列化失败: {err}");
        });
        bytes
    }

    #[test]
    fn writes_and_reads_outline_in_memory() {
        let source = one_page_pdf();
        let root = Bookmark {
            id: "root".to_string(),
            title: "Outlines".to_string(),
            page_num: None,
            level: 0,
            children: vec![Bookmark {
                id: "chapter-1".to_string(),
                title: "第一章".to_string(),
                page_num: Some(1),
                level: 1,
                children: Vec::new(),
            }],
        };

        let output = set_outline_on_bytes(&source, root, 0, ViewScaleType::None)
            .unwrap_or_else(|err| panic!("写入目录失败: {err}"));
        let result =
            get_outline_from_bytes(&output, 0).unwrap_or_else(|err| panic!("读取目录失败: {err}"));

        assert_eq!(result.page_count, 1);
        assert_eq!(result.outline.children.len(), 1);
        assert_eq!(result.outline.children[0].title, "第一章");
        assert_eq!(result.outline.children[0].page_num, Some(1));
    }
}
