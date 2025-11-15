package com.ririv.quickoutline.pdfProcess.itextImpl;

import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.HtmlConverter;
import com.itextpdf.layout.font.FontProvider;
import com.ririv.quickoutline.service.FontManager;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

public class ItextHtmlConverter implements com.ririv.quickoutline.pdfProcess.HtmlConverter {

    private final FontManager fontManager;

    public ItextHtmlConverter(FontManager fontManager) {
        this.fontManager = fontManager;
    }

    @Override
    public void convertToPdf(String html, String baseUri, OutputStream outputStream,
                             Consumer<String> onMessage, Consumer<String> onError) throws IOException {
        List<Path> fontPaths = fontManager.getFontPaths(onMessage, onError);

        ConverterProperties properties = new ConverterProperties();
        FontProvider fontProvider = new FontProvider();
        for (Path fontPath : fontPaths) {
            fontProvider.addFont(fontPath.toString());
        }
        properties.setFontProvider(fontProvider);
        // Set base URI for resolving relative resources (e.g. images)
        if (baseUri != null && !baseUri.isBlank()) {
            properties.setBaseUri(baseUri);
        }

        HtmlConverter.convertToPdf(html, outputStream, properties);
    }
}
