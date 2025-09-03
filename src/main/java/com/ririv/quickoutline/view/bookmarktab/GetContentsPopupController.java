package com.ririv.quickoutline.view.bookmarktab;

import com.ririv.quickoutline.view.LocalizationManager;
import com.ririv.quickoutline.view.controls.radioButton2.RadioButton2;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.util.ResourceBundle;

public class GetContentsPopupController extends StackPane {

    private final ResourceBundle bundle = LocalizationManager.getResourceBundle();

    @FXML
    private ToggleGroup myToggleGroup;

    @FXML
    public RadioButton2 bookmarkBtn;

    @FXML
    public RadioButton2 tocBtn;


    public GetContentsPopupController() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(
                "GetContentsPopup.fxml"),
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
        // Add a listener to the ToggleGroup to prevent deselection
        myToggleGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == null) {
                myToggleGroup.selectToggle(oldToggle);
            }
        });
    }

    public RadioButton2 getSelected() {
        return (RadioButton2) myToggleGroup.getSelectedToggle();
    }
}
