package com.ririv.quickoutline.pdfProcess.itextImpl;

import com.ririv.quickoutline.pdfProcess.itextImpl.model.Style;
import com.ririv.quickoutline.pdfProcess.itextImpl.model.TextBlock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TocAnalyser {

    private static final Pattern TOC_DOT_PATTERN = Pattern.compile(".*([.]\\s*|\\s{2,}){4,}\\s*\\d+\\s*$");
    private static final Pattern TOC_NUMERIC_END_PATTERN = Pattern.compile("^(.*\\D)\\s+(\\d+)\\s*$");

    /**
     * [新公共方法1] 从一批文本块中找出主要样式。
     * 对应两阶段处理的第一阶段。
     * @param allBlocks 所有文本块
     * @return 主要样式
     */
    public Style findDominantStyle(List<TextBlock> allBlocks) {
        if (allBlocks == null || allBlocks.isEmpty()) {
            return new Style("Default", 10f);
        }
        return allBlocks.stream()
                .map(TextBlock::getPrimaryStyle)
                .collect(Collectors.groupingBy(s -> s, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(new Style("Default", 10f));
    }

    /**
     * [新公共方法2] 在给定主要样式的前提下，从一页的文本块中找出潜在的目录块。
     * 对应两阶段处理的第二阶段。
     * @param pageBlocks 一页的文本块
     * @param dominantStyle 全局主要样式
     * @return 该页的目录文本块列表
     */
    public List<TextBlock> findTocBlocksInPage(List<TextBlock> pageBlocks, Style dominantStyle) {
        List<TextBlock> tocCandidates = new ArrayList<>();
        for (TextBlock block : pageBlocks) {
            if (isTocLikeBlock(block, dominantStyle)) {
                tocCandidates.add(block);
            }
        }
        // 只有当一页中至少有3个候选项时，才认为它们是目录的一部分
        if (tocCandidates.size() >= 3) {
            return tocCandidates;
        }
        return Collections.emptyList();
    }

    /**
     * (保留为私有) 判断单个块是否像目录项，这是内部实现细节。
     */
    private boolean isTocLikeBlock(TextBlock block, Style dominantStyle) {
        String trimmed = block.getText().trim();
        if (trimmed.length() > 150 || trimmed.length() < 3) {
            return false;
        }

        if (TOC_DOT_PATTERN.matcher(trimmed).matches()) {
            return true;
        }

        if (TOC_NUMERIC_END_PATTERN.matcher(trimmed).matches()) {
            boolean isAbnormal = block.getPrimaryStyle().getFontSize() > dominantStyle.getFontSize() + 0.5 && trimmed.length() < 80;
            return isAbnormal && !trimmed.endsWith(".");
        }

        return false;
    }

    /**
     * (可选) 保留原有的 analyze 方法，并让它使用新的公共方法。
     * 这样它就成了一个方便的、用于单线程处理的封装。
     */
    public List<TextBlock> analyze(List<TextBlock> allBlocks) {
        if (allBlocks == null || allBlocks.isEmpty()) {
            return new ArrayList<>();
        }

        // 1. 计算全局样式
        Style dominantStyle = findDominantStyle(allBlocks);

        // 2. 按页分组，并对每页进行分析
        Map<Integer, List<TextBlock>> blocksByPage = allBlocks.stream()
                .collect(Collectors.groupingBy(b -> b.getPrimaryLine().getPageNum()));

        List<TextBlock> tocBlocks = new ArrayList<>();
        for (List<TextBlock> pageBlocks : blocksByPage.values()) {
            tocBlocks.addAll(findTocBlocksInPage(pageBlocks, dominantStyle));
        }
        return tocBlocks;
    }
}
