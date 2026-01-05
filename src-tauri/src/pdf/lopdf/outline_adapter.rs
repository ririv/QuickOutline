use lopdf::{Document, Object, ObjectId, Dictionary};
use crate::pdf_outline::traits::OutlineEngine;
use crate::pdf_outline::model::ViewScaleType;
use crate::pdf::lopdf::utils::{resolve_object, decode_pdf_string};
use anyhow::{Result, anyhow};
use std::collections::BTreeMap;

pub struct LopdfOutlineAdapter<'a> {
    pub doc: &'a mut Document,
    page_id_to_num: BTreeMap<ObjectId, u32>,
    page_num_to_id: BTreeMap<u32, ObjectId>,
}

impl<'a> LopdfOutlineAdapter<'a> {
    pub fn new(doc: &'a mut Document) -> Self {
        let pages = doc.get_pages();
        let mut page_id_to_num = BTreeMap::new();
        let mut page_num_to_id = BTreeMap::new();
        for (num, id) in &pages {
            page_id_to_num.insert(*id, *num);
            page_num_to_id.insert(*num, *id);
        }
        Self { doc, page_id_to_num, page_num_to_id }
    }

    fn parse_id(id_str: &str) -> Result<ObjectId> {
        let parts: Vec<&str> = id_str.split(',').collect();
        if parts.len() != 2 { return Err(anyhow!("Invalid ID format: {}", id_str)); }
        Ok((parts[0].parse()?, parts[1].parse()?))
    }

    fn format_id(id: ObjectId) -> String {
        format!("{},{}", id.0, id.1)
    }
}

impl<'a> OutlineEngine for LopdfOutlineAdapter<'a> {
    fn get_root_node_id(&self) -> Result<Option<String>> {
        let catalog_id = self.doc.trailer.get(b"Root")?.as_reference()?;
        let catalog = self.doc.get_object(catalog_id)?.as_dict()?;
        if let Ok(outlines_ref) = catalog.get(b"Outlines").and_then(|o| o.as_reference()) {
            return Ok(Some(Self::format_id(outlines_ref)));
        }
        Ok(None)
    }

    fn get_node_title(&self, node_id: &str) -> Result<String> {
        let id = Self::parse_id(node_id)?;
        let dict = self.doc.get_object(id)?.as_dict()?;
        let title_obj_raw = dict.get(b"Title")?;
        let title_obj = resolve_object(self.doc, title_obj_raw)?;
        match title_obj {
            Object::String(bytes, _) => Ok(decode_pdf_string(bytes)),
            _ => Ok("Untitled".to_string()),
        }
    }

    fn get_node_dest_page(&self, node_id: &str) -> Result<Option<i32>> {
        let id = Self::parse_id(node_id)?;
        let dict = self.doc.get_object(id)?.as_dict()?;
        
        let dest_obj = if let Ok(dest) = dict.get(b"Dest") {
            Some(dest)
        } else if let Ok(action) = dict.get(b"A").and_then(|o| o.as_dict()) {
            if matches!(action.get(b"S").and_then(|o| o.as_name()), Ok(b"GoTo")) {
                action.get(b"D").ok()
            } else { None }
        } else { None };

        if let Some(dest) = dest_obj {
            let resolved = resolve_object(self.doc, dest)?;
            if let Object::Array(arr) = resolved {
                if !arr.is_empty() {
                    if let Ok(page_ref) = arr[0].as_reference() {
                        return Ok(self.page_id_to_num.get(&page_ref).map(|&n| n as i32));
                    }
                }
            }
        }
        Ok(None)
    }

    fn get_first_child_id(&self, node_id: &str) -> Result<Option<String>> {
        let id = Self::parse_id(node_id)?;
        let dict = self.doc.get_object(id)?.as_dict()?;
        if let Ok(first) = dict.get(b"First").and_then(|o| o.as_reference()) {
            return Ok(Some(Self::format_id(first)));
        }
        Ok(None)
    }

    fn get_next_sibling_id(&self, node_id: &str) -> Result<Option<String>> {
        let id = Self::parse_id(node_id)?;
        let dict = self.doc.get_object(id)?.as_dict()?;
        if let Ok(next) = dict.get(b"Next").and_then(|o| o.as_reference()) {
            return Ok(Some(Self::format_id(next)));
        }
        Ok(None)
    }

    fn create_node(&mut self, title: &str, page_num: Option<i32>, scale: ViewScaleType) -> Result<String> {
        let mut dict = Dictionary::new();
        dict.set("Title", Object::String(title.as_bytes().to_vec(), lopdf::StringFormat::Literal));
        
        if let Some(pn) = page_num {
            if let Some(page_id) = self.page_num_to_id.get(&(pn as u32)) {
                let dest = match scale {
                    ViewScaleType::FitToPage => vec![Object::Reference(*page_id), Object::Name(b"Fit".to_vec())],
                    _ => vec![Object::Reference(*page_id), Object::Name(b"XYZ".to_vec()), Object::Null, Object::Null, Object::Null],
                };
                dict.set("Dest", Object::Array(dest));
            }
        }

        let id = self.doc.add_object(dict);
        Ok(Self::format_id(id))
    }

    fn link_nodes(&mut self, parent_id: &str, first_child_id: Option<&str>, last_child_id: Option<&str>, count: i32) -> Result<()> {
        let pid = Self::parse_id(parent_id)?;
        let dict = self.doc.get_object_mut(pid)?.as_dict_mut()?;
        if let Some(fid) = first_child_id { dict.set("First", Object::Reference(Self::parse_id(fid)?)); }
        if let Some(lid) = last_child_id { dict.set("Last", Object::Reference(Self::parse_id(lid)?)); }
        dict.set("Count", Object::Integer(count as i64));
        Ok(())
    }

    fn set_sibling_links(&mut self, current_id: &str, next_id: Option<&str>, prev_id: Option<&str>) -> Result<()> {
        let cid = Self::parse_id(current_id)?;
        let dict = self.doc.get_object_mut(cid)?.as_dict_mut()?;
        if let Some(nid) = next_id { dict.set("Next", Object::Reference(Self::parse_id(nid)?)); }
        if let Some(pid) = prev_id { dict.set("Prev", Object::Reference(Self::parse_id(pid)?)); }
        Ok(())
    }

    fn set_parent(&mut self, node_id: &str, parent_id: &str) -> Result<()> {
        let cid = Self::parse_id(node_id)?;
        let pid = Self::parse_id(parent_id)?;
        let dict = self.doc.get_object_mut(cid)?.as_dict_mut()?;
        dict.set("Parent", Object::Reference(pid));
        Ok(())
    }

    fn update_catalog_outlines(&mut self, outlines_dict_id: &str) -> Result<()> {
        let oid = Self::parse_id(outlines_dict_id)?;
        let catalog_id = self.doc.trailer.get(b"Root")?.as_reference()?;
        let catalog = self.doc.get_object_mut(catalog_id)?.as_dict_mut()?;
        catalog.set("Outlines", Object::Reference(oid));
        Ok(())
    }
}
