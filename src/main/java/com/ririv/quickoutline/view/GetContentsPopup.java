package com.ririv.quickoutline.view;

import com.ririv.quickoutline.service.TocService;
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
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

import java.io.IOException;

public class GetContentsPopup extends StackPane {

    private StringProperty filepath = new SimpleStringProperty();

    public HBox pageNumRangeLayout;

    private MainController mainController;

    @FXML
    private Switch autoRecognizeSwitch;

    @FXML
    private TextField startTF;

    @FXML
    private TextField endTF;

    @FXML
    private Button extractTocBtn;

    private BooleanProperty autoRecognize = new SimpleBooleanProperty(true);

    public GetContentsPopup(MainController mainController) {
        this.mainController = mainController;

        // 通过 FXMLLoader 加载 FXML
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("GetContentsPopup.fxml"));
        fxmlLoader.setRoot(this); // 设置根节点为当前 Message 实例
        fxmlLoader.setController(this); // 设置控制器为当前 Message 实例
        try {
            fxmlLoader.load(); // 加载 FXML 布局
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void initialize() {
        TocService tocService = new TocService();


        autoRecognize.bind(autoRecognizeSwitch.valueProperty());


        pageNumRangeLayout.disableProperty().bind(autoRecognizeSwitch.valueProperty());


        startTF.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.isEmpty() && !newValue.matches("^\\d+$")) {
                newValue = newValue.replaceAll("[^0-9]", "");
                startTF.setText(newValue);
            }
        });

        endTF.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.isEmpty() && !newValue.matches("^\\d+$")) {
                newValue = newValue.replaceAll("[^0-9]", "");
                endTF.setText(newValue);
            }
        });

        extractTocBtn.setOnAction(event -> {
            if (filepath.get() == null || filepath.get().isEmpty()) {
                mainController.messageDialog.showMessage("请选择PDF文件", Message.MessageType.WARNING);
                return;
            }

            String contents = "";
            if (autoRecognize.get()) {
                contents = tocService.extract(filepath.get());
            } else {
                if (startTF.getText().isEmpty() || endTF.getText().isEmpty()) {
                    mainController.messageDialog.showMessage("请输入起始页码和结束页码", Message.MessageType.WARNING);
                }
                contents = tocService.extract(filepath.get(), Integer.parseInt(startTF.getText()), Integer.parseInt(endTF.getText()));
            }
            this.mainController.textModeController.contentsTextArea.setText(contents);
        });
    }

    public StringProperty filepathProperty() {
        return filepath;
    }

}
