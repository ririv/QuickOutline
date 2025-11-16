package com.ririv.quickoutline.pdfProcess.itextImpl;

import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.HtmlConverter;
import com.itextpdf.layout.font.FontProvider;
import com.ririv.quickoutline.service.DownloadEvent;
import com.ririv.quickoutline.service.FontManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class ItextHtmlConverter implements com.ririv.quickoutline.pdfProcess.HtmlConverter {

    private static final Logger log = LoggerFactory.getLogger(ItextHtmlConverter.class);
    private final FontManager fontManager  = new FontManager();

    @Override
    public void convertToPdf(String html,
                             String baseUri,
                             OutputStream outputStream,
                             Consumer<DownloadEvent> onEvent) {
        try {
            ConverterProperties properties = new ConverterProperties();

            // 这里直接拿 FontProvider，不再用 Optional.ifPresent
            properties.setFontProvider(fontManager.getFontProvider(onEvent));

            // Set base URI for resolving relative resources (e.g. images)
            if (baseUri != null && !baseUri.isBlank()) {
                properties.setBaseUri(baseUri);
            }

            HtmlConverter.convertToPdf(html, outputStream, properties);
        } catch (IOException e) {
            // 不在这一层做 UI 文案，交给 MarkdownService 的 catch 统一提示
            throw new RuntimeException("Failed to setup font provider for HTML to PDF conversion", e);
        }
    }
}
