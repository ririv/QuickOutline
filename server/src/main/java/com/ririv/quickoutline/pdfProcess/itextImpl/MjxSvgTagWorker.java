package com.ririv.quickoutline.pdfProcess.itextImpl;

import com.itextpdf.html2pdf.attach.ITagWorker;
import com.itextpdf.html2pdf.attach.ProcessorContext;
import com.itextpdf.html2pdf.attach.util.AccessiblePropHelper;
import com.itextpdf.html2pdf.attach.util.AlternateDescriptionResolver;
import com.itextpdf.html2pdf.attach.util.ContextMappingHelper;
import com.itextpdf.html2pdf.css.CssConstants;
import com.itextpdf.html2pdf.logs.Html2PdfLogMessageConstant;
import com.itextpdf.html2pdf.util.SvgProcessingUtil;
import com.itextpdf.layout.IPropertyContainer;
import com.itextpdf.layout.properties.InlineVerticalAlignment;
import com.itextpdf.layout.properties.InlineVerticalAlignmentType;
import com.itextpdf.layout.properties.Property;
import com.itextpdf.styledxmlparser.css.CommonCssConstants;
import com.itextpdf.styledxmlparser.css.util.CssDimensionParsingUtils;
import com.itextpdf.styledxmlparser.node.IElementNode;
import com.itextpdf.svg.SvgConstants;
import com.itextpdf.svg.element.SvgImage;
import com.itextpdf.svg.exceptions.SvgProcessingException;
import com.itextpdf.svg.processors.ISvgProcessorResult;
import com.itextpdf.svg.processors.impl.DefaultSvgProcessor;
import com.itextpdf.svg.processors.impl.SvgConverterProperties;
import com.itextpdf.svg.xobject.SvgImageXObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MjxSvgTagWorker implements ITagWorker {
    private static final Logger LOGGER = LoggerFactory.getLogger(MjxSvgTagWorker.class);

    private SvgImage svgImage;
    private ISvgProcessorResult processingResult;

    public MjxSvgTagWorker(IElementNode element, ProcessorContext context) {
        svgImage = null;
        SvgConverterProperties props = ContextMappingHelper.mapToSvgConverterProperties(context);
        try {
            processingResult = new DefaultSvgProcessor().process(element, props);
        } catch (SvgProcessingException spe) {
            LOGGER.error(Html2PdfLogMessageConstant.UNABLE_TO_PROCESS_SVG_ELEMENT, spe);
        }
        context.startProcessingInlineSvg();
    }

    @Override
    public void processEnd(IElementNode element, ProcessorContext context) {
        if (processingResult != null) {
            SvgImageXObject svgImageXObject = new SvgProcessingUtil(context.getResourceResolver())
                    .createXObjectFromProcessingResult(processingResult, context, true);
            svgImage = new SvgImage(svgImageXObject);

            AccessiblePropHelper.trySetLangAttribute(svgImage, element);
            context.getDIContainer().getInstance(AlternateDescriptionResolver.class).resolve(svgImage, element);

            // --- 1. 处理宽高 (你之前的逻辑) ---
            String widthStr = element.getAttribute(SvgConstants.Attributes.WIDTH);
            String heightStr = element.getAttribute(SvgConstants.Attributes.HEIGHT);
            float em = context.getCssContext().getCurrentFontSize();

            if (widthStr != null && heightStr != null) {
                try {
                    float finalWidthInPt = parseDimension(widthStr, em);
                    float finalHeightInPt = parseDimension(heightStr, em);

                    svgImage.setWidth(finalWidthInPt);
                    svgImage.setHeight(finalHeightInPt);
                    LOGGER.info("Resized SVG: {} x {}", finalWidthInPt, finalHeightInPt);
                } catch (Exception e) {
                    LOGGER.error("Failed to set SVG dimensions.", e);
                }
            }

            // --- 2. 处理垂直偏移 (新增逻辑) ---
            // MathJax 通常把 vertical-align 写在 style 里，而不是 attribute
            String vAlignStr = null;
            if (element.getStyles() != null) {
                vAlignStr = element.getStyles().get(CssConstants.VERTICAL_ALIGN);
            }
            // 有时候也在属性里，双重检查
            if (vAlignStr == null) {
                vAlignStr = element.getAttribute("vertical-align");
            }

            if (vAlignStr != null && !vAlignStr.isEmpty() && !"baseline".equals(vAlignStr)) {
                try {
                    // 复用之前的解析逻辑，得到 pt 值
                    // vertical-align: -0.5ex -> -3.0pt
                    float offsetPt = parseDimension(vAlignStr, em);

//                    svgImage.setRelativePosition(0, 0, 0, offsetPt);  // 错误，底部会截断
//                    svgImage.setProperty(Property.TEXT_RISE, offsetPt); // 错误，不起作用
                    svgImage.setProperty(Property.INLINE_VERTICAL_ALIGNMENT,
                            new InlineVerticalAlignment(InlineVerticalAlignmentType.FIXED, offsetPt));

                    LOGGER.info("Applied vertical-align offset: {}pt (from {})", offsetPt, vAlignStr);
                } catch (Exception e) {
                    LOGGER.warn("Failed to parse vertical-align: {}", vAlignStr);
                }
            }

            context.endProcessingInlineSvg();
        }
    }

    // 这里相对值仅做了EX的解析
    private float parseDimension(String valueStr, float em) {
        if (valueStr.endsWith(CommonCssConstants.EX)) {
            return CssDimensionParsingUtils.parseRelativeValue(valueStr, em);
        } else {
            return CssDimensionParsingUtils.parseAbsoluteLength(valueStr);
        }
    }

    @Override public boolean processContent(String content, ProcessorContext context) { return false; }
    @Override public boolean processTagChild(ITagWorker childTagWorker, ProcessorContext context) { return false; }
    @Override public IPropertyContainer getElementResult() { return svgImage; }
}