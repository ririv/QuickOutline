package com.ririv.quickoutline.pdfProcess.itextImpl.model;

import java.util.Objects;

public class Style {
    private final String fontName;
    private final float fontSize;

    public Style(String n, float s) {
        this.fontName = n;
        this.fontSize = s;
    }

    public String getFontName() { return fontName; }
    public float getFontSize() { return fontSize; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Style style = (Style) o;
        return Float.compare(style.fontSize, fontSize) == 0 && Objects.equals(fontName, style.fontName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fontName, fontSize);
    }
}
