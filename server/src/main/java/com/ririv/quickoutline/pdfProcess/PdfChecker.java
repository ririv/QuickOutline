package com.ririv.quickoutline.pdfProcess;

import java.io.IOException;

public interface PdfChecker {
    boolean checkEncrypted(String srcFilePath) throws IOException;
}
