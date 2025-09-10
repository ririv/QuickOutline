package com.ririv.quickoutline.pdfProcess.itextImpl;

import com.itextpdf.kernel.pdf.PageLabelNumberingStyle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.ririv.quickoutline.pdfProcess.PageLabel;
import com.ririv.quickoutline.pdfProcess.PageLabelProcessor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.ririv.quickoutline.utils.PathUtils.getUserHomePath;


// https://kb.itextpdf.com/itext/page-labels
public class ItextPageLabelProcessor implements PageLabelProcessor<PageLabelNumberingStyle> {

    public static void main(String[] args) throws IOException {
        final String SRC = getUserHomePath() +"/Downloads/统计学习方法_第2版.pdf";
        final String DEST = "./tmp/page_labels_example.pdf";
        System.out.println("Source file: " + SRC);

        List<PageLabel> labelList = new ArrayList<>();
        // 不从第一页开始设置页码标签，则后面的规则会失效。
        labelList.add(new PageLabel(1, PageLabel.PageLabelNumberingStyle.UPPERCASE_LETTERS,"T",3));
        labelList.add(new PageLabel(3, PageLabel.PageLabelNumberingStyle.UPPERCASE_ROMAN_NUMERALS,"G",3));
        File file = new File(DEST);
        file.getParentFile().mkdirs();
        new ItextPageLabelProcessor().setPageLabels(SRC, DEST, labelList);

    }

    public String[] setPageLabels(String srcFilePath, String destFilePath, List<PageLabel> labelList) throws IOException {
        PdfDocument pdfDoc = new PdfDocument(new PdfReader(srcFilePath), new PdfWriter(destFilePath));

        // 不从第一页开始设置页码标签，则后面的规则会失效。
        pdfDoc.getPage(1).setPageLabel(
                PageLabelNumberingStyle.DECIMAL_ARABIC_NUMERALS, // 标准数字
                null, // 无前缀
                1     // 从1开始
        );

        for(PageLabel label: labelList){
            int pageNum = label.pageNum();
            PageLabelNumberingStyle numberingStyle = mapPageLabelNumberingStyle(label.numberingStyle());

            String labelPrefix = label.labelPrefix();
            Integer firstPage = label.firstPage();

            if (label.firstPage() == null){
                pdfDoc.getPage(pageNum).setPageLabel(numberingStyle, labelPrefix);
            } else {
                pdfDoc.getPage(pageNum).setPageLabel(numberingStyle, labelPrefix, firstPage);
            }
        }
        pdfDoc.close();
        return getPageLabels(destFilePath);
    }

    @Override
    /**
     * 从PDF文件中获取所有页面的页面标签。
     *
     * @param srcFilePath PDF文件的路径。
     * @return 一个包含所有页面标签的字符串数组。
     * 如果文档没有自定义标签，则返回与页码对应的数字字符串数组。
     * @throws IOException 当读取文件失败时抛出。
     */

    // 注：关于PDF页面标签显示的问题，纯字母的页码，阅读器会显示为，aaa，bbb，ccc这样的页码
    // 但实际上我们获取的页面标签，非重复字母，这应该不是本程序的问题，只是阅读器显示的逻辑不同
    public String[] getPageLabels(String src) throws IOException {
        PdfReader reader = new PdfReader(src);
        PdfDocument pdfDoc = new PdfDocument(reader);
        String[] pageLabels = pdfDoc.getPageLabels();
        reader.close();
        return pageLabels;
    }

    @Override
    public PageLabelNumberingStyle mapPageLabelNumberingStyle(PageLabel.PageLabelNumberingStyle numberingStyle) {
        if (numberingStyle == null) return null;
        return switch (numberingStyle) {
            case DECIMAL_ARABIC_NUMERALS -> PageLabelNumberingStyle.DECIMAL_ARABIC_NUMERALS;
            case LOWERCASE_ROMAN_NUMERALS -> PageLabelNumberingStyle.LOWERCASE_ROMAN_NUMERALS;
            case UPPERCASE_ROMAN_NUMERALS -> PageLabelNumberingStyle.UPPERCASE_ROMAN_NUMERALS;
            case LOWERCASE_LETTERS -> PageLabelNumberingStyle.LOWERCASE_LETTERS;
            case UPPERCASE_LETTERS -> PageLabelNumberingStyle.UPPERCASE_LETTERS;
            case NONE -> null;
        };
    }
}