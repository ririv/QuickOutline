package com.ririv.quickoutline.pdfProcess.itextImpl;

import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.HtmlConverter;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.ririv.quickoutline.pdfProcess.MarkdownPageGenerator;
import com.ririv.quickoutline.service.FontManager;
import jakarta.inject.Inject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.function.Consumer;

public class iTextMarkdownPageGenerator implements MarkdownPageGenerator {

    private final FontManager fontManager;

    @Inject
    public iTextMarkdownPageGenerator(FontManager fontManager) {
        this.fontManager = fontManager;
    }

    @Override
    public void generateAndInsertMarkdownPage(String srcFile, String destFile, String htmlContent, int insertPos,
                                              Consumer<String> onMessage, Consumer<String> onError) throws IOException {
        // Step 1: Convert HTML to a temporary in-memory PDF
        ByteArrayOutputStream tempPdfBaos = new ByteArrayOutputStream();
        ConverterProperties properties = new ConverterProperties();
        fontManager.getFontProvider(onMessage, onError).ifPresent(properties::setFontProvider);
        HtmlConverter.convertToPdf(htmlContent, tempPdfBaos, properties);

        // Step 2: Merge the temporary PDF with the source PDF
        PdfDocument srcDoc = new PdfDocument(new PdfReader(srcFile));
        PdfDocument destDoc = new PdfDocument(new PdfWriter(destFile));

        // Copy pages from original document that come before the insertion point
        if (insertPos > 1 && insertPos <= srcDoc.getNumberOfPages() + 1) {
            srcDoc.copyPagesTo(1, insertPos - 1, destDoc);
        }

        // Copy pages from the temporary markdown PDF
        PdfDocument tempDoc = new PdfDocument(new PdfReader(new ByteArrayInputStream(tempPdfBaos.toByteArray())));
        tempDoc.copyPagesTo(1, tempDoc.getNumberOfPages(), destDoc);
        tempDoc.close();

        // Copy pages from original document that come after the insertion point
        if (insertPos <= srcDoc.getNumberOfPages()) {
            srcDoc.copyPagesTo(insertPos, srcDoc.getNumberOfPages(), destDoc);
        }

        srcDoc.close();
        destDoc.close();
    }
}
