package com.ririv.quickoutline.pdfProcess.itextImpl.model;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

// 语义上的“块/段落”
public class TextBlock {
    public int type = 0; 
    private final List<LineWithMetadata> lines = new ArrayList<>();
    private String cachedText = null;
    private CharacterPattern charPattern = null;

    private static final Pattern NUMBERING_PATTERN = Pattern.compile("^\\s*([\\d.]+|[A-Za-z][.]|[IVXLCDM]+[.)]).*\s*$");


    public TextBlock(LineWithMetadata initialLine) { addLine(initialLine); }
    public void addLine(LineWithMetadata line) {
        lines.add(line);
        cachedText = null;
        charPattern = null;
    }
    public List<LineWithMetadata> getLines() { return lines; }
    public LineWithMetadata getPrimaryLine() { return lines.get(0); }
    public String getText() {
        if (cachedText == null) {
            cachedText = lines.stream().map(LineWithMetadata::getTextContent).collect(Collectors.joining(" ")); // Changed to space
        }
        return cachedText;
    }
    public Style getPrimaryStyle() { return lines.isEmpty() ? new Style("Default", 10f) : lines.get(0).getStyle(); }
    public boolean isBold() { return !lines.isEmpty() && getPrimaryStyle().getFontName().toLowerCase().contains("bold"); }
    public float getX() { return lines.isEmpty() ? 0f : getPrimaryLine().getX(); }
    public float getY() { return lines.isEmpty() ? 0f : getPrimaryLine().getY(); }
    public float getWidth() {
        if (lines.isEmpty()) return 0f;
        return lines.stream().map(LineWithMetadata::getWidth).max(Float::compareTo).orElse(0f);
    }

    public float getHeight() {
        if (lines.isEmpty()) return 0f;
        float top = lines.get(0).getY();
        float bottom = lines.get(lines.size() - 1).getY() - lines.get(lines.size() - 1).getStyle().getFontSize();
        return top - bottom;
    }
    public CharacterPattern getCharPattern() {
        if (charPattern == null) {
            charPattern = new CharacterPattern(getText());
        }
        return charPattern;
    }

    public double getSkew() {
        if (lines.isEmpty()) return 0.0;
        return lines.stream().mapToDouble(LineWithMetadata::getSkew).average().orElse(0.0);
    }

    public String reconstructBlockWithSpaces() {
        StringBuilder textBuilder = new StringBuilder();
        for (int lineIdx = 0; lineIdx < lines.size(); lineIdx++) {
            LineWithMetadata line = lines.get(lineIdx);
            List<TextChunk> chunks = line.getChunks();

            if (chunks == null || chunks.isEmpty()) {
                textBuilder.append(line.getTextContent());
                continue;
            }

            TextChunk firstChunk = chunks.get(0);
            textBuilder.append(firstChunk.getText());

            for (int i = 1; i < chunks.size(); i++) {
                TextChunk prev = chunks.get(i - 1);
                TextChunk curr = chunks.get(i);

                float spaceWidth = prev.getSingleSpaceWidth();
                if (spaceWidth <= 0) {
                    spaceWidth = prev.getFontSize() * 0.25f;
                }
                float gap = curr.getX() - (prev.getX() + prev.getWidth());

                if (gap > spaceWidth * 5) {
                    textBuilder.append("     ");
                } else if (gap > spaceWidth * 0.3f) {
                    textBuilder.append(" ");
                }
                textBuilder.append(curr.getText());
            }
            if (lineIdx < lines.size() - 1) {
                textBuilder.append("\n");
            }
        }
        return textBuilder.toString();
    }

    public static List<TextBlock> aggregateLinesIntoBlocks(List<LineWithMetadata> lines) {
        List<TextBlock> blocks = new ArrayList<>();
        if (lines.isEmpty()) return blocks;

        TextBlock currentBlock = new TextBlock(lines.get(0));

        for (int i = 1; i < lines.size(); i++) {
            LineWithMetadata currentLine = lines.get(i);
            if (shouldMerge(currentBlock, currentLine)) {
                currentBlock.addLine(currentLine);
            } else {
                blocks.add(currentBlock);
                currentBlock = new TextBlock(currentLine);
            }
        }
        blocks.add(currentBlock);
        return blocks;
    }

    private static boolean shouldMerge(TextBlock block, LineWithMetadata nextLine) {
        LineWithMetadata lastLine = block.getLines().get(block.getLines().size() - 1);
        if (lastLine.getPageNum() != nextLine.getPageNum()) return false;
        double verticalGap = lastLine.getY() - nextLine.getY();
        if (verticalGap > lastLine.getStyle().getFontSize() * 1.8) return false;
        if (!lastLine.getStyle().equals(nextLine.getStyle())) return false;
        if (Math.abs(lastLine.getX() - nextLine.getX()) > 5.0) return false;
        String prevText = lastLine.getTextContent().trim();
        if (prevText.endsWith(".") || prevText.endsWith("?") || prevText.endsWith("!") || prevText.endsWith(":")) return false;
        String nextText = nextLine.getTextContent().trim();
        if (nextText.isEmpty() || NUMBERING_PATTERN.matcher(nextText).matches()) return false;

        if (!Character.isLowerCase(nextText.charAt(0))) {
            return prevText.length() <= 60;
        }

        return true;
    }
}