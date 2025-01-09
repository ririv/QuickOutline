package com.ririv.quickoutline.pdfProcess.itextImpl;

import org.junit.jupiter.api.Test;

import java.io.IOException;

class TOCExtractorTest {

    @Test
    void extract() throws IOException {
        TOCExtractor tocExtractor = new TOCExtractor("src/test/resources/contents.pdf");
        System.out.println(tocExtractor.extract());
    }

    @Test
    void recognize() {

    }
}