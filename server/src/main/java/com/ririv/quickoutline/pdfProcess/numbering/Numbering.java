package com.ririv.quickoutline.pdfProcess.numbering;

import com.ririv.quickoutline.pdfProcess.PageLabel;
import com.ririv.quickoutline.pdfProcess.itextImpl.ItextNumbering;

public interface Numbering {

    Numbering numbering = new ItextNumbering();

    default String toRoman(int number, boolean upperCase) {
        return upperCase ? toRomanUpperCase(number) : toRomanLowerCase(number);
    }

    String toRomanLowerCase(int number);

    String toRomanUpperCase(int number);

    String toLatinAlphabetNumberLowerCase(int number);

    String toLatinAlphabetNumberUpperCase(int number);

    default String toLatinAlphabetNumber(int number, boolean upperCase) {
        return upperCase ? toLatinAlphabetNumberUpperCase(number) : toLatinAlphabetNumberLowerCase(number);
    }

    static String formatPageNumber(PageLabel.PageLabelNumberingStyle style, int number, String prefix) {
        if (style == null) { // Fallback for undefined or null styles
            return (prefix != null ? prefix : "") + number;
        }
        String label = switch (style) {
            case NONE -> ""; // For NONE numberingStyle, display nothing.
            case DECIMAL_ARABIC_NUMERALS -> String.valueOf(number);
            case UPPERCASE_ROMAN_NUMERALS -> numbering.toRomanUpperCase(number);
            case LOWERCASE_ROMAN_NUMERALS -> numbering.toRomanLowerCase(number);
            case UPPERCASE_LETTERS -> numbering.toLatinAlphabetNumberUpperCase(number);
            case LOWERCASE_LETTERS -> numbering.toLatinAlphabetNumberLowerCase(number);
        };
        return prefix != null ? prefix + label : label;
    }
}
