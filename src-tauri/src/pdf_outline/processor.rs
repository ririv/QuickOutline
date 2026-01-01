use lopdf::{Document, Object, ObjectId, Dictionary, Stream};
use crate::pdf_outline::model::{Bookmark, ViewScaleType};
use anyhow::{Result, anyhow, Context};
use std::collections::BTreeMap;
use log::{info, warn, error};

pub fn get_outline(path: &str, offset: i32) -> Result<Bookmark> {
    let doc = Document::load(path).map_err(|e| anyhow!("Failed to load PDF: {}", e))?;
    
    // 1. Build Page Object ID to Page Number (1-based) map for quick lookup
    let pages = doc.get_pages();
    let mut page_id_to_num = BTreeMap::new();
    for (num, id) in &pages {
        page_id_to_num.insert(*id, *num);
    }

    // 2. Get Catalog -> Outlines
    let catalog = doc.trailer.get(b"Root")
        .context("Missing Root object")?
        .as_reference()?;
        
    let catalog_obj = doc.get_object(catalog)?;
    let catalog_dict = catalog_obj.as_dict().context("Root is not a dictionary")?;

    // Handle case where Outlines is missing
    if !catalog_dict.has(b"Outlines") {
        return Ok(Bookmark::new("Outlines".to_string(), None, 0));
    }

    let outlines_ref = catalog_dict.get(b"Outlines")?.as_reference()?;
    let outlines_obj = doc.get_object(outlines_ref)?;
    let outlines_dict = outlines_obj.as_dict().context("Outlines is not a dictionary")?;

    let mut root_bookmark = Bookmark::new("Outlines".to_string(), None, 0);

    if let Ok(first_ref) = outlines_dict.get(b"First").and_then(|o| o.as_reference()) {
        let children = parse_outline_chain(&doc, first_ref, &page_id_to_num, offset, 1)?;
        root_bookmark.children = children;
    }

    Ok(root_bookmark)
}

fn parse_outline_chain(
    doc: &Document, 
    start_node_id: ObjectId, 
    page_map: &BTreeMap<ObjectId, u32>, 
    offset: i32,
    level: i32
) -> Result<Vec<Bookmark>> {
    let mut bookmarks = Vec::new();
    let mut current_id = Some(start_node_id);

    while let Some(id) = current_id {
        let obj = doc.get_object(id)?;
        let dict = obj.as_dict().context("Outline item is not a dictionary")?;

        // 1. Title
        let title_obj_raw = dict.get(b"Title").context("Outline item missing Title")?;
        // Try to resolve references (e.g. 365 0 R -> ... -> String)
        let title_obj = resolve_object(doc, title_obj_raw).unwrap_or(title_obj_raw);

        let title = match title_obj {
            Object::String(bytes, _) => decode_pdf_string(bytes),
            _ => {
                // If it's still not a string after resolution, log warning
                warn!("Warning: Title is not a string after resolution. Type: {:?}", title_obj.type_name());
                "Untitled (Type Error)".to_string()
            }
        };

        // 2. Page Number (Dest)
        let mut page_num: Option<i32> = None;
        
        // Dest can be explicit array or named destination
        if let Ok(dest) = dict.get(b"Dest") {
            page_num = resolve_dest_page(doc, dest, page_map);
        } else if let Ok(action) = dict.get(b"A") {
            if let Ok(action_dict) = action.as_dict() {
                if let Ok(s) = action_dict.get(b"S").and_then(|o| o.as_name()) {
                    if s == b"GoTo" {
                        if let Ok(d) = action_dict.get(b"D") {
                            page_num = resolve_dest_page(doc, d, page_map);
                        }
                    }
                }
            }
        }

        // Apply offset inverse logic?
        // Java: finalPageNum = pageNumber + offset;
        // Logic: The bookmark in PDF points to physical page X.
        // The user wants to see "logical page".
        // If offset is +5, and PDF points to page 10. Logical is 15.
        // Frontend displays logical.
        let final_page_num = page_num.map(|n| n + offset);

        let mut bookmark = Bookmark::new(title, final_page_num, level);

        // 3. Children (Recursion)
        if let Ok(first_child) = dict.get(b"First").and_then(|o| o.as_reference()) {
            let children = parse_outline_chain(doc, first_child, page_map, offset, level + 1)?;
            bookmark.children = children;
        }

        bookmarks.push(bookmark);

        // Next sibling
        current_id = dict.get(b"Next").ok().and_then(|o| o.as_reference().ok());
    }

    Ok(bookmarks)
}

fn resolve_dest_page(doc: &Document, dest_obj: &Object, page_map: &BTreeMap<ObjectId, u32>) -> Option<i32> {
    // Dest can be:
    // 1. Name (String/Name) -> Lookup in Names tree (Complex!)
    // 2. Array [PageRef, /Fit...]
    
    match dest_obj {
        Object::Array(arr) => {
            if arr.is_empty() { return None; }
            if let Ok(page_ref) = arr[0].as_reference() {
                return page_map.get(&page_ref).map(|&n| n as i32);
            }
        }
        Object::String(name, _) | Object::Name(name) => {
            // Need to lookup in Dests or Names dictionary.
            // This is non-trivial in lopdf as it requires traversing Name trees.
            // For now, let's assume explicit destinations are used mostly.
            // If needed, we can implement Named Destination lookup.
            warn!("Named destinations not fully supported yet: {:?}", String::from_utf8_lossy(name));
        }
        _ => {}
    }
    None
}

fn resolve_object<'a>(doc: &'a Document, mut obj: &'a Object) -> Result<&'a Object> {
    let mut depth = 0;
    while let Object::Reference(id) = obj {
        depth += 1;
        if depth > 10 {
            return Err(anyhow!("Reference depth limit exceeded"));
        }
        obj = doc.get_object(*id)?;
    }
    Ok(obj)
}

// Simple PDF string decoder
// lopdf Document::decode_text is available but requires encoding info.
// Usually strings in dictionaries are PDFDocEncoding or UTF-16BE (with BOM).
fn decode_pdf_string(bytes: &[u8]) -> String {
    if bytes.starts_with(b"\xFE\xFF") {
        // UTF-16BE
        let u16: Vec<u16> = bytes[2..]
            .chunks_exact(2)
            .map(|c| u16::from_be_bytes([c[0], c[1]]))
            .collect();
        String::from_utf16_lossy(&u16)
    } else {
        // PDFDocEncoding (simplified as Latin1/ASCII for now, lopdf has helpers)
        // lopdf::Document::decode_text(encoding, bytes)
        // For simplicity:
        String::from_utf8_lossy(bytes).to_string()
    }
}

// --- Set Outline Logic ---

pub fn set_outline(
    src_path: &str, 
    dest_path: &str, 
    root: Bookmark, 
    offset: i32, 
    scale_type: ViewScaleType
) -> Result<()> {
    let mut doc = Document::load(src_path).map_err(|e| anyhow!("Failed to load PDF: {}", e))?;
    
    // 1. Prepare
    let catalog_id = doc.trailer.get(b"Root")?.as_reference()?;
    let pages = doc.get_pages(); // Map<u32, ObjectId>

    // 2. Clean old outlines
    // Find Root -> Outlines
    let catalog = doc.get_object(catalog_id)?.as_dict()?;
    let old_outlines_id = catalog.get(b"Outlines").ok().and_then(|o| o.as_reference().ok());

    if let Some(_outlines_id) = old_outlines_id {
        // Recursively delete all outline items
        // To be safe and clean, we should delete them.
        // But simply creating a new Outlines dictionary and dereferencing the old one is enough for functionality (old ones become garbage).
        // For file size, we should delete.
        // delete_outline_tree(&mut doc, outlines_id)?; 
        // Note: Implementing delete_outline_tree requires reading the tree first.
        // Let's skip deep delete for MVP, just unlink.
    }

    // 3. Create new Outlines Dictionary
    let outlines_dict_id = doc.add_object(Dictionary::new());

    // 4. Build new items
    let (first, last, count) = build_outline_level(&mut doc, &root.children, &pages, offset, scale_type)?;

    // 4.1 Fix Top-level Parent pointers
    if let Some(f) = first {
        let mut ptr = Some(f);
        while let Some(id) = ptr {
            let item = doc.get_object_mut(id)?.as_dict_mut()?;
            item.set("Parent", outlines_dict_id);
            ptr = item.get(b"Next").ok().and_then(|o| o.as_reference().ok());
        }
    }

    // 5. Update Outlines Dictionary
    let outlines_dict = doc.get_object_mut(outlines_dict_id)?.as_dict_mut()?;
    outlines_dict.set("Type", "Outlines");
    if let Some(f) = first { outlines_dict.set("First", f); }
    if let Some(l) = last { outlines_dict.set("Last", l); }
    outlines_dict.set("Count", count); // Absolute count of open items

    // 6. Link to Catalog
    let catalog = doc.get_object_mut(catalog_id)?.as_dict_mut()?;
    catalog.set("Outlines", outlines_dict_id);

    // 7. Save
    // compress: true ensures smaller file size
    doc.save(dest_path).map_err(|e| anyhow!("Failed to save PDF: {}", e))?;

    Ok(())
}

fn build_outline_level(
    doc: &mut Document, 
    bookmarks: &[Bookmark], 
    pages: &BTreeMap<u32, ObjectId>,
    offset: i32,
    scale_type: ViewScaleType
) -> Result<(Option<ObjectId>, Option<ObjectId>, i32)> {
    if bookmarks.is_empty() {
        return Ok((None, None, 0));
    }

    let mut first_id: Option<ObjectId> = None;
    let mut prev_id: Option<ObjectId> = None;
    let mut total_count = 0; // "Count" for the parent

    for item in bookmarks {
        let mut dict = Dictionary::new();
        dict.set("Title", Object::String(item.title.as_bytes().to_vec(), lopdf::StringFormat::Literal));
        dict.set("Parent", Object::Null); // Placeholder, will be ignored by reader usually if referenced by First/Last/Next/Prev structure correctly? 
        // Actually, Parent MUST be set. But we don't know Parent ID in this function call easily unless passed down.
        // Wait, Parent is the object that calls this function.
        // We need to pass `parent_id` to this function.
        // But for Root Outlines, parent is the Outlines dictionary (which we just created).
        // Let's refactor to accept parent_id.
        
        // Destination
        if let Some(pn) = item.page_num {
            // Java: int offsetPageNum = pageNum + offset;
            // Here page_num is 1-based (from user input). offset is int.
            // Target page index (1-based) = pn + offset.
            let target_page_num = (pn + offset) as u32;
            
            if let Some(page_id) = pages.get(&target_page_num) {
                let dest_array = create_destination(doc, *page_id, scale_type)?;
                dict.set("Dest", Object::Array(dest_array));
            } else {
                warn!("Page not found: {}", target_page_num);
            }
        }

        let current_id = doc.add_object(dict);

        // Link List
        if first_id.is_none() { first_id = Some(current_id); }
        
        if let Some(p) = prev_id {
            doc.get_object_mut(p)?.as_dict_mut()?.set("Next", current_id);
            doc.get_object_mut(current_id)?.as_dict_mut()?.set("Prev", p);
        }
        prev_id = Some(current_id);

        // Children
        let (child_first, child_last, child_count) = build_outline_level(doc, &item.children, pages, offset, scale_type)?;
        
        if let Some(cf) = child_first {
            let me = doc.get_object_mut(current_id)?.as_dict_mut()?;
            me.set("First", cf);
            me.set("Last", child_last.unwrap());
            // Count: Positive if open, negative if closed.
            // Reference implementation/User requirement: Default open?
            // "expanded" in frontend. Java: "bookmarkToOutlines" -> addOutline. Reference implementation defaults to open.
            // Let's assume Open.
            me.set("Count", child_count); // Positive = Open
            
            total_count += 1 + child_count; // Me + visible descendants
            
            // Set Parent for children
            // This is tricky: we created children objects already, but didn't set Parent.
            // We need to iterate children chain or traverse?
            // Actually, we can traverse the linked list starting at child_first
            let mut ptr = Some(cf);
            while let Some(child_id) = ptr {
                let child = doc.get_object_mut(child_id)?.as_dict_mut()?;
                child.set("Parent", current_id);
                ptr = child.get(b"Next").ok().and_then(|o| o.as_reference().ok());
            }
        } else {
            total_count += 1; // Just me
        }
    }

    // Return (First, Last, TotalCount)
    // Last is prev_id after loop
    Ok((first_id, prev_id, total_count))
}

fn create_destination(doc: &Document, page_id: ObjectId, scale_type: ViewScaleType) -> Result<Vec<Object>> {
    // Need to get page height for XYZ top.
    // Page object -> MediaBox (Array)
    // MediaBox is [x1, y1, x2, y2]. Height = y2 (usually).
    // Or CropBox.
    
    // Simple helper to get top
    let page_obj = doc.get_object(page_id)?.as_dict()?;
    let media_box = page_obj.get(b"CropBox")
        .or_else(|_| page_obj.get(b"MediaBox")) // Fallback to MediaBox if CropBox is missing
        .and_then(|o| o.as_array())
        .map(|a| a.iter().map(|n| match n {
            Object::Integer(i) => *i as f64,
            Object::Real(f) => (*f).into(),
            _ => 0.0,
        }).collect::<Vec<f64>>())
        .unwrap_or(vec![0.0, 0.0, 595.0, 842.0]); // Default A4

    // Typically MediaBox is [0, 0, width, height]
    // left = box[0], top = box[3]
    let left = if media_box.len() >= 1 { media_box[0] } else { 0.0 };
    let bottom = if media_box.len() >= 2 { media_box[1] } else { 0.0 };
    let right = if media_box.len() >= 3 { media_box[2] } else { 595.0 };
    let top = if media_box.len() >= 4 { media_box[3] } else { 842.0 };

    // [PageRef, /Name, args...]
    let mut dest = vec![Object::Reference(page_id)];

    match scale_type {
        ViewScaleType::FitToPage => {
            dest.push(Object::Name(b"Fit".to_vec()));
        },
        ViewScaleType::ActualSize => {
            // [page /XYZ left top 1.0]
            dest.push(Object::Name(b"XYZ".to_vec()));
            dest.push(Object::Real(left as f32));
            dest.push(Object::Real(top as f32));
            dest.push(Object::Real(1.0)); 
        },
        ViewScaleType::FitToWidth => {
            // [page /FitH top]
            dest.push(Object::Name(b"FitH".to_vec()));
            dest.push(Object::Real(top as f32));
        },
        ViewScaleType::FitToHeight => {
            // [page /FitV left]
            dest.push(Object::Name(b"FitV".to_vec()));
            dest.push(Object::Real(left as f32));
        },
        ViewScaleType::FitToBox => {
             // [page /FitR left bottom right top]
             dest.push(Object::Name(b"FitR".to_vec()));
             dest.push(Object::Real(left as f32));
             dest.push(Object::Real(bottom as f32));
             dest.push(Object::Real(right as f32));
             dest.push(Object::Real(top as f32));
        },
        ViewScaleType::None => {
             // [page /XYZ left top null] -> Keep current zoom
             dest.push(Object::Name(b"XYZ".to_vec()));
             dest.push(Object::Real(left as f32));
             dest.push(Object::Real(top as f32));
             dest.push(Object::Null);
        }
    }

    Ok(dest)
}
