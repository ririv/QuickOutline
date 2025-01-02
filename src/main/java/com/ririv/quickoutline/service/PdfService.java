package com.ririv.quickoutline.service;


import com.ririv.quickoutline.entity.Bookmark;
import com.ririv.quickoutline.pdfProcess.PdfProcess;
import com.ririv.quickoutline.pdfProcess.itextImpl.ItextProcess;
import com.ririv.quickoutline.textProcess.methods.Form;
import com.ririv.quickoutline.textProcess.methods.Indent;
import com.ririv.quickoutline.textProcess.methods.Method;
import com.ririv.quickoutline.textProcess.methods.seq.CnSeq;

import java.io.IOException;


public class PdfService {

    private final PdfProcess pdfProcess = new ItextProcess();

    //下面两个方法本质相同
    public void addContents(String text, String srcFile, String destFile, int offset, Method method) {
        if (srcFile.isEmpty()) throw new RuntimeException("PDF路径为空");
//        Bookmark rootBookmark = textToBookmarkByMethod(text, offset, method,true);
        Bookmark rootBookmark = textToBookmarkByMethod(text, offset, method);

        try {
            pdfProcess.setContents(rootBookmark, srcFile, destFile);
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }

    //先经过text生成bookmark，在将bookmark转化为text
    public String autoFormatBySeq(String text) {
//        Bookmark rootBookmark = textToBookmarkByMethod(text, 0, Method.SEQ,false);
        Bookmark rootBookmark = textToBookmarkByMethod(text, 0, Method.SEQ);
        return rootBookmark.toText();
    }

    public Bookmark textToBookmarkByMethod(String text, int offset, Method method) {
//    public static Bookmark textToBookmarkByMethod(String text, int offset, Method method,boolean isSkipEmptyLine) {
        Form form;
        if (method == Method.SEQ) {
            form = new CnSeq();
        } else {
            form = new Indent();
        }

        return form.generateBookmarkTree(text, offset);
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

