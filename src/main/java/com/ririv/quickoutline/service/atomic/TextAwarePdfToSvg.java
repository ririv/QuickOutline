package com.ririv.quickoutline.service.atomic;

import org.apache.pdfbox.contentstream.PDFGraphicsStreamEngine;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.graphics.image.PDImage;
// 显式导入，防止歧义
import org.apache.pdfbox.util.Matrix;
import org.apache.pdfbox.util.Vector;

import java.awt.geom.Point2D;
import java.io.IOException;

public class TextAwarePdfToSvg extends PDFGraphicsStreamEngine {

    private final StringBuilder svgContent = new StringBuilder();
    private final StringBuilder currentPath = new StringBuilder();

    public TextAwarePdfToSvg(PDPage page) {
        super(page);
        PDRectangle cropBox = page.getCropBox();
        float width = cropBox.getWidth();
        float height = cropBox.getHeight();

        // 初始化 SVG，翻转坐标系
        svgContent.append(String.format(
                "<svg xmlns='http://www.w3.org/2000/svg' version='1.1' " +
                        "width='%.2fpt' height='%.2fpt' viewBox='0 0 %.2f %.2f'>\n" +
                        "<g transform='translate(0, %.2f) scale(1, -1)'>\n",
                width, height, width, height, height
        ));
    }

    public String getSvgResult() {
        return svgContent.toString() + "</g></svg>";
    }

    /**
     * 驱动引擎处理页面
     */
    public void process() throws IOException {
        processPage(getPage());
    }

    // ============================================================
    // 1. 文字处理 (适配 PDFBox 3.x)
    // ============================================================

    @Override
    protected void showGlyph(Matrix textRenderingMatrix, PDFont font, int code, Vector displacement) throws IOException {
        // 1. 调用 super (虽然父类可能没做事，但保持习惯)
        super.showGlyph(textRenderingMatrix, font, code, displacement);

        // 2. 【关键修正】手动获取 Unicode
        // PDFBox 3.x 移除了参数里的 unicode，需要通过 font.toUnicode(code) 获取
        String unicode = font.toUnicode(code);

        if (unicode == null || unicode.isEmpty()) return;

        // 3. 获取坐标和属性
        float x = textRenderingMatrix.getTranslateX();
        float y = textRenderingMatrix.getTranslateY();
        float fontSize = textRenderingMatrix.getScalingFactorY();

        String fontName = font.getName();
        if (fontName != null && fontName.contains("+")) {
            fontName = fontName.substring(fontName.indexOf("+") + 1);
        }

        // 4. 生成 SVG
        svgContent.append(String.format(
                "<text x='%.2f' y='%.2f' transform='scale(1, -1)' " +
                        "font-family='%s, sans-serif' font-size='%.2f' fill='black'>%s</text>\n",
                x, -y,
                fontName,
                fontSize,
                escapeXml(unicode)
        ));
    }

    // ============================================================
    // 2. 路径绘制
    // ============================================================

    @Override
    public void moveTo(float x, float y) throws IOException {
        currentPath.append(String.format("M %.2f %.2f ", x, y));
    }

    @Override
    public void lineTo(float x, float y) throws IOException {
        currentPath.append(String.format("L %.2f %.2f ", x, y));
    }

    @Override
    public void strokePath() throws IOException {
        if (currentPath.length() > 0) {
            float lineWidth = getGraphicsState().getLineWidth();
            svgContent.append(String.format(
                    "<path d='%s' stroke='black' stroke-width='%.2f' fill='none'/>\n",
                    currentPath.toString(), lineWidth
            ));
            currentPath.setLength(0);
        }
    }

    @Override
    public void fillPath(int windingRule) throws IOException {
        if (currentPath.length() > 0) {
            svgContent.append(String.format(
                    "<path d='%s' fill='black' stroke='none'/>\n", currentPath.toString()
            ));
            currentPath.setLength(0);
        }
    }

    @Override
    public void fillAndStrokePath(int windingRule) throws IOException {
        if (currentPath.length() > 0) {
            float lineWidth = getGraphicsState().getLineWidth();
            svgContent.append(String.format(
                    "<path d='%s' fill='black' stroke='black' stroke-width='%.2f'/>\n",
                    currentPath.toString(), lineWidth
            ));
            currentPath.setLength(0);
        }
    }

    // ============================================================
    // 3. 其他必须实现的抽象方法
    // ============================================================

    @Override public void appendRectangle(Point2D p0, Point2D p1, Point2D p2, Point2D p3) throws IOException {
        moveTo((float)p0.getX(), (float)p0.getY());
        lineTo((float)p1.getX(), (float)p1.getY());
        lineTo((float)p2.getX(), (float)p2.getY());
        lineTo((float)p3.getX(), (float)p3.getY());
    }
    @Override public void drawImage(PDImage pdImage) throws IOException { }
    @Override public void clip(int windingRule) throws IOException { }
    @Override public void curveTo(float x1, float y1, float x2, float y2, float x3, float y3) throws IOException { }
    @Override public Point2D getCurrentPoint() throws IOException { return new Point2D.Float(0,0); }
    @Override public void closePath() throws IOException { currentPath.append("Z "); }
    @Override public void endPath() throws IOException { currentPath.setLength(0); }
    @Override public void shadingFill(COSName shadingName) throws IOException { }

    // XML 转义
    private String escapeXml(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}