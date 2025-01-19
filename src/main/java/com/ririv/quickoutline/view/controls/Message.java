package com.ririv.quickoutline.view.controls;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.io.IOException;

public class Message extends StackPane {

    @FXML
    private Label messageText;

    public ParallelTransition parallelOut;
    public MessageType type;


    public enum MessageType {
        SUCCESS, INFO, WARNING, ERROR
    }

    public Message(String text, MessageType type) {
        this.type = type;
        // 通过 FXMLLoader 加载 FXML
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("Message.fxml"));
        fxmlLoader.setRoot(this); // 设置根节点为当前 Message 实例
        fxmlLoader.setController(this); // 设置控制器为当前 Message 实例
        try {
            fxmlLoader.load(); // 加载 FXML 布局
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 设置外部 CSS 样式
        getStylesheets().add(getClass().getResource("Message.css").toExternalForm());  // 加载外部 CSS
        getStyleClass().add("message"); // 添加 message 样式类


        setManaged(false);  // 初始时不参与布局，也不占用空间
        setVisible(false);   // 初始时不可见
        this.setMaxWidth(320);
//        this.setMaxHeight(30);
        messageText.setWrapText(true);
        messageText.setMaxWidth(260); // < MaxWidth - 15*2+15+10 (=55) 即 左右padding+图标宽度+leftInsets

        switch (type) {
            case SUCCESS -> this.getStyleClass().add("message-success");
            case INFO -> this.getStyleClass().add("message-info");
            case WARNING -> this.getStyleClass().add("message-warning");
            case ERROR -> this.getStyleClass().add("message-error");
        }
        messageText.setText(text);
        setMouseTransparent(true);
        this.show();
    }

    @FXML
    public void initialize() {
//        switch (type) {
//            case SUCCESS -> this.getStyleClass().add("message-success");
//            case INFO -> this.getStyleClass().add("message-info");
//            case WARNING -> this.getStyleClass().add("message-warn");
//            case ERROR -> this.getStyleClass().add("message-error");
//        }
//        this.getStyleClass().add("message-info");
    }

    // 显示消息并设置淡入淡出效果
    public void show() {

        // 确保 Message 组件可见并参与布局
        setManaged(true);
        setVisible(true);
        setDisable(false);

        // 创建平移动画（从上方进入）
        TranslateTransition translateIn = new TranslateTransition(Duration.seconds(0.5), this);
        double height = 30;
        translateIn.setFromY(-height);  // 从组件的上方开始
        translateIn.setToY(0);          // 移动到原始位置


        // 创建淡入动画
        FadeTransition fadeIn = new FadeTransition(Duration.seconds(0.5), this);
        fadeIn.setFromValue(0);  // 初始透明度为0
        fadeIn.setToValue(1);    // 目标透明度为1

        // 使用 ParallelTransition 将平移和淡入动画合并
        ParallelTransition parallelIn = new ParallelTransition(translateIn, fadeIn);
        // 淡入和平移同时执行
        parallelIn.play();

        TranslateTransition translateOut = new TranslateTransition(Duration.seconds(0.5), this);
        translateOut.setFromY(0);  // 从原始位置
        translateOut.setToY(-height);  // 向下滑出

        FadeTransition fadeOut = new FadeTransition(Duration.seconds(0.5), this);
        fadeOut.setFromValue(1);  // 初始透明度为1
        fadeOut.setToValue(0);    // 目标透明度为0


        parallelOut = new ParallelTransition(translateOut, fadeOut);
        parallelOut.setDelay(Duration.seconds(3));  // 2秒后开始淡出（展示消息2秒）
        parallelOut.setOnFinished(e -> {
            // 消息消失后清空组件，恢复初始状态
            setVisible(false); // 隐藏消息框
            setManaged(false);

        });

        parallelOut.play();
    }

}