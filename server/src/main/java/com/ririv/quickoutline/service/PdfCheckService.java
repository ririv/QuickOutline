package com.ririv.quickoutline.service;

import com.ririv.quickoutline.exception.EncryptedPdfException;
import com.ririv.quickoutline.pdfProcess.OutlineProcessor;
import com.ririv.quickoutline.pdfProcess.PdfChecker;
import com.ririv.quickoutline.pdfProcess.itextImpl.ItextOutlineProcessor;
import com.ririv.quickoutline.pdfProcess.itextImpl.ItextPdfCheckerImpl;

import java.io.IOException;

public class PdfCheckService {

    private final PdfChecker pdfChecker = new ItextPdfCheckerImpl();
    public void checkOpenFile(String srcFilepath) throws IOException {
        if (srcFilepath.isEmpty()) throw new RuntimeException("PDF路径为空");
        if (pdfChecker.checkEncrypted(srcFilepath)) throw new EncryptedPdfException();
    }

}