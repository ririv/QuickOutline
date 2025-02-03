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

    public void setContents(String text, String srcFilePath, String destFilePath, int offset, Method method, ViewScaleType scaleType) throws IOException {
        if (srcFilePath.isEmpty()) throw new RuntimeException("PDF路径为空");

        Bookmark rootBookmark = convertTextToBookmarkTreeByMethod(text, offset, method);
        outlineProcessor.setContents(rootBookmark, srcFilePath, destFilePath, scaleType);
    }

    public void deleteContents(String srcFilePath, String destFilePath) {
        if (srcFilePath.isEmpty()) throw new RuntimeException("PDF路径为空");
        try {
            outlineProcessor.deleteContents(srcFilePath, destFilePath);
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

}

