package com.ririv.quickoutline.view;

import com.google.inject.Inject;
import com.ririv.quickoutline.view.event.AppEventBus;
import com.ririv.quickoutline.view.event.SwitchTabEvent;
import com.ririv.quickoutline.view.MainController.FnTab;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Objects;

public class LeftPaneController {

    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(LeftPaneController.class);

    private final AppEventBus eventBus;

    public Node root;
    private Stage helpStage;

    public ToggleButton bookmarkTabBtn;
    public ToggleGroup tabToggleGroup;
    public Button helpBtn;

    public ToggleButton tocGeneratorTabBtn;
    public ToggleButton labelTabBtn;
    public ToggleButton previewTabBtn;

    @Inject
    public LeftPaneController(AppEventBus eventBus) {
        this.eventBus = eventBus;
    }

    public void initialize() {
        bookmarkTabBtn.setSelected(true);

        tabToggleGroup.selectedToggleProperty().addListener((event,oldValue,newValue) -> {
            if (newValue == null){
                tabToggleGroup.selectToggle(oldValue);
                return;
            }

            if (newValue == bookmarkTabBtn) {
                eventBus.post(new SwitchTabEvent(FnTab.bookmark));
            } else if (newValue == tocGeneratorTabBtn) {
                eventBus.post(new SwitchTabEvent(FnTab.tocGenerator));
            } else if (newValue == labelTabBtn) {
                eventBus.post(new SwitchTabEvent(FnTab.label));
            } else if (newValue == previewTabBtn) {
                eventBus.post(new SwitchTabEvent(FnTab.preview));
            }
        });
    }

    @FXML
    public void createHelpWindowAction(ActionEvent actionEvent) throws IOException {
        if (helpStage == null) {
            helpStage = new Stage();

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("HelpWindow.fxml"),
                    LocalizationManager.getResourceBundle()
            );
            Parent helpWinRoot = loader.load();

            helpStage.setTitle("Help");
            helpStage.setScene(new Scene(helpWinRoot, 400, 300));

            helpStage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("icon/help_black.png"))));
            helpStage.setResizable(false);
            helpStage.initOwner(((Node)actionEvent.getSource()).getScene().getWindow());
            helpStage.show();
        }
        else {
            helpStage.show();
            helpStage.requestFocus();
        }
    }
}