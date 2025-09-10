package com.ririv.quickoutline.pdfProcess;


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
}
