package com.ririv.quickoutline.service;


import com.ririv.quickoutline.exception.EncryptedPdfException;
import com.ririv.quickoutline.exception.NoOutlineException;
import com.ririv.quickoutline.model.Bookmark;
import com.ririv.quickoutline.pdfProcess.OutlineProcessor;
import com.ririv.quickoutline.pdfProcess.ViewScaleType;
import com.ririv.quickoutline.pdfProcess.itextImpl.ItextOutlineProcessor;
import com.ririv.quickoutline.textProcess.TextProcessor;
import com.ririv.quickoutline.textProcess.methods.Method;

import java.io.IOException;


public class PdfOutlineService {

    private final OutlineProcessor outlineProcessor = new ItextOutlineProcessor();

    public void setOutline(Bookmark rootBookmark, String srcFilePath, String destFilePath, ViewScaleType scaleType) throws IOException {
        if (srcFilePath.isEmpty()) throw new RuntimeException("PDF路径为空");
        outlineProcessor.setOutline(rootBookmark, srcFilePath, destFilePath, scaleType);
    }

    public void setOutline(String text, String srcFilePath, String destFilePath, int offset, Method method, ViewScaleType scaleType) throws IOException {
        Bookmark rootBookmark = convertTextToBookmarkTreeByMethod(text, offset, method);
        setOutline(rootBookmark, srcFilePath, destFilePath, scaleType);
    }

    public void deleteOutline(String srcFilePath, String destFilePath) {
        if (srcFilePath.isEmpty()) throw new RuntimeException("PDF路径为空");
        try {
            outlineProcessor.deleteOutline(srcFilePath, destFilePath);
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }

    //先经过text生成bookmark，在将bookmark转化为text
    public String autoFormat(String text) {
        Bookmark rootBookmark = convertTextToBookmarkTreeByMethod(text, 0, Method.SEQ);
        return rootBookmark.toOutlineString();
    }

    public Bookmark convertTextToBookmarkTreeByMethod(String text, int offset, Method method) {
        TextProcessor textProcessor = new TextProcessor();
        return textProcessor.process(text, offset, method);
    }

    public void checkOpenFile(String srcFilepath) throws IOException {
        if (srcFilepath.isEmpty()) throw new RuntimeException("PDF路径为空");
        if (outlineProcessor.checkEncrypted(srcFilepath)) throw new EncryptedPdfException();
    }

    //此offset用于减，即 偏移后的偏移量-offset = 原页码（v2.0+目前没有应用场景）
    public String getContents(String srcFilepath, int offset) throws NoOutlineException {
        if (srcFilepath.isEmpty()) throw new RuntimeException("PDF路径为空");

        try {
            return outlineProcessor.getContents(srcFilepath, offset);
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    public Bookmark getOutlineAsBookmark(String srcFilepath, int offset) throws NoOutlineException {
        if (srcFilepath.isEmpty()) throw new RuntimeException("PDF路径为空");

        try {
            return outlineProcessor.getOutlineAsBookmark(srcFilepath, offset);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

}

