module quickoutline {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
//    requires javafx.web;
//    requires jdk.jsobject;
//    requires java.validation;
    requires javafx.swing;
    requires org.slf4j;
    requires org.apache.commons.logging;

    // itext
    requires kernel;
    requires layout;
    requires io;
    requires font.asian;

//    requires bouncy.castle.adapter;
//    requires bouncy.castle.connector;
//    requires org.bouncycastle.provider;
    requires org.bouncycastle.pkix;
    requires org.apache.pdfbox;
    requires com.google.guice;
    requires jakarta.inject;
//    requires openai.java.core;
//    requires openai.java.client.okhttp;
    requires com.google.common;
    requires org.commonmark;
    requires org.commonmark.ext.gfm.tables;
    requires html2pdf;
    requires commons;
    requires xml.apis.ext;
    requires jdk.xml.dom;
    requires org.apache.xmlgraphics.batik.svgdom;
    requires org.apache.xmlgraphics.batik.util;
    requires org.apache.xmlgraphics.batik.anim;

//    requires org.bouncycastle.util;

    opens com.ririv.quickoutline.view to javafx.fxml,javafx.graphics, com.google.guice ;
    opens com.ririv.quickoutline.service to com.google.guice;
    opens com.ririv.quickoutline.view.state to com.google.guice;
    opens com.ririv.quickoutline.view.controls to javafx.fxml, javafx.graphics;

    exports com.ririv.quickoutline.view;
    exports com.ririv.quickoutline.model;
    exports com.ririv.quickoutline.utils;
    exports com.ririv.quickoutline.service;
    exports com.ririv.quickoutline.textProcess.methods;
    exports com.ririv.quickoutline.view.controls;
//取消导出 com.ririv.quickoutline.pdfProcess 包，避免对外暴露 BufferedImage（java.desktop 类型）导致的模块可见性告警；模块内使用不受影响
    exports com.ririv.quickoutline.exception;
    exports com.ririv.quickoutline.service.syncWithExternelEditor.externalEditor;
    exports com.ririv.quickoutline.service.syncWithExternelEditor.externalEditor.exceptions;
    exports com.ririv.quickoutline.view.state;
    exports com.ririv.quickoutline.pdfProcess.itextImpl;
    opens com.ririv.quickoutline.pdfProcess.itextImpl to com.google.guice;
    opens com.ririv.quickoutline.pdfProcess to com.google.guice;
    opens com.ririv.quickoutline.di to com.google.guice;
    opens com.ririv.quickoutline.view.event to com.google.guice;
    exports com.ririv.quickoutline.view.controls.select;
    opens com.ririv.quickoutline.view.controls.select to javafx.fxml, javafx.graphics;
    exports com.ririv.quickoutline.view.controls.message;
    opens com.ririv.quickoutline.view.controls.message to javafx.fxml, javafx.graphics;
    exports com.ririv.quickoutline.pdfProcess.itextImpl.model;
    opens com.ririv.quickoutline.pdfProcess.itextImpl.model to com.google.guice;
    exports com.ririv.quickoutline.view.controls.radioButton2;
    opens com.ririv.quickoutline.view.controls.radioButton2 to javafx.fxml, javafx.graphics;
    exports com.ririv.quickoutline.view.controls.slider;
    opens com.ririv.quickoutline.view.controls.slider to javafx.fxml, javafx.graphics;
    exports com.ririv.quickoutline.view.bookmarktab;
    opens com.ririv.quickoutline.view.bookmarktab to com.google.guice, javafx.fxml, javafx.graphics;
    opens com.ririv.quickoutline.service.syncWithExternelEditor to com.google.guice;
    exports com.ririv.quickoutline.view.viewmodel;
    opens com.ririv.quickoutline.view.viewmodel to com.google.guice, javafx.fxml, javafx.graphics;
    exports com.ririv.quickoutline.view.utils;
    opens com.ririv.quickoutline.view.utils to com.google.guice, javafx.fxml, javafx.graphics;
    exports com.ririv.quickoutline.view.icons;
}