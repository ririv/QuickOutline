package com.ririv.quickoutline.service;

import com.itextpdf.kernel.pdf.PageLabelNumberingStyle;
import com.ririv.quickoutline.exception.InvalidPageLabelRuleException;
import com.ririv.quickoutline.pdfProcess.PageLabel;
import com.ririv.quickoutline.pdfProcess.PageLabelProcessor;
import com.ririv.quickoutline.pdfProcess.itextImpl.ItextPageLabelProcessor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class PdfPageLabelService {
    private final PageLabelProcessor<PageLabelNumberingStyle> pageLabelProcessor = new ItextPageLabelProcessor();

    public String[] setPageLabels(String srcFilePath, String destFilePath, List<PageLabel> labelList) throws IOException{
        return pageLabelProcessor.setPageLabels(srcFilePath, destFilePath, labelList);
    }

    public String[] getPageLabels(String string) throws IOException {
        return pageLabelProcessor.getPageLabels(string);
    }

    public List<String> simulatePageLabels(List<PageLabelRule> rules, int totalPages) {
        List<PageLabel> pageLabels = convertRulesToPageLabels(rules);
        List<String> simulatedLabels = new ArrayList<>();
        if (pageLabels.isEmpty()) {
            for (int i = 1; i <= totalPages; i++) {
                simulatedLabels.add(String.valueOf(i));
            }
            return simulatedLabels;
        }

        // Rules are already sorted by fromPage due to TreeMap in convertRulesToPageLabels

        for (int i = 1; i <= totalPages; i++) {
            // Find the correct rule for the current page `i`
            PageLabel currentRule = null;
            for (PageLabel rule : pageLabels) {
                if (i >= rule.pageNum()) {
                    currentRule = rule;
                } else {
                    break; // Rules are sorted, no need to check further
                }
            }

            String label;
            if (currentRule != null) {
                int pageOffset = i - currentRule.pageNum();
                label = generateLabel(currentRule.numberingStyle(), currentRule.firstPage() + pageOffset, currentRule.labelPrefix());
            } else {
                // If no rule has been set yet (e.g., for pages before the first rule),
                // use the default decimal numbering.
                label = String.valueOf(i);
            }
            simulatedLabels.add(label);
        }
        return simulatedLabels;
    }

    private String generateLabel(PageLabel.PageLabelNumberingStyle style, int number, String prefix) {
        if (style == null) { // Fallback for undefined or null styles
            return (prefix != null ? prefix : "") + number;
        }
        String label;
        switch (style) {
            case DECIMAL_ARABIC_NUMERALS -> label = String.valueOf(number);
            case UPPERCASE_ROMAN_NUMERALS -> label = toRoman(number);
            case LOWERCASE_ROMAN_NUMERALS -> label = toRoman(number).toLowerCase();
            case UPPERCASE_LETTERS -> label = toLetters(number);
            case LOWERCASE_LETTERS -> label = toLetters(number).toLowerCase();
            case NONE -> label = ""; // For NONE style, display nothing.
            default -> label = String.valueOf(number);
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

    /**
     * Validates user input and creates a PageLabelRule.
     * This is pure business logic, moved from the controller.
     *
     * @throws InvalidPageLabelRuleException if the rule is invalid.
     */
    public PageLabelRule validateAndCreateRule(String fromPageStr, String prefix, String startStr, PageLabel.PageLabelNumberingStyle style, List<PageLabelRule> existingRules) throws InvalidPageLabelRuleException {
        if (fromPageStr.isEmpty()) {
            throw new InvalidPageLabelRuleException("输入无效，'起始页' 字段不能为空。");
        }

        int fromPage;
        try {
            fromPage = Integer.parseInt(fromPageStr);
        } catch (NumberFormatException e) {
            throw new InvalidPageLabelRuleException("输入无效，'起始页' 必须是有效的数字。");
        }

        if (fromPage <= 0) {
            throw new InvalidPageLabelRuleException("输入无效，页码必须是正数。");
        }

        for (PageLabelRule existingRule : existingRules) {
            if (existingRule.fromPage() == fromPage) {
                throw new InvalidPageLabelRuleException("输入无效，已存在相同起始页的规则。");
            }
        }
        int start = 1;
        if (!startStr.isEmpty()) {
            try {
                start = Integer.parseInt(startStr);
                if (start < 1) {
                    throw new InvalidPageLabelRuleException("输入无效，起始数字必须大于或等于 1。");
                }
            } catch (NumberFormatException e) {
                throw new InvalidPageLabelRuleException("输入无效，'起始于' 必须是有效的数字。");
            }
        }
        return new PageLabelRule(fromPage, style, prefix, start);
    }

    public List<PageLabel> convertRulesToPageLabels(List<PageLabelRule> rules) {
        Map<Integer, PageLabel> pageLabelsMap = new TreeMap<>();
        for (PageLabelRule rule : rules) {
            PageLabel pageLabel = new PageLabel(rule.fromPage(), rule.style(), rule.prefix(), rule.start());
            pageLabelsMap.put(rule.fromPage(), pageLabel);
        }
        return new ArrayList<>(pageLabelsMap.values());
    }
}