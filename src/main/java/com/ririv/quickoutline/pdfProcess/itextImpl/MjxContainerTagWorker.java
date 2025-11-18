package com.ririv.quickoutline.pdfProcess.itextImpl;

import com.itextpdf.html2pdf.attach.ITagWorker;
import com.itextpdf.html2pdf.attach.ProcessorContext;
import com.itextpdf.html2pdf.attach.impl.tags.SvgTagWorker;
import com.itextpdf.layout.IPropertyContainer;
import com.itextpdf.layout.element.Div;
import com.itextpdf.layout.element.Image;
import com.itextpdf.styledxmlparser.node.IElementNode;
import com.itextpdf.svg.element.SvgImage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MjxContainerTagWorker implements ITagWorker {

    private static final Logger log = LoggerFactory.getLogger(MjxContainerTagWorker.class);

    private final boolean isBlock;
    private Div blockResult;
    private Image inlineResult;

    public MjxContainerTagWorker(IElementNode element, ProcessorContext context) {
        String displayAttr = element.getAttribute("display");
        this.isBlock = "true".equalsIgnoreCase(displayAttr);
        if (isBlock) {
            this.blockResult = new Div();
        }
    }

    @Override
    public void processEnd(IElementNode element, ProcessorContext context) {
        // Logic is in processTagChild
    }

    @Override
    public boolean processContent(String content, ProcessorContext context) {
        return false;
    }

    @Override
    public boolean processTagChild(ITagWorker childTagWorker, ProcessorContext context) {
        if (childTagWorker instanceof SvgTagWorker) {
            IPropertyContainer result = childTagWorker.getElementResult();
            if (result instanceof SvgImage) {
                if (isBlock) {
                    blockResult.add((SvgImage) result);
                    log.info("Added SVG to block-level Div container.");
                } else {
                    this.inlineResult = (SvgImage) result;
                    log.info("Stored SVG as inline-level Image.");
                }
            }
        } else {
            log.debug("Ignoring child tag worker of type: {}", childTagWorker.getClass().getSimpleName());
        }
        return true;
    }

    @Override
    public IPropertyContainer getElementResult() {
        return isBlock ? blockResult : inlineResult;
    }
}
