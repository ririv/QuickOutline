package com.ririv.quickoutline.view.bookmarkTab;

import com.google.inject.Inject;
import com.ririv.quickoutline.event.AppEventBus;
import com.ririv.quickoutline.pdfProcess.ViewScaleType;
import com.ririv.quickoutline.utils.LocalizationManager;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
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
    private final ResourceBundle bundle = LocalizationManager.getResourceBundle();

    @FXML
    private ToggleGroup viewScaleToggleGroup;
    @FXML
    private ToggleButton fitToHeightBtn;
    @FXML
    private ToggleButton fitToWidthBtn;
    @FXML
    private ToggleButton actualSizeBtn;
    @FXML
    private Label viewScaleTypeLabel;

    private final ObjectProperty<ViewScaleType> viewScaleTypeProperty = new SimpleObjectProperty<>(ViewScaleType.NONE);

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

    @FXML
    public void initialize() {
        // Bind toggle group to the property
        viewScaleToggleGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == null) {
                viewScaleTypeProperty.set(ViewScaleType.NONE);
            } else if (newToggle == fitToHeightBtn) {
                viewScaleTypeProperty.set(ViewScaleType.FIT_TO_HEIGHT);
            } else if (newToggle == fitToWidthBtn) {
                viewScaleTypeProperty.set(ViewScaleType.FIT_TO_WIDTH);
            } else if (newToggle == actualSizeBtn) {
                viewScaleTypeProperty.set(ViewScaleType.ACTUAL_SIZE);
            }
        });

        // Bind label to the property
        viewScaleTypeProperty.addListener((obs, oldVal, newVal) -> {
            String text = switch (newVal) {
                case FIT_TO_HEIGHT -> bundle.getString("viewScaleTypeLabel.FIT_TO_HEIGHT");
                case FIT_TO_WIDTH -> bundle.getString("viewScaleTypeLabel.FIT_TO_WIDTH");
                case ACTUAL_SIZE -> bundle.getString("viewScaleTypeLabel.ACTUAL_SIZE");
                default -> bundle.getString("viewScaleTypeLabel.NONE");
            };
            viewScaleTypeLabel.setText(text);
        });

    }

    @Override
    public void requestFocus() {
        // This method is called when the popup is shown, we don't publish event here anymore.
    }

    public ViewScaleType getSelectedViewScaleType() {
        return viewScaleTypeProperty.get();
    }
}
