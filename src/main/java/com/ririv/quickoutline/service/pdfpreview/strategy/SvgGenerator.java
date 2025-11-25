package com.ririv.quickoutline.service.pdfpreview.strategy;

import java.awt.Graphics2D;

public interface SvgGenerator {
    /**
     * 获取用于绘图的画笔
     */
    Graphics2D getGraphics2D();

    /**
     * 获取生成后的原始 SVG 字符串
     */
    String getRawSvgString();

    /**
     * 释放资源（如果需要）
     */
    default void dispose() {
        getGraphics2D().dispose();
    }
}
