package com.ririv.quickoutline.service.pdfpreview.strategy;

import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.io.StringWriter;
import java.io.Writer;

public class BatikSvgStrategy implements SvgGenerator {

    private final SVGGraphics2D svgGraphics2D;

    public BatikSvgStrategy(float width, float height) {
        // Batik 初始化需要 DOM
        DOMImplementation domImpl = GenericDOMImplementation.getDOMImplementation();
        Document svgDoc = domImpl.createDocument("http://www.w3.org/2000/svg", "svg", null);
        this.svgGraphics2D = new SVGGraphics2D(svgDoc);
        this.svgGraphics2D.setSVGCanvasSize(new Dimension((int) width, (int) height));

        // 在 setGeneratorContext 调用了 gc.setBackground(gcDefaults.getBackground());
        // 而在 GraphicContext 设置了 protected Color background = new Color(0, 0, 0, 0);
        // 所以默认背景为黑色全透明
    }

    @Override
    public Graphics2D getGraphics2D() {
        return this.svgGraphics2D;
    }

    @Override
    public String getRawSvgString() {
        try (Writer writer = new StringWriter()) {
            svgGraphics2D.stream(writer, true);
            return writer.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate SVG with Batik", e);
        }
    }
}