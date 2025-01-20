package com.ririv.quickoutline.pdfProcess.itextImpl;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.kernel.pdf.action.PdfAction;
import com.itextpdf.kernel.pdf.annot.PdfAnnotation;
import com.itextpdf.kernel.pdf.annot.PdfTextAnnotation;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.xobject.PdfFormXObject;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.AreaBreak;
import com.itextpdf.layout.element.Link;
import com.itextpdf.layout.element.Paragraph;

import java.io.File;
import java.io.IOException;

public class ItextPageLabelSetter {


    public static final String DEST = "./target/sandbox/objects/page_labels.pdf";

    public static void main(String[] args) throws IOException {
        File file = new File(DEST);
        file.getParentFile().mkdirs();
        new ItextPageLabelSetter().setPdfLabels(DEST);
    }

    protected void setPdfLabels(String dest) throws IOException {
        PdfDocument pdfDoc = new PdfDocument(new PdfWriter(dest));
        Document doc = new Document(pdfDoc);

        PdfViewerPreferences viewerPreferences = new PdfViewerPreferences();
        viewerPreferences.setPrintScaling(PdfViewerPreferences.PdfViewerPreferencesConstants.NONE);
        pdfDoc.getCatalog().setPageMode(PdfName.UseThumbs);
        pdfDoc.getCatalog().setPageLayout(PdfName.TwoPageLeft);
        pdfDoc.getCatalog().setViewerPreferences(viewerPreferences);

        doc.add(new Paragraph("Hello World"));
        doc.add(new Paragraph("Hello People"));

        doc.add(new AreaBreak());
        PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA);

        // Add the text to the direct content, but not in the right order
        PdfCanvas canvas = new PdfCanvas(pdfDoc.getPage(2));
        canvas.beginText();
        canvas.setFontAndSize(font, 12);
        canvas.moveText(88.66f, 788);
        canvas.showText("ld");
        canvas.moveText(-22f, 0);
        canvas.showText("Wor");
        canvas.moveText(-15.33f, 0);
        canvas.showText("llo");
        canvas.moveText(-15.33f, 0);
        canvas.showText("He");
        canvas.endText();
        PdfFormXObject formXObject = new PdfFormXObject(new Rectangle(250, 25));
        new PdfCanvas(formXObject, pdfDoc).beginText()
                .setFontAndSize(font, 12)
                .moveText(0, 7)
                .showText("Hello People")
                .endText();
        canvas.addXObjectAt(formXObject, 36, 763);

        pdfDoc.setDefaultPageSize(new PageSize(PageSize.A4).rotate());
        doc.add(new AreaBreak());
        doc.add(new Paragraph("Hello World"));

        pdfDoc.setDefaultPageSize(new PageSize(842, 595));
        doc.add(new AreaBreak());
        doc.add(new Paragraph("Hello World"));

        pdfDoc.setDefaultPageSize(PageSize.A4);
        doc.add(new AreaBreak());
        pdfDoc.getLastPage().setCropBox(new Rectangle(10, 70, 525, 755));
        doc.add(new Paragraph("Hello World"));

        doc.add(new AreaBreak());
        pdfDoc.getLastPage().getPdfObject().put(PdfName.UserUnit, new PdfNumber(5));
        doc.add(new Paragraph("Hello World"));

        doc.add(new AreaBreak());
        pdfDoc.getLastPage().setArtBox(new Rectangle(36, 36, 523, 770));
        Paragraph p = new Paragraph("Hello ")
                .add(new Link("World", PdfAction.createURI("http://maps.google.com")));
        doc.add(p);
        PdfAnnotation a = new PdfTextAnnotation(
                new Rectangle(36, 755, 30, 30))
                .setTitle(new PdfString("Example"))
                .setContents("This is a post-it annotation");
        pdfDoc.getLastPage().addAnnotation(a);


        pdfDoc.getPage(1).setPageLabel(PageLabelNumberingStyle.UPPERCASE_LETTERS, null);
        pdfDoc.getPage(3).setPageLabel(PageLabelNumberingStyle.DECIMAL_ARABIC_NUMERALS, null);
        pdfDoc.getPage(4).setPageLabel(PageLabelNumberingStyle.DECIMAL_ARABIC_NUMERALS, "Custom-", 2);

        doc.close();
    }

}
