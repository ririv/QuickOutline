use crate::pdf_outline::model::{Bookmark, ViewScaleType};
use crate::pdf_outline::traits::OutlineEngine;
use anyhow::Result;

pub struct PdfOutlineProcessor;

impl PdfOutlineProcessor {
    pub fn get_outline<E: OutlineEngine>(engine: &E, offset: i32) -> Result<Bookmark> {
        let mut root_bookmark = Bookmark::new("Outlines".to_string(), None, 0);
        
        if let Some(root_id) = engine.get_root_node_id()? {
            if let Some(first_child_id) = engine.get_first_child_id(&root_id)? {
                root_bookmark.children = Self::parse_chain(engine, &first_child_id, offset, 1)?;
            }
        }

        Ok(root_bookmark)
    }

    fn parse_chain<E: OutlineEngine>(engine: &E, start_id: &str, offset: i32, level: i32) -> Result<Vec<Bookmark>> {
        let mut bookmarks = Vec::new();
        let mut current_id = Some(start_id.to_string());

        while let Some(id) = current_id {
            let title = engine.get_node_title(&id)?;
            let page_num = engine.get_node_dest_page(&id)?;
            
            let final_page_num = page_num.map(|n| n + offset);
            let mut bookmark = Bookmark::new(title, final_page_num, level);

            if let Some(child_id) = engine.get_first_child_id(&id)? {
                bookmark.children = Self::parse_chain(engine, &child_id, offset, level + 1)?;
            }

            bookmarks.push(bookmark);
            current_id = engine.get_next_sibling_id(&id)?;
        }

        Ok(bookmarks)
    }

    pub fn set_outline<E: OutlineEngine>(engine: &mut E, root: Bookmark, offset: i32, scale: ViewScaleType) -> Result<()> {
        let outlines_id = engine.create_node("Outlines", None, ViewScaleType::None)?;
        let (first, last, count) = Self::build_level(engine, &root.children, &outlines_id, offset, scale)?;
        engine.link_nodes(&outlines_id, first.as_deref(), last.as_deref(), count)?;
        engine.update_catalog_outlines(&outlines_id)?;
        Ok(())
    }

    fn build_level<E: OutlineEngine>(
        engine: &mut E, 
        bookmarks: &[Bookmark], 
        parent_id: &str,
        offset: i32,
        scale: ViewScaleType
    ) -> Result<(Option<String>, Option<String>, i32)> {
        if bookmarks.is_empty() {
            return Ok((None, None, 0));
        }

        let mut first_id: Option<String> = None;
        let mut prev_id: Option<String> = None;
        let mut total_count = 0;

        for item in bookmarks {
            let actual_page_num = item.page_num.map(|pn| pn + offset);
            let current_id = engine.create_node(&item.title, actual_page_num, scale)?;
            engine.set_parent(&current_id, parent_id)?;

            if first_id.is_none() { first_id = Some(current_id.clone()); }

            if let Some(ref p) = prev_id {
                engine.set_sibling_links(p, Some(&current_id), None)?;
                engine.set_sibling_links(&current_id, None, Some(p))?;
            }
            prev_id = Some(current_id.clone());

            let (child_first, child_last, child_count) = Self::build_level(engine, &item.children, &current_id, offset, scale)?;
            
            if let Some(ref cf) = child_first {
                engine.link_nodes(&current_id, Some(cf), child_last.as_deref(), child_count)?;
                total_count += 1 + child_count;
            } else {
                total_count += 1;
            }
        }

        Ok((first_id, prev_id, total_count))
    }
}
