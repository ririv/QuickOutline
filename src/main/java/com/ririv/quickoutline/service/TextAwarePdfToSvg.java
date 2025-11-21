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
// 显式导入，防止歧义
import org.apache.pdfbox.util.Matrix;
import org.apache.pdfbox.util.Vector;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.io.StringWriter;

/**
 * 混合模式转换器：
 * 1. 使用 Batik 处理图形 (修复 MathJax 公式和表格)。
 * 2. 使用手动坐标转换 (修复错位和倒立)。
 * 3. 强制输出 Text 标签 (保留文字选中)。
 */

//公式没法正确显示
@Deprecated
public class TextAwarePdfToSvg extends PDFGraphicsStreamEngine {

    private final SVGGraphics2D svgGenerator;
    private final GeneralPath currentPath = new GeneralPath();
    private final float pageHeight;

    public TextAwarePdfToSvg(PDPage page) {
        super(page);

        PDRectangle cropBox = page.getCropBox();
        this.pageHeight = cropBox.getHeight();

        // 1. 初始化 Batik
        DOMImplementation domImpl = GenericDOMImplementation.getDOMImplementation();
        Document svgDoc = domImpl.createDocument("http://www.w3.org/2000/svg", "svg", null);
        this.svgGenerator = new SVGGraphics2D(svgDoc);

        // 2. 设置画布大小
        svgGenerator.setSVGCanvasSize(new Dimension((int) cropBox.getWidth(), (int) cropBox.getHeight()));

        // 【关键】这里不再做任何全局 transform
        // 我们给 Batik 一个干净的、默认的 (左上角原点) 画布
        // 所有的坐标转换都在 Java 层手动完成
    }

    public String getSvgResult() {
        try (StringWriter writer = new StringWriter()) {
            svgGenerator.stream(writer, true);
            return writer.toString();
        } catch (Exception e) {
            return "<svg><text>Error generating SVG</text></svg>";
        }
    }

    // =================================================================
    // 1. 坐标转换核心 (The Truth)
    // =================================================================

    // 将 PDF 坐标 (原点左下) 转换为 SVG 坐标 (原点左上)
    private Point2D toSvg(float x, float y) {
        // 1. 应用 CTM (处理缩放、旋转、位移) -> 得到 PDF 绝对坐标
        Point2D p = getGraphicsState().getCurrentTransformationMatrix().transformPoint(x, y);

        // 2. 手动翻转 Y 轴
        // SVG_Y = PageHeight - PDF_Y
        return new Point2D.Float((float) p.getX(), pageHeight - (float) p.getY());
    }

    // =================================================================
    // 2. 路径构建 (MathJax 公式 & 表格)
    // =================================================================

    @Override
    public void moveTo(float x, float y) throws IOException {
        Point2D p = toSvg(x, y);
        currentPath.moveTo(p.getX(), p.getY());
    }

    @Override
    public void lineTo(float x, float y) throws IOException {
        Point2D p = toSvg(x, y);
        currentPath.lineTo(p.getX(), p.getY());
    }

    @Override
    public void curveTo(float x1, float y1, float x2, float y2, float x3, float y3) throws IOException {
        Point2D p1 = toSvg(x1, y1);
        Point2D p2 = toSvg(x2, y2);
        Point2D p3 = toSvg(x3, y3);
        currentPath.curveTo(p1.getX(), p1.getY(), p2.getX(), p2.getY(), p3.getX(), p3.getY());
    }

    @Override
    public void appendRectangle(Point2D p0, Point2D p1, Point2D p2, Point2D p3) throws IOException {
        Point2D s0 = toSvg((float)p0.getX(), (float)p0.getY());
        Point2D s1 = toSvg((float)p1.getX(), (float)p1.getY());
        Point2D s2 = toSvg((float)p2.getX(), (float)p2.getY());
        // 矩形转为 Path
        currentPath.moveTo(s0.getX(), s0.getY());
        currentPath.lineTo(s1.getX(), s1.getY());
        currentPath.lineTo(s2.getX(), s2.getY());
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

    // =================================================================
    // 3. 绘制操作 (委托给 Batik)
    // =================================================================

    @Override
    public void strokePath() throws IOException {
        // 设置颜色
        svgGenerator.setPaint(getAwtColor(getGraphicsState().getStrokingColor()));

        // 设置线宽 (需要考虑 CTM 的缩放)
        // 简单取 X 轴缩放作为线宽因子
        float scale = getGraphicsState().getCurrentTransformationMatrix().getScalingFactorX();
        svgGenerator.setStroke(new BasicStroke(getGraphicsState().getLineWidth() * scale));

        // 画！
        svgGenerator.draw(currentPath);
        currentPath.reset();
    }

    @Override
    public void fillPath(int windingRule) throws IOException {
        // 设置填充色
        svgGenerator.setPaint(getAwtColor(getGraphicsState().getNonStrokingColor()));

        // 设置填充规则 (NonZero / EvenOdd)
        // 这是 MathJax 显示正确的关键！
        currentPath.setWindingRule(windingRule == 0 ? GeneralPath.WIND_NON_ZERO : GeneralPath.WIND_EVEN_ODD);

        // 填！
        svgGenerator.fill(currentPath);
        currentPath.reset();
    }

    @Override
    public void fillAndStrokePath(int windingRule) throws IOException {
        currentPath.setWindingRule(windingRule == 0 ? GeneralPath.WIND_NON_ZERO : GeneralPath.WIND_EVEN_ODD);

        // 1. Fill
        svgGenerator.setPaint(getAwtColor(getGraphicsState().getNonStrokingColor()));
        svgGenerator.fill(currentPath);

        // 2. Stroke
        svgGenerator.setPaint(getAwtColor(getGraphicsState().getStrokingColor()));
        float scale = getGraphicsState().getCurrentTransformationMatrix().getScalingFactorX();
        svgGenerator.setStroke(new BasicStroke(getGraphicsState().getLineWidth() * scale));
        svgGenerator.draw(currentPath);

        currentPath.reset();
    }

    // =================================================================
    // 4. 文字处理 (保持选中性)
    // =================================================================

    @Override
    protected void showGlyph(Matrix textRenderingMatrix, PDFont font, int code, Vector displacement) throws IOException {
        String unicode = font.toUnicode(code);
        if (unicode == null || unicode.isEmpty()) return;

        // 1. 获取绝对坐标
        float x = textRenderingMatrix.getTranslateX();
        float y = textRenderingMatrix.getTranslateY();

        // 2. 转换为 SVG 坐标
        Point2D p = toSvg(x, y);

        // 3. 获取字号
        float fontSize = textRenderingMatrix.getScalingFactorY();

        // 4. 获取字体名
        String fontName = font.getName();
        if (fontName == null) fontName = "SansSerif";
        else if (fontName.contains("+")) fontName = fontName.substring(fontName.indexOf("+") + 1);

        // 5. 设置绘制属性
        svgGenerator.setPaint(getAwtColor(getGraphicsState().getNonStrokingColor()));

        // 注意：我们只设字体名和大小，不设具体的 Font 对象，
        // 让 Batik 生成 font-family="..." 属性，由浏览器去匹配
        Font awtFont = new Font(fontName, Font.PLAIN, (int) fontSize);
        svgGenerator.setFont(awtFont);

        // 6. 写字！
        // 因为我们已经把坐标转换为了 Top-Left 模式，文字直接画就是正的
        svgGenerator.drawString(unicode, (float)p.getX(), (float)p.getY());
    }

    // --- 辅助方法 ---

    private Color getAwtColor(PDColor pdColor) {
        try {
            float[] rgb = pdColor.getColorSpace().toRGB(pdColor.getComponents());
            // 限制范围 0-255
            int r = Math.max(0, Math.min(255, Math.round(rgb[0] * 255)));
            int g = Math.max(0, Math.min(255, Math.round(rgb[1] * 255)));
            int b = Math.max(0, Math.min(255, Math.round(rgb[2] * 255)));
            return new Color(r, g, b);
        } catch (Exception e) {
            return Color.BLACK; // 默认黑色
        }
    }

    @Override public Point2D getCurrentPoint() { return currentPath.getCurrentPoint(); }
    @Override public void clip(int windingRule) {} // 暂时忽略裁剪
    @Override public void drawImage(PDImage pdImage) {} // 图片暂不处理
    @Override public void shadingFill(COSName shadingName) {}
}