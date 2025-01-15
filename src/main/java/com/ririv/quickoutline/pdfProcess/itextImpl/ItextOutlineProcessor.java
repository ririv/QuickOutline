package com.ririv.quickoutline.pdfProcess.itextImpl;//package com.ririv.contents.utils.impl;


import com.itextpdf.kernel.pdf.*;
import com.itextpdf.kernel.pdf.navigation.PdfExplicitDestination;
import com.ririv.quickoutline.exception.BookmarkFormatException;
import com.ririv.quickoutline.model.Bookmark;
import com.ririv.quickoutline.pdfProcess.OutlineProcessor;
import com.ririv.quickoutline.pdfProcess.PdfViewScaleType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static com.ririv.quickoutline.model.Bookmark.buildLine;


public class ItextOutlineProcessor implements OutlineProcessor {

    private static final Logger log = LoggerFactory.getLogger(ItextOutlineProcessor.class);

    //    如果rootBookmark没有Children，即之前的text为空（当然这种情况已在Controller中被排除）
//    list.clear()没有起作用（不知道原因），最终目录没有影响，怀疑原因是没有写入操作。
    @Override
    public void setContents(Bookmark rootBookmark, String srcFilePath, String destFilePath, PdfViewScaleType scaleType) throws IOException {

        PdfDocument srcDoc = new PdfDocument(new PdfReader(srcFilePath), new PdfWriter(destFilePath));

        /*
         */
        PdfOutline rootOutline = srcDoc.getOutlines(false);
        rootOutline.getAllChildren().clear();
//        rootOutline.removeOutline();

        bookmarkToOutlines(rootBookmark, rootOutline, srcDoc, scaleType);

        srcDoc.close();

    }


    //合并了上面两个函数
    private void bookmarkToOutlines(Bookmark rootBookmark, PdfOutline rootOutline, PdfDocument srcDoc, PdfViewScaleType scaleType) {

        //不为根结点时，进行添加操作
        if (!rootBookmark.isRoot()) {

            String title = rootBookmark.getTitle();

            /*
             */
            rootOutline = rootOutline.addOutline(title);

            int pageNumMax = srcDoc.getNumberOfPages();

//            https://kb.itextpdf.com/itext/chapter-6-creating-actions-destinations-and-bookma
            https://kb.itextpdf.com/itext/itext-7-building-blocks-chapter-6-actions-destinat
            if (rootBookmark.getOffsetPageNum().isPresent()) {
                int pageNum = rootBookmark.getOffsetPageNum().get();
                if (pageNum > -1 && pageNum <= pageNumMax) {
                    PdfPage page = srcDoc.getPage(pageNum);
                    float top = srcDoc.getPage(pageNum).getPageSize().getTop();
                    float left = srcDoc.getPage(pageNum).getPageSize().getLeft();
                    PdfExplicitDestination destination = null;
                    switch (scaleType) {
                        case FIT_TO_PAGE -> {
                            destination = PdfExplicitDestination.createFit(page);
                        }
                        case ACTUAL_SIZE -> {
                            destination = PdfExplicitDestination.createXYZ(page, left, top, 1);
                        }
                        case FIT_TO_WIDTH -> {
                            destination = PdfExplicitDestination.createFitH(page, top);
                        }
                        case FIT_TO_HEIGHT -> {
                            destination = PdfExplicitDestination.createFitV(page, left);
                        }
//                        case FIT_TO_BOX -> {
//                            destination = PdfExplicitDestination.createFitR(page, 0, 0, page.getPageSize().getWidth(), top);
//                        }
                        case CUSTOM_SCALE -> {
//                            destination = PdfExplicitDestination.createXYZ(page, 0, top, zoom);
                        }
                        default -> {
                            throw new IllegalStateException("Unexpected value: " + scaleType);
                        }
                    }
                    rootOutline.addDestination(destination);
    /*
                //未知的原因，下面的写法没有效果，调试时发现Destination值为null
                outline.addAction(PdfAction.createGoTo(
                        PdfExplicitDestination.createFitH(srcDoc.getPage(pageNum),
                        srcDoc.getPage(pageNum).getPageSize().getTop())));
    */
                } else {
                    srcDoc.close();
                    throw new BookmarkFormatException(String.format(
                            "添加页码错误\n\"%s  %d\" 偏移后的页码超过最大页数或为负数\n偏移后的页码范围应为: 0 ~ %d",
                            title, pageNum, pageNumMax));
                }
            }

        }

        if (!rootBookmark.getChildren().isEmpty()) {
            for (Bookmark subBookmark : rootBookmark.getChildren()) {
                bookmarkToOutlines(subBookmark, rootOutline, srcDoc, scaleType);

            }

        }
    }

    //https://kb.itextpdf.com/itext/how-to-create-hierarchical-bookmarks
    @Override
    public String getContents(String srcFilePath, int offset) throws IOException {

        PdfDocument srcDoc = new PdfDocument(new PdfReader(srcFilePath));
        PdfOutline rootOutline = srcDoc.getOutlines(false);
        PdfNameTree nameTree = srcDoc.getCatalog().getNameTree(PdfName.Dests);
//        nameTree.getNames().forEach((p, q) -> System.out.println(p + "  " + q + "\n"));
        StringBuilder text = new StringBuilder();

//         if (rootOutline.getDestination() != null) {

        if (rootOutline == null) {
            log.info("The doc has no outline");
            return "";
        }
        else outlines2Text(rootOutline, text, offset, 1, nameTree, srcDoc);

//        }
        srcDoc.close(); //记得关闭

        return text.toString();
    }

//    https://kb.itextpdf.com/itext/removing-items-from-a-pdf-s-outline-tree
    public void deleteContents(String srcFile, String destFile) throws IOException{
        PdfDocument srcDoc = new PdfDocument(new PdfReader(srcFile), new PdfWriter(destFile));

        srcDoc.getOutlines(true).removeOutline();
        srcDoc.close();
    };


    private void outlines2Text(PdfOutline outlines, StringBuilder text, int offset, int level, PdfNameTree nameTree, PdfDocument srcDoc) {

        if (outlines.getAllChildren() != null) {
            for (PdfOutline child : outlines.getAllChildren()) {
//             Note: 这里返回类型为PdfObject，但调用PdfObject.getType()发现返回为3，查看源码发现3对应Dictionary，因此可以放心将其强制转换为PdfDictionary
//             names参数是负责解决指定目的地的参数，是正确获取页码所必需的，因为PDF可能包含明确的和命名的目的地，要获取参照上面
                String pageNumStr;
                if (child.getDestination() != null){
                    int pageNum = srcDoc.getPageNumber((PdfDictionary) child.getDestination().getDestinationPage(nameTree));
                    pageNum = pageNum - offset; //原始页码
                    pageNumStr = Integer.toString(pageNum);
                } else {
                    pageNumStr = "";
                }

                buildLine(text, level, child.getTitle(), pageNumStr);

                outlines2Text(child, text, offset, level + 1, nameTree, srcDoc);
            }
        }
    }

}
