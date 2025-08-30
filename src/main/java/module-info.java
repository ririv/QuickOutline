module quickoutline {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
//    requires javafx.web;
//    requires java.validation;
    requires javafx.swing;
    requires org.slf4j;
    requires org.apache.commons.logging;

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
    requires org.checkerframework.checker.qual;
    requires openai.java.core;
    requires openai.java.client.okhttp;
    requires com.google.common;
//    requires org.bouncycastle.util;

    opens com.ririv.quickoutline.view to javafx.fxml,javafx.graphics, com.google.guice ;
    opens com.ririv.quickoutline.service to com.google.guice;
    opens com.ririv.quickoutline.state to com.google.guice;
    opens com.ririv.quickoutline.view.controls to javafx.fxml, javafx.graphics;

    exports com.ririv.quickoutline.view;
    exports com.ririv.quickoutline.model;
    exports com.ririv.quickoutline.utils;
    exports com.ririv.quickoutline.service;
    exports com.ririv.quickoutline.textProcess.methods;
    exports com.ririv.quickoutline.view.controls;
    exports com.ririv.quickoutline.pdfProcess;
    exports com.ririv.quickoutline.exception;
    exports com.ririv.quickoutline.service.syncWithExternelEditor.externalEditor;
    exports com.ririv.quickoutline.service.syncWithExternelEditor.externalEditor.exceptions;
    exports com.ririv.quickoutline.state;
    exports com.ririv.quickoutline.pdfProcess.itextImpl;
    opens com.ririv.quickoutline.pdfProcess.itextImpl to com.google.guice;
    opens com.ririv.quickoutline.pdfProcess to com.google.guice;
    opens com.ririv.quickoutline.di to com.google.guice;
    opens com.ririv.quickoutline.event to com.google.guice;
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
}