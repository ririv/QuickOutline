package com.ririv.quickoutline.service;

import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.pdfbox.contentstream.PDFGraphicsStreamEngine;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.image.PDImage;
// 显式导入
import org.apache.pdfbox.util.Matrix;
import org.apache.pdfbox.util.Vector;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.io.StringWriter;

public class TextAwarePdfToSvg extends PDFGraphicsStreamEngine {

    private final SVGGraphics2D svgGenerator;
    // 使用 Java AWT 的 Path 对象来收集路径，交给 Batik 处理复杂填充
    private final GeneralPath currentPath = new GeneralPath();

    // 文本合并缓冲区
    private final StringBuilder textBuffer = new StringBuilder();
    private float bufferX = 0, bufferY = 0;
    private String bufferFontName = "";
    private float bufferFontSize = 0;
    private Color bufferColor = Color.BLACK;

    public TextAwarePdfToSvg(PDPage page) {
        super(page);
        PDRectangle cropBox = page.getCropBox();
        float width = cropBox.getWidth();
        float height = cropBox.getHeight();

        // 1. 初始化 Batik
        DOMImplementation domImpl = GenericDOMImplementation.getDOMImplementation();
        Document svgDoc = domImpl.createDocument("http://www.w3.org/2000/svg", "svg", null);
        this.svgGenerator = new SVGGraphics2D(svgDoc);

        // 2. 设置画布大小
        svgGenerator.setSVGCanvasSize(new Dimension((int) width, (int) height));

        // 3. 【关键】全局坐标系设置
        // PDF 原点在左下 (Y向上)，SVG 原点在左上 (Y向下)。
        // 我们让 Batik 全局翻转，这样我们就可以直接传入 PDF 的原始坐标。
        // Batik 会自动处理 Path 的翻转，保证公式形状正确。
        svgGenerator.translate(0, height);
        svgGenerator.scale(1, -1);
    }

    public String getSvgResult() {
        flushTextBuffer();
        try (StringWriter writer = new StringWriter()) {
            svgGenerator.stream(writer, true);
            return writer.toString();
        } catch (Exception e) {
            return "<svg><text>Error</text></svg>";
        }
    }

    // =================================================================
    // 1. 路径 (公式/表格) -> 委托给 Batik 处理
    // =================================================================
    // 我们不需要自己拼接 "M x y L x y"，而是构建 AWT GeneralPath，
    // 这样 Batik 会帮我们处理 Winding Rule (非零环绕)，修复公式显示问题。

    // 辅助：应用 CTM (当前变换矩阵)
    private Point2D transform(float x, float y) {
        return getGraphicsState().getCurrentTransformationMatrix().transformPoint(x, y);
    }

    @Override
    public void moveTo(float x, float y) throws IOException {
        flushTextBuffer();
        Point2D p = transform(x, y);
        currentPath.moveTo(p.getX(), p.getY());
    }

    @Override
    public void lineTo(float x, float y) throws IOException {
        Point2D p = transform(x, y);
        currentPath.lineTo(p.getX(), p.getY());
    }

    @Override
    public void curveTo(float x1, float y1, float x2, float y2, float x3, float y3) throws IOException {
        Point2D p1 = transform(x1, y1);
        Point2D p2 = transform(x2, y2);
        Point2D p3 = transform(x3, y3);
        currentPath.curveTo(p1.getX(), p1.getY(), p2.getX(), p2.getY(), p3.getX(), p3.getY());
    }

    @Override
    public void appendRectangle(Point2D p0, Point2D p1, Point2D p2, Point2D p3) throws IOException {
        flushTextBuffer();
        Point2D t0 = transform((float)p0.getX(), (float)p0.getY());
        Point2D t1 = transform((float)p1.getX(), (float)p1.getY());
        Point2D t2 = transform((float)p2.getX(), (float)p2.getY());
        // AWT Path 手动闭合矩形
        currentPath.moveTo(t0.getX(), t0.getY());
        currentPath.lineTo(t1.getX(), t1.getY());
        currentPath.lineTo(t2.getX(), t2.getY());
        currentPath.closePath();
    }

    @Override
    public void closePath() throws IOException {
        currentPath.closePath();
    }

    @Override
    public void endPath() throws IOException {
        currentPath.reset();
    }

    // --- 绘制指令：把 Path 交给 Batik ---

    @Override
    public void strokePath() throws IOException {
        flushTextBuffer();

        // 设置颜色
        svgGenerator.setPaint(getAwtColor(getGraphicsState().getStrokingColor()));

        // 设置线宽 (根据 CTM X轴缩放)
        float scale = getGraphicsState().getCurrentTransformationMatrix().getScalingFactorX();
        svgGenerator.setStroke(new BasicStroke(getGraphicsState().getLineWidth() * scale));

        // 【关键】Batik 画图！它会自动生成 <path d="...">
        svgGenerator.draw(currentPath);
        currentPath.reset();
    }

    @Override
    public void fillPath(int windingRule) throws IOException {
        flushTextBuffer();

        svgGenerator.setPaint(getAwtColor(getGraphicsState().getNonStrokingColor()));

        // 设置填充规则 (0=NonZero, 1=EvenOdd)
        // AWT GeneralPath 支持这两种规则，Batik 会将其转为 fill-rule
        currentPath.setWindingRule(windingRule == 0 ? GeneralPath.WIND_NON_ZERO : GeneralPath.WIND_EVEN_ODD);

        svgGenerator.fill(currentPath);
        currentPath.reset();
    }

    @Override
    public void fillAndStrokePath(int windingRule) throws IOException {
        flushTextBuffer();

        // 先填
        currentPath.setWindingRule(windingRule == 0 ? GeneralPath.WIND_NON_ZERO : GeneralPath.WIND_EVEN_ODD);
        svgGenerator.setPaint(getAwtColor(getGraphicsState().getNonStrokingColor()));
        svgGenerator.fill(currentPath);

        // 后描
        svgGenerator.setPaint(getAwtColor(getGraphicsState().getStrokingColor()));
        float scale = getGraphicsState().getCurrentTransformationMatrix().getScalingFactorX();
        svgGenerator.setStroke(new BasicStroke(getGraphicsState().getLineWidth() * scale));
        svgGenerator.draw(currentPath);

        currentPath.reset();
    }

    // =================================================================
    // 2. 文字处理 (手动拦截 -> Batik drawString)
    // =================================================================

    @Override
    protected void showGlyph(Matrix textRenderingMatrix, PDFont font, int code, Vector displacement) throws IOException {
        String unicode = font.toUnicode(code);
        if (unicode == null || unicode.isEmpty()) return;

        // 获取 PDF 绝对坐标 (因为全局有翻转，这里的Y是正的，代表向上)
        float x = textRenderingMatrix.getTranslateX();
        float y = textRenderingMatrix.getTranslateY();

        float fontSize = textRenderingMatrix.getScalingFactorY();
        Color color = getAwtColor(getGraphicsState().getNonStrokingColor());

        String fontName = font.getName();
        if (fontName == null) fontName = "SansSerif";
        else if (fontName.contains("+")) fontName = fontName.substring(fontName.indexOf("+") + 1);

        // 合并逻辑
        boolean shouldFlush = false;
        if (textBuffer.length() > 0) {
            if (Math.abs(y - bufferY) > 1.0f) shouldFlush = true;
            else if (!fontName.equals(bufferFontName) || Math.abs(fontSize - bufferFontSize) > 0.1f || !color.equals(bufferColor)) shouldFlush = true;
        } else {
            bufferX = x; bufferY = y; bufferFontName = fontName; bufferFontSize = fontSize; bufferColor = color;
        }

        if (shouldFlush) {
            flushTextBuffer();
            bufferX = x; bufferY = y; bufferFontName = fontName; bufferFontSize = fontSize; bufferColor = color;
        }

        textBuffer.append(unicode);
    }

    private void flushTextBuffer() {
        if (textBuffer.length() == 0) return;

        // 【核心难点解决】文字倒立问题
        // 我们的全局环境是 scale(1, -1) (Y向上)。
        // 但 Batik 的 drawString 是默认从基线向下画的。
        // 如果直接画，文字是正的，但位置是对的吗？

        // 我们需要保存当前变换
        AffineTransform originalTransform = svgGenerator.getTransform();

        // 构建局部变换：
        // 1. 移动到 (x, y)
        svgGenerator.translate(bufferX, bufferY);

        // 2. 原地翻转文字！让它变正
        // 因为全局是翻转的，我们再翻转一次 (-1 * -1 = 1)，文字就正过来了
        svgGenerator.scale(1, -1);

        // 设置属性
        // 注意：字号这里要用正数。
        svgGenerator.setFont(new Font(bufferFontName, Font.PLAIN, (int) Math.abs(bufferFontSize)));
        svgGenerator.setPaint(bufferColor);

        // 绘制 (在 0,0 处绘制，因为已经 translate 过去了)
        svgGenerator.drawString(textBuffer.toString(), 0, 0);

        // 恢复全局状态
        svgGenerator.setTransform(originalTransform);

        textBuffer.setLength(0);
    }

    // --- Helpers ---

    private Color getAwtColor(PDColor pdColor) {
        try {
            float[] c = pdColor.getColorSpace().toRGB(pdColor.getComponents());
            int r = Math.max(0, Math.min(255, Math.round(c[0] * 255)));
            int g = Math.max(0, Math.min(255, Math.round(c[1] * 255)));
            int b = Math.max(0, Math.min(255, Math.round(c[2] * 255)));
            return new Color(r, g, b);
        } catch (Exception e) { return Color.BLACK; }
    }

    @Override public Point2D getCurrentPoint() { return currentPath.getCurrentPoint(); }
    @Override public void clip(int windingRule) {} // 忽略裁剪
    @Override public void drawImage(PDImage pdImage) {} // 暂不处理图片
    @Override public void shadingFill(COSName shadingName) {}
}