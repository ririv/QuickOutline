package com.ririv.quickoutline.service;

import com.itextpdf.kernel.pdf.PageLabelNumberingStyle;
import com.ririv.quickoutline.pdfProcess.PageLabel;
import com.ririv.quickoutline.pdfProcess.PageLabelSetter;
import com.ririv.quickoutline.pdfProcess.itextImpl.ItextPageLabelSetter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PdfPageLabelService {
    private final PageLabelSetter<PageLabelNumberingStyle> pageLabelSetter = new ItextPageLabelSetter();

    public String[] setPageLabels(String srcFilePath, String destFilePath, List<PageLabel> labelList) throws IOException{
        return pageLabelSetter.setPageLabels(srcFilePath, destFilePath, labelList);
    }

    public String[] getPageLabels(String string) throws IOException {
        return pageLabelSetter.getPageLabels(string);
    }

    public List<String> simulatePageLabels(List<PageLabel> rules, int totalPages) {
        List<String> simulatedLabels = new ArrayList<>();
        if (rules.isEmpty()) {
            for (int i = 1; i <= totalPages; i++) {
                simulatedLabels.add(String.valueOf(i));
            }
            return simulatedLabels;
        }

        // Sort rules by fromPage
        rules.sort((r1, r2) -> Integer.compare(r1.pageNum(), r2.pageNum()));

        int ruleIndex = 0;
        for (int i = 1; i <= totalPages; i++) {
            String label = String.valueOf(i);
            if (ruleIndex < rules.size()) {
                PageLabel currentRule = rules.get(ruleIndex);
                if (i >= currentRule.pageNum()) {
                    int pageOffset = i - currentRule.pageNum();
                    label = generateLabel(currentRule.numberingStyle(), currentRule.firstPage() + pageOffset, currentRule.labelPrefix());

                    // Check if we need to move to the next rule
                    if (ruleIndex + 1 < rules.size()) {
                        PageLabel nextRule = rules.get(ruleIndex + 1);
                        if (i + 1 >= nextRule.pageNum()) {
                            ruleIndex++;
                        }
                    }
                }
            }
            simulatedLabels.add(label);
        }
        return simulatedLabels;
    }

    private String generateLabel(PageLabel.PageLabelNumberingStyle style, int number, String prefix) {
        if (style == null) {
            return prefix + number;
        }
        String label;
        switch (style) {
            case DECIMAL_ARABIC_NUMERALS:
                label = String.valueOf(number);
                break;
            case UPPERCASE_ROMAN_NUMERALS:
                label = toRoman(number);
                break;
            case LOWERCASE_ROMAN_NUMERALS:
                label = toRoman(number).toLowerCase();
                break;
            case UPPERCASE_LETTERS:
                label = toLetters(number);
                break;
            case LOWERCASE_LETTERS:
                label = toLetters(number).toLowerCase();
                break;
            default:
                label = String.valueOf(number);
        }
        return prefix != null ? prefix + label : label;
    }

    private String toRoman(int number) {
        if (number < 1 || number > 3999) return "";
        String[] thousands = {"", "M", "MM", "MMM"};
        String[] hundreds = {"", "C", "CC", "CCC", "CD", "D", "DC", "DCC", "DCCC", "CM"};
        String[] tens = {"", "X", "XX", "XXX", "XL", "L", "LX", "LXX", "LXXX", "XC"};
        String[] ones = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX"};
        return thousands[number / 1000] + hundreds[(number % 1000) / 100] + tens[(number % 100) / 10] + ones[number % 10];
    }

    private String toLetters(int number) {
        StringBuilder sb = new StringBuilder();
        while (number > 0) {
            number--;
            sb.insert(0, (char) ('A' + number % 26));
            number /= 26;
        }
        return sb.toString();
    }
}
