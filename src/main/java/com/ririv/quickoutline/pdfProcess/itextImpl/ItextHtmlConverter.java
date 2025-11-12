package com.ririv.quickoutline.pdfProcess.itextImpl;

import com.ririv.quickoutline.pdfProcess.HtmlConverter;

import java.io.OutputStream;

public class ItextHtmlConverter implements HtmlConverter {

    @Override
    public void convertToPdf(String html, OutputStream outputStream) {
        com.itextpdf.html2pdf.HtmlConverter.convertToPdf(html, outputStream);
    }
}
