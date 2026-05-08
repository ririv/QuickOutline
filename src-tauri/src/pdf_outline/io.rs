use std::path::{Path, PathBuf};

use anyhow::{Result, anyhow};
use lopdf::Document;

use crate::pdf::lopdf::outline_adapter::LopdfOutlineAdapter;
use crate::pdf_outline::model::{Bookmark, ViewScaleType};
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

pub fn get_outline_from_path(src_path: &Path, offset: i32) -> Result<Bookmark> {
    let mut doc = Document::load(src_path)
        .map_err(|err| anyhow!("Failed to load PDF {}: {err}", src_path.display()))?;
    get_outline_from_document(&mut doc, offset)
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
