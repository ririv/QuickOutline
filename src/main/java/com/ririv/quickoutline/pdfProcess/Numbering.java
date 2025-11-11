package com.ririv.quickoutline.pdfProcess;

import com.itextpdf.commons.utils.StringNormalizer;
import com.itextpdf.kernel.numbering.AlphabetNumbering;

public interface Numbering {

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
}
