package com.ririv.quickoutline.view.controls.select;

import javafx.beans.binding.Bindings;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.SkinBase;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;

import java.util.Objects;

public class StyledSelectSkin<T> extends SkinBase<StyledSelect<T>> {

    private final BorderPane box;
    private final Label label;
    private final Region arrow;
    private final SelectPopup<T> popup;

    protected StyledSelectSkin(StyledSelect<T> control) {
        super(control);

        label = new Label();
        label.getStyleClass().add("text");

        arrow = new Region();
        arrow.getStyleClass().add("arrow");
        StackPane arrowContainer = new StackPane(arrow);
        arrowContainer.getStyleClass().add("arrow-container");

        box = new BorderPane();
        box.setCenter(label);
        box.setRight(arrowContainer);
        BorderPane.setAlignment(label, Pos.CENTER_LEFT);
        box.getStyleClass().add("box");

        popup = new SelectPopup<>(control);

        getChildren().add(box);

        label.textProperty().bind(Bindings.createStringBinding(() -> {
            T value = getSkinnable().getValue();
            return value == null ? "" : value.toString();
        }, getSkinnable().valueProperty()));

        control.setOnMouseClicked(event -> {
            if (popup.isShowing()) {
                popup.hide();
            } else {
                popup.buildContent();
                popup.show(control);
            }
        });
    }

    private static class SelectPopup<T> extends Popup {
        private final VBox itemsContainer;
        private final StyledSelect<T> control;
        private boolean isPopupDownwards = true;

        public SelectPopup(StyledSelect<T> control) {
            this.control = control;

            this.itemsContainer = new VBox();
            this.itemsContainer.getStyleClass().add("select-items-container");

            // =================================================================
            // ===========              最终宽度修正                   ===========
            // =================================================================
            // 从控件宽度中减去 8px，以补偿 .select-popup 容器左右各 4px 的 padding。
            this.itemsContainer.minWidthProperty().bind(control.widthProperty().subtract(8));
            this.itemsContainer.prefWidthProperty().bind(control.widthProperty().subtract(8));
            this.itemsContainer.maxWidthProperty().bind(control.widthProperty().subtract(8));
            // =================================================================

            this.setAutoHide(true);

            StackPane root = new StackPane(itemsContainer);
            root.getStyleClass().add("select-popup");

            String cssPath = Objects.requireNonNull(control.getClass().getResource("StyledSelect.css")).toExternalForm();
            if (cssPath != null) {
                root.getStylesheets().add(cssPath);
            }

            getContent().add(root);
        }

        public void buildContent() {
            itemsContainer.getChildren().clear();
            T currentValue = control.getValue();

            for (T item : control.getItems()) {
                if (item == null || item.toString().isEmpty()) {
                    continue;
                }
                Node itemNode = createItemNode(item, item.equals(currentValue));
                itemsContainer.getChildren().add(itemNode);
            }
        }

        private Node createItemNode(T item, boolean isSelected) {
            Label itemLabel = new Label(item.toString());
            StackPane itemNode = new StackPane(itemLabel);
            itemNode.setAlignment(Pos.CENTER_LEFT);
            itemNode.getStyleClass().add("select-item-cell");

            if (isSelected) {
                itemNode.getStyleClass().add("selected");
            }

            itemNode.setOnMouseClicked(event -> {
                control.setValue(item);
                hide();
            });

            return itemNode;
        }

        public void setDirection(boolean isPopupDownwards) {
            this.isPopupDownwards = isPopupDownwards;
        }

        public void show(StyledSelect<T> owner) {
            double shadowRadius = 12.0;
            Point2D screenPos;
            double spacing = 5.0;
            if (isPopupDownwards) {
                screenPos = owner.localToScreen(0, owner.getHeight() + spacing);
            } else {
                screenPos = owner.localToScreen(0, -itemsContainer.getHeight() - spacing);
            }
            this.show(owner, screenPos.getX() - shadowRadius, screenPos.getY() - shadowRadius);
        }
    }
}