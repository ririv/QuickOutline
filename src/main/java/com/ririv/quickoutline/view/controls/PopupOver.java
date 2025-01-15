package com.ririv.quickoutline.view.controls;

import javafx.event.Event;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.stage.Popup;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PopupOver extends Popup {
    private static final Logger logger = LoggerFactory.getLogger(PopupOver.class);


    public PopupOver(Node... nodes) {
        this.getContent().addAll(nodes);
        this.setAutoHide(true);

        // 如果不设置宽高，第一出现popup是他们的值为0，导致出现位置错误
        // 已改为监听宽高，修复错误
//        this.setWidth(popupNode.getPrefWidth());
//        this.setHeight(popupNode.getPrefHeight());
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
            double y = buttonBounds.getMinY() - newValue.doubleValue() - 10;
            this.setY(y);
            logger.info("y: {}", y);
        });

        logger.debug("buttonBounds: {}", buttonBounds);
        logger.debug("popup: w:{} h:{}", this.getWidth(), this.getHeight());
        this.show(ownerNode.getScene().getWindow());
    }

    public void hideEventHandler(Event event) {
        Node ownerNode = (Node) event.getSource();
        // X 秒后隐藏 Popup
        javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(Duration.seconds(2));
        delay.setOnFinished(event2 -> {
            this.hide();
            logger.info("popup hide");
        });
        delay.play();
    }


}
