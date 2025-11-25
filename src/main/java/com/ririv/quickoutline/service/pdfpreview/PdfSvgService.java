package com.ririv.quickoutline.service.pdfpreview;

import jakarta.inject.Singleton;
import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

import java.awt.*;
import java.io.IOException;
import java.io.StringWriter;
import java.util.*;
import java.util.List;

import com.ririv.quickoutline.utils.FastByteArrayOutputStream;
import org.apache.pdfbox.io.RandomAccessReadBuffer;

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

}