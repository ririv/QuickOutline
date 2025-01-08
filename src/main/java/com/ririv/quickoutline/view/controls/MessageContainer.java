package com.ririv.quickoutline.view.controls;

import javafx.fxml.FXML;
import javafx.scene.layout.AnchorPane;


public class MessageContainer extends AnchorPane {
    private double topPos = 0;

    public MessageContainer() {
//        this.setAlignment(Pos.CENTER);
        this.setViewOrder(-99);
//        this.setSpacing(20);
        setMouseTransparent(true); // 使元素不参与交互，只显示
        this.setMaxWidth(0);
    }

    @FXML
    public void initialize() {
    }

    public void showMessage(String text, Message.MessageType messageType) {
        Message message = new Message(text, messageType);
        if (!this.getChildren().isEmpty()){
            topPos +=50;
        }

        this.getChildren().add(message);
        AnchorPane.setTopAnchor(message, topPos);

        message.parallelOut.setOnFinished(event -> {
            topPos -= 50;
        });
    }

}
