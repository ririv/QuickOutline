module quickoutline {
    requires javafx.controls;
    requires javafx.fxml;
    requires kernel;
    requires java.desktop;
//    requires javafx.web;
//    requires java.validation;
    requires org.slf4j; //必须添加，否则会出现找不到 Exception java.lang.NoClassDefFoundError: org/slf4j/LoggerFactory
    requires layout;
    requires io;

    opens com.ririv.quickoutline.view to javafx.fxml,javafx.graphics ; //由于FXML使用反射访问controller
    opens com.ririv.quickoutline.view.controls to javafx.fxml, javafx.graphics;

    exports com.ririv.quickoutline.view;
    exports com.ririv.quickoutline.model;
    exports com.ririv.quickoutline.utils;
    exports com.ririv.quickoutline.service;
    exports com.ririv.quickoutline.textProcess.methods;
    exports com.ririv.quickoutline.view.controls;
    exports com.ririv.quickoutline.pdfProcess;

}