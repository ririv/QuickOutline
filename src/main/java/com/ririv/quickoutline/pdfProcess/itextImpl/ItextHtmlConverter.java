package com.ririv.quickoutline.pdfProcess.itextImpl;

import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.HtmlConverter;
import com.ririv.quickoutline.service.DownloadEvent;
import com.ririv.quickoutline.service.FontManager;

import java.io.IOException;
import java.io.OutputStream;
import java.util.function.Consumer;

public class ItextHtmlConverter implements com.ririv.quickoutline.pdfProcess.HtmlConverter {

    private final FontManager fontManager  = new FontManager();

    @Override
    public void convertToPdf(String html,
                             String baseUri,
                             OutputStream outputStream,
                             Consumer<DownloadEvent> onEvent) {
        ConverterProperties properties = new ConverterProperties();
        properties.setCssApplierFactory(new CustomCssApplierFactory());
        properties.setTagWorkerFactory(new CustomTagWorkerFactory());

        // 这里直接拿 FontProvider，不再用 Optional.ifPresent
        properties.setFontProvider(fontManager.getFontProvider(onEvent));

        // Set base URI for resolving relative resources (e.g. images)
        if (baseUri != null && !baseUri.isBlank()) {
            properties.setBaseUri(baseUri);
        }
        HtmlConverter.convertToPdf(html, outputStream, properties);
    }
}
