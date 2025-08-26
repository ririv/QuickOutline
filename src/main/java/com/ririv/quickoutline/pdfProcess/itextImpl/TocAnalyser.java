package com.ririv.quickoutline.pdfProcess.itextImpl;

import com.ririv.quickoutline.pdfProcess.itextImpl.model.LineWithMetadata;
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
    public List<TextBlock> analyze(List<TextBlock> allBlocks) {
        if (allBlocks.isEmpty()) return new ArrayList<>();

        Style dominantStyle = findDominantStyle(allBlocks);

        Map<Integer, List<TextBlock>> blocksByPage = allBlocks.stream()
                .collect(Collectors.groupingBy(b -> b.getPrimaryLine().getPageNum()));

        List<TextBlock> tocBlocks = new ArrayList<>();

        for (Map.Entry<Integer, List<TextBlock>> entry : blocksByPage.entrySet()) {
            tocBlocks.addAll(getPotentialTocBlocks(entry.getValue(), dominantStyle));
        }
        return tocBlocks;
    }

    private List<TextBlock> getPotentialTocBlocks(List<TextBlock> pageBlocks, Style dominantStyle) {
        List<TextBlock> tocCandidates = new ArrayList<>();
        for (TextBlock block : pageBlocks) {
            if (isTocLikeBlock(block, dominantStyle)) {
                tocCandidates.add(block);
            }
        }
        if (tocCandidates.size() >= 3) {
            return tocCandidates;
        }
        return Collections.emptyList();
    }

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

    private Style findDominantStyle(List<TextBlock> allBlocks) {
        return allBlocks.stream()
                .map(TextBlock::getPrimaryStyle)
                .collect(Collectors.groupingBy(s -> s, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(new Style("Default", 10f));
    }
}
