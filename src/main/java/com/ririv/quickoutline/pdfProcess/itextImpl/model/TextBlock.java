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
}