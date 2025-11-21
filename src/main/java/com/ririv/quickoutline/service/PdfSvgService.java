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

            // 使用官方标准渲染器 (保证视觉绝对正确)
            PDFRenderer renderer = new PDFRenderer(document);

            for (int i = 0; i < currentTotalPages; i++) {
                PDPage page = document.getPage(i);
                // 获取物理尺寸
                // --- 计算视觉尺寸 (考虑旋转) ---
                PDRectangle cropBox = page.getCropBox();
                int rotation = page.getRotation();

                float displayWidth;
                float displayHeight;

                // 如果旋转了 90 或 270 度，交换宽高
                if (rotation == 90 || rotation == 270) {
                    displayWidth = cropBox.getHeight();
                    displayHeight = cropBox.getWidth();
                } else {
                    displayWidth = cropBox.getWidth();
                    displayHeight = cropBox.getHeight();
                }

                // 1. 转换 SVG (传入视觉尺寸)
                String currentSvg = convertPageToSvg(renderer, i, displayWidth, displayHeight);

                // 2. Diff 逻辑
                String cachedSvg = pageCache.get(i);
                if (cachedSvg == null || !cachedSvg.equals(currentSvg)) {
                    pageCache.put(i, currentSvg);
                    // 3. 推送更新 (包含修正后的宽高)
                    updates.add(new SvgPageUpdate(i, currentSvg, currentTotalPages, displayWidth, displayHeight));
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
                        // 同样需要获取第一页的正确尺寸
                        PDPage p0 = document.getPage(0);
                        int r0 = p0.getRotation();
                        float w0 = (r0 == 90 || r0 == 270) ? p0.getCropBox().getHeight() : p0.getCropBox().getWidth();
                        float h0 = (r0 == 90 || r0 == 270) ? p0.getCropBox().getWidth() : p0.getCropBox().getHeight();

                        updates.add(new SvgPageUpdate(0, first, currentTotalPages, w0, h0));
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

            // 【关键】设置画布大小为视觉尺寸 (旋转后的)
            // PDFRenderer 会自动处理内部的内容旋转，适配这个画布
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