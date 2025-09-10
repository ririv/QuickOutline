package com.ririv.quickoutline.pdfProcess.itextImpl;

import com.itextpdf.kernel.pdf.PageLabelNumberingStyle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.AreaBreak;
import com.itextpdf.layout.element.Paragraph;
import org.junit.jupiter.api.Test;

import java.io.IOException;

class ItextPageLabelProcessorTest {

    String SRC = "tmp/page_labels_from_page_3.pdf";

    public static void main(String[] args) throws IOException {

        String dest = "tmp/page_labels_from_page_3.pdf";
        PdfWriter writer = new PdfWriter(dest);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc);


        // 添加足够多的页面（例如5页）
        document.add(new Paragraph("这是第一页。"));
        document.add(new AreaBreak());
        document.add(new Paragraph("这是第二页。"));
        document.add(new AreaBreak());
        document.add(new Paragraph("这是第三页。"));
        document.add(new AreaBreak());
        document.add(new Paragraph("这是第四页。"));
        document.add(new AreaBreak());
        document.add(new Paragraph("这是第五页。"));

        // 在设置页码标签前，可以先检查总页数，确保程序不会出错
        int totalPages = pdfDoc.getNumberOfPages();
        System.out.println("文档总页数: " + totalPages);

        if (totalPages >= 3) {
            // 不从第一页开始设置页码标签，则后面的规则会失效。
            pdfDoc.getPage(1).setPageLabel(
                    PageLabelNumberingStyle.UPPERCASE_LETTERS, // 数字样式
                    "S1-",                                        // 标签前缀
                    1                                             // 从 1 开始计数
            );

            // 核心代码：从第三页开始设置页码标签
            // 1. 样式：使用阿拉伯数字
            // 2. 前缀："正文-"
            // 3. 起始数字：从 1 开始

            pdfDoc.getPage(3).setPageLabel(
                    PageLabelNumberingStyle.DECIMAL_ARABIC_NUMERALS, // 数字样式
                    "T1-",                                        // 标签前缀
                    1                                             // 从 1 开始计数
            );
            System.out.println("已成功为第三页及之后的页面设置页码标签。");
        } else {
            System.out.println("文档页数不足3页，无法设置页码标签。");
        }



        document.close();
    }

    @Test
    void getPageLabels() {

        try{
            String[] labels = new ItextPageLabelProcessor().getPageLabels(SRC);
            for (int i = 0; i < labels.length; i++) {
                System.out.println("Page " + (i + 1) + ": " + labels[i]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}