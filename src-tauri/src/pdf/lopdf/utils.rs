use lopdf::{Document, Object};
use anyhow::{Result, anyhow};

/// Safely resolves indirect objects (Deref).
/// Limits recursion depth to prevent infinite loops in malformed PDFs.
pub fn resolve_object<'a>(doc: &'a Document, mut obj: &'a Object) -> Result<&'a Object> {
    let mut depth = 0;
    while let Object::Reference(id) = obj {
        depth += 1;
        if depth > 10 {
            return Err(anyhow!("Reference depth limit exceeded"));
        }
        obj = doc.get_object(*id).map_err(|e| anyhow!("Failed to get object {}: {}", id.0, e))?;
    }
    Ok(obj)
}

/// Simple PDF string decoder.
/// Handles UTF-16BE (with BOM) and attempts UTF-8 fallback.
pub fn decode_pdf_string(bytes: &[u8]) -> String {
    if bytes.starts_with(b"\xFE\xFF") {
        // UTF-16BE
        let u16: Vec<u16> = bytes[2..]
            .chunks_exact(2)
            .map(|c| u16::from_be_bytes([c[0], c[1]]))
            .collect();
        String::from_utf16_lossy(&u16)
    } else {
        // Fallback to UTF-8 lossy (covers ASCII and PDFDocEncoding mostly)
        String::from_utf8_lossy(bytes).to_string()
    }
}
