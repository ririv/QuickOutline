package com.ririv.quickoutline.view.controls.radioButton2;

import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.layout.HBox;

import java.util.Arrays;
import java.util.List;

public class RadioButton2Group extends HBox {

    private static final String FIRST_IN_GROUP = "first-in-group";
    private static final String MIDDLE_IN_GROUP = "middle-in-group";
    private static final String LAST_IN_GROUP = "last-in-group";
    private static final List<String> POSITIONAL_CLASSES = Arrays.asList(FIRST_IN_GROUP, MIDDLE_IN_GROUP, LAST_IN_GROUP);

    public RadioButton2Group() {
        super();
        // Listen for children being added or removed
        getChildren().addListener((ListChangeListener<Node>) c -> updateStyleClasses());
    }

    private void updateStyleClasses() {
        List<Node> children = getChildren().stream().filter(Node::isVisible).toList();

        // Clear old classes from all children first
        for (Node child : children) {
            child.getStyleClass().removeAll(POSITIONAL_CLASSES);
        }

        if (children.size() > 1) {
            // Add class to the first child
            children.get(0).getStyleClass().add(FIRST_IN_GROUP);

            // Add class to middle children
            for (int i = 1; i < children.size() - 1; i++) {
                children.get(i).getStyleClass().add(MIDDLE_IN_GROUP);
            }
            // Add class to the last child
            children.get(children.size() - 1).getStyleClass().add(LAST_IN_GROUP);
        }
    }
}
