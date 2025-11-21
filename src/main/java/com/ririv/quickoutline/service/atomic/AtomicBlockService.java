package com.ririv.quickoutline.service.atomic;

import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.HtmlConverter;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.font.FontProvider;
import com.itextpdf.layout.layout.LayoutArea;
import com.itextpdf.layout.layout.LayoutContext;
import com.itextpdf.layout.layout.LayoutResult;
import com.itextpdf.layout.properties.Property;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.renderer.DivRenderer;
import com.itextpdf.layout.renderer.IRenderer;
import com.ririv.quickoutline.pdfProcess.itextImpl.CustomCssApplierFactory;
import com.ririv.quickoutline.pdfProcess.itextImpl.CustomTagWorkerFactory;
import com.ririv.quickoutline.service.FontManager;
import com.ririv.quickoutline.service.PdfSvgService;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class AtomicBlockService {
    private static final Logger log = LoggerFactory.getLogger(AtomicBlockService.class);

    // 缓存：Key=Element Hash, Value=DocumentBlock
    private final Map<Integer, List<DocumentBlock>> blockCache = new ConcurrentHashMap<>();
    private static String CACHED_CSS = null;

    @Inject private PdfSvgService pdfSvgService;
    private final FontProvider sharedFontProvider;
    private final ConverterProperties converterProperties;

    // 页面常量
    private static final float PAGE_W = 595f;
    private static final float PAGE_H = 842f;
    private static final float MARGIN = 36f;

    @Inject
    public AtomicBlockService(FontManager fontManager, PdfSvgService pdfSvgService) {
        this.pdfSvgService = pdfSvgService;
        this.sharedFontProvider = fontManager.getFontProvider(null);
        loadGlobalCss();

        // 初始化一次 ConverterProperties，复用配置
        this.converterProperties = new ConverterProperties();
        this.converterProperties.setTagWorkerFactory(new CustomTagWorkerFactory());
        this.converterProperties.setCssApplierFactory(new CustomCssApplierFactory());
        this.converterProperties.setFontProvider(this.sharedFontProvider);
        this.converterProperties.setBaseUri(""); // 初始为空，处理时覆盖
    }

    public List<DocumentBlock> processHtml(String fullHtml, String baseUri) {
        long start = System.currentTimeMillis();

        // 1. 【核心改变】使用 iText 解析整个 HTML 树
        // 这会处理所有的 CSS 上下文、空白折叠、选择器匹配
        if (baseUri != null) this.converterProperties.setBaseUri(baseUri);

        // 注入全局 CSS 包装，确保样式生效
        String wrappedHtml = wrapHtmlWithContext(fullHtml);

        // 解析得到 iText 对象列表 (Paragraph, Table, Div, etc.)
        List<IElement> elements = HtmlConverter.convertToElements(wrappedHtml, this.converterProperties);

        List<DocumentBlock> resultBlocks = new ArrayList<>();

        // 2. 遍历对象树，生成积木
        for (IElement element : elements) {

            // 计算对象指纹 (Hash)
            // 注意：IElement 没有重写 hashCode，我们用 layout 属性 + 内容近似计算
            // 或者简单点：因为有了 convertToElements 的开销，这里的缓存只能基于"位置"或"内容字符串"
            // 为了确保正确性，这里暂时每次重新渲染 Element (因为 SVG 生成很快，且解决了正确性问题)
            // 进阶优化：如果需要缓存，可以结合原始 HTML 片段的 Hash。

            // 渲染单个 Element -> SVG
            List<DocumentBlock> blocks = renderElement(element);
            resultBlocks.addAll(blocks);
        }

        // 3. 骨架布局
        doSkeletonLayout(resultBlocks);

        long duration = System.currentTimeMillis() - start;
        if (duration > 100) log.debug("Atomic render finished. Blocks: {}, Time: {}ms", resultBlocks.size(), duration);

        return resultBlocks;
    }

    /**
     * 将单个 iText Element 渲染为 DocumentBlock
     */
    private List<DocumentBlock> renderElement(IElement element) {
        List<DocumentBlock> blocks = new ArrayList<>();

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(1024 * 10)) {
            PdfWriter writer = new PdfWriter(baos);
            writer.setCompressionLevel(0);

            try (PdfDocument pdfDoc = new PdfDocument(writer)) {
                // 使用 A4 页面，允许 iText 自然切分
                pdfDoc.setDefaultPageSize(PageSize.A4);

                com.itextpdf.layout.Document doc = new com.itextpdf.layout.Document(pdfDoc);
                // 移除 Document 默认边距，让 Element 填满
                doc.setMargins(0, 0, 0, 0);

                // 添加元素
                doc.add((IBlockElement) element);
            }

            baos.flush();
            byte[] pdfBytes = baos.toByteArray();

            // 处理结果 (支持多页切分)
            try (org.apache.pdfbox.pdmodel.PDDocument pdDoc = org.apache.pdfbox.Loader.loadPDF(pdfBytes)) {
                for (int i = 0; i < pdDoc.getNumberOfPages(); i++) {
                    var res = pdfSvgService.convertPageInPdfToSvg(pdDoc, i);

                    DocumentBlock b = new DocumentBlock(""); // HTML 内容不再重要，因为已经对象化
                    b.svgContent = res.svg();
                    b.heightPt = res.heightPt();

                    // 【核心修复】从 Element 读取真实的边距
                    // 解决行距过大问题
                    b.marginTop = getMargin(element, Property.MARGIN_TOP);
                    b.marginBottom = getMargin(element, Property.MARGIN_BOTTOM);

                    blocks.add(b);
                }
            }

        } catch (Exception e) {
            log.error("Element render failed", e);
        }
        return blocks;
    }

    // 读取样式属性的辅助方法
    private float getMargin(IElement element, int propertyId) {
        if (element instanceof IBlockElement) {
            UnitValue uv = element.getProperty(propertyId);
            if (uv != null && uv.isPointValue()) {
                return uv.getValue();
            }
        }
        return 0f; // 默认无边距
    }

    /**
     * 骨架布局：使用 iText 模拟排版
     */
    private void doSkeletonLayout(List<DocumentBlock> blocks) {
        int currentPage = 1;
        float currentY = PAGE_H - MARGIN;
        final float CONTENT_W = PAGE_W - MARGIN * 2;

        for (DocumentBlock block : blocks) {
            Div placeholder = new Div();
            placeholder.setHeight(block.heightPt);
            placeholder.setWidth(CONTENT_W);

            // 【核心】使用从 Element 读取到的真实边距
            placeholder.setMarginTop(block.marginTop);
            placeholder.setMarginBottom(block.marginBottom);

            placeholder.setKeepTogether(true);

            IRenderer renderer = placeholder.createRendererSubTree();

            // 模拟布局... (逻辑同前，略微精简)
            float remainingHeight = currentY - MARGIN;
            LayoutArea area = new LayoutArea(currentPage, new Rectangle(MARGIN, MARGIN, CONTENT_W, remainingHeight));
            LayoutContext context = new LayoutContext(area);
            LayoutResult result = renderer.layout(context);

            if (result.getStatus() != LayoutResult.FULL) {
                currentPage++;
                currentY = PAGE_H - MARGIN;
                area = new LayoutArea(currentPage, new Rectangle(MARGIN, MARGIN, CONTENT_W, PAGE_H - MARGIN * 2));
                context = new LayoutContext(area);
                result = renderer.layout(context);
            }

            Rectangle occupied = result.getOccupiedArea().getBBox();
            block.pageNumber = currentPage;
            block.topPt = PAGE_H - (occupied.getY() + occupied.getHeight());

            // 更新游标：减去高度和下边距
            // 注意：OccupiedArea 包含了 Padding 和 Border，但不包含 Margin
            // 我们需要手动处理 Margin 对下一个元素位置的影响
            // 简单算法：用 occupied.getY() - margin_bottom
            currentY = occupied.getY() - block.marginBottom;
        }
    }

    // 必须保留 wrapHtmlWithContext 来注入 CSS，否则 convertToElements 解析出的对象没有样式
    private String wrapHtmlWithContext(String fragmentHtml) {
        String css = getGlobalCss();
        return "<html><head><style>" + css + "body{margin:0;padding:0;background:transparent}.vditor-reset{width:100%}</style></head><body><div class='vditor-reset'>" + fragmentHtml + "</div></body></html>";
    }

    private void loadGlobalCss() {
        if (CACHED_CSS != null) return;
        try (InputStream is = getClass().getResourceAsStream("/web/vditor/dist/index.css")) {
            if (is != null) CACHED_CSS = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) { CACHED_CSS = ""; }
    }
    private String getGlobalCss() { if (CACHED_CSS == null) loadGlobalCss(); return CACHED_CSS; }
    public void clearCache() { blockCache.clear(); }
}