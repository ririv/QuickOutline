package com.ririv.quickoutline.view;

import com.google.inject.Inject;
import com.ririv.quickoutline.event.*;
import com.ririv.quickoutline.pdfProcess.ViewScaleType;
import com.ririv.quickoutline.textProcess.methods.Method;
import com.ririv.quickoutline.utils.LocalizationManager;
import com.ririv.quickoutline.view.controls.PopupCard;
import com.ririv.quickoutline.view.controls.Remind;
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

    private PopupCard getContentsPopup;
    private PopupCard setContentsPopup;

    @FXML public Button getContentsBtn;
    @FXML public Button setContentsBtn;
    @FXML public TextField offsetTF;
    @FXML public Button deleteBtn;
    @FXML public RadioButton seqRBtn;
    @FXML public RadioButton indentRBtn;
    @FXML public ToggleGroup methodToggleGroup;
    @FXML public Remind indentRBtnRemind;
    @FXML public Remind seqRBtnRemind;
    @FXML public Button setPageLabelBtn;
    @FXML public GridPane outlineBottomPane;
    @FXML public GridPane pageLabelBottomPane;

    @Inject
    public BottomPaneController(GetContentsPopupController getContentsPopupController, SetContentsPopupController setContentsPopupController, AppEventBus eventBus) {
        this.getContentsPopupController = getContentsPopupController;
        this.setContentsPopupController = setContentsPopupController;
        this.eventBus = eventBus;
    }

    @FXML
    public void initialize() {
        eventBus.subscribe(AutoToggleToIndentEvent.class, event -> autoToggleToIndentMethod());

        seqRBtn.setUserData(Method.SEQ);
        indentRBtn.setUserData(Method.INDENT);
        seqRBtn.setSelected(true);

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

        methodToggleGroup.selectedToggleProperty().addListener(event -> {
            eventBus.publish(new ReconstructTreeEvent());
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

    public Method getSelectedMethod() {
        return (Method) methodToggleGroup.getSelectedToggle().getUserData();
    }

    public void autoToggleToIndentMethod() {
        if (methodToggleGroup.getSelectedToggle() != indentRBtn) {
            methodToggleGroup.selectToggle(indentRBtn);
            indentRBtnRemind.play();
        }
    }
}
