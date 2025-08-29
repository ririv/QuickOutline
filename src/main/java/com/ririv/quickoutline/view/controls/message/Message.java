package com.ririv.quickoutline.view.controls.message;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.io.IOException;
import java.util.Objects;

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
        getStylesheets().add(Objects.requireNonNull(getClass().getResource("Message.css")).toExternalForm());  // 加载外部 CSS
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

        // Add listeners to keep the message centered horizontally
        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                setTranslateX((newScene.getWidth() - getWidth()) / 2);
            }
        });
        widthProperty().addListener((obs, oldVal, newVal) -> {
            if (getScene() != null) {
                setTranslateX((getScene().getWidth() - newVal.doubleValue()) / 2);
            }
        });

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

        // The container will handle all positioning. This component only handles fading.
        setOpacity(0);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(500), this);
        fadeIn.setToValue(1);
        fadeIn.setInterpolator(Interpolator.EASE_OUT);
        fadeIn.play();

        // --- Disappearance Animation ---
        FadeTransition fadeOut = new FadeTransition(Duration.millis(500), this);
        fadeOut.setToValue(0);
        fadeOut.setInterpolator(Interpolator.EASE_IN);

        parallelOut = new ParallelTransition(fadeOut);
        parallelOut.setDelay(Duration.seconds(3));  // 3秒后开始消失
        // The onFinished handler will be set by the MessageContainer

        parallelOut.play();
    }

}