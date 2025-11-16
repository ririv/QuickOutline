package com.ririv.quickoutline.pdfProcess.itextImpl;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.ririv.quickoutline.pdfProcess.PdfPageGenerator;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class iTextPdfPageGenerator implements PdfPageGenerator {


    @Override
    public void generateAndInsertPage(String srcFile,
                                      String destFile,
                                      byte[] pdfPageBytes,
                                      int insertPos) throws IOException {

        // Merge the provided markdown PDF bytes with the source PDF
        PdfDocument srcDoc = new PdfDocument(new PdfReader(srcFile));
        PdfDocument destDoc = new PdfDocument(new PdfWriter(destFile));

        // Copy pages from original document that come before the insertion point
        if (insertPos > 1 && insertPos <= srcDoc.getNumberOfPages() + 1) {
            srcDoc.copyPagesTo(1, insertPos - 1, destDoc);
        }

    // Copy pages from the temporary markdown PDF
    PdfDocument tempDoc = new PdfDocument(new PdfReader(new ByteArrayInputStream(pdfPageBytes)));
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
