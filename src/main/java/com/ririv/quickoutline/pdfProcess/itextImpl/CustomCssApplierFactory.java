package com.ririv.quickoutline.pdfProcess.itextImpl;

import com.itextpdf.html2pdf.css.apply.ICssApplier;
import com.itextpdf.html2pdf.css.apply.impl.BlockCssApplier;
import com.itextpdf.html2pdf.css.apply.impl.DefaultCssApplierFactory;
import com.itextpdf.styledxmlparser.node.IElementNode;
import com.itextpdf.svg.SvgConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomCssApplierFactory extends DefaultCssApplierFactory {

    private static final Logger log = LoggerFactory.getLogger(CustomCssApplierFactory.class);

    @Override
    public ICssApplier getCustomCssApplier(IElementNode tag) {
        if ("mjx-container".equalsIgnoreCase(tag.name())) {
            log.info("Handling mjx-container css with BlockCssApplier.");
            return new BlockCssApplier();
        }
        return super.getCustomCssApplier(tag);
    }
}
