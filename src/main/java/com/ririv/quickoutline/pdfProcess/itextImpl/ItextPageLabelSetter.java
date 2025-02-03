package com.ririv.quickoutline.pdfProcess.itextImpl;

import com.itextpdf.kernel.pdf.PageLabelNumberingStyle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.ririv.quickoutline.pdfProcess.PageLabel;
import com.ririv.quickoutline.pdfProcess.PageLabelSetter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


// https://kb.itextpdf.com/itext/page-labels
public class ItextPageLabelSetter implements PageLabelSetter<PageLabelNumberingStyle> {

    public static void main(String[] args) throws IOException {
        final String SRC = "";
        final String DEST = "./target/sandbox/objects/page_labels.pdf";

        List<PageLabel> labelList = new ArrayList<>();
        labelList.add(new PageLabel(1, PageLabel.PageLabelNumberingStyle.UPPERCASE_LETTERS,null,null));
        File file = new File(DEST);
        file.getParentFile().mkdirs();
        new ItextPageLabelSetter().setPageLabels(SRC, DEST, labelList);
    }

    public void setPageLabels(String srcFilePath, String destFilePath, List<PageLabel> labelList) throws IOException {
        PdfDocument pdfDoc = new PdfDocument(new PdfReader(srcFilePath), new PdfWriter(destFilePath));

        for(PageLabel label: labelList){
            int pageNum = label.pageNum();
            PageLabelNumberingStyle numberingStyle = mapPageLabelNumberingStyle(label.numberingStyle());

            String labelPrefix = label.labelPrefix();
            Integer firstPage = label.firstPage();

            if (label.firstPage() ==null){
                pdfDoc.getPage(pageNum).setPageLabel(numberingStyle, labelPrefix);
            } else {
                pdfDoc.getPage(pageNum).setPageLabel(numberingStyle, labelPrefix, firstPage);
            }
        }

    }

    @Override
    public PageLabelNumberingStyle mapPageLabelNumberingStyle(PageLabel.PageLabelNumberingStyle numberingStyle) {

        switch (numberingStyle) {
            case PageLabel.PageLabelNumberingStyle.DECIMAL_ARABIC_NUMERALS -> {
                return PageLabelNumberingStyle.DECIMAL_ARABIC_NUMERALS;
            }
            case PageLabel.PageLabelNumberingStyle.LOWERCASE_ROMAN_NUMERALS -> {
                return PageLabelNumberingStyle.LOWERCASE_ROMAN_NUMERALS;
            }
            case PageLabel.PageLabelNumberingStyle.UPPERCASE_ROMAN_NUMERALS-> {
                return PageLabelNumberingStyle.UPPERCASE_ROMAN_NUMERALS;
            }
            case PageLabel.PageLabelNumberingStyle.LOWERCASE_LETTERS-> {
                return PageLabelNumberingStyle.LOWERCASE_LETTERS;
            }
            case PageLabel.PageLabelNumberingStyle.UPPERCASE_LETTERS-> {
                return PageLabelNumberingStyle.UPPERCASE_LETTERS;
            }
            case null -> {
                return null;
            }
        }
    }

}
