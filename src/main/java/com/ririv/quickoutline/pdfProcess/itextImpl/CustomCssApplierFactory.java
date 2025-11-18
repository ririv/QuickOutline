package com.ririv.quickoutline.pdfProcess.itextImpl;

import com.itextpdf.html2pdf.css.CssConstants;
import com.itextpdf.html2pdf.css.apply.ICssApplier;
import com.itextpdf.html2pdf.css.apply.impl.BlockCssApplier;
import com.itextpdf.html2pdf.css.apply.impl.DefaultCssApplierFactory;
import com.itextpdf.html2pdf.css.apply.impl.SpanTagCssApplier;
import com.itextpdf.styledxmlparser.node.IElementNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomCssApplierFactory extends DefaultCssApplierFactory {

    private static final Logger log = LoggerFactory.getLogger(CustomCssApplierFactory.class);

    @Override
    public ICssApplier getCustomCssApplier(IElementNode tag) {
        if ("mjx-container".equalsIgnoreCase(tag.name())) {
            log.info("Handling mjx-container css.");
            return new BlockCssApplier();
        }
        return super.getCustomCssApplier(tag);
    }
}
