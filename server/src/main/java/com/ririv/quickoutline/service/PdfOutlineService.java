package com.ririv.quickoutline.service;


import com.ririv.quickoutline.model.Bookmark;
import com.ririv.quickoutline.textProcess.TextProcessor;
import com.ririv.quickoutline.textProcess.methods.Method;


public class PdfOutlineService {

    public Bookmark convertTextToBookmarkTreeByMethod(String text, Method method) {
        TextProcessor textProcessor = new TextProcessor();
        return textProcessor.process(text, method);
    }

}

