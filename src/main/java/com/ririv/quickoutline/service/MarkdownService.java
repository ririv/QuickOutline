package com.ririv.quickoutline.service;

import com.ririv.quickoutline.pdfProcess.HtmlConverter;
import com.ririv.quickoutline.pdfProcess.itextImpl.ItextHtmlConverter;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import java.io.ByteArrayOutputStream;

public class MarkdownService {

    private final Parser parser;
    private final HtmlRenderer renderer;

    private final HtmlConverter htmlConverter = new ItextHtmlConverter();

    public MarkdownService() {
        this.parser = Parser.builder().build();
        this.renderer = HtmlRenderer.builder().build();
    }

    /**
     * Converts a Markdown string to a PDF byte array.
     *
     * @param markdownText The Markdown content to convert.
     * @return A byte array representing the generated PDF.
     * @throws RuntimeException if the conversion fails.
     */
    public byte[] convertMarkdownToPdfBytes(String markdownText) {
        if (markdownText == null || markdownText.isBlank()) {
            return new byte[0];
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Node document = parser.parse(markdownText);
            String htmlContent = renderer.render(document);
            htmlConverter.convertToPdf(htmlContent, baos);
            return baos.toByteArray();
        } catch (Exception e) {
            // Wrap checked exceptions in a RuntimeException for use in lambdas/streams
            throw new RuntimeException("Failed to convert Markdown to PDF", e);
        }
    }
}
