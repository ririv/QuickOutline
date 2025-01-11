package com.ririv.quickoutline.service;


import com.ririv.quickoutline.model.Bookmark;
import com.ririv.quickoutline.pdfProcess.PdfProcessor;
import com.ririv.quickoutline.pdfProcess.itextImpl.ItextProcessor;
import com.ririv.quickoutline.textProcess.TextProcessor;
import com.ririv.quickoutline.textProcess.methods.Method;

import java.io.IOException;


public class PdfService {

    private final PdfProcessor pdfProcessor = new ItextProcessor();

    public void addContents(String text, String srcFilePath, String destFilePath, int offset, Method method) {
        if (srcFilePath.isEmpty()) throw new RuntimeException("PDF路径为空");

        Bookmark rootBookmark = convertTextToBookmarkTreeByMethod(text, offset, method);

        try {
            pdfProcessor.setContents(rootBookmark, srcFilePath, destFilePath);
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }

    public void deleteContents(String srcFilePath, String destFilePath) {
        if (srcFilePath.isEmpty()) throw new RuntimeException("PDF路径为空");
        try {
            pdfProcessor.deleteContents(srcFilePath, destFilePath);
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }

    //先经过text生成bookmark，在将bookmark转化为text
    public String autoFormat(String text) {
        Bookmark rootBookmark = convertTextToBookmarkTreeByMethod(text, 0, Method.SEQ);
        return rootBookmark.toTreeText();
    }

    public Bookmark convertTextToBookmarkTreeByMethod(String text, int offset, Method method) {
        TextProcessor textProcessor = new TextProcessor();
        return textProcessor.process(text, offset, method);
    }


    //此offset用于减，即 偏移后的偏移量-offset = 原页码（v2.0+目前没有应用场景）
    public String getContents(String srcFile, int offset){
        if (srcFile.isEmpty()) throw new RuntimeException("PDF路径为空");

        try {
            return pdfProcessor.getContents(srcFile, offset);
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

}

