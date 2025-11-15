package com.ririv.quickoutline.pdfProcess;

import java.io.IOException;
import java.util.function.Consumer;

public interface MarkdownPageGenerator {
    void generateAndInsertMarkdownPage(String srcFile,
                                       String destFile,
                                       String htmlContent,
                                       int insertPos,
                                       String baseUri,
                                       Consumer<String> onMessage,
                                       Consumer<String> onError) throws IOException;
}
