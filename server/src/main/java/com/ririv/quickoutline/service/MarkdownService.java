package com.ririv.quickoutline.service;

import com.ririv.quickoutline.pdfProcess.PdfPageGenerator;
import com.ririv.quickoutline.pdfProcess.itextImpl.iTextPdfPageGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MarkdownService {

    private static final Logger log = LoggerFactory.getLogger(MarkdownService.class);

    private final PdfPageGenerator pdfPageGenerator = new iTextPdfPageGenerator();

}