use serde::{Deserialize, Serialize};

#[derive(Debug, Serialize, Deserialize, Clone)]
#[serde(rename_all = "camelCase")]
pub struct Bookmark {
    pub id: String,
    pub title: String,
    pub page_num: Option<i32>,
    pub level: i32,
    pub children: Vec<Bookmark>,
}

#[derive(Debug, Serialize, Deserialize, Clone, Copy, PartialEq)]
#[serde(rename_all = "SCREAMING_SNAKE_CASE")]
pub enum ViewScaleType {
    /// `/Fit`: 缩放页面以适应整个窗口。
    /// 对应 Adobe: "适合页面" (Fit Page)
    FitToPage,
    /// `/XYZ ... 1.0`: 以 100% 实际大小显示页面。
    /// 对应 Adobe: "实际大小" (Actual Size)
    ActualSize,
    /// `/FitH`: 缩放页面以适应窗口宽度。
    /// 对应 Adobe: "适合宽度" (Fit Width)
    FitToWidth,
    /// `/FitV`: 缩放页面以适应窗口高度。
    /// 对应 Adobe: "适合高度" (Fit Height)
    FitToHeight,
    /// `/FitR`: 缩放页面以适应指定的矩形框（默认整页）。
    /// 对应 Adobe: "适合可见" (Fit Visible)
    FitToBox,
    /// `/XYZ ... null`: 继承阅读器当前的缩放级别（保持不变）。
    /// 对应 Adobe: "继承缩放" (Inherit Zoom)
    None,
}

impl Bookmark {
    pub fn new(title: String, page_num: Option<i32>, level: i32) -> Self {
        Self {
            id: uuid::Uuid::new_v4().to_string(),
            title,
            page_num,
            level,
            children: Vec::new(),
        }
    }
}
