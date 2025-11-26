package com.ririv.quickoutline.pdfProcess.numbering;

public class NumberingImpl implements Numbering {

    @Override
    public String toRomanLowerCase(int number) {
        return toRomanUpperCase(number).toLowerCase();
    }

    @Override
    public String toRomanUpperCase(int number) {
        if (number < 1 || number > 3999) return "";
        String[] thousands = {"", "M", "MM", "MMM"};
        String[] hundreds = {"", "C", "CC", "CCC", "CD", "D", "DC", "DCC", "DCCC", "CM"};
        String[] tens = {"", "X", "XX", "XXX", "XL", "L", "LX", "LXX", "LXXX", "XC"};
        String[] ones = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX"};
        return thousands[number / 1000] + hundreds[(number % 1000) / 100] + tens[(number % 100) / 10] + ones[number % 10];
    }

    @Override
    public String toLatinAlphabetNumberLowerCase(int number) {
        return toLatinAlphabetNumberUpperCase(number).toLowerCase();
    }

    @Override
    public String toLatinAlphabetNumberUpperCase(int number) {
        StringBuilder sb = new StringBuilder();
        while (number > 0) {
            number--;
            sb.insert(0, (char) ('A' + number % 26));
            number /= 26;
        }
        return sb.toString();
    }
}
