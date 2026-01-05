use lopdf::{Document, Object, Dictionary, StringFormat};
use serde::{Deserialize, Serialize};
use crate::pdf::numbering::Numbering;
use crate::pdf::lopdf::utils::resolve_object;
use std::path::Path;
use log::info;

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

    /// Pure logic to simulate page labels without a PDF file.
    pub fn simulate_page_labels(rules: Vec<PageLabel>, total_pages: u32) -> Vec<String> {
        let mut simulated_labels = Vec::with_capacity(total_pages as usize);
        let mut sorted_rules = rules;
        sorted_rules.sort_by_key(|r| r.page_num);

        for i in 1..=total_pages {
            // Find the applicable rule for the current page `i`
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

    /// Updates page labels in an existing lopdf::Document.
    pub fn set_page_labels_in_doc(doc: &mut Document, label_list: Vec<PageLabel>) -> Result<(), Box<dyn std::error::Error>> {
        // Ensure page 1 has a label if not provided? Java code did this.
        // Let's stick to the provided list.
        // Note: PDF requires PageLabels to be sorted by index.
        
        let mut nums = Vec::new();
        
        // Sort labels by pageNum just in case
        let mut sorted_list = label_list.clone();
        sorted_list.sort_by_key(|l| l.page_num);

        // Check if page 1 is covered. If not, maybe we should add a default?
        // Java code: pdfDoc.getPage(1).setPageLabel(DECIMAL...) explicitly.
        // If the first rule doesn't start at 0 (page 1), PDF might be invalid or fallback.
        // We will just process what's given.

        for label in sorted_list {
            let page_index = label.page_num - 1; // 0-based
            
            let mut dict = Dictionary::new();
            
            // Style
            if let Some(s) = Self::map_style_to_name(&label.numbering_style) {
                dict.set("S", Object::Name(s.as_bytes().to_vec()));
            }
            
            // Prefix
            if let Some(prefix) = &label.label_prefix {
                // Determine if we need to escape or verify string format? 
                // lopdf handles basic string. Literal string is safer.
                dict.set("P", Object::String(prefix.as_bytes().to_vec(), StringFormat::Literal));
            }
            
            // Start
            if let Some(start) = label.first_page {
                dict.set("St", Object::Integer(start as i64));
            }
            
            nums.push(Object::Integer(page_index as i64));
            nums.push(Object::Dictionary(dict));
        }
        
        // Construct PageLabels dictionary
        let mut page_labels_dict = Dictionary::new();
        page_labels_dict.set("Nums", Object::Array(nums));
        
        // Add PageLabels object first to satisfy borrow checker
        let page_labels_id = doc.add_object(Object::Dictionary(page_labels_dict));

        // Attach to Catalog
        let catalog_id = doc.trailer.get(b"Root")?.as_reference()?;
        
        if let Some(catalog) = doc.objects.get_mut(&catalog_id) {
            if let Object::Dictionary(dict) = catalog {
                dict.set("PageLabels", Object::Reference(page_labels_id));
            }
        }
        
        Ok(())
    }

    /// Merges new page labels into the existing ones in a Document.
    /// New rules will be inserted.
    pub fn merge_page_labels_in_doc(doc: &mut Document, label_list: Vec<PageLabel>) -> Result<(), Box<dyn std::error::Error>> {
        // 1. Get existing rules
        let mut current_rules = Self::get_page_label_rules_from_doc(doc)?;

        // 2. Merge logic
        for new_label in label_list {
            // Find if there is an existing rule starting at the same page
            if let Some(existing_rule) = current_rules.iter_mut().find(|r| r.page_num == new_label.page_num) {
                // Update existing
                *existing_rule = new_label;
            } else {
                // Insert new
                current_rules.push(new_label);
            }
        }
        
        // 3. Sort (set_page_labels handles sorting, but good to be explicit)
        current_rules.sort_by_key(|r| r.page_num);

        // 4. Write back using the overwrite method
        Self::set_page_labels_in_doc(doc, current_rules)
    }

    /// Removes page label rules that start at the specified page numbers from a Document.
    pub fn remove_page_labels_in_doc(doc: &mut Document, page_nums: Vec<i32>) -> Result<(), Box<dyn std::error::Error>> {
        // 1. Get existing rules
        let mut current_rules = Self::get_page_label_rules_from_doc(doc)?;
        current_rules.retain(|r| !page_nums.contains(&r.page_num));

        // 2. Filter out rules to be removed
        Self::set_page_labels_in_doc(doc, current_rules)
    }

    /// Gets formatted page labels from a Document.
    pub fn get_page_labels_from_doc(doc: &Document) -> Result<Vec<String>, Box<dyn std::error::Error>> {
        let page_count = doc.get_pages().len();
        let mut labels = vec![String::new(); page_count];
        
        // Default labels: 1, 2, 3...
        for i in 0..page_count {
            labels[i] = (i + 1).to_string();
        }

        // Reuse the parsing logic from get_page_label_rules
        let rules = Self::get_page_label_rules_from_doc(doc)?;

        // No rules found, return default
        if rules.is_empty() {
            return Ok(labels);
        }

        // Apply rules
        // Note: rules are already sorted by page_num
        for i in 0..rules.len() {
            let rule = &rules[i];
            let start_index = (rule.page_num - 1) as usize; // Convert 1-based to 0-based
            
            let next_index = if i + 1 < rules.len() {
                (rules[i+1].page_num - 1) as usize
            } else {
                page_count
            };
            
            // Should usually be 1, but if rule.first_page is None, what's the default?
            // In get_page_label_rules, we default to Some(1).
            let mut current_num = rule.first_page.unwrap_or(1);
            
            for p in start_index..next_index {
                if p >= page_count { break; }
                labels[p] = Numbering::format_page_number(&rule.numbering_style, current_num, rule.label_prefix.as_deref());
                current_num += 1;
            }
        }
        Ok(labels)
    }

    /// Gets page label rules from a Document.
    pub fn get_page_label_rules_from_doc(doc: &Document) -> Result<Vec<PageLabel>, Box<dyn std::error::Error>> {
        let start = std::time::Instant::now();
        let mut rules_list = Vec::new();

        let catalog_id = doc.trailer.get(b"Root")?.as_reference()?;
        let catalog = doc.get_object(catalog_id)?.as_dict()?;
        
        if let Ok(page_labels_obj) = catalog.get(b"PageLabels") {
            let page_labels_obj = resolve_object(doc, page_labels_obj).map_err(|e| e.to_string())?;
            let page_labels_dict = page_labels_obj.as_dict()?;
            
            if let Ok(nums_obj) = page_labels_dict.get(b"Nums") {
                let nums_obj = resolve_object(doc, nums_obj).map_err(|e| e.to_string())?;
                let nums_array = nums_obj.as_array()?;

                for chunk in nums_array.chunks(2) {
                    if chunk.len() != 2 { break; }
                    
                    // 1. Resolve Index
                    let index_obj = resolve_object(doc, &chunk[0]).map_err(|e| e.to_string())?;
                    let index = index_obj.as_i64()? as i32;

                    // 2. Resolve Label Dictionary
                    let dict_obj = resolve_object(doc, &chunk[1]).map_err(|e| e.to_string())?;
                    let dict = dict_obj.as_dict()?;
                    
                    // 3. Resolve Style (S)
                    let style = if let Ok(s_raw) = dict.get(b"S") {
                        let s_obj = resolve_object(doc, s_raw).map_err(|e| e.to_string())?;
                        Self::map_name_to_style(std::str::from_utf8(s_obj.as_name()?)?)
                    } else {
                        PageLabelNumberingStyle::None
                    };
                    
                    // 4. Resolve Prefix (P)
                    let prefix = if let Ok(p_raw) = dict.get(b"P") {
                        let p_obj = resolve_object(doc, p_raw).map_err(|e| e.to_string())?;
                        Some(String::from_utf8_lossy(p_obj.as_str()?).to_string())
                    } else {
                        None
                    };
                    
                    // 5. Resolve Start (St)
                    let start_num = if let Ok(st_raw) = dict.get(b"St") {
                        let st_obj = resolve_object(doc, st_raw).map_err(|e| e.to_string())?;
                        Some(st_obj.as_i64()? as i32)
                    } else {
                        Some(1)
                    };
                    
                    rules_list.push(PageLabel {
                        page_num: index + 1,
                        numbering_style: style,
                        label_prefix: prefix,
                        first_page: start_num,
                    });
                }
            }
        }
        
        rules_list.sort_by_key(|r| r.page_num);
        info!("[PageLabel] Parse PageLabels (rules only) took {:?}", start.elapsed());
        Ok(rules_list)
    }

    fn map_style_to_name(style: &PageLabelNumberingStyle) -> Option<String> {
        match style {
            PageLabelNumberingStyle::DecimalArabicNumerals => Some("D".to_string()),
            PageLabelNumberingStyle::UppercaseRomanNumerals => Some("R".to_string()),
            PageLabelNumberingStyle::LowercaseRomanNumerals => Some("r".to_string()),
            PageLabelNumberingStyle::UppercaseLetters => Some("A".to_string()),
            PageLabelNumberingStyle::LowercaseLetters => Some("a".to_string()),
            PageLabelNumberingStyle::None => None,
        }
    }

    fn map_name_to_style(name: &str) -> PageLabelNumberingStyle {
        match name {
            "D" => PageLabelNumberingStyle::DecimalArabicNumerals,
            "R" => PageLabelNumberingStyle::UppercaseRomanNumerals,
            "r" => PageLabelNumberingStyle::LowercaseRomanNumerals,
            "A" => PageLabelNumberingStyle::UppercaseLetters,
            "a" => PageLabelNumberingStyle::LowercaseLetters,
            _ => PageLabelNumberingStyle::None,
        }
    }
}
