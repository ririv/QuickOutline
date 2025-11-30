package com.ririv.quickoutline.service.pdfpreview.strategy;

import org.jfree.svg.SVGGraphics2D; // 新版包名
import org.jfree.svg.SVGUnits;

import java.awt.*;

// https://github.com/jfree/jfreesvg
// LICENSE: GPL-3.0
public class JFreeSvgStrategy implements SvgGenerator {

    private final SVGGraphics2D svgGraphics2D;

    public JFreeSvgStrategy(float width, float height) {
        // JFreeSVG 初始化极简
        this.svgGraphics2D = new SVGGraphics2D(width, height, SVGUnits.PX);

        // org.jfree.svg.SVGGraphics2D 成员变量写了 private Color background = Color.BLACK;
        // clearRect
        //
        // 告诉 JFreeSVG：你的背景色是白色
        // 一般pdf页面背景是白色的，除非特殊情况
//        this.svgGraphics2D.setBackground(Color.WHITE);

        // 也这里模仿了 Batik 的做法，设置为黑色全透明背景
        // 但可能出现PDF字是黑的，背景是透明的，而用户又开了深色模式时，文字就看不见了
        // 当然目前我们在前端设了纸张背景为白色，所以目前没有担心的必要了
        this.svgGraphics2D.setBackground(new Color(0, 0, 0, 0));



    }

    @Override
    public Graphics2D getGraphics2D() {
        return this.svgGraphics2D;
    }

    @Override
    public String getRawSvgString() {
        return this.svgGraphics2D.getSVGElement();
    }
}
