package com.ririv.quickoutline.service;

import de.rototor.pdfbox.graphics2d.PdfBoxGraphics2D;
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

import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
public class PdfSvgService {
    private static final Logger log = LoggerFactory.getLogger(PdfSvgService.class);

    // 缓存：Key=页码, Value=SVG内容
    // 注意：在多文件切换时，需要在 Controller 里调用 clearCache()
    private final Map<Integer, String> pageCache = new HashMap<>();
    private int lastTotalPages = 0;

    // 数据传输对象：仅包含变化的页
    public record SvgPageUpdate(int pageIndex, String svgContent, int totalPages) {}

    public void clearCache() {
        pageCache.clear();
        lastTotalPages = 0;
    }

    /**
     * 核心方法：输入 PDF 字节，输出差异更新列表
     */
    public List<SvgPageUpdate> diffPdfToSvg(byte[] pdfBytes) {
        List<SvgPageUpdate> updates = new ArrayList<>();

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            int currentTotalPages = document.getNumberOfPages();

            // 遍历每一页
            for (int i = 0; i < currentTotalPages; i++) {
                PDPage page = document.getPage(i);

                // 1. 转换当前页为 SVG
                String currentSvg = convertPageToSvg(document, i);

                // 2. 与缓存对比 (Diff)
                String cachedSvg = pageCache.get(i);

                // 3. 如果内容变了，或者这是新的一页
                if (cachedSvg == null || !cachedSvg.equals(currentSvg)) {
                    pageCache.put(i, currentSvg);
                    // 加入更新列表
                    updates.add(new SvgPageUpdate(i, currentSvg, currentTotalPages));
                }
            }

            // 记录这次的总页数，用于前端处理删除页面的情况
            lastTotalPages = currentTotalPages;

        } catch (Exception e) {
            log.error("Error converting PDF to SVG", e);
        }

        return updates;
    }
    private String convertPageToSvg(PDDocument doc, int pageIndex) {
        try {
            // 1. 初始化 Batik SVG 生成器
            // DOMImplementation 是 XML DOM 的工厂
            org.w3c.dom.DOMImplementation domImpl = org.apache.batik.dom.GenericDOMImplementation.getDOMImplementation();

            // 创建一个空的 SVG 文档对象模型
            String svgNS = "http://www.w3.org/2000/svg";
            org.w3c.dom.Document svgDoc = domImpl.createDocument(svgNS, "svg", null);

            // SVGGraphics2D 是一个 Java Graphics2D 的实现，
            // 它不画在屏幕上，而是把绘图指令录制成 SVG XML
            SVGGraphics2D svgGenerator = new SVGGraphics2D(svgDoc);

            // 2. 使用 PDFBox 渲染
            // PDFRenderer 是 PDFBox 的核心渲染类
            PDFRenderer renderer = new PDFRenderer(doc);

            // 【核心魔法】
            // 我们告诉 PDFBox："请把第 pageIndex 页画出来。"
            // PDFBox 问："画到哪？"
            // 我们说："画到 svgGenerator 上。"
            // 于是 PDFBox 的绘制指令就被 Batik 捕获并转成了 SVG。
            renderer.renderPageToGraphics(pageIndex, svgGenerator);

            // 3. 导出为字符串
            try (StringWriter writer = new StringWriter()) {
                // useCss=true: 让 Batik 尽量使用 CSS 样式而不是 style 属性，减小体积
                svgGenerator.stream(writer, true);
                return writer.toString();
            }

        } catch (Exception e) {
            log.error("Failed to convert page " + pageIndex + " to SVG", e);
            // 出错时返回一个包含错误信息的 SVG，方便调试
            return "<svg xmlns='http://www.w3.org/2000/svg' width='500' height='50'>" +
                    "<text x='10' y='30' fill='red' font-size='20'>Error rendering page " + pageIndex + "</text>" +
                    "</svg>";
        }
    }
}