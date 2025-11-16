package com.ririv.quickoutline.pdfProcess;

import java.io.IOException;

public interface PdfPageGenerator {

    /**
     * Merge the given markdown PDF bytes into the source PDF at the given position
     * and write the result to destFile.
     */
    void generateAndInsertPage(String srcFile,
                               String destFile,
                               byte[] pdfPageBytes,
                               int insertPos) throws IOException;
}
