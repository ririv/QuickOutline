module quickoutline {
    requires javafx.controls;
    requires javafx.fxml;
    requires itextpdf;
    requires kernel;
    requires java.desktop;
    requires javafx.web;
    requires com.jfoenix;
    requires java.validation;

    opens com.ririv.quickoutline.view to javafx.fxml,javafx.graphics ; //由于FXML使用反射访问controller


    exports com.ririv.quickoutline.view;
    exports com.ririv.quickoutline.entity;
    exports com.ririv.quickoutline.enums;
    exports com.ririv.quickoutline.utils;
}