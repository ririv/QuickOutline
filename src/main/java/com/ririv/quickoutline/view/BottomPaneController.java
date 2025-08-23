package com.ririv.quickoutline.view;

import com.google.inject.Inject;
import com.ririv.quickoutline.event.*;
import com.ririv.quickoutline.utils.LocalizationManager;
import com.ririv.quickoutline.view.controls.PopupCard;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;

import java.util.ResourceBundle;

public class BottomPaneController {

    private final ResourceBundle bundle = LocalizationManager.getResourceBundle();
    private final GetContentsPopupController getContentsPopupController;
    private final SetContentsPopupController setContentsPopupController;
    private final AppEventBus eventBus;

    public Button switchToTreeEditViewBtn;
    public Button switchToTextEditViewBtn;

    private PopupCard getContentsPopup;
    private PopupCard setContentsPopup;

    @FXML public Button getContentsBtn;
    @FXML public Button setContentsBtn;
    @FXML public TextField offsetTF;
    @FXML public Button deleteBtn;

    @FXML public GridPane outlineBottomPane;

    @Inject
    public BottomPaneController(GetContentsPopupController getContentsPopupController, SetContentsPopupController setContentsPopupController, AppEventBus eventBus) {
        this.getContentsPopupController = getContentsPopupController;
        this.setContentsPopupController = setContentsPopupController;
        this.eventBus = eventBus;
    }

    @FXML
    public void initialize() {


        offsetTF.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.isEmpty() && !newValue.matches("^-?[1-9]\\d*$|^0$|^-$")) {
                if (newValue.charAt(0) == '-') {
                    newValue = newValue.substring(1);
                    newValue = newValue.replaceAll("[^0-9]", "");
                    newValue = '-'+newValue;
                }
                else newValue = newValue.replaceAll("[^0-9]", "");
                offsetTF.setText(newValue);
            }
        });



        // Initialize popups once
        getContentsPopup = new PopupCard(getContentsPopupController);
        setContentsPopup = new PopupCard(setContentsPopupController);

        getContentsBtn.addEventHandler(MouseEvent.MOUSE_ENTERED, getContentsPopup::showEventHandler);
        setContentsBtn.addEventHandler(MouseEvent.MOUSE_ENTERED, setContentsPopup::showEventHandler);
    }

    @FXML
    private void getContentsBtnAction(ActionEvent event) {
        eventBus.publish(new GetContentsEvent());
    }

    @FXML
    private void setContentsBtnAction(ActionEvent event) {
        eventBus.publish(new SetContentsEvent());
    }

    @FXML
    public void deleteBtnAction(ActionEvent event) {
        eventBus.publish(new DeleteContentsEvent());
    }

    public int getOffset() {
        String offsetText = this.offsetTF.getText();
        try {
            return Integer.parseInt(offsetText != null && !offsetText.isEmpty() && !offsetText.equals("-") ? offsetText : "0");
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        return 0;
    }

    @FXML
    private void switchEditViewAction(ActionEvent event) {
        Button btn = (Button) event.getSource();
        if (btn == switchToTreeEditViewBtn) {
            switchToTreeEditViewBtn.setVisible(false);
            switchToTextEditViewBtn.setVisible(true);
            eventBus.publish(new SwitchBookmarkViewEvent(SwitchBookmarkViewEvent.View.TEXT));
        } else if (btn == switchToTextEditViewBtn) {
            switchToTreeEditViewBtn.setVisible(true);
            switchToTextEditViewBtn.setVisible(false);
            eventBus.publish(new SwitchBookmarkViewEvent(SwitchBookmarkViewEvent.View.TREE));
        }
    }
}
