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
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDFontDescriptor;
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
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.io.StringWriter;
import java.util.*;
import java.util.List;

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

            // 处理页数减少的情况（清理缓存）
            if (currentTotalPages < lastTotalPages) {
                for (int k = currentTotalPages; k < lastTotalPages; k++) pageCache.remove(k);
                // 如果变为空或减少，确保至少返回第一页（如果存在）
                if (updates.isEmpty() && currentTotalPages > 0) {
                    PDPage p0 = document.getPage(0);
                    // 简单处理，重新触发第一页更新以防前端状态不一致
                    // 这里逻辑视具体业务需求而定
                }
            }
            lastTotalPages = currentTotalPages;

        } catch (Exception e) {
            log.error("Error converting PDF to SVG", e);
        }
        return updates;
    }

    private String convertPageToSvg(PDFRenderer renderer, int pageIndex, float width, float height) throws IOException {
        DOMImplementation domImpl = GenericDOMImplementation.getDOMImplementation();
        Document svgDoc = domImpl.createDocument("http://www.w3.org/2000/svg", "svg", null);
        SVGGraphics2D svgGenerator = new SVGGraphics2D(svgDoc);
        svgGenerator.setSVGCanvasSize(new Dimension((int) width, (int) height));

        renderer.renderPageToGraphics(pageIndex, svgGenerator, 1.0f);

        // 获取该页生成的 CSS (字体嵌入)
        String fontCss = "";
        if (renderer instanceof CustomPDFRenderer customRenderer) {
            fontCss = customRenderer.getCurrentPageFontCss();
        }

        try (StringWriter writer = new StringWriter()) {
            svgGenerator.stream(writer, true);
            String rawSvg = writer.toString();

            int svgTagEndIndex = rawSvg.indexOf(">", rawSvg.indexOf("<svg")) + 1;
            int svgCloseTagIndex = rawSvg.lastIndexOf("</svg>");

            if (svgTagEndIndex > 0 && svgCloseTagIndex > svgTagEndIndex) {
                String svgContent = rawSvg.substring(svgTagEndIndex, svgCloseTagIndex);

                String styleBlock = "";
                if (!fontCss.isEmpty()) {
                    styleBlock = "<style>" + fontCss + "</style>";
                }

                // text-rendering: optimizeSpeed;：最关键。告诉浏览器不要计算复杂的字距（Kerning）和连字，追求速度。这在移动端能带来巨大的性能提升。
                // shape-rendering: crispEdges;：关闭反锯齿（如果对画质要求没那么高），可以提速。
                String newHeader = String.format(
                        "<svg xmlns=\"http://www.w3.org/2000/svg\" " +
                                "viewBox=\"0 0 %s %s\" " +
                                "width=\"100%%\" height=\"100%%\" " +
                                "preserveAspectRatio=\"xMidYMid meet\" " +
                                "style=\"display:block; overflow:hidden; " +
                                "text-rendering: optimizeSpeed; shape-rendering: crispEdges; ...\">",
//                                "text-rendering:geometricPrecision;\">",
                        fmt(width), fmt(height));

                return newHeader + styleBlock + svgContent + "</svg>";
            }
            return rawSvg;
        }
    }

    private String fmt(float d) {
        if (d == (long) d) return String.format("%d", (long) d);
        return String.format("%s", d);
    }

    // =================================================================
    // 自定义渲染器逻辑
    // =================================================================

    private static class CustomPDFRenderer extends PDFRenderer {
        private TextReplacementPageDrawer currentDrawer;

        public CustomPDFRenderer(PDDocument document) {
            super(document);
        }

        @Override
        protected PageDrawer createPageDrawer(PageDrawerParameters parameters) throws IOException {
            PDPage page = parameters.getPage();
            float pageHeight = page.getCropBox().getHeight();
            if (page.getRotation() == 90 || page.getRotation() == 270) {
                pageHeight = page.getCropBox().getWidth();
            }
            this.currentDrawer = new TextReplacementPageDrawer(parameters, pageHeight);
            return this.currentDrawer;
        }

        public String getCurrentPageFontCss() {
            return currentDrawer != null ? currentDrawer.getGeneratedCss() : "";
        }
    }

    private static class TextReplacementPageDrawer extends PageDrawer {
        private final float pageHeight;
        private final StringBuilder textBuffer = new StringBuilder();
        private Matrix startMatrix = null;
        private PDFont currentFont = null;
        private float lastEndX = -1;
        private float currentBufferWidth = 0; // 用于计算拉伸

        private final Map<String, String> fontMapping = new LinkedHashMap<>();
        private final StringBuilder cssBuffer = new StringBuilder();

        public TextReplacementPageDrawer(PageDrawerParameters parameters, float pageHeight) throws IOException {
            super(parameters);
            this.pageHeight = pageHeight;
        }

        public String getGeneratedCss() {
            return cssBuffer.toString();
        }

        /**
         * 将 PDF 字体注册为 CSS @font-face，并返回对应的 font-family 名称
         */
        private String registerFont(PDFont font) {
            String pdfFontName = font.getName();
            if (pdfFontName == null) return "sans-serif";

            // 如果已经注册过，直接返回
            if (fontMapping.containsKey(pdfFontName)) return fontMapping.get(pdfFontName);

            String cssFontFamily = "f_" + Math.abs(pdfFontName.hashCode()) + "_" + fontMapping.size();
            try {
                PDFontDescriptor descriptor = font.getFontDescriptor();
                if (descriptor != null) {
                    PDStream fontFile = descriptor.getFontFile2(); // TrueType
                    if (fontFile == null) fontFile = descriptor.getFontFile3(); // Type1/CFF

                    if (fontFile != null) {
                        byte[] fontBytes = fontFile.toByteArray();
                        String base64 = Base64.getEncoder().encodeToString(fontBytes);

                        // 生成 CSS。注意：这里简单认定为 ttf，实际可能需要更细致的 MIME 类型判断
                        cssBuffer.append(String.format(
                                "@font-face { font-family: '%s'; src: url(data:application/x-font-ttf;charset=utf-8;base64,%s); } ",
                                cssFontFamily, base64
                        ));
                        fontMapping.put(pdfFontName, cssFontFamily);
                        return cssFontFamily;
                    }
                }
            } catch (Exception ignored) {
                // 字体提取失败，降级处理
            }

            // 提取失败或无嵌入文件，使用默认
            fontMapping.put(pdfFontName, "sans-serif");
            return "sans-serif";
        }

        @Override
        protected void showGlyph(Matrix textRenderingMatrix, PDFont font, int code, Vector displacement) throws IOException {
            String unicode = null;
            try { unicode = font.toUnicode(code); } catch (Exception ignored) {}

            // 【修复点 1】：去掉 .trim()
            // 只过滤掉 null 或者真正的空字符串。保留 " " (空格) 以便后续逻辑处理。
            if (unicode == null || unicode.isEmpty()) {
                flushBuffer();
                super.showGlyph(textRenderingMatrix, font, code, displacement);
                return;
            }

            // 2. 准备参数
            float x = textRenderingMatrix.getTranslateX();
            float y = textRenderingMatrix.getTranslateY();
            float fontSize = textRenderingMatrix.getScalingFactorY();
            float charWidth = (font.getWidth(code) / 1000f) * fontSize;

            boolean shouldFlush = false;

            // 【修复点 2】：增强空格判断
            // 有些 PDF 的空格可能是 \u00A0 (NBSP)，所以用正则或单纯判断是否空白字符更稳健
            boolean isSpace = unicode.equals(" ") || unicode.matches("\\s+");

            if (!textBuffer.isEmpty() && startMatrix != null) {
                // A. 换行
                if (Math.abs(y - startMatrix.getTranslateY()) > 1.0f) {
                    shouldFlush = true;
                }
                // B. 字体变了
                else if (!font.equals(currentFont)) {
                    shouldFlush = true;
                }
                // C. 遇到空格 -> 强制断开
                else if (isSpace) {
                    shouldFlush = true;
                }
                // D. 间距异常
                else {
                    float gap = Math.abs(x - lastEndX);
                    if (gap > (fontSize * 0.3f) || x < lastEndX) {
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

            // 空格不计入 buffer 内容，只触发位置重置
            if (!isSpace) {
                textBuffer.append(unicode);
                currentBufferWidth += charWidth;
            } else {
                // 如果是空格，不添加到 textBuffer，但重置 startMatrix
                // 这样下一个非空格字符会作为新的一段文字 (Text Chunk) 开始，坐标会重新校准
                startMatrix = null;
            }

            lastEndX = x + charWidth;
        }
        private void flushBuffer() {
            if (textBuffer.isEmpty() || startMatrix == null) return;

            try {
                Graphics2D g2d = getGraphics();
                AffineTransform saveAT = g2d.getTransform();
                g2d.setTransform(new AffineTransform());

                float pdfX = startMatrix.getTranslateX();
                float pdfY = startMatrix.getTranslateY();
                float svgX = pdfX;
                float svgY = pageHeight - pdfY;

                // 颜色设置 (来自 Ver 2 的优化)
                Color color = getAwtColor(getGraphicsState().getNonStrokingColor());
                g2d.setColor(color);

                // 字体设置
                float fontSize = startMatrix.getScalingFactorY();
                String cssFamily = registerFont(currentFont); // 获取嵌入字体名
                Font javaFont = new Font(cssFamily, Font.PLAIN, (int) fontSize);
                g2d.setFont(javaFont);

                // 计算拉伸 (Scale X) - 即使有嵌入字体，为了完美对齐，保留微量调整逻辑
                // 但如果用了嵌入字体，通常 scaleX 会非常接近 1.0
                double scaleX = 1.0;
                if (textBuffer.length() > 1 && currentBufferWidth > 0) {
                    // 只有当宽度偏差较大时才拉伸，避免性能损耗
                    // 注意：因为 SVG 渲染引擎和 Java FontRenderContext 这里的计算可能不一致
                    // 这里主要是一个保底策略，防止使用 sans-serif 时的巨大错位
                    FontRenderContext frc = new FontRenderContext(new AffineTransform(), true, true);
                    double sysWidth = javaFont.deriveFont(fontSize).getStringBounds(textBuffer.toString(), frc).getWidth();
                    if (sysWidth > 0) {
                        scaleX = currentBufferWidth / sysWidth;
                    }
                    // 限制拉伸范围，防止极端变形
                    if (scaleX < 0.8 || scaleX > 1.2) scaleX = 1.0;
                }

                AffineTransform at = new AffineTransform();
                at.translate(svgX, svgY);
                if (Math.abs(scaleX - 1.0) > 0.01) {
                    at.scale(scaleX, 1.0);
                }

                g2d.transform(at);
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
        @Override public void strokePath() throws IOException { flushBuffer(); super.strokePath(); }
        @Override public void fillPath(int windingRule) throws IOException { flushBuffer(); super.fillPath(windingRule); }
        @Override public void drawImage(PDImage pdImage) throws IOException { flushBuffer(); super.drawImage(pdImage); }
        @Override protected void processOperator(Operator operator, List<COSBase> operands) throws IOException {
            String op = operator.getName();
            // 常见的文本结束/移动操作符
            if ("ET".equals(op)||"Td".equals(op)||"TD".equals(op)||"T*".equals(op)||"Tm".equals(op)) flushBuffer();
            super.processOperator(operator, operands);
        }
        @Override public void showAnnotation(PDAnnotation annotation) throws IOException {}

        // 【关键修复】不要重写 moveTo/lineTo 等方法为空！
        // 保持父类的默认实现，这样表格线和背景才能画出来。
    }
}