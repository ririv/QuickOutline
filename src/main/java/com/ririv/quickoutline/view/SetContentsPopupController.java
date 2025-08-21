package com.ririv.quickoutline.view;

import com.google.inject.Inject;
import com.ririv.quickoutline.event.AppEventBus;
import com.ririv.quickoutline.event.SetContentsEvent;
import com.ririv.quickoutline.pdfProcess.ViewScaleType;
import com.ririv.quickoutline.utils.LocalizationManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.util.ResourceBundle;

public class SetContentsPopupController extends StackPane {

    private final AppEventBus eventBus;

    @FXML
    private Label viewScaleTypeLabel;

    @FXML
    private ToggleGroup viewScaleToggleGroup;

    @FXML
    private ToggleButton fitToWidthBtn;

    @FXML
    private ToggleButton fitToHeightBtn;

    @FXML
    private ToggleButton actualSizeBtn;

    @Inject
    public SetContentsPopupController(AppEventBus eventBus) {
        this.eventBus = eventBus;

        FXMLLoader fxmlLoader = new FXMLLoader(
                getClass().getResource("SetContentsPopup.fxml"),
                LocalizationManager.getResourceBundle()
        );
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void initialize() {
        ResourceBundle bundle = LocalizationManager.getResourceBundle();

        viewScaleToggleGroup.selectedToggleProperty().addListener( (event,oldValue, newValue) -> {
            SimpleStringProperty labelText = new SimpleStringProperty();
            ViewScaleType viewScaleType;
            if (newValue == fitToWidthBtn){
                labelText.set(bundle.getString("viewScaleTypeLabel.FIT_TO_WIDTH"));
                viewScaleType = ViewScaleType.FIT_TO_WIDTH;
            } else if (newValue == fitToHeightBtn){
                labelText.set(bundle.getString("viewScaleTypeLabel.FIT_TO_HEIGHT"));
                viewScaleType = ViewScaleType.FIT_TO_HEIGHT;
            } else if (newValue == actualSizeBtn){
                labelText.set(bundle.getString("viewScaleTypeLabel.ACTUAL_SIZE"));
                viewScaleType = ViewScaleType.ACTUAL_SIZE;
            } else { // null
                labelText.set(bundle.getString("viewScaleTypeLabel.NONE"));
                viewScaleType = ViewScaleType.NONE;
            }
            viewScaleTypeLabel.textProperty().bind(labelText);
            eventBus.publish(new SetContentsEvent(viewScaleType));
            });
    }

}