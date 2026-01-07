use lopdf::{Document, Object, Dictionary, StringFormat};
use crate::pdf::page_label::{PageLabel, PageLabelNumberingStyle};
use crate::pdf::page_label_traits::PageLabelEngine;
use crate::pdf::lopdf::utils::resolve_object;
use anyhow::{Result, anyhow};

pub struct LopdfPageLabelAdapter<'a> {
    pub doc: &'a mut Document,
}

impl<'a> LopdfPageLabelAdapter<'a> {
    pub fn new(doc: &'a mut Document) -> Self {
        Self { doc }
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

impl<'a> PageLabelEngine for LopdfPageLabelAdapter<'a> {
    fn get_page_count(&self) -> Result<usize> {
        Ok(self.doc.get_pages().len())
    }

    fn get_label_rules(&self) -> Result<Vec<PageLabel>> {
        let mut rules_list = Vec::new();
        let catalog_id = self.doc.trailer.get(b"Root")?.as_reference().map_err(|e| anyhow!("{:?}", e))?;
        let catalog = self.doc.get_object(catalog_id)?.as_dict().map_err(|e| anyhow!("{:?}", e))?;
        
        if let Ok(page_labels_obj) = catalog.get(b"PageLabels") {
            let page_labels_obj = resolve_object(self.doc, page_labels_obj)?;
            let page_labels_dict = page_labels_obj.as_dict().map_err(|e| anyhow!("{:?}", e))?;
            
            if let Ok(nums_obj) = page_labels_dict.get(b"Nums") {
                let nums_obj = resolve_object(self.doc, nums_obj)?;
                let nums_array = nums_obj.as_array().map_err(|e| anyhow!("{:?}", e))?;

                for chunk in nums_array.chunks(2) {
                    if chunk.len() != 2 { break; }
                    let index_obj = resolve_object(self.doc, &chunk[0])?;
                    let index = index_obj.as_i64().map_err(|e| anyhow!("{:?}", e))? as i32;

                    let dict_obj = resolve_object(self.doc, &chunk[1])?;
                    let dict = dict_obj.as_dict().map_err(|e| anyhow!("{:?}", e))?;
                    
                    let style = if let Ok(s_raw) = dict.get(b"S") {
                        let s_obj = resolve_object(self.doc, s_raw)?;
                        Self::map_name_to_style(std::str::from_utf8(s_obj.as_name().map_err(|e| anyhow!("{:?}", e))?)?)
                    } else {
                        PageLabelNumberingStyle::None
                    };
                    
                    let prefix = if let Ok(p_raw) = dict.get(b"P") {
                        let p_obj = resolve_object(self.doc, p_raw)?;
                        Some(String::from_utf8_lossy(p_obj.as_str().map_err(|e| anyhow!("{:?}", e))?).to_string())
                    } else {
                        None
                    };
                    
                    let start_num = if let Ok(st_raw) = dict.get(b"St") {
                        let st_obj = resolve_object(self.doc, st_raw)?;
                        Some(st_obj.as_i64().map_err(|e| anyhow!("{:?}", e))? as i32)
                    } else {
                        Some(1)
                    };
                    
                    rules_list.push(PageLabel {
                        page_index: index + 1,
                        numbering_style: style,
                        label_prefix: prefix,
                        start_value: start_num,
                    });
                }
            }
        }
        rules_list.sort_by_key(|r| r.page_index);
        Ok(rules_list)
    }

    fn set_label_rules(&mut self, rules: Vec<PageLabel>) -> Result<()> {
        let mut nums = Vec::new();
        let mut sorted_list = rules;
        sorted_list.sort_by_key(|l| l.page_index);

        for label in sorted_list {
            let page_index = label.page_index - 1;
            let mut dict = Dictionary::new();
            if let Some(s) = Self::map_style_to_name(&label.numbering_style) {
                dict.set("S", Object::Name(s.as_bytes().to_vec()));
            }
            if let Some(prefix) = &label.label_prefix {
                dict.set("P", Object::String(prefix.as_bytes().to_vec(), StringFormat::Literal));
            }
            if let Some(start) = label.start_value {
                dict.set("St", Object::Integer(start as i64));
            }
            nums.push(Object::Integer(page_index as i64));
            nums.push(Object::Dictionary(dict));
        }
        
        let mut page_labels_dict = Dictionary::new();
        page_labels_dict.set("Nums", Object::Array(nums));
        let page_labels_id = self.doc.add_object(Object::Dictionary(page_labels_dict));

        let catalog_id = self.doc.trailer.get(b"Root")?.as_reference().map_err(|e| anyhow!("{:?}", e))?;
        let catalog = self.doc.get_object_mut(catalog_id)?.as_dict_mut().map_err(|e| anyhow!("{:?}", e))?;
        catalog.set("PageLabels", Object::Reference(page_labels_id));
        
        Ok(())
    }
}
