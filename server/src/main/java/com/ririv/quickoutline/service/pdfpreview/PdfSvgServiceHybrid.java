package com.ririv.quickoutline.service.pdfpreview;

import jakarta.inject.Singleton;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.contentstream.PDFGraphicsStreamEngine;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDSimpleFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.image.PDImage;
import org.apache.pdfbox.util.Matrix;
import org.apache.pdfbox.util.Vector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.*;


// 视觉层复用 + 交互层透明 + 字体缓存
@Singleton
public class PdfSvgServiceHybrid {
    private static final Logger log = LoggerFactory.getLogger(PdfSvgServiceHybrid.class);

    private final Map<Integer, String> pageCache = new HashMap<>();
    private int lastTotalPages = 0;

    public record SvgPageUpdate(int pageIndex, String svgContent, int totalPages, float widthPt, float heightPt) {}

    public void clearCache() {
        pageCache.clear();
        lastTotalPages = 0;
    }

    // 同步方法防止并发 NPE
    public synchronized List<SvgPageUpdate> diffPdfToSvg(byte[] pdfBytes) {
        List<SvgPageUpdate> updates = new ArrayList<>();
        if (pdfBytes == null || pdfBytes.length == 0) return updates;

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            int currentTotalPages = document.getNumberOfPages();

            for (int i = 0; i < currentTotalPages; i++) {
                PDPage page = document.getPage(i);
                PDRectangle cropBox = page.getCropBox();
                int rotation = page.getRotation();

                float displayWidth, displayHeight;
                if (rotation == 90 || rotation == 270) {
                    displayWidth = cropBox.getHeight();
                    displayHeight = cropBox.getWidth();
                } else {
                    displayWidth = cropBox.getWidth();
                    displayHeight = cropBox.getHeight();
                }

                String currentSvg = convertPageToSvg(page, displayWidth, displayHeight);

                String cachedSvg = pageCache.get(i);
                if (cachedSvg == null || !cachedSvg.equals(currentSvg)) {
                    pageCache.put(i, currentSvg);
                    updates.add(new SvgPageUpdate(i, currentSvg, currentTotalPages, displayWidth, displayHeight));
                }
            }

            if (currentTotalPages < lastTotalPages) {
                for (int k = currentTotalPages; k < lastTotalPages; k++) pageCache.remove(k);
                if (updates.isEmpty() && currentTotalPages > 0) {
                    PDPage p0 = document.getPage(0);
                    int r0 = p0.getRotation();
                    float w0 = (r0 == 90 || r0 == 270) ? p0.getCropBox().getHeight() : p0.getCropBox().getWidth();
                    float h0 = (r0 == 90 || r0 == 270) ? p0.getCropBox().getWidth() : p0.getCropBox().getHeight();
                    String first = pageCache.get(0);
                    if(first != null) updates.add(new SvgPageUpdate(0, first, currentTotalPages, w0, h0));
                }
            }
            lastTotalPages = currentTotalPages;

        } catch (Exception e) {
            log.error("Error converting PDF to SVG", e);
        }
        return updates;
    }

    private String convertPageToSvg(PDPage page, float width, float height) {
        try {
            HybridSvgConverter converter = new HybridSvgConverter(page, width, height);
            converter.processPage(page);
            return converter.getSvgResult();
        } catch (Exception e) {
            log.error("Failed to convert page", e);
            return "";
        }
    }

    // 辅助：格式化浮点数，强制使用 US Locale (用点号 . 而不是逗号 ,)
    private static String fmt(float d) {
        if (d == (long) d) return String.format("%d", (long) d);
        return String.format(java.util.Locale.US, "%s", d);
    }

    // =================================================================
    // 核心引擎
    // =================================================================

    private static class HybridSvgConverter extends PDFGraphicsStreamEngine {

        private final float pageHeight;
        private final float displayWidth, displayHeight;

        private final StringBuilder defsBuffer = new StringBuilder();
        private final StringBuilder contentBuffer = new StringBuilder();

        // 缓存：Key = FontName_Code, Value = GlyphID
        private final Map<String, String> glyphCache = new HashMap<>();

        private final StringBuilder currentPathData = new StringBuilder();
        private Point2D currentPoint = new Point2D.Float(0,0);

        // 文本缓冲
        private final StringBuilder textBuffer = new StringBuilder();
        private Matrix startMatrix = null;
        private float lastEndX = -1;
        private float currentFontSize = 0;

        public HybridSvgConverter(PDPage page, float displayWidth, float displayHeight) {
            super(page);
            this.pageHeight = page.getCropBox().getHeight();
            this.displayWidth = displayWidth;
            this.displayHeight = displayHeight;
        }

        public String getSvgResult() {
            flushTextBuffer();
            return String.format(java.util.Locale.US,
                    "<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 %s %s' width='100%%' height='100%%' numberingStyle='overflow:hidden;'>" +
                            "<defs>%s</defs>" +
                            "<g transform='translate(0, %s) scale(1, -1)'>%s</g>" +
                            "</svg>",
                    fmt(displayWidth), fmt(displayHeight),
                    defsBuffer.toString(),
                    fmt(pageHeight),
                    contentBuffer.toString()
            );
        }

        private String fmt(float d) { return PdfSvgServiceHybrid.fmt(d); }

        // ----------------------------------------------------------------
        // 1. 文字处理
        // ----------------------------------------------------------------

        @Override
        protected void showGlyph(Matrix textRenderingMatrix, PDFont font, int code, Vector displacement) throws IOException {
            // --- A. 视觉层 (Visual Layer) ---
            String fontName = font.getName();
            if (fontName == null) fontName = "U";
            String cacheKey = fontName + "_" + code;
            String glyphId = glyphCache.get(cacheKey);

            // 1. 缓存路径
            if (glyphId == null) {
                try {
                    GeneralPath path = getGlyphPath(font, code);
                    if (path != null) {
                        // 【核心修复】应用字体矩阵 (FontMatrix)
                        // 将 1000x1000 的字形缩放到 1x1 标准空间
                        Matrix fontMatrix = font.getFontMatrix();
                        if (fontMatrix != null) {
                            path.transform(fontMatrix.createAffineTransform());
                        }

                        String d = getPathString(path);
                        if (!d.isEmpty()) {
                            glyphId = "g_" + Math.abs(cacheKey.hashCode());
                            defsBuffer.append(String.format("<path id='%s' d='%s' />", glyphId, d));
                            glyphCache.put(cacheKey, glyphId);
                        }
                    }
                } catch (Exception ignored) {}
            }

            // 2. 绘制 <use>
            if (glyphId != null) {
                String color = getColorString(getGraphicsState().getNonStrokingColor());
                float a = textRenderingMatrix.getValue(0, 0);
                float b = textRenderingMatrix.getValue(0, 1);
                float c = textRenderingMatrix.getValue(1, 0);
                float d = textRenderingMatrix.getValue(1, 1);
                float e = textRenderingMatrix.getValue(2, 0);
                float f = textRenderingMatrix.getValue(2, 1);

                // 必须使用 US Locale 格式化，否则 SVG 里的 matrix(...) 可能会有逗号，导致渲染失败
                contentBuffer.append(String.format(java.util.Locale.US,
                        "<use href='#%s' fill='%s' transform='matrix(%f %f %f %f %f %f)' />",
                        glyphId, color, a, b, c, d, e, f
                ));
            }

            // --- B. 交互层 (Interaction Layer) ---
            String unicode = null;
            try { unicode = font.toUnicode(code); } catch (Exception ignored) {}

            if (unicode == null || unicode.isEmpty()) return;

            float fontSize = textRenderingMatrix.getScalingFactorY();
            float charWidth = (font.getWidth(code) / 1000f) * fontSize;

            boolean shouldFlush = false;
            if (startMatrix != null) {
                if (Math.abs(textRenderingMatrix.getTranslateY() - startMatrix.getTranslateY()) > 1.0f
                        || Math.abs(fontSize - currentFontSize) > 0.1f) {
                    shouldFlush = true;
                } else {
                    float x = textRenderingMatrix.getTranslateX();
                    if (Math.abs(x - lastEndX) > fontSize * 0.3f || x < lastEndX) {
                        if (x > lastEndX) textBuffer.append(" ");
                        shouldFlush = true;
                    }
                }
            } else {
                startMatrix = textRenderingMatrix.clone();
                currentFontSize = fontSize;
            }

            if (shouldFlush) {
                flushTextBuffer();
                startMatrix = textRenderingMatrix.clone();
                currentFontSize = fontSize;
            }
            textBuffer.append(unicode);
            lastEndX = textRenderingMatrix.getTranslateX() + charWidth;
        }

        private void flushTextBuffer() {
            if (textBuffer.length() == 0 || startMatrix == null) return;

            float x = startMatrix.getTranslateX();
            float y = startMatrix.getTranslateY();

            // 透明文字层
            contentBuffer.append(String.format(java.util.Locale.US,
                    "<text transform='translate(%f %f) scale(1, -1)' " +
                            "font-family='sans-serif' font-size='%f' fill='transparent' numberingStyle='white-space:pre; cursor:text;'>%s</text>",
                    x, y, currentFontSize, escapeXml(textBuffer.toString())
            ));

            textBuffer.setLength(0);
            startMatrix = null;
        }

        // ----------------------------------------------------------------
        // 2. 路径处理
        // ----------------------------------------------------------------

        @Override public void moveTo(float x, float y) {
            flushTextBuffer();
            currentPathData.append(String.format(java.util.Locale.US, "M %f %f ", x, y));
            currentPoint = new Point2D.Float(x, y);
        }

        @Override public void lineTo(float x, float y) {
            currentPathData.append(String.format(java.util.Locale.US, "L %f %f ", x, y));
            currentPoint = new Point2D.Float(x, y);
        }

        @Override public void curveTo(float x1, float y1, float x2, float y2, float x3, float y3) {
            currentPathData.append(String.format(java.util.Locale.US, "C %f %f %f %f %f %f ", x1, y1, x2, y2, x3, y3));
            currentPoint = new Point2D.Float(x3, y3);
        }

        @Override public void appendRectangle(Point2D p0, Point2D p1, Point2D p2, Point2D p3) {
            flushTextBuffer();
            currentPathData.append(String.format(java.util.Locale.US, "M %f %f L %f %f L %f %f L %f %f Z ",
                    p0.getX(), p0.getY(), p1.getX(), p1.getY(), p2.getX(), p2.getY(), p3.getX(), p3.getY()));
        }

        @Override public void closePath() { currentPathData.append("Z "); }
        @Override public void endPath() { currentPathData.setLength(0); }

        @Override public void strokePath() throws IOException {
            flushTextBuffer();
            String color = getColorString(getGraphicsState().getStrokingColor());
            float width = getGraphicsState().getLineWidth();
            contentBuffer.append(String.format(java.util.Locale.US,
                    "<path d='%s' stroke='%s' stroke-width='%f' fill='none' stroke-linecap='round' stroke-linejoin='round' />",
                    currentPathData.toString(), color, width
            ));
            currentPathData.setLength(0);
        }

        @Override public void fillPath(int windingRule) throws IOException {
            flushTextBuffer();
            String color = getColorString(getGraphicsState().getNonStrokingColor());
            String rule = (windingRule == 0) ? "nonzero" : "evenodd";
            contentBuffer.append(String.format(java.util.Locale.US,
                    "<path d='%s' fill='%s' fill-rule='%s' stroke='none' />",
                    currentPathData.toString(), color, rule
            ));
            currentPathData.setLength(0);
        }

        @Override public void fillAndStrokePath(int windingRule) throws IOException {
            flushTextBuffer();
            String fillColor = getColorString(getGraphicsState().getNonStrokingColor());
            String strokeColor = getColorString(getGraphicsState().getStrokingColor());
            float width = getGraphicsState().getLineWidth();
            String rule = (windingRule == 0) ? "nonzero" : "evenodd";
            contentBuffer.append(String.format(java.util.Locale.US,
                    "<path d='%s' fill='%s' fill-rule='%s' stroke='%s' stroke-width='%f' />",
                    currentPathData.toString(), fillColor, rule, strokeColor, width
            ));
            currentPathData.setLength(0);
        }

        // --- Helpers ---

        private GeneralPath getGlyphPath(PDFont font, int code) throws IOException {
            if (font instanceof PDType0Font) return ((PDType0Font) font).getPath(code);
            else if (font instanceof PDSimpleFont) {
                String name = ((PDSimpleFont) font).getEncoding().getName(code);
                return ((PDSimpleFont) font).getPath(name);
            }
            return null;
        }

        private String getPathString(GeneralPath path) {
            StringBuilder d = new StringBuilder();
            float[] coords = new float[6];
            PathIterator pi = path.getPathIterator(null);
            while (!pi.isDone()) {
                int type = pi.currentSegment(coords);
                switch (type) {
                    case PathIterator.SEG_MOVETO: d.append(String.format(java.util.Locale.US, "M %f %f ", coords[0], coords[1])); break;
                    case PathIterator.SEG_LINETO: d.append(String.format(java.util.Locale.US, "L %f %f ", coords[0], coords[1])); break;
                    case PathIterator.SEG_QUADTO: d.append(String.format(java.util.Locale.US, "Q %f %f %f %f ", coords[0], coords[1], coords[2], coords[3])); break;
                    case PathIterator.SEG_CUBICTO: d.append(String.format(java.util.Locale.US, "C %f %f %f %f %f %f ", coords[0], coords[1], coords[2], coords[3], coords[4], coords[5])); break;
                    case PathIterator.SEG_CLOSE: d.append("Z "); break;
                }
                pi.next();
            }
            return d.toString();
        }

        private String getColorString(PDColor pdColor) {
            try {
                float[] c = pdColor.getColorSpace().toRGB(pdColor.getComponents());
                int r = Math.max(0, Math.min(255, Math.round(c[0] * 255)));
                int g = Math.max(0, Math.min(255, Math.round(c[1] * 255)));
                int b = Math.max(0, Math.min(255, Math.round(c[2] * 255)));
                return String.format("#%02x%02x%02x", r, g, b);
            } catch (Exception e) { return "black"; }
        }

        private String escapeXml(String s) {
            return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;");
        }

        @Override public Point2D getCurrentPoint() { return currentPoint; }
        @Override public void clip(int windingRule) {}
        @Override public void drawImage(PDImage pdImage) {}
        @Override public void shadingFill(COSName shadingName) {}
    }
}