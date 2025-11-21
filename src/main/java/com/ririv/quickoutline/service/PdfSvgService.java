package com.ririv.quickoutline.service;

import jakarta.inject.Singleton;
import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.contentstream.PDFGraphicsStreamEngine;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.graphics.image.PDImage;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.util.Matrix;
import org.apache.pdfbox.util.Vector; // 显式导入，防止歧义
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

import java.awt.Dimension;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.StringWriter;

@Singleton
public class PdfSvgService {
    private static final Logger log = LoggerFactory.getLogger(PdfSvgService.class);

    // 原子块转换结果：SVG源码 + 真实高度
    public record BlockSvgResult(String svg, float heightPt) {}

    /**
     * 场景 A: 单个原子块转换
     * 输入 PDF 字节流，输出 SVG 和高度。
     */
    public BlockSvgResult convertSingleBlockToSvg(byte[] pdfBytes) {
        if (pdfBytes == null || pdfBytes.length == 0) {
            return new BlockSvgResult("", 0);
        }

        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            if (doc.getNumberOfPages() == 0) return new BlockSvgResult("", 0);

            // 对于单块，通常只有一页（除非无限高被切分，但这里假设是一页）
            return convertPageInPdfToSvg(doc, 0);

        } catch (Exception e) {
            log.error("Failed to convert block PDF to SVG", e);
            return new BlockSvgResult("", 0);
        }
    }

    /**
     * 【新增】场景 B: 多页块转换中的某一页
     * 输入已打开的 Document 和页码，输出 SVG 和高度。
     * 用于 AtomicBlockService 处理被 iText 切分的长内容。
     */
    public BlockSvgResult convertPageInPdfToSvg(PDDocument doc, int pageIndex) {
        try {
            PDPage page = doc.getPage(pageIndex);
            PDRectangle mediaBox = page.getMediaBox();
            float width = mediaBox.getWidth();
            float totalPageHeight = mediaBox.getHeight();

            // 1. 计算真实内容高度
            // 即使是切分后的页面，如果是最后一页，内容可能不满，需要计算
            float contentHeight = calculateContentHeight(page);

            // 2. 渲染 SVG
            String svg = renderPageToSvgString(doc, pageIndex, width, totalPageHeight);

            return new BlockSvgResult(svg, contentHeight);

        } catch (Exception e) {
            log.error("Failed to convert page " + pageIndex, e);
            return new BlockSvgResult("", 0);
        }
    }

    // =================================================================
    // 私有辅助方法
    // =================================================================

    private String renderPageToSvgString(PDDocument doc, int pageIndex, float width, float height) throws Exception {
        // 初始化 Batik
        DOMImplementation domImpl = GenericDOMImplementation.getDOMImplementation();
        String svgNS = "http://www.w3.org/2000/svg";
        Document svgDoc = domImpl.createDocument(svgNS, "svg", null);

        SVGGraphics2D svgGenerator = new SVGGraphics2D(svgDoc);
        svgGenerator.setSVGCanvasSize(new Dimension((int) width, (int) height));

        // 使用 PDFBox 渲染到 Batik
        PDFRenderer renderer = new PDFRenderer(doc);
        renderer.renderPageToGraphics(pageIndex, svgGenerator, 1.0f);

        try (StringWriter writer = new StringWriter()) {
            svgGenerator.stream(writer, true);
            return writer.toString();
        }
    }

    private float calculateContentHeight(PDPage page) throws IOException {
        ContentBoundsFinder finder = new ContentBoundsFinder(page);
        finder.processPage(page);

        float minY = finder.getMinY();
        float pageHeight = page.getMediaBox().getHeight();

        if (minY == Float.MAX_VALUE) return 0;

        // 计算高度：页面顶端 - 内容最低点 + 5pt Padding
        float height = pageHeight - minY + 5f;
        return Math.max(0, height);
    }

    /**
     * 内部类：用于扫描 PDF 指令流，找到内容绘制的最低 Y 坐标
     * (包含 CTM 坐标修正)
     */
    private static class ContentBoundsFinder extends PDFGraphicsStreamEngine {
        private float minY = Float.MAX_VALUE;
        private final GeneralPath currentPath = new GeneralPath();

        protected ContentBoundsFinder(PDPage page) {
            super(page);
        }

        public float getMinY() {
            return minY;
        }

        private void updateBounds(float y) {
            if (y < minY) minY = y;
        }

        private void updateBounds(Rectangle2D rect) {
            if (rect != null) updateBounds((float) rect.getMinY());
        }

        // --- 1. 路径构建 (应用 CTM) ---

        private void addToPath(Point2D p, boolean isMove) {
            Point2D.Float tp = transformedPoint((float)p.getX(), (float)p.getY());
            if (isMove) currentPath.moveTo(tp.x, tp.y);
            else currentPath.lineTo(tp.x, tp.y);
        }

        @Override
        public void appendRectangle(Point2D p0, Point2D p1, Point2D p2, Point2D p3) throws IOException {
            addToPath(p0, true);
            addToPath(p1, false);
            addToPath(p2, false);
            addToPath(p3, false);
            currentPath.closePath();
        }

        @Override
        public void moveTo(float x, float y) throws IOException {
            Point2D.Float p = transformedPoint(x, y);
            currentPath.moveTo(p.x, p.y);
        }

        @Override
        public void lineTo(float x, float y) throws IOException {
            Point2D.Float p = transformedPoint(x, y);
            currentPath.lineTo(p.x, p.y);
        }

        @Override
        public void curveTo(float x1, float y1, float x2, float y2, float x3, float y3) throws IOException {
            Point2D.Float p1 = transformedPoint(x1, y1);
            Point2D.Float p2 = transformedPoint(x2, y2);
            Point2D.Float p3 = transformedPoint(x3, y3);
            currentPath.curveTo(p1.x, p1.y, p2.x, p2.y, p3.x, p3.y);
        }

        @Override public Point2D getCurrentPoint() throws IOException { return currentPath.getCurrentPoint(); }
        @Override public void closePath() throws IOException { currentPath.closePath(); }
        @Override public void endPath() throws IOException { currentPath.reset(); }

        // --- 2. 动作触发 ---

        @Override
        public void strokePath() throws IOException {
            updateBounds(currentPath.getBounds2D());
            currentPath.reset();
        }
        @Override
        public void fillPath(int windingRule) throws IOException {
            updateBounds(currentPath.getBounds2D());
            currentPath.reset();
        }
        @Override
        public void fillAndStrokePath(int windingRule) throws IOException {
            updateBounds(currentPath.getBounds2D());
            currentPath.reset();
        }

        // --- 3. 其他对象 ---

        @Override
        public void drawImage(PDImage pdImage) throws IOException {
            Matrix ctm = getGraphicsState().getCurrentTransformationMatrix();
            updateBounds(ctm.getTranslateY());
        }

        @Override
        protected void showGlyph(Matrix textRenderingMatrix, PDFont font, int code, Vector displacement) throws IOException {
            super.showGlyph(textRenderingMatrix, font, code, displacement);
            float y = textRenderingMatrix.getTranslateY();
            float fontSize = textRenderingMatrix.getScalingFactorY();
            updateBounds(y - (fontSize * 0.3f));
        }

        @Override public void clip(int windingRule) throws IOException {}
        @Override public void shadingFill(COSName shadingName) throws IOException {}
    }
}