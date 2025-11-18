package com.ririv.quickoutline.pdfProcess.itextImpl;

import com.itextpdf.html2pdf.attach.ITagWorker;
import com.itextpdf.html2pdf.css.apply.ICssApplier;
import com.itextpdf.html2pdf.attach.ProcessorContext;
import com.itextpdf.layout.IPropertyContainer;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.properties.Property;
import com.itextpdf.layout.properties.VerticalAlignment;
import com.itextpdf.styledxmlparser.css.CommonCssConstants;
import com.itextpdf.styledxmlparser.css.util.CssDimensionParsingUtils;
import com.itextpdf.styledxmlparser.node.IElementNode;
import com.itextpdf.styledxmlparser.node.IStylesContainer;
import com.itextpdf.svg.SvgConstants;
import com.itextpdf.svg.element.SvgImage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MjxSvgCssApplier implements ICssApplier {

    private static final Logger log = LoggerFactory.getLogger(MjxSvgCssApplier.class);
    private final IElementNode svgNode;

    public MjxSvgCssApplier(IElementNode svgNode) {
        this.svgNode = svgNode;
    }

    @Override
    public void apply(ProcessorContext context, IStylesContainer stylesContainer, ITagWorker tagWorker) {
        IPropertyContainer propertyContainer = tagWorker.getElementResult();
        if (propertyContainer instanceof SvgImage svgImage) {
            // Vditor bundle CSS 中自带的 .vditor-reset svg 样式包含 包含 width 和 height 属性 为 auto，会覆盖掉 Mathjax svg标签中的height和width属性
            // 因此这里直接从 svgNode 上获取 width 和 height 属性（实际尺寸）进行处理
            // 如果不处理。auto属性会导致 svg 图片显示异常大
            String widthStr = svgNode.getAttribute(SvgConstants.Attributes.WIDTH);
            String heightStr = svgNode.getAttribute(SvgConstants.Attributes.HEIGHT);

            if (widthStr != null && heightStr != null) {
                try {
                    float finalWidthInPt;
                    float finalHeightInPt;

                    // Get font-size (em value) from the resolved styles, needed for 'ex' conversion
                    float em = context.getCssContext().getCurrentFontSize();

                    // 不知道为什么 itext svg没法处理 ex 单位，但是CssDimensionParsingUtils提供了处理的方法
                    // 所以这里手动处理
                    // Parse width based on unit
                    if (widthStr.endsWith(CommonCssConstants.EX)) {
                        finalWidthInPt = CssDimensionParsingUtils.parseRelativeValue(widthStr, em);
                    } else {
                        finalWidthInPt = CssDimensionParsingUtils.parseAbsoluteLength(widthStr);
                    }

                    // Parse height based on unit
                    if (heightStr.endsWith(CommonCssConstants.EX)) {
                        finalHeightInPt = CssDimensionParsingUtils.parseRelativeValue(heightStr, em);
                    } else {
//                        parseAbsoluteLength() 可以智能地处理绝对单位
                        finalHeightInPt = CssDimensionParsingUtils.parseAbsoluteLength(heightStr);
                    }

                    svgImage.setWidth(finalWidthInPt);
                    svgImage.setHeight(finalHeightInPt);
                    log.info("Resized SVG to {}pt x {}pt (original: {} x {}) using em {}pt", finalWidthInPt, finalHeightInPt, widthStr, heightStr, em);
                } catch (Exception e) {
                    log.error("Failed to parse or set SVG dimensions for width '{}' height '{}'.", widthStr, heightStr, e);
                }
            }
        }
    }
}
