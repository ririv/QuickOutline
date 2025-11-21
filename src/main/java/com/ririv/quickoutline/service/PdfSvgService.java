package com.ririv.quickoutline.service;

import jakarta.inject.Singleton;
import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.graphics.image.PDImage;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.rendering.PageDrawer;
import org.apache.pdfbox.rendering.PageDrawerParameters;
import org.apache.pdfbox.util.Matrix;
import org.apache.pdfbox.util.Vector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
public class PdfSvgService {
    private static final Logger log = LoggerFactory.getLogger(PdfSvgService.class);

    private final Map<Integer, String> pageCache = new HashMap<>();
    private int lastTotalPages = 0;

    public record SvgPageUpdate(int pageIndex, String svgContent, int totalPages, float widthPt, float heightPt) {}

    public void clearCache() {
        pageCache.clear();
        lastTotalPages = 0;
    }

    public List<SvgPageUpdate> diffPdfToSvg(byte[] pdfBytes) {
        List<SvgPageUpdate> updates = new ArrayList<>();
        if (pdfBytes == null || pdfBytes.length == 0) return updates;

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            int currentTotalPages = document.getNumberOfPages();
            CustomPDFRenderer renderer = new CustomPDFRenderer(document);

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

                String currentSvg = convertPageToSvg(renderer, i, displayWidth, displayHeight);

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
                    if (first != null) updates.add(new SvgPageUpdate(0, first, currentTotalPages, w0, h0));
                }
            }
            lastTotalPages = currentTotalPages;
        } catch (Exception e) {
            log.error("Error converting PDF to SVG", e);
        }
        return updates;
    }

    private String convertPageToSvg(PDFRenderer renderer, int pageIndex, float width, float height) {
        try {
            DOMImplementation domImpl = GenericDOMImplementation.getDOMImplementation();
            String svgNS = "http://www.w3.org/2000/svg";
            Document svgDoc = domImpl.createDocument(svgNS, "svg", null);
            SVGGraphics2D svgGenerator = new SVGGraphics2D(svgDoc);
            svgGenerator.setSVGCanvasSize(new Dimension((int) width, (int) height));

            renderer.renderPageToGraphics(pageIndex, svgGenerator, 1.0f);

            try (StringWriter writer = new StringWriter()) {
                svgGenerator.stream(writer, true);
                String rawSvg = writer.toString();

                // 注入 viewBox 以适配前端缩放 (解决 pt vs px 空白问题)
                int svgTagEndIndex = rawSvg.indexOf(">", rawSvg.indexOf("<svg")) + 1;
                int svgCloseTagIndex = rawSvg.lastIndexOf("</svg>");

                if (svgTagEndIndex > 0 && svgCloseTagIndex > svgTagEndIndex) {
                    String svgContent = rawSvg.substring(svgTagEndIndex, svgCloseTagIndex);
                    String newHeader = String.format(
                            "<svg xmlns=\"http://www.w3.org/2000/svg\" " +
                                    "viewBox=\"0 0 %s %s\" " +
                                    "width=\"100%%\" height=\"100%%\" " +
                                    "preserveAspectRatio=\"xMidYMid meet\" " +
                                    "style=\"display:block; overflow:hidden;\">",
                            fmt(width), fmt(height)
                    );
                    return newHeader + svgContent + "</svg>";
                }
                return rawSvg;
            }
        } catch (Exception e) {
            log.error("Failed to render page {} to SVG", pageIndex, e);
            return "";
        }
    }

    private String fmt(float d) {
        if (d == (long) d) return String.format("%d", (long) d);
        return String.format("%s", d);
    }

    // =================================================================
    // 自定义渲染器
    // =================================================================

    private static class CustomPDFRenderer extends PDFRenderer {
        public CustomPDFRenderer(PDDocument document) { super(document); }

        @Override
        protected PageDrawer createPageDrawer(PageDrawerParameters parameters) throws IOException {
            return new TextReplacementPageDrawer(parameters);
        }
    }

    /**
     * 替换模式 (Replacement Mode):
     * 1. 图形/公式/表格 -> 调用 super (PDFBox 原生绘制)
     * 2. 文字 -> 拦截 -> 绘制 <text> (Batik 绘制)
     */
    private static class TextReplacementPageDrawer extends PageDrawer {

        private final StringBuilder textBuffer = new StringBuilder();
        private Matrix startMatrix = null;
        private PDFont currentFont = null;
        private float lastEndX = -1;

        public TextReplacementPageDrawer(PageDrawerParameters parameters) throws IOException {
            super(parameters);
        }

        @Override
        protected void showGlyph(Matrix textRenderingMatrix, PDFont font, int code, Vector displacement) throws IOException {
            // 1. 【核心修改】不再调用 super.showGlyph
            // 这样 PDFBox 就不会画出不可选中的 Path 轮廓了
            // super.showGlyph(textRenderingMatrix, font, code, displacement); <-- 删掉这行

            // 2. 获取内容
            String unicode = null;
            try { unicode = font.toUnicode(code); } catch (Exception ignored) {}

            // 如果是空内容或者获取不到字符 (比如特殊的 MathJax 绘图符号)，
            // 这时候还是得调用父类画出来，否则公式里的特殊符号会消失
            if (unicode == null || unicode.isEmpty()) {
                flushBuffer();
                super.showGlyph(textRenderingMatrix, font, code, displacement);
                return;
            }

            // 3. 缓冲合并逻辑 (同前)
            float x = textRenderingMatrix.getTranslateX();
            float y = textRenderingMatrix.getTranslateY();
            float fontSize = textRenderingMatrix.getScalingFactorY();
            float charWidth = (font.getWidth(code) / 1000f) * fontSize;

            boolean shouldFlush = false;

            if (textBuffer.length() > 0 && startMatrix != null) {
                if (Math.abs(y - startMatrix.getTranslateY()) > 1.0f) {
                    shouldFlush = true;
                } else if (!font.equals(currentFont)) {
                    shouldFlush = true;
                } else {
                    float gap = Math.abs(x - lastEndX);
                    if (gap > (fontSize * 0.3f) || x < lastEndX) {
                        if (x > lastEndX) textBuffer.append(" ");
                        shouldFlush = true;
                    }
                }
            } else {
                startMatrix = textRenderingMatrix.clone();
                currentFont = font;
            }

            if (shouldFlush) {
                flushBuffer();
                startMatrix = textRenderingMatrix.clone();
                currentFont = font;
            }

            textBuffer.append(unicode);
            lastEndX = x + charWidth;
        }

        private void flushBuffer() {
            if (textBuffer.length() == 0 || startMatrix == null) return;

            try {
                Graphics2D g2d = getGraphics();
                AffineTransform saveAT = g2d.getTransform();
                AffineTransform at = startMatrix.createAffineTransform();
                g2d.transform(at);

                // 【核心修改】使用真实颜色 (可见)
                Color color = getAwtColor(getGraphicsState().getNonStrokingColor());
                g2d.setColor(color);

                // 设置字体
                String fontName = "SansSerif";
                if (currentFont != null && currentFont.getName() != null) {
                    fontName = currentFont.getName();
                    if (fontName.contains("+")) fontName = fontName.substring(fontName.indexOf("+")+1);
                }
                // 字号 1.0 (缩放由 Matrix 控制)
                g2d.setFont(new Font(fontName, Font.PLAIN, 1));

                // 绘制可见的 <text>
                g2d.drawString(textBuffer.toString(), 0, 0);

                g2d.setTransform(saveAT);

            } catch (Exception e) {
                // ignore
            } finally {
                textBuffer.setLength(0);
                startMatrix = null;
            }
        }

        private Color getAwtColor(org.apache.pdfbox.pdmodel.graphics.color.PDColor pdColor) {
            try {
                float[] rgb = pdColor.getColorSpace().toRGB(pdColor.getComponents());
                float r = Math.max(0, Math.min(1, rgb[0]));
                float g = Math.max(0, Math.min(1, rgb[1]));
                float b = Math.max(0, Math.min(1, rgb[2]));
                float alpha = (float) getGraphicsState().getNonStrokeAlphaConstant();
                return new Color(r, g, b, alpha);
            } catch (Exception e) { return Color.BLACK; }
        }

        // --- 拦截器 ---

        // 遇到绘图指令 (表格/公式)，必须先清空文字缓冲区
        // 然后调用 super 让 PDFBox 帮我们画图 (Path)
        @Override public void strokePath() throws IOException { flushBuffer(); super.strokePath(); }
        @Override public void fillPath(int windingRule) throws IOException { flushBuffer(); super.fillPath(windingRule); }
        @Override public void drawImage(PDImage pdImage) throws IOException { flushBuffer(); super.drawImage(pdImage); }

        @Override
        protected void processOperator(Operator operator, List<COSBase> operands) throws IOException {
            String op = operator.getName();
            if ("ET".equals(op) || "Td".equals(op) || "TD".equals(op) || "T*".equals(op)) {
                flushBuffer();
            }
            super.processOperator(operator, operands);
        }

        @Override public void showAnnotation(PDAnnotation annotation) throws IOException {}
    }
}