package com.ririv.quickoutline.view.controls;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.stage.Popup;

import java.util.concurrent.atomic.AtomicReference;

// 使用Popup，没成功实现
public class MessageContainer2 {
    private double topPos = 0;
    private Node root;

    public MessageContainer2(Node root){
        this.root = root;
    }

    @FXML
    public void initialize() {
    }

    public void showMessage(String text, Message.MessageType messageType) {
        Message message = new Message(text, messageType);
        AtomicReference<Popup> popup = new AtomicReference<>(new Popup());
        message.setMouseTransparent(true);
        popup.get().getScene().setRoot(message);
        topPos +=50;


        message.parallelOut.setOnFinished(event -> {
            topPos -= 50;
            popup.get().getContent().clear();
            popup.get().hide();
            popup.set(null);
        });
        popup.get().setY(topPos);
        popup.get().show(root.getScene().getWindow());
    }

}
