package com.ririv.quickoutline.pdfProcess.itextImpl;

import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.event.AbstractPdfDocumentEvent;
import com.itextpdf.kernel.pdf.event.AbstractPdfDocumentEventHandler;
import com.itextpdf.kernel.pdf.event.PdfDocumentEvent;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.properties.TextAlignment;
import com.ririv.quickoutline.model.SectionConfig;
import com.ririv.quickoutline.model.PageLabel.PageLabelNumberingStyle;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas; // Added import

import java.util.function.BiConsumer;

import static com.ririv.quickoutline.pdfProcess.numbering.Numbering.formatPageNumber;

/**
 * Shared EventHandler for drawing headers and footers in iText PDFs.
 * Can be used for both TOC generation and Markdown conversion.
 */
public class HeaderFooterEventHandler extends AbstractPdfDocumentEventHandler {
    private final Document doc;
    private final PageLabelNumberingStyle style;
    private final PdfFont font;
    private final int startPageNum;
    private int totalTocPages;
    private final SectionConfig headerConfig;
    private final SectionConfig footerConfig;

    private static final float DEFAULT_FONT_SIZE = 10f;
    private static final float HEADER_Y_OFFSET = 36f; // From top edge
    private static final float FOOTER_Y_OFFSET = 36f; // From bottom edge
    
    private static final float LINE_WIDTH = 0.5f;
    private static final float LINE_GAP = 5f; // Gap between text and line

    public HeaderFooterEventHandler(Document doc, PageLabelNumberingStyle style, PdfFont font, int startPageNum, int totalTocPages, SectionConfig header, SectionConfig footer) {
        this.doc = doc;
        this.style = style;
        this.font = font;
        this.startPageNum = startPageNum;
        this.totalTocPages = totalTocPages;
        this.headerConfig = header;
        this.footerConfig = footer;
    }

    @Override
    public void onAcceptedEvent(AbstractPdfDocumentEvent currentEvent) {
        PdfDocumentEvent docEvent = (PdfDocumentEvent) currentEvent;
        PdfDocument pdfDoc = docEvent.getDocument();
        PdfPage page = docEvent.getPage();
        int pageNum = pdfDoc.getPageNumber(page);

        if (totalTocPages == -1) {
            // For Markdown or dynamic scenarios, assume all pages if total not specified
            totalTocPages = pdfDoc.getNumberOfPages();
        }

        // Only add page numbers to the TOC pages (or if startPageNum logic applies)
        // For Markdown conversion, startPageNum is usually 1, so this covers all generated pages
        if (pageNum >= startPageNum && pageNum < startPageNum + totalTocPages) {
            // Current page number relative to start (for placeholders like {p})
            int currentRelativePageNum = pageNum - startPageNum + 1;
            String formattedPageNumber = formatPageNumber(style, currentRelativePageNum, "");
            
            Canvas canvas = new Canvas(docEvent.getPage(), page.getPageSize());
            canvas.setFont(font)
                    .setFontSize(DEFAULT_FONT_SIZE);

            // Draw Header
            if (headerConfig != null) {
                drawSectionContent(canvas, page.getPageSize(), headerConfig, true, pageNum, currentRelativePageNum, formattedPageNumber);
                if (headerConfig.drawLine()) {
                    drawLine(page, page.getPageSize(), true);
                }
            }

            // Draw Footer
            if (footerConfig != null) {
                drawSectionContent(canvas, page.getPageSize(), footerConfig, false, pageNum, currentRelativePageNum, formattedPageNumber);
                if (footerConfig.drawLine()) {
                    drawLine(page, page.getPageSize(), false);
                }
            }
            
            canvas.close();
        }
    }

    private void drawLine(PdfPage page, Rectangle pageSize, boolean isHeader) {
        PdfCanvas pdfCanvas = new PdfCanvas(page);
        float y;
        if (isHeader) {
            // Line below header text
            y = pageSize.getTop() - HEADER_Y_OFFSET - LINE_GAP;
        } else {
            // Line above footer text
            y = FOOTER_Y_OFFSET + 10f + LINE_GAP; 
        }

        float x1 = doc.getLeftMargin();
        float x2 = pageSize.getWidth() - doc.getRightMargin();

        pdfCanvas.setStrokeColor(ColorConstants.BLACK)
              .setLineWidth(LINE_WIDTH)
              .moveTo(x1, y)
              .lineTo(x2, y)
              .stroke();
    }

    private void drawSectionContent(Canvas canvas, Rectangle pageSize, SectionConfig config, boolean isHeader, 
                                    int absolutePageNum, int relativePageNum, String formattedPageNumber) {
        
        float y;
        if (isHeader) {
            y = pageSize.getTop() - HEADER_Y_OFFSET;
        } else {
            y = FOOTER_Y_OFFSET;
        }

        boolean isOddPage = absolutePageNum % 2 == 1; // Assuming 1-indexed for physical pages
        float leftMargin = doc.getLeftMargin();
        float rightMargin = doc.getRightMargin();
        float pageWidth = pageSize.getWidth();

        // Helper to process and draw text
        BiConsumer<String, TextAlignment> drawText = (text, alignment) -> {
            if (text == null || text.isBlank()) return;
            String processedText = text.replace("{p}", formattedPageNumber != null ? formattedPageNumber : "");

            float x;
            switch (alignment) {
                case LEFT:
                    x = leftMargin;
                    break;
                case CENTER:
                    x = pageWidth / 2;
                    break;
                case RIGHT:
                    x = pageWidth - rightMargin;
                    break;
                default:
                    x = leftMargin;
            }
            canvas.showTextAligned(processedText, x, y, alignment);
        };

        // Draw Left, Center, Right
        drawText.accept(config.left(), TextAlignment.LEFT);
        drawText.accept(config.center(), TextAlignment.CENTER);
        drawText.accept(config.right(), TextAlignment.RIGHT);

        // Draw Inner/Outer
        if (isOddPage) { // Right-hand page
            drawText.accept(config.inner(), TextAlignment.LEFT); // Inner is Left
            drawText.accept(config.outer(), TextAlignment.RIGHT); // Outer is Right
        } else { // Left-hand page
            drawText.accept(config.inner(), TextAlignment.RIGHT); // Inner is Right
            drawText.accept(config.outer(), TextAlignment.LEFT); // Outer is Left
        }
    }

}
