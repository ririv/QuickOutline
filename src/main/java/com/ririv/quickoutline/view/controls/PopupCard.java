package com.ririv.quickoutline.view.controls;

import javafx.animation.PauseTransition;
import javafx.event.Event;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.input.MouseEvent;
import javafx.stage.Popup;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class PopupCard extends Popup {
    private static final Logger logger = LoggerFactory.getLogger(PopupCard.class);

    PauseTransition delay = new javafx.animation.PauseTransition(Duration.seconds(2));;
    private boolean hideAfterDelayWhenEscaped = true;

    public PopupCard(Parent parent) {

        parent.getStylesheets().add(getClass().getResource("PopupCard.css").toExternalForm());
        parent.getStyleClass().add("card");
        this.getScene().setRoot(parent);
        this.setAutoHide(true);

        // 如果不设置宽高，第一出现popup是他们的值为0，导致出现位置错误
        // 已改为监听宽高，修复错误
//        this.setWidth(popupNode.getPrefWidth());
//        this.setHeight(popupNode.getPrefHeight());
        if (hideAfterDelayWhenEscaped) {
            parent.addEventHandler(MouseEvent.MOUSE_EXITED, event -> hideAfterDelay());
            parent.addEventHandler(MouseEvent.MOUSE_ENTERED, event -> stopDelayHide());
        }
    }

    public void showEventHandler(Event event) {
        Node ownerNode = (Node) event.getSource();
        Bounds buttonBounds = ownerNode.localToScreen( ownerNode.getBoundsInLocal());
        this.widthProperty().addListener((observable, oldValue, newValue) -> {
            double x = buttonBounds.getCenterX() - newValue.doubleValue()/2;
            this.setX(x);
            logger.info("x: {}", x);
        });
        this.heightProperty().addListener((observable, oldValue, newValue) -> {
            double y = buttonBounds.getMinY() - newValue.doubleValue() - 5;
            this.setY(y);
            logger.info("y: {}", y);
        });

        logger.debug("buttonBounds: {}", buttonBounds);
        logger.debug("popup: w:{} h:{}", this.getWidth(), this.getHeight());
        this.show(ownerNode.getScene().getWindow());
    }

    private void hideAfterDelay() {
        delay.setOnFinished(event2 -> {
            this.hide();
            logger.info("popup hide");
        });
        delay.play();
        logger.info("popup hide start");
    }



    private void stopDelayHide() {
        if (delay!= null && delay.getStatus() == javafx.animation.Animation.Status.RUNNING){
            delay.stop();
            logger.info("popup hide stop");
        }
    }

    public void setHideAfterDelayWhenEscaped(boolean value){
        this.hideAfterDelayWhenEscaped = value;
    }

    public void setHideAfterDelayWhenEscaped(boolean value, Duration duration){
        this.hideAfterDelayWhenEscaped = value;
        setHideDelay(duration);
    }

    public void setHideDelay(Duration duration) {
        delay = new PauseTransition(duration);
    }


}
