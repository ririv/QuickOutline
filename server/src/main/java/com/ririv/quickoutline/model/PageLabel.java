package com.ririv.quickoutline.model;


import java.util.LinkedHashMap;
import java.util.Map;

//https://opensource.adobe.com/dc-acrobat-sdk-docs/standards/pdfstandards/pdf/PDF32000_2008.pdf#page=383
//根据PDF标准
//与iText的PageLabel对应
public record PageLabel(int pageNum, PageLabelNumberingStyle numberingStyle, String labelPrefix, Integer firstPage){

    public enum PageLabelNumberingStyle{
        /**
         * 1, 2, 3, 4...
         */
        DECIMAL_ARABIC_NUMERALS,
        /**
         * I, II, III, IV...
         */
        UPPERCASE_ROMAN_NUMERALS,
        /**
         * i, ii, iii, iv...
         */
        LOWERCASE_ROMAN_NUMERALS,
        /**
         * A, B, C, D...
         */
        UPPERCASE_LETTERS,
        /**
         * a, b, c, d...
         */
        LOWERCASE_LETTERS,

        NONE
    }

    public static final Map<String, PageLabelNumberingStyle> STYLE_MAP = new LinkedHashMap<>();
    static {
        STYLE_MAP.put("None", PageLabelNumberingStyle.NONE);
        STYLE_MAP.put("1, 2, 3, ...", PageLabelNumberingStyle.DECIMAL_ARABIC_NUMERALS);
        STYLE_MAP.put("i, ii, iii, ...", PageLabelNumberingStyle.LOWERCASE_ROMAN_NUMERALS);
        STYLE_MAP.put("I, II, III, ...", PageLabelNumberingStyle.UPPERCASE_ROMAN_NUMERALS);
        STYLE_MAP.put("a, b, c, ...", PageLabelNumberingStyle.LOWERCASE_LETTERS);
        STYLE_MAP.put("A, B, C, ...", PageLabelNumberingStyle.UPPERCASE_LETTERS);
    }
}
