use crate::pdf::page_label::PageLabelNumberingStyle;

pub struct Numbering;

impl Numbering {
    pub fn to_roman(number: i32, upper_case: bool) -> String {
        if upper_case {
            Self::to_roman_upper_case(number)
        } else {
            Self::to_roman_lower_case(number)
        }
    }

    pub fn to_roman_lower_case(number: i32) -> String {
        Self::to_roman_upper_case(number).to_lowercase()
    }

    pub fn to_roman_upper_case(number: i32) -> String {
        if number < 1 || number > 3999 {
            return String::new();
        }
        let thousands = ["", "M", "MM", "MMM"];
        let hundreds = ["", "C", "CC", "CCC", "CD", "D", "DC", "DCC", "DCCC", "CM"];
        let tens = ["", "X", "XX", "XXX", "XL", "L", "LX", "LXX", "LXXX", "XC"];
        let ones = ["", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX"];

        let idx_thousands = (number / 1000) as usize;
        let idx_hundreds = ((number % 1000) / 100) as usize;
        let idx_tens = ((number % 100) / 10) as usize;
        let idx_ones = (number % 10) as usize;

        format!(
            "{}{}{}{}",
            thousands[idx_thousands], hundreds[idx_hundreds], tens[idx_tens], ones[idx_ones]
        )
    }

    pub fn to_latin_alphabet_number(number: i32, upper_case: bool) -> String {
        if upper_case {
            Self::to_latin_alphabet_number_upper_case(number)
        } else {
            Self::to_latin_alphabet_number_lower_case(number)
        }
    }

    pub fn to_latin_alphabet_number_lower_case(number: i32) -> String {
        Self::to_latin_alphabet_number_upper_case(number).to_lowercase()
    }

    pub fn to_latin_alphabet_number_upper_case(mut number: i32) -> String {
        if number <= 0 {
            return String::new();
        }
        let mut sb = String::new();
        while number > 0 {
            number -= 1;
            let char_code = (b'A' + (number % 26) as u8) as char;
            sb.insert(0, char_code);
            number /= 26;
        }
        sb
    }

    pub fn format_page_number(
        style: &PageLabelNumberingStyle,
        number: i32,
        prefix: Option<&str>,
    ) -> String {
        let label = match style {
            PageLabelNumberingStyle::None => String::new(),
            PageLabelNumberingStyle::DecimalArabicNumerals => number.to_string(),
            PageLabelNumberingStyle::UppercaseRomanNumerals => Self::to_roman_upper_case(number),
            PageLabelNumberingStyle::LowercaseRomanNumerals => Self::to_roman_lower_case(number),
            PageLabelNumberingStyle::UppercaseLetters => Self::to_latin_alphabet_number_upper_case(number),
            PageLabelNumberingStyle::LowercaseLetters => Self::to_latin_alphabet_number_lower_case(number),
        };

        if let Some(p) = prefix {
            format!("{}{}", p, label)
        } else {
            label
        }
    }
}
