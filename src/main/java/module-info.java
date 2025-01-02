module quickoutline {
    requires javafx.controls;
    requires javafx.fxml;
    requires kernel;
    requires java.desktop;
    requires javafx.web;
    requires com.jfoenix;
    requires java.validation;
    requires org.slf4j; //必须添加，否则会出现找不到 Exception java.lang.NoClassDefFoundError: org/slf4j/LoggerFactory

    opens com.ririv.quickoutline.view to javafx.fxml,javafx.graphics ; //由于FXML使用反射访问controller


    exports com.ririv.quickoutline.view;
    exports com.ririv.quickoutline.entity;
    exports com.ririv.quickoutline.utils;
    exports com.ririv.quickoutline.service;
    exports com.ririv.quickoutline.textProcess.methods;
}