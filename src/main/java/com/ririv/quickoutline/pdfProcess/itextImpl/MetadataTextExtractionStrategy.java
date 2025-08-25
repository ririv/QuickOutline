package com.ririv.quickoutline.pdfProcess.itextImpl;

import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.geom.Matrix;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.canvas.parser.EventType;
import com.itextpdf.kernel.pdf.canvas.parser.data.IEventData;
import com.itextpdf.kernel.pdf.canvas.parser.data.TextRenderInfo;
import com.itextpdf.kernel.pdf.canvas.parser.listener.ITextExtractionStrategy;
import com.ririv.quickoutline.pdfProcess.itextImpl.model.TextChunk;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MetadataTextExtractionStrategy implements ITextExtractionStrategy {

    private final List<TextChunk> textChunks = new ArrayList<>();

    @Override
    public void eventOccurred(IEventData data, EventType type) {
        if (type.equals(EventType.RENDER_TEXT)) {
            TextRenderInfo renderInfo = (TextRenderInfo) data;
            String text = renderInfo.getText();
            if (text == null || text.trim().isEmpty()) {
                return;
            }

            PdfFont font = renderInfo.getFont();
            float fontSize = renderInfo.getFontSize();
            Rectangle baseline = renderInfo.getBaseline().getBoundingRectangle();
            float x = baseline.getX();
            float y = baseline.getY();
            float width = baseline.getWidth();
            float singleSpaceWidth = renderInfo.getSingleSpaceWidth();

            Matrix matrix = renderInfo.getTextMatrix();
            double skew = 0;
            float i11 = matrix.get(Matrix.I11);
            float i12 = matrix.get(Matrix.I12);
            float i21 = matrix.get(Matrix.I21);
            float i22 = matrix.get(Matrix.I22);

            if (i11 != 0 && i22 != 0) {
                double skewX = i12 / i11;
                double skewY = i21 / i22;
                skew = Math.pow(skewX, 2) + Math.pow(skewY, 2);
            }

            textChunks.add(new TextChunk(text, x, y, width, font.getFontProgram().getFontNames().getFontName(), fontSize, singleSpaceWidth, skew));
        }
    }

    @Override
    public Set<EventType> getSupportedEvents() {
        Set<EventType> events = new HashSet<>();
        events.add(EventType.RENDER_TEXT);
        return events;
    }

    @Override
    public String getResultantText() {
        StringBuilder sb = new StringBuilder();
        for (TextChunk chunk : textChunks) {
            sb.append(chunk.getText());
        }
        return sb.toString();
    }

    public List<TextChunk> getTextChunks() {
        return textChunks;
    }
}