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
import java.awt.font.FontRenderContext;
import java.awt.font.TextAttribute;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.io.StringWriter;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
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
        // ... (diffPdfToSvg 和 convertPageToSvg 逻辑保持不变，复制之前的即可) ...
        // 为节省篇幅，此处省略 diffPdfToSvg 和 convertPageToSvg 的外层代码
        // 请确保 convertPageToSvg 里调用的是 CustomPDFRenderer
        List<SvgPageUpdate> updates = new ArrayList<>();
        if (pdfBytes == null || pdfBytes.length == 0) return updates;

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            int currentTotalPages = document.getNumberOfPages();
            CustomPDFRenderer renderer = new CustomPDFRenderer(document);

            for (int i = 0; i < currentTotalPages; i++) {
                PDPage page = document.getPage(i);
                PDRectangle cropBox = page.getCropBox();
                int rotation = page.getRotation();
                float displayWidth = (rotation==90||rotation==270) ? cropBox.getHeight() : cropBox.getWidth();
                float displayHeight = (rotation==90||rotation==270) ? cropBox.getWidth() : cropBox.getHeight();

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
                    updates.add(new SvgPageUpdate(0, pageCache.get(0), currentTotalPages, p0.getCropBox().getWidth(), p0.getCropBox().getHeight()));
                }
            }
            lastTotalPages = currentTotalPages;
        } catch (Exception e) { log.error("Err", e); }
        return updates;
    }

    private String convertPageToSvg(PDFRenderer renderer, int pageIndex, float width, float height) throws IOException {
        DOMImplementation domImpl = GenericDOMImplementation.getDOMImplementation();
        Document svgDoc = domImpl.createDocument("http://www.w3.org/2000/svg", "svg", null);
        SVGGraphics2D svgGenerator = new SVGGraphics2D(svgDoc);
        svgGenerator.setSVGCanvasSize(new Dimension((int) width, (int) height));
        renderer.renderPageToGraphics(pageIndex, svgGenerator, 1.0f);
        try (StringWriter writer = new StringWriter()) {
            svgGenerator.stream(writer, true);
            String rawSvg = writer.toString();
            int start = rawSvg.indexOf(">", rawSvg.indexOf("<svg")) + 1;
            int end = rawSvg.lastIndexOf("</svg>");
            if (start > 0 && end > start) {
                String content = rawSvg.substring(start, end);
                // 注入 pointer-events 控制
                String header = String.format("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 %s %s\" width=\"100%%\" height=\"100%%\" style=\"display:block; overflow:hidden;\">", fmt(width), fmt(height));
                return header + content + "</svg>";
            }
            return rawSvg;
        }
    }
    private String fmt(float d) { if (d == (long) d) return String.format("%d", (long) d); return String.format("%s", d); }


    // =================================================================
    // 核心修复：TextReplacementPageDrawer
    // =================================================================

    private static class CustomPDFRenderer extends PDFRenderer {
        public CustomPDFRenderer(PDDocument document) { super(document); }
        @Override protected PageDrawer createPageDrawer(PageDrawerParameters p) throws IOException {
            PDPage page = p.getPage();
            float h = page.getCropBox().getHeight();
            if(page.getRotation()==90||page.getRotation()==270) h=page.getCropBox().getWidth();
            return new TextReplacementPageDrawer(p, h);
        }
    }

    private static class TextReplacementPageDrawer extends PageDrawer {

        private final float pageHeight;
        private final StringBuilder textBuffer = new StringBuilder();
        private Matrix startMatrix = null;
        private PDFont currentFont = null;
        private float lastEndX = -1;
        // 累计当前 buffer 在 PDF 里的理论宽度
        private float currentBufferWidth = 0;

        public TextReplacementPageDrawer(PageDrawerParameters parameters, float pageHeight) throws IOException {
            super(parameters);
            this.pageHeight = pageHeight;
        }

        @Override
        protected void showGlyph(Matrix textRenderingMatrix, PDFont font, int code, Vector displacement) throws IOException {
            String unicode = null;
            try { unicode = font.toUnicode(code); } catch (Exception ignored) {}

            // 1. 特殊符号 -> 走 Path (保证公式正确)
            if (unicode == null || unicode.trim().isEmpty()) {
                flushBuffer();
                super.showGlyph(textRenderingMatrix, font, code, displacement);
                return;
            }

            // 2. 计算参数
            float x = textRenderingMatrix.getTranslateX();
            float y = textRenderingMatrix.getTranslateY();
            float fontSize = textRenderingMatrix.getScalingFactorY();
            // 字符宽度 (PDF单位)
            float charWidth = (font.getWidth(code) / 1000f) * fontSize;

            // 3. 合并逻辑
            boolean shouldFlush = false;
            if (textBuffer.length() > 0 && startMatrix != null) {
                if (Math.abs(y - startMatrix.getTranslateY()) > 1.0f) shouldFlush = true;
                else if (!font.equals(currentFont)) shouldFlush = true;
                else {
                    float gap = Math.abs(x - lastEndX);
                    // 容差加大一点，避免频繁断开
                    if (gap > (fontSize * 0.4f) || x < lastEndX) {
                        if (x > lastEndX) {
                            textBuffer.append(" ");
                            // 空格也要算宽度吗？通常 PDF 空格宽度在 Gap 里体现了，这里不用累加
                        }
                        shouldFlush = true;
                    }
                }
            } else {
                startMatrix = textRenderingMatrix.clone();
                currentFont = font;
                currentBufferWidth = 0;
            }

            if (shouldFlush) {
                flushBuffer();
                startMatrix = textRenderingMatrix.clone();
                currentFont = font;
                currentBufferWidth = 0;
            }

            textBuffer.append(unicode);
            lastEndX = x + charWidth;
            currentBufferWidth += charWidth;
        }

        private void flushBuffer() {
            if (textBuffer.length() == 0 || startMatrix == null) return;

            try {
                Graphics2D g2d = getGraphics();
                AffineTransform saveAT = g2d.getTransform();

                // 1. 坐标重置
                g2d.setTransform(new AffineTransform());
                float svgX = startMatrix.getTranslateX();
                float svgY = pageHeight - startMatrix.getTranslateY();

                // 2. 设置颜色 (不透明)
                Color color = getAwtColor(getGraphicsState().getNonStrokingColor());
                g2d.setColor(color);

                // 3. 设置基础字体 (SansSerif)
                float fontSize = startMatrix.getScalingFactorY();
                Font sysFont = new Font("SansSerif", Font.PLAIN, (int) fontSize);

                // 4. 【核心修复】计算宽度缩放 (Stretch)
                // 目标：把系统字体渲染出的宽度，强行缩放到 PDF 的理论宽度
                // 这解决了"位置偏差"问题，也不需要嵌入字体

                // A. 计算系统字体宽度
                FontRenderContext frc = new FontRenderContext(new AffineTransform(), true, true);
                double sysWidth = sysFont.getStringBounds(textBuffer.toString(), frc).getWidth();

                // B. 计算缩放比例
                double scaleX = 1.0;
                if (sysWidth > 0 && currentBufferWidth > 0) {
                    scaleX = currentBufferWidth / sysWidth;
                }
                // 限制缩放范围，防止极端情况拉伸太丑 (0.8 ~ 1.2)
                // scaleX = Math.max(0.8, Math.min(1.2, scaleX));

                // C. 应用缩放
                // 注意：transform 顺序是后进先出
                // 先移到位置 -> 再缩放
                AffineTransform at = new AffineTransform();
                at.translate(svgX, svgY);
                at.scale(scaleX, 1.0);

                g2d.transform(at);
                g2d.setFont(sysFont);

                // 5. 绘制
                g2d.drawString(textBuffer.toString(), 0, 0);

                g2d.setTransform(saveAT);

            } catch (Exception e) {
                // ignore
            } finally {
                textBuffer.setLength(0);
                startMatrix = null;
                currentBufferWidth = 0;
            }
        }

        // ... Helper methods (getAwtColor) and Interceptors (strokePath...) remain same ...
        private Color getAwtColor(org.apache.pdfbox.pdmodel.graphics.color.PDColor pdColor) {
            try {
                float[] rgb = pdColor.getColorSpace().toRGB(pdColor.getComponents());
                return new Color(Math.max(0,Math.min(1,rgb[0])), Math.max(0,Math.min(1,rgb[1])), Math.max(0,Math.min(1,rgb[2])), (float)getGraphicsState().getNonStrokeAlphaConstant());
            } catch (Exception e) { return Color.BLACK; }
        }
        @Override public void strokePath() throws IOException { flushBuffer(); super.strokePath(); }
        @Override public void fillPath(int windingRule) throws IOException { flushBuffer(); super.fillPath(windingRule); }
        @Override public void drawImage(PDImage pdImage) throws IOException { flushBuffer(); super.drawImage(pdImage); }
        @Override protected void processOperator(Operator operator, List<COSBase> operands) throws IOException {
            String op = operator.getName();
            if ("ET".equals(op)||"Td".equals(op)||"TD".equals(op)||"T*".equals(op)) flushBuffer();
            super.processOperator(operator, operands);
        }
        @Override public void showAnnotation(PDAnnotation annotation) throws IOException {}
    }
}