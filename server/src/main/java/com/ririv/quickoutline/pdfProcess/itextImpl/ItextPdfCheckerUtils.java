package com.ririv.quickoutline.pdfProcess.itextImpl;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;

import java.io.IOException;

public class ItextPdfCheckerUtils {

    public static boolean checkEncrypted(String srcFilePath) throws IOException {
        PdfReader reader = new PdfReader(srcFilePath);
        PdfDocument doc = new PdfDocument(reader);
        boolean isEncrypted = reader.isEncrypted();
        doc.close();
        return isEncrypted;
    }
}
