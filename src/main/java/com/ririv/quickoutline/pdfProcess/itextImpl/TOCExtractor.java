package com.ririv.quickoutline.pdfProcess.itextImpl;

import com.itextpdf.kernel.pdf.*;
import com.itextpdf.kernel.pdf.annot.PdfAnnotation;
import com.itextpdf.kernel.pdf.annot.PdfLinkAnnotation;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import com.itextpdf.kernel.pdf.navigation.PdfDestination;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


//先找目录/contents，找到后，第一行文字（如“第一章...”）记录下来，随后一直找到第二次出现这个文字前的页面，都是目录。
public class TOCExtractor {
    private static final Pattern TOCPattern = Pattern.compile("^.*?\\s+(\\.|\\. ){3,}\\s?\\d+$|^\\d+\\.+\\d+\\s+.*?\\d+$");
    private static final Pattern ContentsPattern = Pattern.compile("^contents|目录$"); // 注意转化为小写再匹配

    private final PdfDocument pdfDoc;


    public TOCExtractor(String pdfPath) throws IOException {
        this.pdfDoc = new PdfDocument(new PdfReader(pdfPath));
    }

    public List<String> extract() {
        return recognize();
    }


    public List<String> extract(int startPageNum, int endPageNum) {
        List<String> tocPages = new ArrayList<>();
        for (int i = startPageNum; i <= endPageNum; i++) {
            String pageContent = PdfTextExtractor.getTextFromPage(pdfDoc.getPage(i));
            tocPages.add(pageContent);
        }

        return tocPages;
    }

    public List<String> recognize() {
        boolean startTOC = false;
        boolean afterFirstTOC = false;

        // 存储所有可能的 TOC 文本
        List<String> tocPages = new ArrayList<>();

//         假设目录在前 30 页内，提取每页内容
        int maxPages = Math.min(50, pdfDoc.getNumberOfPages());
        String firstTOCStr = null;
        String contentsStr;
        for (int i = 1; i <= maxPages; i++) {
            String pageContent = PdfTextExtractor.getTextFromPage(pdfDoc.getPage(i));
//            System.out.println("Page " + i + " content:");
//            System.out.println(pageContent);


            if (!startTOC) {
                for (String s : pageContent.split("\n")) {
                    Matcher matcher = ContentsPattern.matcher(s.toLowerCase());
                    if (matcher.find()) {
                        contentsStr = matcher.group(0);
                        startTOC = true;
                        System.out.println("startTOC");
                        int j = pageContent.lastIndexOf(contentsStr) + contentsStr.length();
                        if (s.equals("")) {
                            continue;
                        } else {
                            firstTOCStr = pageContent.substring(j + 1).strip().split("\n")[0].replaceFirst("\\d+$", "").strip().replaceAll("\\s", "");
                        }
                        System.out.println(firstTOCStr);
                        tocPages.add(pageContent);
                        break;
                    }
                }
            } else {
                afterFirstTOC = true;
            }

            // 寻找正文
            if (afterFirstTOC && firstTOCStr != null && pageContent.replaceAll("\\s", "").contains(firstTOCStr)) {
                startTOC = false;
            }
            if (startTOC) {
                if (isPotentialTOC(pageContent)) {
                    tocPages.add(pageContent);
                }
            }
            if (!startTOC && afterFirstTOC) { // 找到正文，不再继续
                break;
            }
        }

        pdfDoc.close();

        // 打印所有检测到的目录页
        System.out.println("Potential TOC Pages:");
        for (String page : tocPages) {
            System.out.println(page);
        }

        return tocPages;
    }

    // 判断页面是否可能是 TOC
    private boolean isPotentialTOC(String content) {
        // 简单规则：包含章节编号或点线
        for (String line : content.split("\n")) {
            if (TOCPattern.matcher(line).find()) {
                return true;
            }
        }
        return false;
    }

    private void extractAndHandleLinks(int pageNumber) {
        // 获取当前页的所有注释
        List<PdfAnnotation> annotations = pdfDoc.getPage(pageNumber).getAnnotations();

        for (PdfAnnotation annotation : annotations) {
            if (annotation instanceof PdfLinkAnnotation link) {

                // 提取目标（内部或外部链接）
                PdfObject destinationObject = link.getDestinationObject();
                if (destinationObject != null) {
                    destinationObject = link.getAction().get(PdfName.D);

                    PdfDestination destination = PdfDestination.makeDestination(destinationObject);
                    PdfNameTree nameTree = pdfDoc.getCatalog().getNameTree(PdfName.Dests);
                    PdfDictionary destinationPage = (PdfDictionary) destination.getDestinationPage(nameTree);
                    if (destinationPage != null) {
                        int targetPageNum = pdfDoc.getPageNumber(destinationPage) + 1;
                        // 内部跳转链接
                        System.out.println("Internal Link to Page " + targetPageNum);
                        handleInternalLink(targetPageNum);
                    }
                } else if (link.getAction() != null && link.getAction().getAsString(PdfName.URI) != null) {
                    // 外部 URL
                    System.out.println("External Link to URL: " + link.getAction().getAsString(PdfName.URI));
                }
            }
        }
    }

    private void handleInternalLink(int targetPage) {
        if (targetPage > 0 && targetPage <= pdfDoc.getNumberOfPages()) {
            System.out.println("Jumping to Page " + targetPage);
            // 读取并打印目标页面内容
            String content = PdfTextExtractor.getTextFromPage(pdfDoc.getPage(targetPage));
//            System.out.println("Content of Page " + targetPage + ":");
//            System.out.println(content.substring(0, Math.min(200, content.length())));
        } else {
            System.out.println("Invalid target page: " + targetPage);
        }
    }
}
