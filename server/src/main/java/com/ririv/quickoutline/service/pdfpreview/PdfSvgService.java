package com.ririv.quickoutline.service.pdfpreview;

import com.ririv.quickoutline.service.pdfpreview.strategy.BatikSvgStrategy;
import com.ririv.quickoutline.service.pdfpreview.strategy.JFreeSvgStrategy;
import com.ririv.quickoutline.service.pdfpreview.strategy.SvgGenerator;
import com.ririv.quickoutline.utils.FastByteArrayOutputStream;
import jakarta.inject.Singleton;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
public class PdfSvgService {
    private static final Logger log = LoggerFactory.getLogger(PdfSvgService.class);

    // 开关：控制使用哪个引擎，JFreeSvg 会更快一点，但注意License为GPL 3.0
    // 默认使用 JFreeSVG (true)
    private boolean useJFreeSvg = false;

    private final Map<Integer, String> pageCache = new HashMap<>();
    private int lastTotalPages = 0;

    public record SvgPageUpdate(int pageIndex, String svgContent, int totalPages, float widthPt, float heightPt) {}

    public void setUseJFreeSvg(boolean useJFreeSvg) {
        this.useJFreeSvg = useJFreeSvg;
    }

    public void clearCache() {
        pageCache.clear();
        lastTotalPages = 0;
    }

    // 工厂方法：创建策略
    private SvgGenerator createSvgGenerator(float width, float height) {
        if (this.useJFreeSvg) {
            return new JFreeSvgStrategy(width, height);
        } else {
            return new BatikSvgStrategy(width, height);
        }
    }

    public List<SvgPageUpdate> diffPdfToSvg(FastByteArrayOutputStream pdfStream) {
        List<SvgPageUpdate> updates = new ArrayList<>();
        if (pdfStream == null || pdfStream.size() == 0) return updates;

        try (PDDocument document = Loader.loadPDF(new RandomAccessReadBuffer(pdfStream.getBuffer()))) {
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

                // 核心变化：逻辑解耦
                String currentSvg = renderPage(renderer, i, displayWidth, displayHeight);

                String cachedSvg = pageCache.get(i);
                if (cachedSvg == null || !cachedSvg.equals(currentSvg)) {
                    pageCache.put(i, currentSvg);
                    updates.add(new SvgPageUpdate(i, currentSvg, currentTotalPages, displayWidth, displayHeight));
                }
            }

            if (currentTotalPages < lastTotalPages) {
                for (int k = currentTotalPages; k < lastTotalPages; k++) pageCache.remove(k);
            }
            lastTotalPages = currentTotalPages;

        } catch (Exception e) {
            log.error("Error converting PDF to SVG", e);
        }
        return updates;
    }

    private String renderPage(PDFRenderer renderer, int pageIndex, float width, float height) throws IOException {
        // 1. 获取策略实例
        SvgGenerator generator = createSvgGenerator(width, height);

        try {
            // 2. 渲染 (PDFBox 负责画，Generator 负责记录)
            renderer.renderPageToGraphics(pageIndex, generator.getGraphics2D(), 1.0f);

            // 3. 获取原始 SVG
            String rawSvg = generator.getRawSvgString();

            // 4. 获取字体 CSS (如果有)
            String fontCss = "";
            if (renderer instanceof CustomPDFRenderer customRenderer) {
                fontCss = customRenderer.getCurrentPageFontCss();
            }

            // 5. 统一包装处理 (这部分逻辑对两个库是通用的)
            return wrapSvgWithOptimizedHeader(rawSvg, fontCss, width, height);
        } finally {
            generator.dispose();
        }
    }

    /**
     * 统一的 SVG 包装逻辑：剥离原始标签，注入高性能 Header 和 CSS
     */
    private String wrapSvgWithOptimizedHeader(String rawSvg, String fontCss, float width, float height) {
        // 简单的 XML 解析，提取 <svg> 内部的内容
        int svgTagEndIndex = rawSvg.indexOf(">") + 1;
        int svgCloseTagIndex = rawSvg.lastIndexOf("</svg>");

        if (svgTagEndIndex > 0 && svgCloseTagIndex > svgTagEndIndex) {
            String svgContent = rawSvg.substring(svgTagEndIndex, svgCloseTagIndex);

            String styleBlock = "";
//            if (fontCss != null && !fontCss.isEmpty()) {
//                styleBlock = "<style>" + fontCss + "</style>";
//            }

            // 高性能 Header
            String newHeader = String.format(
                    "<svg xmlns=\"http://www.w3.org/2000/svg\" " +
                            "viewBox=\"0 0 %s %s\" " +
                            "width=\"100%%\" height=\"100%%\" " +
                            "preserveAspectRatio=\"xMidYMid meet\" " +
                            "style=\"display:block; overflow:hidden; " +
                            "text-rendering: optimizeSpeed; shape-rendering: crispEdges;\">",
//                            "text-rendering: geometricPrecision; shape-rendering: geometricPrecision;\">",
                    fmt(width), fmt(height));

            return newHeader + styleBlock + svgContent + "</svg>";
        }
        return rawSvg; // Fallback
    }

    private String fmt(float d) {
        if (d == (long) d) return String.format("%d", (long) d);
        return String.format("%s", d);
    }
}