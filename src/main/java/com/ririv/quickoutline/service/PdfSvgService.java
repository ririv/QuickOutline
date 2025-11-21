package com.ririv.quickoutline.service;

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

import java.awt.Dimension;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
public class PdfSvgService {
    private static final Logger log = LoggerFactory.getLogger(PdfSvgService.class);

    // 缓存：Key=页码, Value=SVG内容
    private final Map<Integer, String> pageCache = new HashMap<>();
    private int lastTotalPages = 0;

    // 数据传输对象
    public record SvgPageUpdate(int pageIndex, String svgContent, int totalPages, float widthPt, float heightPt) {}

    public void clearCache() {
        pageCache.clear();
        lastTotalPages = 0;
        log.info("PDF-SVG cache cleared.");
    }

    /**
     * 核心方法：对比 PDF 变化，只返回变动的页面
     */
    public List<SvgPageUpdate> diffPdfToSvg(byte[] pdfBytes) {
        List<SvgPageUpdate> updates = new ArrayList<>();

        if (pdfBytes == null || pdfBytes.length == 0) return updates;

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            int currentTotalPages = document.getNumberOfPages();

            // 使用官方标准渲染器
            PDFRenderer renderer = new PDFRenderer(document);

            for (int i = 0; i < currentTotalPages; i++) {
                PDPage page = document.getPage(i);
                // 获取物理尺寸
                PDRectangle cropBox = page.getCropBox();
                float width = cropBox.getWidth();
                float height = cropBox.getHeight();

                // 转换 SVG (标准转换，文字转曲线)
                String currentSvg = convertPageToSvg(renderer, i, width, height);

                // Diff 逻辑
                String cachedSvg = pageCache.get(i);
                if (cachedSvg == null || !cachedSvg.equals(currentSvg)) {
                    pageCache.put(i, currentSvg);
                    updates.add(new SvgPageUpdate(i, currentSvg, currentTotalPages, width, height));
                }
            }

            // 处理页面减少
            if (currentTotalPages < lastTotalPages) {
                for (int k = currentTotalPages; k < lastTotalPages; k++) {
                    pageCache.remove(k);
                }
                // 强制刷新第一页以重置容器结构
                if (updates.isEmpty() && currentTotalPages > 0) {
                    String first = pageCache.get(0);
                    if (first != null) {
                        PDPage p0 = document.getPage(0);
                        updates.add(new SvgPageUpdate(0, first, currentTotalPages,
                                p0.getCropBox().getWidth(), p0.getCropBox().getHeight()));
                    }
                }
            }
            lastTotalPages = currentTotalPages;

        } catch (Exception e) {
            log.error("Error converting PDF to SVG", e);
        }
        return updates;
    }

    /**
     * 将单页 PDF 渲染为 SVG 字符串
     * 使用 PDFBox 原生渲染能力 -> Batik SVG Generator
     */
    private String convertPageToSvg(PDFRenderer renderer, int pageIndex, float width, float height) {
        try {
            // 1. 初始化 Batik SVG 生成器
            DOMImplementation domImpl = GenericDOMImplementation.getDOMImplementation();
            String svgNS = "http://www.w3.org/2000/svg";
            Document svgDoc = domImpl.createDocument(svgNS, "svg", null);

            SVGGraphics2D svgGenerator = new SVGGraphics2D(svgDoc);

            // 设置画布大小
            svgGenerator.setSVGCanvasSize(new Dimension((int) width, (int) height));

            // 2. 核心渲染
            // scale = 1.0f: 保持原始矢量精度
            // 这一步会将 PDF 里的所有内容（包括文字）转换成 Graphics2D 的绘图指令
            // 文字会被转为矢量轮廓 (Curves/Paths)，确保在任何设备上显示一致
            renderer.renderPageToGraphics(pageIndex, svgGenerator, 1.0f);

            // 3. 导出为字符串
            try (StringWriter writer = new StringWriter()) {
                // useCss=true: 让 Batik 尽量使用 CSS 样式而不是 style 属性，减小体积
                svgGenerator.stream(writer, true);
                return writer.toString();
            }

        } catch (Exception e) {
            log.error("Failed to render page {} to SVG", pageIndex, e);
            // 出错时返回一个包含错误信息的 SVG，方便调试
            return "<svg xmlns='http://www.w3.org/2000/svg' width='500' height='50'>" +
                    "<text x='10' y='30' fill='red' font-size='20'>Error rendering page " + pageIndex + "</text>" +
                    "</svg>";
        }
    }
}