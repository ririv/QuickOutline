package com.ririv.quickoutline.view;

import com.ririv.quickoutline.pdfProcess.PdfViewScaleType;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.StackPane;

import java.io.IOException;

public class SetContentsPopupView extends StackPane {

    private MainController mainController;

    @FXML
    private Label label;

    @FXML
    private ToggleGroup viewScaleToggleGroup;

    @FXML
    private ToggleButton fitToWidthBtn;

    @FXML
    private ToggleButton fitToHeightBtn;

    @FXML
    private ToggleButton actualSizeBtn;

    public SetContentsPopupView(MainController mainController) {
        this.mainController = mainController;

        // 通过 FXMLLoader 加载 FXML
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("SetContentsPopup.fxml"));
        fxmlLoader.setRoot(this); // 设置根节点为当前 Message 实例
        fxmlLoader.setController(this); // 设置控制器为当前 Message 实例
        try {
            fxmlLoader.load(); // 加载 FXML 布局
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void initialize() {
        viewScaleToggleGroup.selectedToggleProperty().addListener( (event,oldValue, newValue) -> {
            String labelText = "";
            if (newValue == fitToWidthBtn){
                labelText = "适合宽度";
                this.mainController.viewScaleType = PdfViewScaleType.FIT_TO_WIDTH;
            } else if (newValue == fitToHeightBtn){
                labelText = "适合高度";
                this.mainController.viewScaleType = PdfViewScaleType.FIT_TO_HEIGHT;
            } else if (newValue == actualSizeBtn){
                labelText = "实际大小";
                this.mainController.viewScaleType = PdfViewScaleType.ACTUAL_SIZE;
            }
            label.setText(labelText);
            });
        viewScaleToggleGroup.selectToggle(fitToHeightBtn);
    }

}

