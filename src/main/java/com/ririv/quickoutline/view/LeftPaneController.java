package com.ririv.quickoutline.view;

import com.ririv.quickoutline.utils.LocalizationManager;
import com.ririv.quickoutline.view.MainController.FnTab;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Objects;

public class LeftPaneController {

    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(LeftPaneController.class);


    public Node root;
    private Stage helpStage;


    public ToggleButton textTabBtn;
    public ToggleButton treeTabBtn;
    public ToggleGroup tabToggleGroup;
    public Button helpBtn;

    public ToggleButton tocTabBtn;
    public ToggleButton labelTabBtn;


    public MainController mainController;

    FnTab currenTab;


    void setMainController(MainController mainController){
        this.mainController = mainController;
        this.root = mainController.root;
        this.currenTab = mainController.currenTab;
    }


    public void initialize() {
        textTabBtn.setSelected(true);

        tabToggleGroup.selectedToggleProperty().addListener((event,oldValue,newValue) -> {
            // 保持选中状态，防止取消选中
            if (newValue == null){
                tabToggleGroup.selectToggle(oldValue);
                logger.info("select {}", oldValue);
            }

            if (textTabBtn.isSelected()) {
                mainController.switchTab(FnTab.text);
            } else if (treeTabBtn.isSelected()) {
                mainController.switchTab(FnTab.tree);
            } else if (tocTabBtn.isSelected()) {
                mainController.switchTab(FnTab.toc);
            } else if (labelTabBtn.isSelected()) {
                mainController.switchTab(FnTab.label);
            }
        });
    }


    @FXML
    public void createHelpWindowAction(ActionEvent actionEvent) throws IOException {
        if (helpStage == null) {
            helpStage = new Stage();

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("HelpWindow.fxml"),
                    LocalizationManager.getResourceBundle()
            );
            Parent helpWinRoot = loader.load();

            helpStage.setTitle("Help");
            helpStage.setScene(new Scene(helpWinRoot, 400, 300));

            helpStage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("icon/help_black.png"))));
            helpStage.setResizable(false); //不可调整大小，且使最大化不可用
            helpStage.initOwner(root.getScene().getWindow());//可以使最小化不可用，配合上一条语句，可以使最小化最大化隐藏，只留下"×"
            helpStage.show();
//            helpStage.focusedProperty().addListener((observable, oldValue, newValue) -> {
//                if (!observable.getValue()) {
//                    helpStage.hide();
//                    ((Stage) root.getScene().getWindow()).toFront();
//                }
//            });
        }
        else {
            // 如果窗口已经创建但被隐藏，重新显示
            // 如果窗口已存在，将其聚焦到前台
            helpStage.show();
//            helpStage.toFront();
            helpStage.requestFocus();
        }
    }
}
