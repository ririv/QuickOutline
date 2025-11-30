package com.ririv.quickoutline.view.controls;

import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;

import java.util.Objects;

//<Button>
//<graphic>
//    <StackPane>
//    <Label StackPane.alignment="CENTER" text="获取目录" />
//<Label StackPane.alignment="CENTER_RIGHT">
//    <graphic >
//        <Label styleClass="icon, arrowUpIcon" prefWidth="8.0" prefHeight="3.0"/>
//    </graphic>
//</Label>
//    </StackPane>
//</graphic>
//</Button>

public class SplitButton extends HBox {
    private final Button mainButton = new Button("Main Action"); // 主按钮
    private final Button arrowButton = new Button("▼"); // 箭头按钮
    private final Popup popup = new Popup();
    private final VBox menuContent = new VBox(); // 弹出的菜单内容

    public SplitButton() {
        // 设置样式类
        getStylesheets().add(Objects.requireNonNull(getClass().getResource("SplitButton.css")).toExternalForm());  // 加载外部 CSS

        this.getStyleClass().add("split-button");
        mainButton.getStyleClass().add("main-button");
        arrowButton.getStyleClass().add("arrow-button");


        // 设置箭头按钮事件，点击时显示/隐藏 Popup
        arrowButton.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> togglePopup());

        // 初始化 Popup，内容为菜单项容器
        popup.setAutoHide(true);
        popup.getContent().add(menuContent);

        // 中间占位的 Region 确保主按钮居中
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS); // 设置占位符自动扩展

        // 添加按钮到布局：主按钮 -> 占位符 -> 箭头按钮
        this.getChildren().addAll(spacer, mainButton, arrowButton);
    }

    // 切换 Popup 显示/隐藏状态
    private void togglePopup() {
        if (popup.isShowing()) {
            popup.hide();
        } else {
            // 显示 Popup，位置与箭头按钮对齐
            popup.show(arrowButton, arrowButton.localToScreen(0, 0).getX(),
                    arrowButton.localToScreen(0, 0).getY() + arrowButton.getHeight());
        }
    }

    // 添加菜单项
    public void addMenuItem(MenuItem item) {
        Button menuButton = new Button(item.getText()); // 模拟菜单项为按钮
        menuButton.setOnAction(item.getOnAction()); // 绑定菜单项行为
        menuContent.getChildren().add(menuButton);
    }

    // 设置主按钮文字
    public void setText(String text) {
        mainButton.setText(text);
    }

    // 获取主按钮文字
    public String getText() {
        return mainButton.getText();
    }

    // 设置主按钮行为
    public void setOnMainAction(Runnable action) {
        mainButton.setOnAction(event -> action.run());
    }
}
