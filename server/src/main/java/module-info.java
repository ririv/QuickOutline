module quickoutline {
    requires java.desktop;
    requires jdk.xml.dom;
//    requires java.validation;
    requires jdk.httpserver;
    requires jakarta.inject;

    requires org.slf4j;
    requires org.slf4j.simple; // jpackage 后没有这个会不写日志
    requires org.apache.commons.logging;

    // itext
    requires kernel;
    requires layout;
    requires io;
    requires font.asian;
    requires html2pdf;
    requires commons;
    requires svg;
    requires styled.xml.parser;

//    requires bouncy.castle.adapter;
//    requires bouncy.castle.connector;
//    requires org.bouncycastle.provider;
//    requires org.bouncycastle.util;
    requires org.bouncycastle.pkix;

    requires xml.apis.ext;

//    requires openai.java.core;
//    requires openai.java.client.okhttp;

    requires com.google.gson;

    requires org.apache.pdfbox;
    requires org.apache.xmlgraphics.batik.svgdom;
    requires org.apache.xmlgraphics.batik.util;
    requires org.apache.xmlgraphics.batik.anim;
    requires org.apache.xmlgraphics.batik.transcoder;
    requires org.apache.xmlgraphics.batik.svggen;
    requires de.rototor.pdfbox.graphics2d;
    requires org.apache.xmlgraphics.batik.dom;
    requires org.apache.pdfbox.io;

    requires org.jfree.svg;

    requires io.vertx.core;


    exports com.ririv.quickoutline.model;
    exports com.ririv.quickoutline.utils;
    exports com.ririv.quickoutline.service;
    exports com.ririv.quickoutline.textProcess.methods;
//取消导出 com.ririv.quickoutline.pdfProcess 包，避免对外暴露 BufferedImage（java.desktop 类型）导致的模块可见性告警；模块内使用不受影响
    exports com.ririv.quickoutline.exception;
    exports com.ririv.quickoutline.pdfProcess;
    exports com.ririv.quickoutline.service.syncWithExternelEditor.externalEditor;
    exports com.ririv.quickoutline.service.syncWithExternelEditor.externalEditor.exceptions;
    exports com.ririv.quickoutline.pdfProcess.itextImpl;
    exports com.ririv.quickoutline.pdfProcess.itextImpl.model;
    exports com.ririv.quickoutline.service.webserver;
    exports com.ririv.quickoutline.service.pdfpreview;
    exports com.ririv.quickoutline.service.pdfpreview.strategy;
    exports com.ririv.quickoutline.pdfProcess.numbering;
    opens com.ririv.quickoutline.api.model to com.google.gson;
    exports com.ririv.quickoutline.pdfProcess.itextImpl.html2pdf;
}