package com.ririv.quickoutline.pdfProcess.itextImpl.html2pdf;

import com.itextpdf.html2pdf.css.apply.ICssApplier;
import com.itextpdf.html2pdf.css.apply.impl.BlockCssApplier;
import com.itextpdf.html2pdf.css.apply.impl.DefaultCssApplierFactory;
import com.itextpdf.html2pdf.css.apply.impl.SpanTagCssApplier;
import com.itextpdf.styledxmlparser.node.IElementNode;

public class CustomCssApplierFactory extends DefaultCssApplierFactory {

    @Override
    public ICssApplier getCustomCssApplier(IElementNode tag) {
        if ("mjx-container".equalsIgnoreCase(tag.name())) {
            String display = tag.getAttribute("display");

            if ("true".equalsIgnoreCase(display)) {
                // 块级 Worker -> BlockCssApplier
                return new BlockCssApplier();
            } else {
                // 行内 Worker -> SpanTagCssApplier
                return new SpanTagCssApplier();
            }
        }
        return super.getCustomCssApplier(tag);
    }
}