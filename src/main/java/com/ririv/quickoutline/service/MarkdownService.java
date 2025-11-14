package com.ririv.quickoutline.service;

import com.ririv.quickoutline.pdfProcess.HtmlConverter;
import com.ririv.quickoutline.pdfProcess.MarkdownPageGenerator;
import com.ririv.quickoutline.pdfProcess.itextImpl.ItextHtmlConverter;
import com.ririv.quickoutline.pdfProcess.itextImpl.iTextMarkdownPageGenerator;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.function.Consumer;

@Singleton
public class MarkdownService {

    private final Parser parser;
    private final HtmlRenderer renderer;
    private final HtmlConverter htmlConverter;
    private final MarkdownPageGenerator markdownPageGenerator;

    @Inject
    public MarkdownService(FontManager fontManager) {
        this.parser = Parser.builder().build();
        this.renderer = HtmlRenderer.builder().build();
        this.htmlConverter = new ItextHtmlConverter(fontManager);
        this.markdownPageGenerator = new iTextMarkdownPageGenerator(fontManager);
    }

    public void createMarkdownPage(String srcFile, String destFile, String markdownText, int insertPos, Consumer<String> onMessage, Consumer<String> onError) throws IOException {
        Node document = parser.parse(markdownText);
        String htmlContent = renderer.render(document);
        markdownPageGenerator.generateAndInsertMarkdownPage(srcFile, destFile, htmlContent, insertPos, onMessage, onError);
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
        final String safe = markdownText == null ? "" : markdownText;
        if (safe.isBlank()) {
            // 空内容直接返回，不报错，避免误触发失败提示
            System.out.println("[MarkdownService] skip empty content");
            return new byte[0];
        }
        long start = System.currentTimeMillis();
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Node document = parser.parse(safe);
            String htmlContent = renderer.render(document);
            System.out.println("[MarkdownService] parsed length=" + safe.length() + ", html length=" + htmlContent.length());
            htmlConverter.convertToPdf(htmlContent, baos, onMessage, onError);
            byte[] bytes = baos.toByteArray();
            System.out.println("[MarkdownService] PDF bytes=" + bytes.length + ", cost=" + (System.currentTimeMillis()-start) + "ms");
            return bytes;
        } catch (Exception e) {
            String msg = "Failed to convert Markdown to PDF: " + e.getMessage();
            System.out.println("[MarkdownService] ERROR " + msg);
            onError.accept(msg);
            throw new RuntimeException(msg, e);
        }
    }
}
