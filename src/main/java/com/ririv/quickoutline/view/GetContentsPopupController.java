package com.ririv.quickoutline.view;

import com.ririv.quickoutline.service.PdfTocService;
import com.ririv.quickoutline.utils.LocalizationManager;
import com.ririv.quickoutline.view.controls.Message;
import com.ririv.quickoutline.view.controls.Switch;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.util.ResourceBundle;

public class GetContentsPopupController extends StackPane {

    private StringProperty filepath = new SimpleStringProperty();

    private final ResourceBundle bundle = LocalizationManager.getResourceBundle();

    public HBox pageNumRangeLayout;

    private final MainController mainController;

    @FXML
    private Switch autoRecognizeSwitch;

    @FXML
    private TextField startTF;

    @FXML
    private TextField endTF;

    @FXML
    private Button extractTocBtn;

    private int backspaceCount = 0;

    private BooleanProperty autoRecognize = new SimpleBooleanProperty(true);

    public GetContentsPopupController(MainController mainController) {
        this.mainController = mainController;

        // 通过 FXMLLoader 加载 FXML
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(
                "GetContentsPopup.fxml"),
                LocalizationManager.getResourceBundle()
        );
        fxmlLoader.setRoot(this); // 设置根节点为当前 Message 实例
        fxmlLoader.setController(this); // 设置控制器为当前 Message 实例
        try {
            fxmlLoader.load(); // 加载 FXML 布局
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void initialize() {
        PdfTocService pdfTocService = new PdfTocService();


        autoRecognize.bind(autoRecognizeSwitch.valueProperty());


        pageNumRangeLayout.disableProperty().bind(autoRecognizeSwitch.valueProperty());


        startTF.addEventFilter(KeyEvent.KEY_TYPED, event -> {
            if (!"0123456789".contains(event.getCharacter())) {
                event.consume();
            }
        });

        endTF.addEventFilter(KeyEvent.KEY_TYPED, event -> {
            if (!"0123456789".contains(event.getCharacter())) {
                event.consume();
            }
        });

        // 启用tab键自动转到endTF
        endTF.focusTraversableProperty().bind(startTF.focusedProperty());

        endTF.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (endTF.getText().isEmpty()){
                if (event.getCode() == KeyCode.BACK_SPACE) {
                    backspaceCount++;
                    if (backspaceCount == 2) {
                        // 当退格键被按下两次时触发的事件
                        startTF.requestFocus();
                        // 重置计数器
                        backspaceCount = 0;
                    }
                }
            }

        });

        extractTocBtn.setOnAction(event -> {
            if (filepath.get() == null || filepath.get().isEmpty()) {
                mainController.messageManager.showMessage( bundle.getString("message.choosePDFFile"), Message.MessageType.WARNING);
                return;
            }

            String contents;
            if (autoRecognize.get()) {
                contents = pdfTocService.extract(filepath.get());
            } else {
                if (startTF.getText().isEmpty() || endTF.getText().isEmpty()) {
                    mainController.messageManager.showMessage(bundle.getString("message.inputPageNumRange"), Message.MessageType.WARNING);
                }
                contents = pdfTocService.extract(filepath.get(), Integer.parseInt(startTF.getText()), Integer.parseInt(endTF.getText()));
            }
            this.mainController.textTabViewController.contentsTextArea.setText(contents);
        });
    }

    public StringProperty filepathProperty() {
        return filepath;
    }

}
