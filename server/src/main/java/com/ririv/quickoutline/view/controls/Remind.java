package com.ririv.quickoutline.view.controls;

import javafx.animation.FadeTransition;
import javafx.animation.SequentialTransition;
import javafx.scene.layout.Region;
import javafx.util.Duration;

public class Remind extends Region {

    private final Duration duration = Duration.millis(200);

    public Remind(){
        this.setStyle("-fx-background-color: rgba(255, 0, 0, 0.35); -fx-background-radius: 2px");
        this.setOpacity(0);
        setMouseTransparent(true);
    }

    public void play(){
        FadeTransition fadeTransition1 = new FadeTransition(duration, this);
        fadeTransition1.setFromValue(0.0);
        fadeTransition1.setToValue(1.0);

        FadeTransition fadeTransition2 = new FadeTransition(duration, this);
        fadeTransition2.setFromValue(1.0);
        fadeTransition2.setToValue(0.0);
        SequentialTransition sequentialTransition = new SequentialTransition(fadeTransition1, fadeTransition2);
        sequentialTransition.play();
    }
}
