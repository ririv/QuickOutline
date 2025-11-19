package com.ririv.quickoutline.pdfProcess.itextImpl;

import com.itextpdf.html2pdf.attach.ITagWorker;
import com.itextpdf.html2pdf.attach.ProcessorContext;
import com.itextpdf.html2pdf.attach.impl.DefaultTagWorkerFactory;
import com.itextpdf.styledxmlparser.node.IElementNode;
import com.itextpdf.svg.SvgConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomTagWorkerFactory extends DefaultTagWorkerFactory {

    private static final Logger log = LoggerFactory.getLogger(CustomTagWorkerFactory.class);

    @Override
    public ITagWorker getCustomTagWorker(IElementNode tag, ProcessorContext context) {
        log.debug("Processing tag in CustomTagWorkerFactory: {}", tag.name());
        if ("mjx-container".equalsIgnoreCase(tag.name())) {
            log.info("Handling mjx-container with MjxContainerTagWorker.");
            return new MjxContainerTagWorker(tag, context);
        }
        if (SvgConstants.Tags.SVG.equalsIgnoreCase(tag.name())
                && tag.parentNode() instanceof IElementNode // Ensure parent is an IElementNode
                && "mjx-container".equalsIgnoreCase(((IElementNode) tag.parentNode()).name())) { // Cast to IElementNode
            log.info("Handling MathJax SVG.");
            return new MjxSvgTagWorker(tag, context);
        }
        return super.getCustomTagWorker(tag, context);
    }
}
