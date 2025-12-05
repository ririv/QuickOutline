package com.ririv.quickoutline.service;

import com.ririv.quickoutline.exception.EncryptedPdfException;
import com.ririv.quickoutline.pdfProcess.OutlineProcessor;
import com.ririv.quickoutline.pdfProcess.itextImpl.ItextOutlineProcessor;

import java.io.IOException;

public class PdfCheckService {

    private final OutlineProcessor outlineProcessor = new ItextOutlineProcessor();
    public void checkOpenFile(String srcFilepath) throws IOException {
        if (srcFilepath.isEmpty()) throw new RuntimeException("PDF路径为空");
        if (outlineProcessor.checkEncrypted(srcFilepath)) throw new EncryptedPdfException();
    }

}