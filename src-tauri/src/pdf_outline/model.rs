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
#[serde(rename_all = "SCREAMING_SNAKE_CASE")] // Matches Java ViewScaleType enum constant names usually?
// Java enum: FIT_TO_PAGE, ACTUAL_SIZE...
// Let's check Java code provided earlier.
// ViewScaleType.java was not provided, but ItextOutlineProcessor.java uses: ViewScaleType.FIT_TO_PAGE
// So it expects SCREAMING_SNAKE_CASE in JSON if it's enum.
// But frontend uses "NONE" string in rpc.ts.
// "NONE", "FIT_TO_PAGE"...
pub enum ViewScaleType {
    FitToPage,
    ActualSize,
    FitToWidth,
    FitToHeight,
    FitToBox,
    CustomScale,
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
