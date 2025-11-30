package com.ririv.quickoutline.pdfProcess.itextImpl.model;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

// 词元
public class TextChunk {
    private final String text;
    private final String fontName;
    private final float fontSize;
    private final float x;
    private final float y;
    private final float width;
    private final float singleSpaceWidth;
    private final double skew;

    public TextChunk(String text, float x, float y, float width, String fontName, float fontSize, float singleSpaceWidth, double skew) {
        this.text = text;
        this.x = x;
        this.y = y;
        this.width = width;
        this.fontName = fontName;
        this.fontSize = fontSize;
        this.singleSpaceWidth = singleSpaceWidth;
        this.skew = skew;
    }

    public String getText() { return text; }
    public String getFontName() { return fontName; }
    public float getFontSize() { return fontSize; }
    public float getX() { return x; }
    public float getY() { return y; }
    public float getWidth() { return width; }
    public float getSingleSpaceWidth() { return singleSpaceWidth; }
    public double getSkew() { return skew; }

    @Override
    public String toString() {
        return "TextChunk{"
               + "text=\'" + text + "\'"
               + ", fontName=\'" + fontName + "\'"
               + ", fontSize=" + fontSize
               + ", x=" + x
               + ", y=" + y
               + ", width=" + width
               + ", singleSpaceWidth=" + singleSpaceWidth
               + ", skew=" + skew
               + "}";
    }


    public static List<LineWithMetadata> convertChunksToLines(List<TextChunk> chunks, PdfPage page, PdfDocument pdfDoc) {
        if (chunks.isEmpty()) {
            return new ArrayList<>();
        }

        chunks.sort(Comparator.comparingDouble(TextChunk::getY).reversed().thenComparingDouble(TextChunk::getX));

        List<LineWithMetadata> lines = new ArrayList<>();
        List<TextChunk> currentLineChunks = new ArrayList<>();
        TextChunk firstChunk = chunks.get(0);
        currentLineChunks.add(firstChunk);

        for (int i = 1; i < chunks.size(); i++) {
            TextChunk currentChunk = chunks.get(i);
            TextChunk prevChunk = chunks.get(i - 1);

            if (Math.abs(currentChunk.getY() - prevChunk.getY()) < 1.0) {
                currentLineChunks.add(currentChunk);
            } else {
                lines.add(createLineFromChunks(currentLineChunks, page, pdfDoc));
                currentLineChunks.clear();
                currentLineChunks.add(currentChunk);
            }
        }
        lines.add(createLineFromChunks(currentLineChunks, page, pdfDoc));

        return lines;
    }

    private static LineWithMetadata createLineFromChunks(List<TextChunk> chunks, PdfPage page, PdfDocument pdfDoc) {
        String text = chunks.stream().map(TextChunk::getText).collect(Collectors.joining());
        TextChunk first = chunks.get(0);
        Style style = new Style(first.getFontName(), first.getFontSize());
        float x = first.getX();
        float y = first.getY();
        float width = (float) chunks.stream().mapToDouble(TextChunk::getWidth).sum();
        double skew = chunks.stream().mapToDouble(TextChunk::getSkew).average().orElse(0.0);
        return new LineWithMetadata(text, x, y, width, style, pdfDoc.getPageNumber(page), page.getPageSize(), skew, new ArrayList<>(chunks));
    }

}
