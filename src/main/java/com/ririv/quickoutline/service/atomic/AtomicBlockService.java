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
     * 获取元素的边距值 (pt)
     * @param element iText 元素 (如 Paragraph, Table)
     * @param propertyId 属性 ID (如 Property.MARGIN_TOP)
     * @return 边距值，如果不存在或非数值则返回 0
     */
    private float getMargin(IElement element, int propertyId) {
        // 只有块级元素才有 margin
        if (element instanceof IBlockElement) {
            // 获取属性值 (iText 已经把 CSS 解析成了 UnitValue)
            UnitValue uv = element.getProperty(propertyId);

            // 确保值存在且是绝对数值 (Point Value)
            // 注意：iText 这里的 getValue() 通常已经是转换后的 pt 值
            if (uv != null && uv.isPointValue()) {
                return uv.getValue();
            }
        }
        return 0f; // 默认无边距
    }

    /**
     * 将单个 iText Element 渲染为 DocumentBlock
     */
    private List<DocumentBlock> renderElement(IElement element) {
        List<DocumentBlock> blocks = new ArrayList<>();

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(1024 * 50)) {
            PdfWriter writer = new PdfWriter(baos);
            writer.setCompressionLevel(0);

            try (PdfDocument pdfDoc = new PdfDocument(writer)) {
                pdfDoc.setDefaultPageSize(PageSize.A4);

                com.itextpdf.layout.Document doc = new com.itextpdf.layout.Document(pdfDoc);
                // 保持与骨架布局一致的边距
                doc.setMargins(MARGIN, MARGIN, MARGIN, MARGIN);

                if (element instanceof IBlockElement) {
                    doc.add((IBlockElement) element);
                } else if (element instanceof com.itextpdf.layout.element.ILeafElement) {
                    doc.add(new Paragraph().add((com.itextpdf.layout.element.ILeafElement) element));
                }
            }

            baos.flush();
            byte[] pdfBytes = baos.toByteArray();

            // 计算页面有效内容高度 (842 - 36 - 36 = 770)
            float maxContentHeight = PAGE_H - MARGIN * 2;

            try (org.apache.pdfbox.pdmodel.PDDocument pdDoc = org.apache.pdfbox.Loader.loadPDF(pdfBytes)) {
                int totalPages = pdDoc.getNumberOfPages();

                for (int i = 0; i < totalPages; i++) {
                    var res = pdfSvgService.convertPageInPdfToSvg(pdDoc, i);

                    DocumentBlock b = new DocumentBlock("");
                    b.svgContent = res.svg();

                    // 【修复 1】高度安全钳：消除 +2pt 误差导致的换页
                    // 如果计算出的高度略微超过了最大内容高度，强制修剪为最大高度
                    // 0.1f 是浮点容差
                    if (res.heightPt() > maxContentHeight + 0.1f) {
                        b.heightPt = maxContentHeight;
                    } else {
                        b.heightPt = res.heightPt();
                    }

                    // 【修复 2】智能边距分配
                    // 只有第一页保留上边距
                    if (i == 0) {
                        b.marginTop = getMargin(element, Property.MARGIN_TOP);
                    } else {
                        b.marginTop = 0; // 中间/结尾页没有上边距
                    }

                    // 只有最后一页保留下边距
                    if (i == totalPages - 1) {
                        b.marginBottom = getMargin(element, Property.MARGIN_BOTTOM);
                    } else {
                        b.marginBottom = 0; // 开头/中间页没有下边距，紧贴底部
                    }

                    blocks.add(b);
                }
            }

        } catch (Exception e) {
            log.error("Element render failed", e);
        }
        return blocks;
    }

    /**
     * 骨架布局：使用 iText 模拟排版
     */
    private void doSkeletonLayout(List<DocumentBlock> blocks) {
        int currentPage = 1;
        // 注意：PDF坐标系 Y=0 在底部。内容区顶部 Y = 842 - 36 = 806
        float currentY = PAGE_H - MARGIN;
        final float CONTENT_W = PAGE_W - MARGIN * 2;
        final float CONTENT_H = PAGE_H - MARGIN * 2;

        for (DocumentBlock block : blocks) {
            // ... 创建 placeholder 代码不变 ...
            Div placeholder = new Div();
            placeholder.setHeight(block.heightPt);
            placeholder.setWidth(CONTENT_W);
            // 还原 margin
            placeholder.setMarginTop(block.marginTop);
            placeholder.setMarginBottom(block.marginBottom);
            placeholder.setKeepTogether(true);

            IRenderer renderer = placeholder.createRendererSubTree();

            // 第一次尝试：在当前页剩余空间布局
            float remainingHeight = currentY - MARGIN; // 到底部的距离
            LayoutArea area = new LayoutArea(currentPage, new Rectangle(MARGIN, MARGIN, CONTENT_W, remainingHeight));
            LayoutContext context = new LayoutContext(area);
            LayoutResult result = renderer.layout(context);

            // 如果放不下 (NOT_FITTING 或 SPLIT 但我们要求 keepTogether)，换页
            if (result.getStatus() != LayoutResult.FULL) {
                currentPage++;
                currentY = PAGE_H - MARGIN;

                // 新页面：给足完整的 Content Height
                area = new LayoutArea(currentPage, new Rectangle(MARGIN, MARGIN, CONTENT_W, CONTENT_H));
                context = new LayoutContext(area);
                result = renderer.layout(context);
            }

            // 【核心修复】NPE 防御
            // 如果换了页还是放不下 (说明这个块本身高度 > CONTENT_H)，iText 可能会返回 null occupiedArea
            if (result.getOccupiedArea() == null) {
                log.warn("Block too large for page! Force placing. ID: {}, Height: {}", block.id, block.heightPt);
                // 强制放置策略：虽然溢出，但至少算出一个坐标让它显示出来
                block.pageNumber = currentPage;
                block.topPt = MARGIN; // 顶格放

                // 游标直接到底，强制下一个块换页
                currentY = MARGIN;
            } else {
                // 正常逻辑
                Rectangle occupied = result.getOccupiedArea().getBBox();
                block.pageNumber = result.getOccupiedArea().getPageNumber(); // 使用 iText 实际决定的页码
                block.topPt = PAGE_H - (occupied.getY() + occupied.getHeight());

                // 更新游标
                // 考虑 margin-bottom 对光标的影响
                // iText 的 layout 结果通常已经消耗了 margin 空间，occupied area 是包含 margin 的盒子吗？
                // 不，getBBox() 通常是 Border Box。我们需要手动减去 Margin。
                currentY = occupied.getY() - block.marginBottom;
            }
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