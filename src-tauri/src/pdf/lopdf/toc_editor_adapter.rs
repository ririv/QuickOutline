use anyhow::{Result, format_err, anyhow};
use crate::pdf::toc_traits::TocEditor;
use crate::pdf::toc::{TocLinkDto};
use crate::pdf::manager::{PdfSession};
use crate::pdf::page_label::{PageLabel, PageLabelProcessor};
use lopdf::{Document, Object, Dictionary};

pub struct LopdfTocAdapter<'a> {
    pub session: &'a mut PdfSession,
}

impl<'a> LopdfTocAdapter<'a> {
    pub fn new(session: &'a mut PdfSession) -> Self {
        Self { session }
    }

    fn parse_id(id_str: &str) -> Result<lopdf::ObjectId> {
        let parts: Vec<&str> = id_str.split(',').collect();
        if parts.len() != 2 { return Err(anyhow!("Invalid ID format: {}", id_str)); }
        Ok((parts[0].parse()?, parts[1].parse()?))
    }

    fn format_id(id: lopdf::ObjectId) -> String {
        format!("{},{}", id.0, id.1)
    }

    fn doc_to_bytes(mut doc: Document) -> Result<Vec<u8>> {
        let mut buffer = Vec::new();
        doc.save_to(&mut buffer).map_err(|e| format_err!("Lopdf save error: {}", e))?;
        Ok(buffer)
    }
}

impl<'a> TocEditor for LopdfTocAdapter<'a> {
    fn capture_page_identifiers(&mut self) -> Result<Vec<String>> {
        let doc = self.session.get_lopdf_doc_mut()?;
        let pages = doc.get_pages();
        let mut ids = vec![];
        let mut sorted_keys: Vec<_> = pages.keys().cloned().collect();
        sorted_keys.sort();
        for key in sorted_keys {
            ids.push(Self::format_id(pages[&key]));
        }
        Ok(ids)
    }

    fn inject_links(&mut self, pdf_bytes: &[u8], links: Vec<TocLinkDto>, insert_pos: usize, original_page_ids: &[String]) -> Result<Vec<u8>> {
        let mut doc = Document::load_mem(pdf_bytes).map_err(|e| format_err!("Lopdf reload error: {}", e))?;
        let merged_pages_map = doc.get_pages(); 
        let mut merged_pages_list: Vec<lopdf::ObjectId> = vec![];
        let mut sorted_keys: Vec<_> = merged_pages_map.keys().cloned().collect();
        sorted_keys.sort();
        for key in sorted_keys {
            merged_pages_list.push(merged_pages_map[&key]);
        }
        
        for link in links {
            let toc_page_idx_in_merged = insert_pos + link.toc_page_index;
            if toc_page_idx_in_merged >= merged_pages_list.len() { continue; }
            let source_page_id = merged_pages_list[toc_page_idx_in_merged];
            
            let target_idx = link.target_page_index as usize;
            let target_page_id = if link.target_is_original_doc {
                if target_idx >= original_page_ids.len() { continue; }
                Self::parse_id(&original_page_ids[target_idx])?
            } else {
                if target_idx >= merged_pages_list.len() { continue; }
                merged_pages_list[target_idx]
            };
            
            let page_height = {
                let page_dict = doc.get_object(source_page_id)?.as_dict()?;
                let box_array = page_dict.get(b"CropBox")
                    .or_else(|_| page_dict.get(b"MediaBox"))
                    .ok()
                    .and_then(|o| o.as_array().ok())
                    .map(|a| a.iter().filter_map(|n| n.as_float().ok().map(|f| f as f64)).collect::<Vec<f64>>());
                
                if let Some(ba) = box_array {
                    if ba.len() >= 4 { ba[3] } else { 842.0 }
                } else { 842.0 }
            };
            
            let rect = vec![
                Object::Real(link.x as f32), 
                Object::Real((page_height - (link.y + link.height)) as f32), 
                Object::Real((link.x + link.width) as f32), 
                Object::Real((page_height - link.y) as f32)
            ];
            
            let annotation = Dictionary::from_iter(vec![
                ("Type", Object::Name(b"Annot".to_vec())), 
                ("Subtype", Object::Name(b"Link".to_vec())), 
                ("Rect", Object::Array(rect)), 
                ("Border", Object::Array(vec![Object::Integer(0), Object::Integer(0), Object::Integer(0)])), 
                ("Dest", Object::Array(vec![Object::Reference(target_page_id), Object::Name(b"Fit".to_vec())]))
            ]);
            let ann_id = doc.add_object(annotation);
            if let Ok(page) = doc.get_object_mut(source_page_id).and_then(|o| o.as_dict_mut()) {
                 if !page.has(b"Annots") { page.set("Annots", Object::Array(vec![])); }
                 if let Ok(annots) = page.get_mut(b"Annots").and_then(|o| o.as_array_mut()) { 
                     annots.push(Object::Reference(ann_id)); 
                 }
            }
        }
        Self::doc_to_bytes(doc)
    }

    fn apply_page_labels(&mut self, pdf_bytes: &[u8], toc_pdf_path: &str, insert_pos: i32, toc_label: Option<&PageLabel>) -> Result<Vec<u8>> {
        let mut doc = Document::load_mem(pdf_bytes).map_err(|e| format_err!("Lopdf reload error: {}", e))?;
        
        let src_adapter = crate::pdf::lopdf::page_label_adapter::LopdfPageLabelAdapter::new(self.session.get_lopdf_doc_mut()?);
        use crate::pdf::page_label_traits::PageLabelEngine;
        
        if let Ok(rules) = src_adapter.get_label_rules() {
            let (toc_len, toc_rules) = if let Ok(mut toc_doc) = Document::load(toc_pdf_path) {
                let len = toc_doc.get_pages().len() as i32;
                let toc_adapter = crate::pdf::lopdf::page_label_adapter::LopdfPageLabelAdapter::new(&mut toc_doc);
                let r = toc_adapter.get_label_rules().unwrap_or_default();
                (len, r)
            } else {
                (0, vec![])
            };

            if toc_len > 0 {
                let new_rules = crate::pdf::toc::calculate_merged_rules(rules, insert_pos, toc_len, toc_rules, toc_label);
                let mut dest_adapter = crate::pdf::lopdf::page_label_adapter::LopdfPageLabelAdapter::new(&mut doc);
                dest_adapter.set_label_rules(new_rules)?;
            }
        }
        
        Self::doc_to_bytes(doc)
    }
}
