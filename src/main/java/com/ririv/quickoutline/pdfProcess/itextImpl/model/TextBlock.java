package com.ririv.quickoutline.pdfProcess.itextImpl.model;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TextBlock {
    public int type = 0; 
    private final List<LineWithMetadata> lines = new ArrayList<>();
    private String cachedText = null;
    private CharacterPattern charPattern = null;

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
}