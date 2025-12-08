package com.ririv.quickoutline.pdfProcess.itextImpl.html2pdf;

import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.HtmlConverter;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.event.PdfDocumentEvent;
import com.ririv.quickoutline.model.SectionConfig;
import com.ririv.quickoutline.model.PageLabel.PageLabelNumberingStyle;
import com.ririv.quickoutline.pdfProcess.itextImpl.HeaderFooterEventHandler;
import com.ririv.quickoutline.service.DownloadEvent;
import com.ririv.quickoutline.service.FontManager;

import java.io.OutputStream;
import java.util.function.Consumer;

public class ItextHtmlConverter implements com.ririv.quickoutline.pdfProcess.HtmlConverter {

    private final FontManager fontManager  = new FontManager();

    @Override
    public void convertToPdf(String html,
                             String baseUri,
                             OutputStream outputStream,
                             Consumer<DownloadEvent> onEvent) {
        convertToPdf(html, baseUri, outputStream, null, null, onEvent);
    }

    public void convertToPdf(String html,
                             String baseUri,
                             OutputStream outputStream,
                             SectionConfig header,
                             SectionConfig footer,
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

        // If header or footer is present, we need to manually manage PdfDocument to attach event handler
        if (header != null || footer != null) {
            try {
                PdfWriter writer = new PdfWriter(outputStream);
                PdfDocument pdfDoc = new PdfDocument(writer);
                
                // Register Header/Footer handler
                // For Markdown, we usually don't use complex numbering styles from the payload (yet), 
                // so we pass NONE or DECIMAL as default if placeholders are used.
                // Also, we grab the font from the provider if possible, but HeaderFooterEventHandler needs a PdfFont.
                // The FontProvider manages FontSelector strategies.
                // To get a generic font for the header/footer, we might need to pick one.
                // For simplicity, we can try to reuse the font logic or just let iText handle it if we could.
                // But HeaderFooterEventHandler takes a PdfFont.
                // Let's try to get a font from the provider just like in TOC generator.
                // Since ItextHtmlConverter doesn't have easy access to "getPdfFont" helper, 
                // we might need to duplicate that logic or assume a standard font.
                // Actually, let's use the FontManager's provider to get a font.
                
                com.itextpdf.layout.font.FontProvider fp = fontManager.getFontProvider(null);
                com.itextpdf.kernel.font.PdfFont font = null;
                if (!fp.getFontSet().getFonts().isEmpty()) {
                    try {
                        font = fp.getPdfFont(fp.getFontSet().getFonts().iterator().next());
                    } catch (Exception ignored) {}
                }
                
                pdfDoc.addEventHandler(PdfDocumentEvent.END_PAGE, 
                        new HeaderFooterEventHandler(new com.itextpdf.layout.Document(pdfDoc),
                                PageLabelNumberingStyle.DECIMAL_ARABIC_NUMERALS, 
                                font, 1, -1, header, footer));

                HtmlConverter.convertToPdf(html, pdfDoc, properties);
            } catch (Exception e) {
                throw new RuntimeException("Failed to convert with header/footer", e);
            }
        } else {
            HtmlConverter.convertToPdf(html, outputStream, properties);
        }
    }
}
