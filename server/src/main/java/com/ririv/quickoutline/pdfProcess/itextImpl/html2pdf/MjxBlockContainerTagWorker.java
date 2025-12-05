package com.ririv.quickoutline.pdfProcess.itextImpl.html2pdf;

import com.itextpdf.html2pdf.attach.ITagWorker;
import com.itextpdf.html2pdf.attach.ProcessorContext;
import com.itextpdf.layout.IPropertyContainer;
import com.itextpdf.layout.element.Div;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.styledxmlparser.css.CommonCssConstants;
import com.itextpdf.styledxmlparser.css.util.CssDimensionParsingUtils;
import com.itextpdf.styledxmlparser.node.IElementNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class MjxBlockContainerTagWorker implements ITagWorker {
    private static final Logger log = LoggerFactory.getLogger(MjxBlockContainerTagWorker.class);

    private Div container;

    public MjxBlockContainerTagWorker(IElementNode element, ProcessorContext context) {
        this.container = new Div();
        // 1. 宽度设为 100%，这是块级居中的基础
        this.container.setWidth(UnitValue.createPercentValue(100));

        // 2. 动态读取样式
        // iText 已经将 "margin: 1em 0" 展开为 margin-top, margin-right, ...
        Map<String, String> styles = element.getStyles();

        // 获取当前基准字号 (用于计算 em)
        float emSize = context.getCssContext().getCurrentFontSize();
        if (emSize <= 0) emSize = 12f;

        // --- [上边距] (对应 1em) ---
        if (styles != null && styles.containsKey(CommonCssConstants.MARGIN_TOP)) {
            float val = CssDimensionParsingUtils.parseRelativeValue(
                    styles.get(CommonCssConstants.MARGIN_TOP), emSize);
            this.container.setMarginTop(val);
        } else {
            // 兜底：如果没解析到，给个默认值 1em
            this.container.setMarginTop(emSize);
        }

        // --- [下边距] (对应 1em) ---
        if (styles != null && styles.containsKey(CommonCssConstants.MARGIN_BOTTOM)) {
            float val = CssDimensionParsingUtils.parseRelativeValue(
                    styles.get(CommonCssConstants.MARGIN_BOTTOM), emSize);
            this.container.setMarginBottom(val);
        } else {
            // 兜底：默认值 1em
            this.container.setMarginBottom(emSize);
        }

        // --- [左右边距] (对应 0) ---
        // 虽然 Div 默认也是 0，但为了严谨，我们也读一下
        if (styles != null) {
            if (styles.containsKey(CommonCssConstants.MARGIN_LEFT)) {
                float val = CssDimensionParsingUtils.parseRelativeValue(
                        styles.get(CommonCssConstants.MARGIN_LEFT), emSize);
                this.container.setMarginLeft(val);
            }
            if (styles.containsKey(CommonCssConstants.MARGIN_RIGHT)) {
                float val = CssDimensionParsingUtils.parseRelativeValue(
                        styles.get(CommonCssConstants.MARGIN_RIGHT), emSize);
                this.container.setMarginRight(val);
            }
        }

        log.info("MjxBlock: Margins applied dynamically based on CSS.");
    }

    @Override
    public boolean processTagChild(ITagWorker childTagWorker, ProcessorContext context) {
        IPropertyContainer childResult = childTagWorker.getElementResult();

        if (childResult instanceof Image) {
            // 使用 Paragraph 包装以实现居中 (Method 1)
            Paragraph p = new Paragraph();
            p.add((Image) childResult);
            p.setTextAlignment(TextAlignment.CENTER);

            // 重要：清除 Paragraph 自身的默认 Margin，
            // 确保总边距完全由外层的 Div (我们刚才设置的那些) 控制
            p.setMargin(0);

            this.container.add(p);
        }
        return true;
    }

    @Override
    public IPropertyContainer getElementResult() {
        return container;
    }

    @Override public void processEnd(IElementNode element, ProcessorContext context) {}
    @Override public boolean processContent(String content, ProcessorContext context) { return false; }
}