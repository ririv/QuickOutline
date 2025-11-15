package com.ririv.quickoutline.pdfProcess.itextImpl;

import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.HtmlConverter;
import com.ririv.quickoutline.service.FontManager;

import java.io.OutputStream;
import java.util.function.Consumer;

public class ItextHtmlConverter implements com.ririv.quickoutline.pdfProcess.HtmlConverter {

    private final FontManager fontManager  = new FontManager();

    @Override
    public void convertToPdf(String html, String baseUri, OutputStream outputStream,
                             Consumer<String> onMessage, Consumer<String> onError) {
        ConverterProperties properties = new ConverterProperties();
        fontManager.getFontProvider(onMessage, onError).ifPresent(properties::setFontProvider);
        // Set base URI for resolving relative resources (e.g. images)
        if (baseUri != null && !baseUri.isBlank()) {
            properties.setBaseUri(baseUri);
        }

        HtmlConverter.convertToPdf(html, outputStream, properties);
    }
}
