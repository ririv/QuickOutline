package com.ririv.quickoutline.pdfProcess.itextImpl;//package com.ririv.contents.utils.impl;


import com.itextpdf.kernel.pdf.*;
import com.itextpdf.kernel.pdf.navigation.PdfDestination;
import com.itextpdf.kernel.pdf.navigation.PdfExplicitDestination;
import com.ririv.quickoutline.exception.BookmarkFormatException;
import com.ririv.quickoutline.exception.EncryptedPdfException;
import com.ririv.quickoutline.exception.NoOutlineException;
import com.ririv.quickoutline.model.Bookmark;
import com.ririv.quickoutline.pdfProcess.OutlineProcessor;
import com.ririv.quickoutline.pdfProcess.ViewScaleType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static com.ririv.quickoutline.model.Bookmark.buildLine;


public class ItextOutlineProcessor implements OutlineProcessor {

    private static final Logger logger = LoggerFactory.getLogger(ItextOutlineProcessor.class);

    //    如果rootBookmark没有Children，即之前的text为空（当然这种情况已在Controller中被排除）
//    list.clear()没有起作用（不知道原因），最终目录没有影响，怀疑原因是没有写入操作。
    @Override
    public void setOutline(Bookmark rootBookmark, String srcFilePath, String destFilePath, ViewScaleType scaleType) throws IOException {
        if (checkEncrypted(srcFilePath)) throw new EncryptedPdfException();

        PdfDocument pdfDoc = new PdfDocument(new PdfReader(srcFilePath), new PdfWriter(destFilePath));

            PdfOutline rootOutline = pdfDoc.getOutlines(false);
            rootOutline.getAllChildren().clear();
//            rootOutline.removeOutline(); //如果使用了这个方法，后续将添加不了书签，不会报错，这是由于rootOutline失去了在Doc中的引用，

            bookmarkToOutlines(rootBookmark, rootOutline, pdfDoc, scaleType);

            pdfDoc.close();
    }


    //合并了上面两个函数
    private void bookmarkToOutlines(Bookmark parentBookmark, PdfOutline rootOutline, PdfDocument srcDoc, ViewScaleType scaleType) {

        //不为根结点时，进行添加操作
        if (!parentBookmark.isRoot()) {

            String title = parentBookmark.getTitle();

            /*
             */
            rootOutline = rootOutline.addOutline(title);

            int pageNumMax = srcDoc.getNumberOfPages();

//            https://kb.itextpdf.com/itext/chapter-6-creating-actions-destinations-and-bookma
            https://kb.itextpdf.com/itext/itext-7-building-blocks-chapter-6-actions-destinat
            if (parentBookmark.getOffsetPageNum().isPresent()) {
                int pageNum = parentBookmark.getOffsetPageNum().get();
                if (pageNum > -1 && pageNum <= pageNumMax) {
                    // getPage的pageNum参数是从1开始的，即与实际的未偏移页码相对应
                    PdfPage page = srcDoc.getPage(pageNum);
                    // Page的坐标系统的原点位于页面的左下角（与正常的坐标系一致）
                    // 因此top的值为页面高度，bottom才是为0；left也即为0
                    // Destination设置top会向上偏移（即当前页的内容从下向上开始浮现），最多偏移页面高度，更大的top值不会显示更上一页的内容
                    // 如果Destination设置top为0（left也为0），PDF的阅读器的左上角将定位于页面的左下角（原点）
                    // 显示效果为，从此页与下一页的间隔开始显示，并显示下一页内容。
                    // 所以，Destination的top必须设置为页面高度，才能将阅读器的左上角定位于页面的左上角，从当前页开始显示

                    float top = srcDoc.getPage(pageNum).getPageSize().getTop();
                    float left = srcDoc.getPage(pageNum).getPageSize().getLeft();
                    logger.info("top: {}, left: {}", top, left);

                    PdfDestination destination = null;
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
                            //
                            destination = PdfExplicitDestination.createFitV(page, left);
//                            destination = PdfDestination.makeDestination();
                        }
//                        case FIT_TO_BOX -> {
//                            destination = PdfExplicitDestination.createFitR(page, 0, 0, page.getPageSize().getWidth(), top);
//                        }
                        case CUSTOM_SCALE -> {
//                            destination = PdfExplicitDestination.createXYZ(page, 0, top, zoom);
                        }
                        case NONE -> {
//                            zoom 设为0，就会在跳转书签时保持缩放大小。
                            destination = PdfExplicitDestination.createXYZ(page, left, top, 0);
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

        if (!parentBookmark.getChildren().isEmpty()) {
            for (Bookmark subBookmark : parentBookmark.getChildren()) {
                bookmarkToOutlines(subBookmark, rootOutline, srcDoc, scaleType);

            }

        }
    }

    public boolean checkEncrypted(String srcFilePath) throws IOException {
        PdfReader reader = new PdfReader(srcFilePath);
        PdfDocument doc = new PdfDocument(reader);
        boolean isEncrypted = reader.isEncrypted();
        doc.close();
        return isEncrypted;
    }

    //https://kb.itextpdf.com/itext/how-to-create-hierarchical-bookmarks
    @Override
    public String getContents(String srcFilePath, int offset) throws IOException,NoOutlineException {

        PdfDocument srcDoc = new PdfDocument(new PdfReader(srcFilePath));
        PdfOutline rootOutline = srcDoc.getOutlines(false);
        PdfNameTree nameTree = srcDoc.getCatalog().getNameTree(PdfName.Dests);
//        nameTree.getNames().forEach((p, q) -> System.out.println(p + "  " + q + "\n"));
        StringBuilder text = new StringBuilder();

//         if (rootOutline.getDestination() != null) {

        if (rootOutline == null) {
            NoOutlineException e = new NoOutlineException();
            logger.info(e.getMessage());
            throw e;
        }
        else outlines2Text(rootOutline, text, offset, 1, nameTree, srcDoc);

//        }
        srcDoc.close(); //记得关闭

        return text.toString();
    }

//    https://kb.itextpdf.com/itext/removing-items-from-a-pdf-s-outline-tree
    public void deleteOutline(String srcFilePath, String destFilePath) throws IOException {
        // ... (existing code)
    }

    public Bookmark getOutlineAsBookmark(String srcFilePath, int offset) throws IOException, NoOutlineException {
        PdfDocument pdfDocument = new PdfDocument(new PdfReader(srcFilePath));
        PdfOutline outlines = pdfDocument.getOutlines(false);

        if (outlines == null) {
            pdfDocument.close();
            throw new NoOutlineException();
        }

        Bookmark rootBookmark = Bookmark.createRoot();
        PdfNameTree nameTree = pdfDocument.getCatalog().getNameTree(PdfName.Dests);
        processPdfOutlines(outlines.getAllChildren(), rootBookmark, pdfDocument, nameTree, offset, 1);

        pdfDocument.close();
        return rootBookmark;
    }

    private Integer getPageNumber(PdfOutline outline, PdfDocument pdfDocument, PdfNameTree nameTree) {
        PdfDestination destination = outline.getDestination();
        if (destination == null) {
            return null;
        }
        PdfObject destinationObject = destination.getDestinationPage(nameTree);
        if (destinationObject != null && destinationObject.isDictionary()) {
            return pdfDocument.getPageNumber((PdfDictionary) destinationObject);
        }
        return null;
    }

    private void processPdfOutlines(java.util.List<PdfOutline> pdfOutlines, Bookmark parentBookmark, PdfDocument pdfDocument, PdfNameTree nameTree, int offset, int level) {
        if (pdfOutlines == null || pdfOutlines.isEmpty()) {
            return;
        }

        for (PdfOutline pdfOutline : pdfOutlines) {
            String title = pdfOutline.getTitle();
            Integer pageNumber = getPageNumber(pdfOutline, pdfDocument, nameTree);

            Integer finalPageNum = (pageNumber != null) ? pageNumber + offset : null;

            Bookmark newBookmark = new Bookmark(title, finalPageNum, level);

            parentBookmark.addChild(newBookmark);

            // process the children of the current outline
            processPdfOutlines(pdfOutline.getAllChildren(), newBookmark, pdfDocument, nameTree, offset, level + 1);
        }
    }

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
