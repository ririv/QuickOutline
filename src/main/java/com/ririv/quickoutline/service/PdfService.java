package com.ririv.quickoutline.service;


import com.ririv.quickoutline.model.Bookmark;
import com.ririv.quickoutline.pdfProcess.PdfProcess;
import com.ririv.quickoutline.pdfProcess.itextImpl.ItextProcess;
import com.ririv.quickoutline.textProcess.TextProcessor;
import com.ririv.quickoutline.textProcess.methods.Method;

import java.io.IOException;


public class PdfService {

    private final PdfProcess pdfProcess = new ItextProcess();

    //下面两个方法本质相同
    public void addContents(String text, String srcFilePath, String destFilePath, int offset, Method method) {
        if (srcFilePath.isEmpty()) throw new RuntimeException("PDF路径为空");

        Bookmark rootBookmark = convertTextToBookmarkTreeByMethod(text, offset, method);

        try {
            pdfProcess.setContents(rootBookmark, srcFilePath, destFilePath);
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }

    public void deleteContents(String srcFilePath, String destFilePath) {
        if (srcFilePath.isEmpty()) throw new RuntimeException("PDF路径为空");
        try {
            pdfProcess.deleteContents(srcFilePath, destFilePath);
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }

    //先经过text生成bookmark，在将bookmark转化为text
    public String autoFormat(String text) {
//        Bookmark rootBookmark = textToBookmarkByMethod(text, 0, Method.SEQ,false);
        Bookmark rootBookmark = convertTextToBookmarkTreeByMethod(text, 0, Method.SEQ);
        return rootBookmark.toTreeText();
    }

    public Bookmark convertTextToBookmarkTreeByMethod(String text, int offset, Method method) {
        TextProcessor textProcessor = new TextProcessor();
        return textProcessor.process(text, offset, method);
    }


    //此offset用于减，即 偏移后的偏移量-offset = 原页码（作为返回给用户的页码）
    public String getContents(String srcFile, int offset){
        if (srcFile.isEmpty()) throw new RuntimeException("PDF路径为空");

        try {
            return pdfProcess.getContents(srcFile, offset);
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    /*    为避免错乱，不提供seq参数，请先使用setSeq()方法设置current的seq
        无使用场景，搁置*/
    public Bookmark addBookmarkBySeq(Bookmark root, Bookmark current) {
        return null;
    }

}

