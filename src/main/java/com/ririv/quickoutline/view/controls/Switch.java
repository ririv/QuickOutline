package com.ririv.quickoutline.view.controls;

import javafx.animation.TranslateTransition;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.BooleanPropertyBase;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.css.PseudoClass;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

// https://stackoverflow.com/questions/73036432/animate-custom-switch-button/73043801#73043801
public class Switch extends HBox {
    private static final double TRACK_WIDTH = 40;
    private static final double TRACK_HEIGHT = 20; //一般高度为宽度的一半
    private static final double KNOB_DIAMETER = TRACK_HEIGHT *4/5;
    private static final Duration ANIMATION_DURATION = Duration.seconds(0.3);
    private static final PseudoClass ON_PSEUDO_CLASS = PseudoClass.getPseudoClass("on");


    private final TranslateTransition onTransition;
    private final TranslateTransition offTransition;

    @FXML
    public StringProperty text = new SimpleStringProperty();

    // Note: 请不要叫on，因为以on开头的，fxml会认为是一个事件处理器，导致错误
    @FXML
    public BooleanProperty value =
            new BooleanPropertyBase(false) {
                @Override protected void invalidated() {
                    pseudoClassStateChanged(ON_PSEUDO_CLASS, get());
                }

                @Override public Object getBean() {
                    return Switch.this;
                }

                @Override public String getName() {
                    return "value";
                }
            };

    public Switch() {
        // construct switch UI
        getStylesheets().add(getClass().getResource("Switch.css").toExternalForm());
        getStyleClass().add("switch");

//        Rectangle track = new Rectangle(TRACK_WIDTH, TRACK_HEIGHT);
//        track.getStyleClass().add("track");
//        track.setArcHeight(track.getHeight());
//        track.setArcWidth(track.getHeight());
//        setAlignment(track, Pos.CENTER_LEFT);

        Button knob = new Button();
        knob.getStyleClass().add("knob");
        knob.setShape(new Circle(KNOB_DIAMETER / 2));
        knob.setMaxSize(KNOB_DIAMETER, KNOB_DIAMETER);
        knob.setMinSize(KNOB_DIAMETER, KNOB_DIAMETER);
        knob.setFocusTraversable(false);

        this.setSpacing(10.0);
        this.setAlignment(Pos.CENTER_LEFT);

        Label textLabel = new Label();

        StackPane container = new StackPane();
        container.setMaxSize(TRACK_WIDTH+1, TRACK_HEIGHT+1);
        container.setMinSize(TRACK_WIDTH+1, TRACK_HEIGHT+1);
        container.getStyleClass().add("container");
        container.getChildren().addAll(knob);
        StackPane.setAlignment(knob, Pos.CENTER_LEFT);

        getChildren().addAll(textLabel, container);

        setMinSize(TRACK_WIDTH, TRACK_HEIGHT);

        // define animations
        onTransition = new TranslateTransition(ANIMATION_DURATION, knob);
        onTransition.setFromX(0);
        onTransition.setToX(TRACK_WIDTH - KNOB_DIAMETER);

        offTransition = new TranslateTransition(ANIMATION_DURATION, knob);
        offTransition.setFromX(TRACK_WIDTH - KNOB_DIAMETER);
        offTransition.setToX(0);

        // add event handling
        EventHandler<Event> click = e -> setValue(!getValue());
        container.setOnMouseClicked(click);
        knob.setMouseTransparent(true);

        valueProperty().addListener((observable, wasOn, nowOn) -> updateState(nowOn));
        updateState(getValue());

        textLabel.textProperty().bind(text);
    }


    private void updateState(Boolean nowOn) {
        onTransition.stop();
        offTransition.stop();

        if (nowOn != null && nowOn) {
            onTransition.play();
        } else {
            offTransition.play();
        }
    }

    public void setValue(boolean value) {
        this.value.set(value);
    }

    public boolean getValue() {
        return value.get();
    }

    public BooleanProperty valueProperty() {
        return value;
    }

    public String getText() {
        return text.get();
    }

    public void setText(String text) {
        this.text.set(text);
    }

    public StringProperty textProperty() {
        return text;
    }

}