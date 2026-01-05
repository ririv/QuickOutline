use anyhow::Result;
use crate::pdf_outline::model::{Bookmark, ViewScaleType};

pub trait OutlineEngine {
    // Reading
    fn get_root_node_id(&self) -> Result<Option<String>>;
    fn get_node_title(&self, node_id: &str) -> Result<String>;
    fn get_node_dest_page(&self, node_id: &str) -> Result<Option<i32>>;
    fn get_first_child_id(&self, node_id: &str) -> Result<Option<String>>;
    fn get_next_sibling_id(&self, node_id: &str) -> Result<Option<String>>;

    // Writing
    fn create_node(&mut self, title: &str, page_num: Option<i32>, scale: ViewScaleType) -> Result<String>;
    fn link_nodes(&mut self, parent_id: &str, first_child_id: Option<&str>, last_child_id: Option<&str>, count: i32) -> Result<()>;
    fn set_sibling_links(&mut self, current_id: &str, next_id: Option<&str>, prev_id: Option<&str>) -> Result<()>;
    fn set_parent(&mut self, node_id: &str, parent_id: &str) -> Result<()>;
    fn update_catalog_outlines(&mut self, outlines_dict_id: &str) -> Result<()>;
}