package com.ririv.quickoutline.service;

import com.ririv.quickoutline.pdfProcess.HtmlConverter;
import com.ririv.quickoutline.pdfProcess.itextImpl.ItextHtmlConverter;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import java.io.ByteArrayOutputStream;
import java.util.function.Consumer;

@Singleton
public class MarkdownService {

    private final Parser parser;
    private final HtmlRenderer renderer;
    private final HtmlConverter htmlConverter;

    @Inject
    public MarkdownService(FontManager fontManager) {
        this.parser = Parser.builder().build();
        this.renderer = HtmlRenderer.builder().build();
        this.htmlConverter = new ItextHtmlConverter(fontManager);
    }

    /**
     * Converts a Markdown string to a PDF byte array.
     *
     * @param markdownText The Markdown content to convert.
     * @param onMessage Callback for informational messages (e.g., font download progress).
     * @param onError   Callback for error messages.
     * @return A byte array representing the generated PDF.
     * @throws RuntimeException if the conversion fails.
     */
    public byte[] convertMarkdownToPdfBytes(String markdownText, Consumer<String> onMessage, Consumer<String> onError) {
        if (markdownText == null || markdownText.isBlank()) {
            return new byte[0];
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Node document = parser.parse(markdownText);
            String htmlContent = renderer.render(document);
            htmlConverter.convertToPdf(htmlContent, baos, onMessage, onError);
            return baos.toByteArray();
        } catch (Exception e) {
            // Wrap checked exceptions in a RuntimeException for use in lambdas/streams
            onError.accept("Failed to convert Markdown to PDF: " + e.getMessage());
            throw new RuntimeException("Failed to convert Markdown to PDF", e);
        }
    }
}
