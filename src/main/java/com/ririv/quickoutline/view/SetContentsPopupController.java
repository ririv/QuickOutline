package com.ririv.quickoutline.view;

import com.ririv.quickoutline.pdfProcess.ViewScaleType;
import com.ririv.quickoutline.utils.LocalizationManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.util.ResourceBundle;

public class SetContentsPopupController extends StackPane {

    private MainController mainController;

    @FXML
    private Label viewScaleTypeLabel;

    @FXML
    private ToggleGroup viewScaleToggleGroup;

    @FXML
    private ToggleButton fitToWidthBtn;

    @FXML
    private ToggleButton fitToHeightBtn;

    @FXML
    private ToggleButton actualSizeBtn;

    public SetContentsPopupController(MainController mainController) {
        this.mainController = mainController;

        // 通过 FXMLLoader 加载 FXML
        FXMLLoader fxmlLoader = new FXMLLoader(
                getClass().getResource("SetContentsPopup.fxml"),
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
        ResourceBundle bundle = LocalizationManager.getResourceBundle();

        viewScaleToggleGroup.selectedToggleProperty().addListener( (event,oldValue, newValue) -> {
            SimpleStringProperty labelText = new SimpleStringProperty();
            if (newValue == fitToWidthBtn){
                labelText.set(bundle.getString("viewScaleTypeLabel.FIT_TO_WIDTH"));
                this.mainController.viewScaleType = ViewScaleType.FIT_TO_WIDTH;
            } else if (newValue == fitToHeightBtn){
                labelText.set(bundle.getString("viewScaleTypeLabel.FIT_TO_HEIGHT"));
                this.mainController.viewScaleType = ViewScaleType.FIT_TO_HEIGHT;
            } else if (newValue == actualSizeBtn){
                labelText.set(bundle.getString("viewScaleTypeLabel.ACTUAL_SIZE"));
                this.mainController.viewScaleType = ViewScaleType.ACTUAL_SIZE;
            } else { // null
                labelText.set(bundle.getString("viewScaleTypeLabel.NONE"));
                this.mainController.viewScaleType = ViewScaleType.NONE;
            }
            viewScaleTypeLabel.textProperty().bind(labelText);
            });
    }

}

