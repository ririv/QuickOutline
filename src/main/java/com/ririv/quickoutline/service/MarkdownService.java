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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.function.Consumer;

@Singleton
public class MarkdownService {

    private static final Logger log = LoggerFactory.getLogger(MarkdownService.class);

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

    public void createMarkdownPage(String srcFile,
                                String destFile,
                                String markdownText,
                                int insertPos,
                                String baseUri,
                                Consumer<String> onMessage,
                                Consumer<String> onError) throws IOException {
        final String safe = markdownText == null ? "" : markdownText;
        Node document = parser.parse(safe);
        String htmlContent = renderer.render(document);
        markdownPageGenerator.generateAndInsertMarkdownPage(
                srcFile, destFile, htmlContent, insertPos, baseUri, onMessage, onError);
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
        long start = System.currentTimeMillis();
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Node document = parser.parse(safe);
            String htmlContent = renderer.render(document);
            if (log.isDebugEnabled()) {
                String snippet = htmlContent.substring(0, Math.min(500, htmlContent.length()));
                log.debug("[MD->PDF] baseUri={}, mdLen={}, htmlLen={}, htmlSnippet={}",
                        baseUri, safe.length(), htmlContent.length(), snippet);
            }
            htmlConverter.convertToPdf(htmlContent, baseUri, baos, onMessage, onError);
            byte[] bytes = baos.toByteArray();
            if (log.isDebugEnabled()) log.debug("PDF bytes={}, cost={}ms", bytes.length, (System.currentTimeMillis()-start));
            return bytes;
        } catch (Exception e) {
            String msg = "Failed to convert Markdown to PDF: " + e.getMessage();
            log.error(msg, e);
            onError.accept(msg);
            throw new RuntimeException(msg, e);
        }
    }

    /**
     * Backwards-compatible overload that converts Markdown to PDF bytes without an explicit baseUri.
     * Relative resources will be resolved without a base directory, which may break image loading.
     */
    public byte[] convertMarkdownToPdfBytes(String markdownText,
                                            Consumer<String> onMessage, Consumer<String> onError) {
        return convertMarkdownToPdfBytes(markdownText, null, onMessage, onError);
    }
}
