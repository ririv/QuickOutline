package com.ririv.quickoutline.pdfProcess;

import java.io.IOException;

public interface MarkdownPageGenerator {

    /**
     * Merge the given markdown PDF bytes into the source PDF at the given position
     * and write the result to destFile.
     */
    void generateAndInsertMarkdownPage(String srcFile,
                                       String destFile,
                                       byte[] markdownPdfBytes,
                                       int insertPos) throws IOException;
}
