package com.ririv.quickoutline.pdfProcess.itextImpl.html2pdf;

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
        if ("mjx-container".equalsIgnoreCase(tag.name())) {
            String display = tag.getAttribute("display");

            // 【核心分流】
            if ("true".equalsIgnoreCase(display)) {
                // 块级 -> MjxBlockContainerTagWorker (Div)
                return new MjxBlockContainerTagWorker(tag, context);
            } else {
                // 行内 -> MjxInlineContainerTagWorker (Span)
                return new MjxInlineContainerTagWorker(tag, context);
            }
        }

        // ... SVG 拦截逻辑保持不变 ...
        if (SvgConstants.Tags.SVG.equalsIgnoreCase(tag.name())
                && tag.parentNode() instanceof IElementNode
                && "mjx-container".equalsIgnoreCase(((IElementNode) tag.parentNode()).name())) {
            return new MjxSvgTagWorker(tag, context);
        }

        return super.getCustomTagWorker(tag, context);
    }
}