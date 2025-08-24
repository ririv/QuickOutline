package com.ririv.quickoutline.view.controls;

import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.layout.AnchorPane;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageContainer extends AnchorPane {
    private static final Logger logger = LoggerFactory.getLogger(MessageContainer.class);
    private static final double SPACING = 10.0;
    private static final double TOP_MARGIN = 10.0;

    public MessageContainer() {
        this.setViewOrder(-99);
        setMouseTransparent(true);
    }

    @FXML
    public void initialize() {
    }

    public void showMessage(String text, Message.MessageType messageType) {
        final Message message = new Message(text, messageType);

        // Restore original centering logic by listening to width changes
        message.widthProperty().addListener((obs, oldVal, newVal) -> {
            if (message.getScene() != null) {
                message.setTranslateX((getScene().getWidth() - newVal.doubleValue()) / 2);
            }
        });
        // Also listen for scene changes to apply centering if the scene is not yet available
        message.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                message.setTranslateX((newScene.getWidth() - message.getWidth()) / 2);
            }
        });

        // Set initial position to avoid jarring animation from the top
        double startY = TOP_MARGIN;
        if (!getChildren().isEmpty()) {
            Node lastMessage = getChildren().get(getChildren().size() - 1);
            startY = lastMessage.getTranslateY() + lastMessage.getBoundsInParent().getHeight() + SPACING;
        }
        message.setTranslateY(startY);

        getChildren().add(message);

        // When the message's fade-out animation is finished, remove it and relayout others
        message.parallelOut.setOnFinished(event -> {
            getChildren().remove(message);
            relayoutMessages();
        });

        // Defer the first layout until the next pulse, so height is calculated
        Platform.runLater(this::relayoutMessages);
    }

    private void relayoutMessages() {
        double currentY = TOP_MARGIN;
        for (Node node : getChildren()) {
            if (node instanceof Message) {
                Message msg = (Message) node;
                double targetY = currentY;

                // Animate the message to its new vertical position
                TranslateTransition tt = new TranslateTransition(Duration.millis(300), msg);
                tt.setToY(targetY);
                tt.play();

                currentY += msg.getHeight() + SPACING;
            }
        }
    }
}

