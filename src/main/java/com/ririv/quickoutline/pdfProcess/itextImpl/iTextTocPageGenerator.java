package com.ririv.quickoutline.pdfProcess.itextImpl;

import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.action.PdfAction;
import com.itextpdf.kernel.pdf.canvas.draw.DottedLine;
import com.itextpdf.kernel.pdf.navigation.PdfExplicitDestination;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.AreaBreak;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Tab;
import com.itextpdf.layout.element.TabStop;
import com.itextpdf.layout.properties.AreaBreakType;
import com.itextpdf.layout.properties.TabAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.ririv.quickoutline.model.Bookmark;
import com.ririv.quickoutline.pdfProcess.TocPageGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class iTextTocPageGenerator implements TocPageGenerator {

    private static final Logger log = LoggerFactory.getLogger(iTextTocPageGenerator.class);

    @Override
    public void generateAndInsertToc(String srcFilePath, String destFilePath, String title, List<Bookmark> bookmarks) throws IOException {
        PdfDocument pdfDoc = new PdfDocument(new PdfReader(srcFilePath), new PdfWriter(destFilePath));
        // 记录原始文档的页数
        int originalPageNum = pdfDoc.getNumberOfPages();
        Document doc = new Document(pdfDoc);

        PdfFont font;
        try {
            String fontResourcePath = "/fonts/default.ttf";
            String defaultFontPath = iTextTocPageGenerator.class.getResource(fontResourcePath).toExternalForm();
            font = PdfFontFactory.createFont(defaultFontPath, PdfEncodings.IDENTITY_H, PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
        } catch (IOException | NullPointerException e) {
            log.warn("未找到字体文件，错误信息：{}",e.getMessage());
            font = PdfFontFactory.createFont(StandardFonts.TIMES_ROMAN);
        }


        // ======================= !!! FINAL, DEFINITIVE FIX !!! =======================
        // 这是一个两步操作，以确保我们总是在文档的末尾添加新页面
        // 1. 首先，显式地将布局引擎的光标移动到现有文档的“最后一页”。
        doc.add(new AreaBreak(AreaBreakType.LAST_PAGE));
        // 2. 然后，再添加一个标准的分页符，这将在最后一页之后创建一个“新的空白页”。
        doc.add(new AreaBreak());
        // 这个两步过程保证了无论 Document 的初始状态如何，目录总是从文档末尾的一个干净页面开始。
        // ==============================================================================

        Paragraph titleParagraph = new Paragraph(title)
                .setFont(font)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(20)
                .setMarginBottom(20);
        doc.add(titleParagraph);

        // 设置制表符
        List<TabStop> tabStops = new ArrayList<>();
        tabStops.add(new TabStop(580, TabAlignment.RIGHT, new DottedLine()));

        // Add TOC entries
        for (Bookmark bookmark : bookmarks) {
            int pageNumInDest = bookmark.getPageNum().orElse(1);
            Paragraph p = new Paragraph()
                    .addTabStops(tabStops)
                    .setFont(font)
                    .setPaddingLeft((bookmark.getLevel() - 1) * 20) // Indent based on level
                    .add(bookmark.getTitle())
                    .add(new Tab())
                    .add(String.valueOf(bookmark.getPageNum().orElse(0)))
                    .setAction(PdfAction.createGoTo(PdfExplicitDestination.createFit(pdfDoc.getPage(pageNumInDest))));
            doc.add(p);
        }

        // 添加完TOC后，计算TOC所占的页数
        int totalPages = pdfDoc.getNumberOfPages();
        int actualTocPages = totalPages - originalPageNum;

        // 将刚刚添加到末尾的整个TOC块移动到文档开头
        // firstTocPageInDoc现在是TOC在当前文档中的起始页码
        int firstTocPageInDoc = totalPages - actualTocPages + 1;
        for (int i = 0; i < actualTocPages; i++) {
            pdfDoc.movePage(firstTocPageInDoc + i, i + 1);
        }

        doc.close();
    }

}
