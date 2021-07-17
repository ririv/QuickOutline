package com.ririv.quickoutline.process.itextImpl;


import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;
import com.itextpdf.text.pdf.SimpleBookmark;
import com.ririv.quickoutline.exception.BookmarkFormatException;
import com.ririv.quickoutline.entity.Bookmark;
import com.ririv.quickoutline.process.PdfProcess;
import com.ririv.quickoutline.process.TextProcess;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


//itext5已弃用，本类不再改动
public class Itext5Process implements PdfProcess {

    //添加目录，并输出到指定路径
    public void addContents(Bookmark rootBookmark, String srcFile, String destFile) {
        List<HashMap<String, Object>> rootOutlines = new ArrayList<>(); //顶级目录,定义此类型的原因是因为itext5中直接获得的类型
        bookmarkToOutlines(rootBookmark, rootOutlines);

        try {
            PdfReader reader = new PdfReader(srcFile);
            PdfReader.unethicalreading = true;
            PdfStamper stamper = new PdfStamper(reader, new FileOutputStream(destFile));
            stamper.setOutlines(rootOutlines);
            stamper.close();
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }


    //获取当前pdf的目录
    public String getContents(String srcFile, int offset) {

        try {
            PdfReader reader = new PdfReader(srcFile);
            List<HashMap<String, Object>> outlines;
            PdfReader.unethicalreading = true;
            outlines = SimpleBookmark.getBookmark(reader);
            StringBuilder text = new StringBuilder();
            outlinesToText(outlines, text, offset, 0);
            return text.toString();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);

        }
    }


    //递归，使用itext5添加目录的script,
    public void bookmarkToOutlines(Bookmark rootBookmark, List<HashMap<String, Object>> rootOutlines) {

        if (!rootBookmark.getChildren().isEmpty()) {
            for (Bookmark subBookmark : rootBookmark.getChildren()) {

                HashMap<String, Object> outline = new HashMap<>();

                outline.put("Title", subBookmark.getTitle());
                outline.put("Action", "GoTo");
//        if (rootBookmark.getPageNum() >= 0)
                outline.put("Page", String.format("%d", subBookmark.getPageNum().orElseThrow(()->
                        new BookmarkFormatException(String.format(
                                "添加页码错误\n\"%s\"无页码",
                                subBookmark.getTitle()),subBookmark.getIndex()
                        )))); //"Fit"

                List<HashMap<String, Object>> childOutlines = new ArrayList<>();
                rootOutlines.add(outline);

                bookmarkToOutlines(subBookmark, childOutlines);
                outline.put("Kids", childOutlines);
            }
        }
    }


    //递归,将每行bookmark转化为文本
    @SuppressWarnings("unchecked")
    public void outlinesToText(List<HashMap<String, Object>> outLines, StringBuilder text, int offset, int level) {
        for (HashMap<String, Object> entry : outLines) {
            int  pageNum = Integer.parseInt(entry.get("Page").toString().split(" ")[0]) - offset;

            TextProcess.toLine(text, level, (String) entry.get("Title"), Integer.toString(pageNum));

            if (entry.get("Kids") != null) {
                outlinesToText((List<HashMap<String, Object>>) entry.get("Kids"), text, offset, level + 1);
            }
        }
    }


}
