package com.ririv.quickoutline.pdfProcess.itextImpl;//package com.ririv.contents.utils.impl;


import com.itextpdf.kernel.pdf.*;
import com.itextpdf.kernel.pdf.navigation.PdfExplicitDestination;
import com.ririv.quickoutline.entity.Bookmark;
import com.ririv.quickoutline.exception.BookmarkFormatException;
import com.ririv.quickoutline.pdfProcess.PdfProcess;

import java.io.IOException;

import static com.ririv.quickoutline.entity.Bookmark.buildLine;


public class Itext7Process implements PdfProcess {

    @Override
    public void setContents(Bookmark rootBookmark, String srcFile, String destFile) throws IOException {

        PdfDocument srcDoc = new PdfDocument(new PdfReader(srcFile), new PdfWriter(destFile));

        /*
         */
        PdfOutline rootOutline = srcDoc.getOutlines(false);
        rootOutline.getAllChildren().clear();

        bookMarkToOutlines(rootBookmark, rootOutline, srcDoc);

        srcDoc.close();

    }

    /*
     */
//    public PdfOutline addOutline(PdfOutline outline, String title, int pageNum, PdfDocument srcDoc) {
//        PdfOutline current = outline.addOutline(title);
//
//        int pageNumMax = srcDoc.getNumberOfPages();
//
//        if (pageNum > -1 && pageNum <= pageNumMax) {
//            PdfExplicitDestination destination = PdfExplicitDestination.createFitH(srcDoc.getPage(pageNum), srcDoc.getPage(pageNum).getPageSize().getTop());
//            current.addDestination(destination);
//
///*
//            //未知的原因，下面的写法没有效果，调试时发现Destination值为null
//            outline.addAction(PdfAction.createGoTo(
//                    PdfExplicitDestination.createFitH(srcDoc.getPage(pageNum),
//                    srcDoc.getPage(pageNum).getPageSize().getTop())));
//*/
//
//            System.out.println(title + "  " + pageNum);
//            return current;
//        } else {
//            srcDoc.close();
//            throw new BookmarkFormatException(String.format(
//                    "添加页码错误\n\"%s  %d\"的页码超过最大页数或为负数\n页码应为: 0 ~ %d",
//                    title, pageNum, pageNumMax));
//        }
//    }
//
//    private void bookMarkToOutlines(Bookmark rootBookmark, PdfOutline rootOutline, PdfDocument srcDoc) {
//        //不为根结点时且有效时，进行添加操作
//        if (rootBookmark.getLevel() != -1 && !rootBookmark.isInvalid()) {
//            try {
//                rootOutline = addOutline(rootOutline,
//                        rootBookmark.getTitle(),
//                        rootBookmark.getPageNum().orElseThrow(()->
//                                new BookmarkFormatException(String.format(
//                                        "添加页码错误\n\"%s\"无页码",
//                                        rootBookmark.getTitle()),rootBookmark.getIndex())
//                        ),
//                        srcDoc);
//            }
//            catch (BookmarkFormatException e){
//                throw new BookmarkFormatException(e.getMessage(),rootBookmark.getIndex());
//            }
//
//        }
//
//        if (!rootBookmark.getChildren().isEmpty()) {
//            for (Bookmark subBookmark : rootBookmark.getChildren()) {
//                bookMarkToOutlines(subBookmark, rootOutline, srcDoc);
//
//            }
//
//        }
//    }

    //合并了上面两个函数
    private void bookMarkToOutlines(Bookmark rootBookmark, PdfOutline rootOutline, PdfDocument srcDoc) {

        //不为根结点时，进行添加操作
        if (rootBookmark.getLevel() != -1) {

            String title = rootBookmark.getTitle();

            /*
             */
            rootOutline = rootOutline.addOutline(title);

            int pageNumMax = srcDoc.getNumberOfPages();
            int pageNum = rootBookmark.getOffsetPageNum().orElseThrow(() ->{
                    srcDoc.close();
                    return new BookmarkFormatException(String.format(
                            "添加页码错误\n\"%s\"无页码",
                            title), rootBookmark.getIndex());
                    });

            if (pageNum > -1 && pageNum <= pageNumMax) {
                PdfExplicitDestination destination = PdfExplicitDestination.createFitH(srcDoc.getPage(pageNum), srcDoc.getPage(pageNum).getPageSize().getTop());
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
                        title, pageNum, pageNumMax), rootBookmark.getIndex());
            }
        }

        if (!rootBookmark.getChildren().isEmpty()) {
            for (Bookmark subBookmark : rootBookmark.getChildren()) {
                bookMarkToOutlines(subBookmark, rootOutline, srcDoc);

            }

        }
    }


    @Override
    public String getContents(String srcFile, int offset) throws IOException {

        PdfDocument srcDoc = new PdfDocument(new PdfReader(srcFile));
        PdfOutline rootOutline = srcDoc.getOutlines(false);
        PdfNameTree nameTree = srcDoc.getCatalog().getNameTree(PdfName.Dests);
//        nameTree.getNames().forEach((p, q) -> System.out.println(p + "  " + q + "\n"));
        StringBuilder text = new StringBuilder();

//         if (rootOutline.getDestination() != null) {

        if (rootOutline == null) {
            System.out.println("The doc has no outline");
            return "";
        }
        else outlines2Text(rootOutline, text, offset, 0, nameTree, srcDoc);

//        }
        srcDoc.close(); //记得关闭

        return text.toString();
    }


    private void outlines2Text(PdfOutline outlines, StringBuilder text, int offset, int level, PdfNameTree nameTree, PdfDocument srcDoc) {

        if (outlines.getAllChildren() != null) {
            for (PdfOutline child : outlines.getAllChildren()) {
                /*
                 */
                int pageNum = srcDoc.getPageNumber((PdfDictionary) child.getDestination().getDestinationPage(nameTree));
                pageNum = pageNum - offset; //原始页码

                buildLine(text, level, child.getTitle(), Integer.toString(pageNum));

                outlines2Text(child, text, offset, level + 1, nameTree, srcDoc);
            }
        }
    }

}
