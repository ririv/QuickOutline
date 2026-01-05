use anyhow::Result;
use crate::pdf::page_label::PageLabel;

pub trait PageLabelEngine {
    /// 获取 PDF 的总页数
    fn get_page_count(&self) -> Result<usize>;

    /// 从 PDF 中读取现有的页码标签规则
    fn get_label_rules(&self) -> Result<Vec<PageLabel>>;

    /// 将新的页码标签规则写入 PDF
    fn set_label_rules(&mut self, rules: Vec<PageLabel>) -> Result<()>;
}
