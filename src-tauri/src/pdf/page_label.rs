use serde::{Deserialize, Serialize};
use crate::pdf::numbering::Numbering;
use crate::pdf::page_label_traits::PageLabelEngine;
use anyhow::Result;

#[derive(Debug, Serialize, Deserialize, Clone, PartialEq)]
pub enum PageLabelNumberingStyle {
    #[serde(rename = "DECIMAL_ARABIC_NUMERALS")]
    DecimalArabicNumerals,
    #[serde(rename = "UPPERCASE_ROMAN_NUMERALS")]
    UppercaseRomanNumerals,
    #[serde(rename = "LOWERCASE_ROMAN_NUMERALS")]
    LowercaseRomanNumerals,
    #[serde(rename = "UPPERCASE_LETTERS")]
    UppercaseLetters,
    #[serde(rename = "LOWERCASE_LETTERS")]
    LowercaseLetters,
    #[serde(rename = "NONE")]
    None,
}

#[derive(Debug, Serialize, Deserialize, Clone)]
#[serde(rename_all = "camelCase")]
pub struct PageLabel {
    pub page_num: i32, // 1-based start index
    pub numbering_style: PageLabelNumberingStyle,
    pub label_prefix: Option<String>,
    pub first_page: Option<i32>, // Start number, defaults to 1
}

pub struct PageLabelProcessor;

impl PageLabelProcessor {

    /// Pure logic to simulate page labels based on rules.
    pub fn simulate_page_labels(rules: Vec<PageLabel>, total_pages: u32) -> Vec<String> {
        let mut simulated_labels = Vec::with_capacity(total_pages as usize);
        let mut sorted_rules = rules;
        sorted_rules.sort_by_key(|r| r.page_num);

        for i in 1..=total_pages {
            let active_rule = sorted_rules.iter()
                .filter(|r| i as i32 >= r.page_num)
                .last();

            let label = if let Some(rule) = active_rule {
                let page_offset = (i as i32) - rule.page_num;
                let start_val = rule.first_page.unwrap_or(1);
                Numbering::format_page_number(&rule.numbering_style, start_val + page_offset, rule.label_prefix.as_deref())
            } else {
                i.to_string()
            };
            simulated_labels.push(label);
        }
        simulated_labels
    }

    /// Business Logic: Get formatted labels using any engine.
    pub fn get_formatted_labels<E: PageLabelEngine>(engine: &E) -> Result<Vec<String>> {
        let page_count = engine.get_page_count()?;
        let rules = engine.get_label_rules()?;
        Ok(Self::simulate_page_labels(rules, page_count as u32))
    }

    /// Business Logic: Merge new rules.
    pub fn merge_rules<E: PageLabelEngine>(engine: &mut E, new_rules: Vec<PageLabel>) -> Result<()> {
        let mut current_rules = engine.get_label_rules()?;
        for new_label in new_rules {
            if let Some(existing_rule) = current_rules.iter_mut().find(|r| r.page_num == new_label.page_num) {
                *existing_rule = new_label;
            } else {
                current_rules.push(new_label);
            }
        }
        current_rules.sort_by_key(|r| r.page_num);
        engine.set_label_rules(current_rules)
    }

    /// Business Logic: Remove rules.
    pub fn remove_rules<E: PageLabelEngine>(engine: &mut E, page_nums: Vec<i32>) -> Result<()> {
        let mut current_rules = engine.get_label_rules()?;
        current_rules.retain(|r| !page_nums.contains(&r.page_num));
        engine.set_label_rules(current_rules)
    }
}