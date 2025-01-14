package com.ririv.quickoutline.pdfProcess.itextImpl;
//https://kb.itextpdf.com/itext/toc-as-first-page

/*
    This file is part of the iText (R) project.
    Copyright (c) 1998-2023 Apryse Group NV
    Authors: Apryse Software.

    For more information, please contact iText Software at this address:
    sales@itextpdf.com
 */

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.kernel.pdf.action.PdfAction;
import com.itextpdf.kernel.pdf.canvas.draw.DottedLine;
import com.itextpdf.kernel.pdf.navigation.PdfDestination;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.AreaBreak;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Tab;
import com.itextpdf.layout.element.TabStop;
import com.itextpdf.layout.layout.LayoutContext;
import com.itextpdf.layout.layout.LayoutResult;
import com.itextpdf.layout.properties.TabAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.renderer.ParagraphRenderer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;

public class ItextTocConstructor {
    public static final String DEST = "./target/sandbox/bookmarks/table_of_contents.pdf";

    public static final String SRC = "./src/main/resources/txt/tree.txt";

    public static void main(String[] args) throws Exception {
        File file = new File(DEST);
        file.getParentFile().mkdirs();

        new ItextTocConstructor().manipulatePdf(DEST);
    }

    public void manipulatePdf(String dest) throws Exception {
        PdfFont font = PdfFontFactory.createFont(StandardFonts.TIMES_ROMAN);
        PdfFont bold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
        PdfDocument pdfDoc = new PdfDocument(new PdfWriter(dest));
        Document document = new Document(pdfDoc);
        document
                .setTextAlignment(TextAlignment.JUSTIFIED)
                .setFont(font)
                .setFontSize(11);
        List<SimpleEntry<String, SimpleEntry<String, Integer>>> toc = new ArrayList<>();

        // Parse text to PDF
        createPdfWithOutlines(SRC, document, toc, bold);

        // Remove the main title from the table of contents list
        toc.removeFirst();

        // Create table of contents
        document.add(new AreaBreak());
        Paragraph p = new Paragraph("Table of Contents")
                .setFont(bold)
                .setDestination("toc");
        document.add(p);
        List<TabStop> tabStops = new ArrayList<>();
        tabStops.add(new TabStop(580, TabAlignment.RIGHT, new DottedLine()));
        for (SimpleEntry<String, SimpleEntry<String, Integer>> entry : toc) {
            SimpleEntry<String, Integer> text = entry.getValue();
            p = new Paragraph()
                    .addTabStops(tabStops)
                    .add(text.getKey())
                    .add(new Tab())
                    .add(String.valueOf(text.getValue()))
                    .setAction(PdfAction.createGoTo(entry.getKey()));
            document.add(p);
        }

        // Move the table of contents to the first page
        int tocPageNumber = pdfDoc.getNumberOfPages();
        pdfDoc.movePage(tocPageNumber, 1);

        // Add page labels
        pdfDoc.getPage(1).setPageLabel(PageLabelNumberingStyle.UPPERCASE_LETTERS,
                null, 1);
        pdfDoc.getPage(2).setPageLabel(PageLabelNumberingStyle.DECIMAL_ARABIC_NUMERALS,
                null, 1);

        document.close();
    }

    private static void createPdfWithOutlines(String path, Document document,
                                              List<SimpleEntry<String, SimpleEntry<String, Integer>>> toc, PdfFont titleFont) throws Exception {
        PdfDocument pdfDocument = document.getPdfDocument();

        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            boolean title = true;
            int counter = 0;
            PdfOutline outline = null;
            while ((line = br.readLine()) != null) {
                Paragraph p = new Paragraph(line);
                p.setKeepTogether(true);
                if (title) {
                    String name = String.format("title%02d", counter++);
                    outline = createOutline(outline, pdfDocument, line, name);
                    SimpleEntry<String, Integer> titlePage = new SimpleEntry<>(line, pdfDocument.getNumberOfPages());
                    p
                            .setFont(titleFont)
                            .setFontSize(12)
                            .setKeepWithNext(true)
                            .setDestination(name)

                            // Add the current page number to the table of contents list
                            .setNextRenderer(new UpdatePageRenderer(p, titlePage));
                    document.add(p);
                    toc.add(new SimpleEntry<>(name, titlePage));
                    title = false;
                } else {
                    p.setFirstLineIndent(36);
                    if (line.isEmpty()) {
                        p.setMarginBottom(12);
                        title = true;
                    } else {
                        p.setMarginBottom(0);
                    }

                    document.add(p);
                }
            }
        }
    }

    private static PdfOutline createOutline(PdfOutline outline, PdfDocument pdf, String title, String name) {
        if (outline == null) {
            outline = pdf.getOutlines(false);
            outline = outline.addOutline(title);
            outline.addDestination(PdfDestination.makeDestination(new PdfString(name)));
        } else {
            PdfOutline kid = outline.addOutline(title);
            kid.addDestination(PdfDestination.makeDestination(new PdfString(name)));
        }

        return outline;
    }

    private static class UpdatePageRenderer extends ParagraphRenderer {
        protected SimpleEntry<String, Integer> entry;

        public UpdatePageRenderer(Paragraph modelElement, SimpleEntry<String, Integer> entry) {
            super(modelElement);
            this.entry = entry;
        }

        @Override
        public LayoutResult layout(LayoutContext layoutContext) {
            LayoutResult result = super.layout(layoutContext);
            entry.setValue(layoutContext.getArea().getPageNumber());
            return result;
        }
    }
}
