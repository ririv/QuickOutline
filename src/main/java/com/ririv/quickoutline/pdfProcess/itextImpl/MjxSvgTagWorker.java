package com.ririv.quickoutline.pdfProcess.itextImpl;

import com.itextpdf.html2pdf.attach.ITagWorker;
import com.itextpdf.html2pdf.attach.ProcessorContext;
import com.itextpdf.html2pdf.attach.util.AccessiblePropHelper;
import com.itextpdf.html2pdf.attach.util.AlternateDescriptionResolver;
import com.itextpdf.html2pdf.attach.util.ContextMappingHelper;
import com.itextpdf.html2pdf.logs.Html2PdfLogMessageConstant;
import com.itextpdf.html2pdf.util.SvgProcessingUtil;
import com.itextpdf.layout.IPropertyContainer;
import com.itextpdf.layout.element.Image;
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

/**
 * TagWorker class for the {@code svg} element.
 */
public class MjxSvgTagWorker implements ITagWorker {
    private static final Logger LOGGER = LoggerFactory.getLogger(MjxSvgTagWorker.class);

    private SvgImage svgImage;
    private ISvgProcessorResult processingResult;

    /**
     * Creates a new {@link MjxSvgTagWorker} instance.
     *
     * @param element the element
     * @param context the context
     */
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
//            element.getStyles().put(CommonCssConstants.DISPLAY, CommonCssConstants.INLINE_BLOCK);

            SvgImageXObject svgImageXObject = new SvgProcessingUtil(context.getResourceResolver())
                    .createXObjectFromProcessingResult(processingResult, context, true);
            svgImage = new SvgImage(svgImageXObject);

            AccessiblePropHelper.trySetLangAttribute(svgImage, element);
            context.getDIContainer().getInstance(AlternateDescriptionResolver.class).resolve(svgImage, element);
            context.endProcessingInlineSvg();
        }

        String widthStr = element.getAttribute(SvgConstants.Attributes.WIDTH);
        String heightStr = element.getAttribute(SvgConstants.Attributes.HEIGHT);

        if (widthStr != null && heightStr != null) {
            try {
                float finalWidthInPt;
                float finalHeightInPt;

                // Get font-size (em value) from the resolved styles, needed for 'ex' conversion
                float em = context.getCssContext().getCurrentFontSize();

                // 不知道为什么 itext svg没法处理 ex 单位，但是CssDimensionParsingUtils提供了处理的方法
                // 所以这里手动处理
                // parseRelativeValue源码中表明 1ex = 0.5 * baseValue (即 0.5em)
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
                //parseAbsoluteLength() 可以智能地处理绝对单位
                    finalHeightInPt = CssDimensionParsingUtils.parseAbsoluteLength(heightStr);
                }

                svgImage.setWidth(finalWidthInPt);
                svgImage.setHeight(finalHeightInPt);
                LOGGER.info("Resized SVG to {}pt x {}pt (original: {} x {}) using em {}pt", finalWidthInPt, finalHeightInPt, widthStr, heightStr, em);
            } catch (Exception e) {
                LOGGER.error("Failed to parse or set SVG dimensions for width '{}' height '{}'.", widthStr, heightStr, e);
            }
        }
    }

    @Override
    public boolean processContent(String content, ProcessorContext context) {
        return false;
    }

    @Override
    public boolean processTagChild(ITagWorker childTagWorker, ProcessorContext context) {
        return false;
    }

    @Override
    public IPropertyContainer getElementResult() {
        return svgImage;
    }
}
