module quickoutline {
    requires java.desktop;
    requires jdk.xml.dom;
//    requires java.validation;
    requires jdk.httpserver;
    requires jakarta.inject;

    requires org.slf4j;
    requires org.slf4j.simple; // jpackage 后没有这个会不写日志

    // itext
    requires kernel;
    requires layout;
    requires io;
    requires font.asian;
    requires commons;

//    requires bouncy.castle.adapter;
//    requires bouncy.castle.connector;
//    requires org.bouncycastle.provider;
//    requires org.bouncycastle.util;
    requires org.bouncycastle.pkix;


//    requires openai.java.core;
//    requires openai.java.client.okhttp;

    requires com.google.gson;


    requires io.vertx.core;


    exports com.ririv.quickoutline.model;
    exports com.ririv.quickoutline.utils;
    exports com.ririv.quickoutline.service;
    exports com.ririv.quickoutline.textProcess.methods;
    exports com.ririv.quickoutline.exception;
    exports com.ririv.quickoutline.pdfProcess;
    exports com.ririv.quickoutline.service.syncWithExternelEditor.externalEditor;
    exports com.ririv.quickoutline.service.syncWithExternelEditor.externalEditor.exceptions;
    exports com.ririv.quickoutline.pdfProcess.itextImpl;
    exports com.ririv.quickoutline.pdfProcess.itextImpl.model;
    exports com.ririv.quickoutline.service.webserver;
    exports com.ririv.quickoutline.pdfProcess.numbering;
    opens com.ririv.quickoutline.api.model to com.google.gson;
}