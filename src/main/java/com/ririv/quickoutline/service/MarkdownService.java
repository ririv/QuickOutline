package com.ririv.quickoutline.service;

import com.ririv.quickoutline.pdfProcess.HtmlConverter;
import com.ririv.quickoutline.pdfProcess.MarkdownPageGenerator;
import com.ririv.quickoutline.pdfProcess.itextImpl.ItextHtmlConverter;
import com.ririv.quickoutline.pdfProcess.itextImpl.iTextMarkdownPageGenerator;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.function.Consumer;


public class MarkdownService {

    private static final Logger log = LoggerFactory.getLogger(MarkdownService.class);

    private final Parser parser = Parser.builder().build();
    private final HtmlRenderer renderer = HtmlRenderer.builder().build();;
    private final HtmlConverter htmlConverter = new ItextHtmlConverter();
    private final MarkdownPageGenerator markdownPageGenerator = new iTextMarkdownPageGenerator();


    /**
     * Common helper: convert Markdown text to HTML string using the shared parser/renderer.
     */
    private String convertMarkdownToHtml(String markdownText) {
        final String safe = markdownText == null ? "" : markdownText;
        Node document = parser.parse(safe);
        return renderer.render(document);
    }

    private byte[] convertHtmlToPdfBytes(String htmlContent, String baseUri,
                                         Consumer<String> onMessage, Consumer<String> onError) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            htmlConverter.convertToPdf(
                    htmlContent,
                    baseUri,
                    baos,
                    event -> {
                        if (event == null) return;
                        switch (event.getType()) {
                            case START -> onMessage.accept("正在下载字体: " + event.getResourceName() + "...");
                            case SUCCESS -> onMessage.accept("字体 " + event.getResourceName() + " 下载完成。");
                            case ERROR -> onError.accept("字体下载失败: " + event.getResourceName() +
                                    (event.getDetail() == null ? "" : " - " + event.getDetail()));
                            case PROGRESS -> { /* currently unused */ }
                        }
                    }
            );
            return baos.toByteArray();
        } catch (Exception e) {
            String msg = "Failed to convert HTML to PDF: " + e.getMessage();
            log.error(msg, e);
            onError.accept(msg);
            throw new RuntimeException(msg, e);
        }
    }

    /**
     * Converts a Markdown string to a PDF byte array.
     *
     * @param markdownText The Markdown content to convert.
     * @param baseUri      Base URI used to resolve relative resources (images, CSS, etc.).
     *                     Typically the directory of the source PDF, e.g. {@code file:///.../pdfDir/}.
     * @param onMessage    Callback for informational messages (e.g., font download progress).
     * @param onError      Callback for error messages.
     * @return A byte array representing the generated PDF.
     * @throws RuntimeException if the conversion fails.
     */
    public byte[] convertMarkdownToPdfBytes(String markdownText, String baseUri,
                                            Consumer<String> onMessage, Consumer<String> onError) {
        final String safe = markdownText == null ? "" : markdownText;
        if (safe.isBlank()) {
            // 空内容直接返回，不报错，避免误触发失败提示
            if (log.isDebugEnabled()) log.debug("skip empty content");
            return new byte[0];
        }
        try {
            String htmlContent = convertMarkdownToHtml(safe);
            return convertHtmlToPdfBytes(htmlContent, baseUri, onMessage, onError);
        } catch (Exception e) {
            String msg = "Failed to convert Markdown to PDF: " + e.getMessage();
            log.error(msg, e);
            onError.accept(msg);
            throw new RuntimeException(msg, e);
        }
    }

    public void createMarkdownPage(String srcFile,
                                   String destFile,
                                   String markdownText,
                                   int insertPos,
                                   String baseUri,
                                   Consumer<String> onMessage,
                                   Consumer<String> onError) throws IOException {
        byte[] markdownPdfBytes = convertMarkdownToPdfBytes(markdownText, baseUri, onMessage, onError);
        markdownPageGenerator.generateAndInsertMarkdownPage(
                srcFile, destFile, markdownPdfBytes, insertPos);
    }

}
