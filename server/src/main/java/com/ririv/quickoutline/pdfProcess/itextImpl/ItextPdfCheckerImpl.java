package com.ririv.quickoutline.pdfProcess.itextImpl;

import com.ririv.quickoutline.pdfProcess.PdfChecker;

import java.io.IOException;

public class ItextPdfCheckerImpl implements PdfChecker {

    @Override
    public boolean checkEncrypted(String srcFilePath) throws IOException {
        return ItextPdfCheckerUtils.checkEncrypted(srcFilePath);
    }

}
