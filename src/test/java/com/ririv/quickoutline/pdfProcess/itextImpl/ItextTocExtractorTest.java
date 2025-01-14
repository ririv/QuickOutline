package com.ririv.quickoutline.pdfProcess.itextImpl;

import org.junit.jupiter.api.Test;

import java.io.IOException;

class ItextTocExtractorTest {

    @Test
    void extract() throws IOException {
        ItextTocExtractor itextTocExtractor = new ItextTocExtractor("src/test/resources/contents.pdf");
        System.out.println(itextTocExtractor.extract());
    }

    @Test
    void recognize() {

    }
}