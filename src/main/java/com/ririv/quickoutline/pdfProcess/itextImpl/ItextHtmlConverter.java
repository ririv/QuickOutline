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
    public void convertToPdf(String html, OutputStream outputStream, Consumer<String> onMessage, Consumer<String> onError) throws IOException {
        List<Path> fontPaths = fontManager.getFontPaths(onMessage, onError);

        ConverterProperties properties = new ConverterProperties();
        FontProvider fontProvider = new FontProvider();
        for (Path fontPath : fontPaths) {
            fontProvider.addFont(fontPath.toString());
        }
        properties.setFontProvider(fontProvider);
        // Set a default font family that matches the font we added
        properties.setBaseUri("body{ font-family: 'Source Han Sans SC'; }");

        HtmlConverter.convertToPdf(html, outputStream, properties);
    }
}
