package com.ririv.quickoutline.view;

import com.google.inject.Inject;
import com.ririv.quickoutline.event.AppEventBus;
import com.ririv.quickoutline.event.ExtractTocEvent;
import com.ririv.quickoutline.service.PdfTocService;
import com.ririv.quickoutline.utils.LocalizationManager;
import com.ririv.quickoutline.view.controls.Switch;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.util.ResourceBundle;

public class GetContentsPopupController extends StackPane {

    private final ResourceBundle bundle = LocalizationManager.getResourceBundle();
    private final AppEventBus eventBus;
    private final PdfTocService pdfTocService;

    public HBox pageNumRangeLayout;

    @FXML
    private Switch autoRecognizeSwitch;

    @FXML
    private TextField startTF;

    @FXML
    private TextField endTF;

    @FXML
    private Button extractTocBtn;

    private int backspaceCount = 0;

    private final BooleanProperty autoRecognize = new SimpleBooleanProperty(true);

    @Inject
    public GetContentsPopupController(AppEventBus eventBus, PdfTocService pdfTocService) {
        this.eventBus = eventBus;
        this.pdfTocService = pdfTocService;

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
        autoRecognize.bind(autoRecognizeSwitch.valueProperty());
        pageNumRangeLayout.disableProperty().bind(autoRecognizeSwitch.valueProperty());

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

        extractTocBtn.setOnAction(event -> {
            Integer startPage = null;
            Integer endPage = null;
            if (!autoRecognize.get()) {
                if (startTF.getText().isEmpty() || endTF.getText().isEmpty()) {
                    // Maybe publish a ShowMessageEvent here in the future
                    return;
                }
                startPage = Integer.parseInt(startTF.getText());
                endPage = Integer.parseInt(endTF.getText());
            }
            eventBus.publish(new ExtractTocEvent(startPage, endPage));
        });
    }
}