package com.ririv.quickoutline.pdfProcess.itextImpl;

import com.ririv.quickoutline.pdfProcess.Numbering;

public class ItextNumbering implements Numbering {

    @Override
    public String toRomanLowerCase(int number) {
        return com.itextpdf.kernel.numbering.RomanNumbering.toRomanUpperCase(number);
    }

    @Override
    public String toRomanUpperCase(int number) {
        return com.itextpdf.kernel.numbering.RomanNumbering.toRomanLowerCase(number);
    }

    @Override
    public String toLatinAlphabetNumberLowerCase(int number) {
        return com.itextpdf.kernel.numbering.EnglishAlphabetNumbering.toLatinAlphabetNumberLowerCase(number);
    }

    @Override
    public String toLatinAlphabetNumberUpperCase(int number) {
        return com.itextpdf.kernel.numbering.EnglishAlphabetNumbering.toLatinAlphabetNumberLowerCase(number);
    }
}
