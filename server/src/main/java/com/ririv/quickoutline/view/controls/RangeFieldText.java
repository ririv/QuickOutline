package com.ririv.quickoutline.view.controls;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

public class RangeFieldText {

    @FXML
    private TextField startTF;

    @FXML
    private TextField endTF;

    private int backspaceCount = 0;


    public RangeFieldText() {
        startTF.addEventFilter(KeyEvent.KEY_TYPED, event -> {
            if (!"0123456789".contains(event.getCharacter())) {
                event.consume();
            }
        });

        endTF.addEventFilter(KeyEvent.KEY_TYPED, event -> {
            if (!"0123456789".contains(event.getCharacter())) {
                event.consume();
            }
        });

        endTF.focusTraversableProperty().bind(startTF.focusedProperty());

        endTF.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (endTF.getText().isEmpty()){
                if (event.getCode() == KeyCode.BACK_SPACE) {
                    backspaceCount++;
                    if (backspaceCount == 2) {
                        startTF.requestFocus();
                        backspaceCount = 0;
                    }
                }
            }
        });
    }


    public int getStart() {
        String text = startTF.getText();
        if (text == null || text.isEmpty()) {
            return -1;
        }
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public int getEnd() {
        String text = endTF.getText();
        if (text == null || text.isEmpty()) {
            return -1;
        }
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

}
