package com.ririv.quickoutline.view;

import com.google.inject.Inject;
import com.ririv.quickoutline.event.AppEventBus;
import com.ririv.quickoutline.event.ExtractTocEvent;
import com.ririv.quickoutline.service.PdfTocExtractorService;
import com.ririv.quickoutline.utils.LocalizationManager;
import com.ririv.quickoutline.view.controls.Switch;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.util.ResourceBundle;

public class GetContentsPopupController extends StackPane {

    private final ResourceBundle bundle = LocalizationManager.getResourceBundle();
    private final AppEventBus eventBus;
    private final PdfTocExtractorService pdfTocExtractorService;


    @FXML
    private Button extractTocBtn;

    @FXML
    private ToggleGroup myToggleGroup;

    @Inject
    public GetContentsPopupController(AppEventBus eventBus, PdfTocExtractorService pdfTocExtractorService) {
        this.eventBus = eventBus;
        this.pdfTocExtractorService = pdfTocExtractorService;

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
        extractTocBtn.setOnAction(event -> {
            eventBus.post(new ExtractTocEvent());
        });

        // Add a listener to the ToggleGroup to prevent deselection
        myToggleGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == null) {
                myToggleGroup.selectToggle(oldToggle);
            }
        });
    }
}