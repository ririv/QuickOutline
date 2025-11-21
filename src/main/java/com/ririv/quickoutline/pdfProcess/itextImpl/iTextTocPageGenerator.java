package com.ririv.quickoutline.pdfProcess.itextImpl;

import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.kernel.pdf.action.PdfAction;
import com.itextpdf.kernel.pdf.canvas.draw.DottedLine;
import com.itextpdf.kernel.pdf.event.AbstractPdfDocumentEvent;
import com.itextpdf.kernel.pdf.event.AbstractPdfDocumentEventHandler;
import com.itextpdf.kernel.pdf.event.PdfDocumentEvent;
import com.itextpdf.kernel.pdf.navigation.PdfExplicitDestination;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.AreaBreak;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Tab;
import com.itextpdf.layout.element.TabStop;
import com.itextpdf.layout.font.FontProvider;
import com.itextpdf.layout.properties.AreaBreakType;
import com.itextpdf.layout.properties.TabAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.ririv.quickoutline.model.Bookmark;
import com.ririv.quickoutline.pdfProcess.PageLabel.PageLabelNumberingStyle;
import com.ririv.quickoutline.pdfProcess.TocPageGenerator;
import com.ririv.quickoutline.service.FontManager;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static com.itextpdf.kernel.numbering.EnglishAlphabetNumbering.toLatinAlphabetNumberLowerCase;
import static com.itextpdf.kernel.numbering.EnglishAlphabetNumbering.toLatinAlphabetNumberUpperCase;
import static com.itextpdf.kernel.numbering.RomanNumbering.toRomanLowerCase;
import static com.itextpdf.kernel.numbering.RomanNumbering.toRomanUpperCase;

public class iTextTocPageGenerator implements TocPageGenerator {

    private static final Logger log = LoggerFactory.getLogger(iTextTocPageGenerator.class);
    private final FontManager fontManager;

    @Inject
    public iTextTocPageGenerator(FontManager fontManager) {
        this.fontManager = fontManager;
    }

    private PdfFont getPdfFont(Consumer<String> onMessage, Consumer<String> onError) throws IOException {
        // 使用 FontManager 的下载逻辑，确保字体存在
        fontManager.ensureFontsAreAvailable(event -> {
            if (event == null) return;
            switch (event.type()) {
                case START -> onMessage.accept("正在下载字体: " + event.resourceName() + "...");
                case SUCCESS -> onMessage.accept("字体 " + event.resourceName() + " 下载完成。");
                case ERROR -> onError.accept("字体下载失败: " + event.resourceName() +
                        (event.detail() == null ? "" : " - " + event.detail()));
                case PROGRESS -> { /* 目前不需要进度粒度 */ }
            }
        });

        // 获取 FontProvider (里面包含了加载好的字体)
        FontProvider fontProvider = fontManager.getFontProvider(null); // null callback as we already ensured availability

        // 从 FontProvider 中取出一个可用的字体
        // 由于 FontManager 加载了 Source Han Sans，我们直接拿 FontSet 里的第一个
        // 这是一个简化的做法，为了兼容 createFont 的返回值类型
        if (!fontProvider.getFontSet().getFonts().isEmpty()) {
            // 拿到 FontInfo，然后转换成 PdfFont
            // 注意：FontProvider.getPdfFont() 可能会抛异常，需要处理
            try {
                return fontProvider.getPdfFont(fontProvider.getFontSet().getFonts().iterator().next());
            } catch (Exception e) {
                log.warn("Failed to get font from provider, fallback to default", e);
            }
        }

        // 兜底：万一没取到，尝试加载 Helvetica (不支持中文) 或直接加载文件
        // 为了保险，我们还是直接加载文件路径 (这是最稳的，因为 FontManager 保证了文件存在)
        // 假设 FontManager 下载的是 .otf/.ttf
        String fontPath = System.getProperty("user.home") + "/.quickoutline/fonts/SourceHanSansSC-Regular.otf";
        try {
            return PdfFontFactory.createFont(fontPath, PdfEncodings.IDENTITY_H, PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
        } catch (IOException e) {
            e.printStackTrace();
            // 终极兜底
            return PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA);
        }
    }

    @Override
    public void generateAndInsertToc(String srcFilePath, String destFilePath, String title, int insertPos,
                                     PageLabelNumberingStyle style, List<Bookmark> bookmarks,
                                     Consumer<String> onMessage, Consumer<String> onError) throws IOException {
        PdfDocument pdfDoc = new PdfDocument(new PdfReader(srcFilePath), new PdfWriter(destFilePath));
        // 记录原始文档的页数
        int originalPageNum = pdfDoc.getNumberOfPages();
        Document doc = new Document(pdfDoc);

        PdfFont font = getPdfFont(onMessage, onError);

        if (style != PageLabelNumberingStyle.NONE) {
            pdfDoc.addEventHandler(PdfDocumentEvent.END_PAGE, new PageNumberEventHandler(doc, style, font, insertPos, -1)); // We don't know total pages yet
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
            pdfDoc.movePage(firstTocPageInDoc + i, insertPos + i);
        }

        doc.close();
    }

    @Override
    public void generateTocPagePreview(String title, PageLabelNumberingStyle style, List<Bookmark> bookmarks,
                                       java.io.OutputStream outputStream,
                                       Consumer<String> onMessage, Consumer<String> onError) throws IOException {
        PdfDocument pdfDoc = new PdfDocument(new PdfWriter(outputStream));
        Document doc = new Document(pdfDoc);

        PdfFont font = getPdfFont(onMessage, onError);

        if (style != null && style != PageLabelNumberingStyle.NONE) {
            pdfDoc.addEventHandler(PdfDocumentEvent.END_PAGE, new PageNumberEventHandler(doc, style, font, 1, -1));
        }

        Paragraph titleParagraph = new Paragraph(title)
                .setFont(font)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(20)
                .setMarginBottom(20);
        doc.add(titleParagraph);

        List<TabStop> tabStops = new ArrayList<>();
        tabStops.add(new TabStop(580, TabAlignment.RIGHT, new DottedLine()));

        for (Bookmark bookmark : bookmarks) {
            Paragraph p = new Paragraph()
                    .addTabStops(tabStops)
                    .setFont(font)
                    .setPaddingLeft((bookmark.getLevel() - 1) * 20)
                    .add(bookmark.getTitle())
                    .add(new Tab())
                    .add(String.valueOf(bookmark.getPageNum().orElse(0)));
            doc.add(p);
        }

        doc.close();
    }

    private static class PageNumberEventHandler extends AbstractPdfDocumentEventHandler {
        private final Document doc;
        private final PageLabelNumberingStyle style;
        private final PdfFont font;
        private final int startPageNum;
        private int totalTocPages;

        public PageNumberEventHandler(Document doc, PageLabelNumberingStyle style, PdfFont font, int startPageNum, int totalTocPages) {
            this.doc = doc;
            this.style = style;
            this.font = font;
            this.startPageNum = startPageNum;
            this.totalTocPages = totalTocPages;
        }

        @Override
        public void onAcceptedEvent(AbstractPdfDocumentEvent currentEvent) {
            PdfDocumentEvent docEvent = (PdfDocumentEvent) currentEvent;
            PdfDocument pdfDoc = docEvent.getDocument();
            PdfPage page = docEvent.getPage();
            int pageNum = pdfDoc.getPageNumber(page);

            if (totalTocPages == -1) {
                totalTocPages = pdfDoc.getNumberOfPages();
            }

            // Only add page numbers to the TOC pages
            if (pageNum >= startPageNum && pageNum < startPageNum + totalTocPages) {
                String pageNumberText = formatPageNumber(pageNum - startPageNum + 1);
                if (pageNumberText == null) return;
                Canvas canvas = new Canvas(docEvent.getPage(), page.getPageSize());
                canvas.setFont(font)
                        .setFontSize(10)
                        .showTextAligned(pageNumberText, page.getPageSize().getWidth() / 2, doc.getBottomMargin(), TextAlignment.CENTER)
                        .close();
            }
        }

        private String formatPageNumber(int number) {
            return switch (style) {
                case DECIMAL_ARABIC_NUMERALS -> String.valueOf(number);
                case UPPERCASE_ROMAN_NUMERALS -> toRomanUpperCase(number);
                case LOWERCASE_ROMAN_NUMERALS -> toRomanLowerCase(number);
                case UPPERCASE_LETTERS -> toLatinAlphabetNumberUpperCase(number);
                case LOWERCASE_LETTERS -> toLatinAlphabetNumberLowerCase(number);
                default -> null;
            };
        }
    }
}