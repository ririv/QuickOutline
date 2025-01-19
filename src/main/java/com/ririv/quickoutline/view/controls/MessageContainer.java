package com.ririv.quickoutline.view.controls;

import javafx.fxml.FXML;
import javafx.scene.layout.AnchorPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MessageContainer extends AnchorPane {
    private static final Logger logger = LoggerFactory.getLogger(MessageContainer.class);

    private double topPos = 0;

    private double messageHeight;

    public MessageContainer() {
//        this.setAlignment(Pos.CENTER);
        this.setViewOrder(-99);
        setMouseTransparent(true); // 使元素不参与交互，只显示
    }

    @FXML
    public void initialize() {
    }

    public void showMessage(String text, Message.MessageType messageType) {
        Message message = new Message(text, messageType);


        message.widthProperty().addListener((observable, oldValue, newValue) -> {
            message.setTranslateX((message.getScene().getWidth()-newValue.doubleValue())/2);
            logger.info("message width:: oldValue: {}, newValue: {}", oldValue, newValue);
        });

        message.heightProperty().addListener((observable, oldValue, newValue) -> {
            messageHeight = newValue.doubleValue();
            logger.info("message height:: oldValue: {}, newValue: {}", oldValue, newValue);
        });

        if (!this.getChildren().isEmpty()){
            topPos += 10 + messageHeight;
        }

        this.getChildren().add(message);
        AnchorPane.setTopAnchor(message, topPos);

        message.parallelOut.setOnFinished(event -> {
            topPos -= 10 + messageHeight;
        });
    }

}
