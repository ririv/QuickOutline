package com.ririv.quickoutline.view.bookmarktab;

import jakarta.inject.Inject;
import com.ririv.quickoutline.pdfProcess.ViewScaleType;
import com.ririv.quickoutline.view.state.BookmarkSettingsState;
import com.ririv.quickoutline.view.state.CurrentFileState;
import com.ririv.quickoutline.view.utils.LocalizationManager;
import com.ririv.quickoutline.view.controls.PopupCard;
import com.ririv.quickoutline.view.event.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.GridPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ResourceBundle;
import java.util.function.UnaryOperator;

public class BookmarkBottomPaneController {

    private static final Logger logger = LoggerFactory.getLogger(BookmarkBottomPaneController.class);

    private final ResourceBundle bundle = LocalizationManager.getResourceBundle();
    private final GetContentsPopupController getContentsPopupController;
    private final SetContentsPopupController setContentsPopupController;
    private final AppEventBus eventBus;
    private final CurrentFileState currentFileState;
    private final BookmarkSettingsState bookmarkSettingsState;

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
        public BookmarkBottomPaneController(GetContentsPopupController getContentsPopupController, SetContentsPopupController setContentsPopupController, AppEventBus eventBus, CurrentFileState currentFileState, BookmarkSettingsState bookmarkSettingsState) {
        this.getContentsPopupController = getContentsPopupController;
        this.setContentsPopupController = setContentsPopupController;
        this.eventBus = eventBus;
        this.currentFileState = currentFileState;
        this.bookmarkSettingsState = bookmarkSettingsState;
    }

    @FXML
    public void initialize() {

        currentFileState.srcFileProperty().addListener((obs, old, nu) -> {
            if (nu != null) {
                offsetTF.setText(null);
            }
        });

        // Bind the offset text field to the shared state manually
        bookmarkSettingsState.offsetProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                offsetTF.setText("");
            } else {
                if (!newVal.toString().equals(offsetTF.getText())) {
                    offsetTF.setText(newVal.toString());
                }
            }
        });

        offsetTF.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.isEmpty() || "-".equals(newVal)) {
                bookmarkSettingsState.setOffset(0);
            } else {
                try {
                    bookmarkSettingsState.setOffset(Integer.parseInt(newVal));
                } catch (NumberFormatException e) {
                    // Invalid number format, set state to null
                    bookmarkSettingsState.setOffset(0);
                }
            }
        });

        // Use a TextFormatter to allow only integer input
        UnaryOperator<TextFormatter.Change> integerFilter = change -> {
            String newText = change.getControlNewText();
            if (newText.matches("-?(\\d*)")) { // Allows empty, a minus sign, and digits
                return change;
            }
            return null; // Reject the change
        };

        offsetTF.setTextFormatter(new TextFormatter<>(integerFilter));

        // Setup for getContentsBtn
        getContentsPopup = new PopupCard(getContentsPopupController);
        getContentsPopup.setPosition(PopupCard.PopupPosition.TOP_CENTER);
        getContentsPopup.setTriggers(PopupCard.TriggerType.INSTANT_ON_HOVER);
        getContentsPopup.attachTo(getContentsBtn);

        // Setup for setContentsBtn
        setContentsPopup = new PopupCard(setContentsPopupController);
        setContentsPopup.setPosition(PopupCard.PopupPosition.TOP_CENTER);
        setContentsPopup.setTriggers(PopupCard.TriggerType.INSTANT_ON_HOVER);
        setContentsPopup.attachTo(setContentsBtn);
    }

    @FXML
    private void getContentsBtnAction(ActionEvent event) {
        if (getContentsPopupController.getSelected() == getContentsPopupController.bookmarkBtn) {
            eventBus.post(new GetContentsEvent());
        } else if (getContentsPopupController.getSelected() == getContentsPopupController.tocBtn) {
            eventBus.post(new ExtractTocEvent());
        }
    }

    @FXML
    private void setContentsBtnAction(ActionEvent event) {
        ViewScaleType selectedType = setContentsPopupController.getSelectedViewScaleType();
        eventBus.post(new SetContentsEvent(selectedType));
    }

    

    @FXML
    public void deleteBtnAction(ActionEvent event) {
        eventBus.post(new DeleteContentsEvent());
    }

    

    @FXML
    private void switchEditViewAction(ActionEvent event) {
        Button btn = (Button) event.getSource();
        if (btn == switchToTreeEditViewBtn) {
            switchToTreeEditViewBtn.setVisible(false);
            switchToTextEditViewBtn.setVisible(true);
            eventBus.post(new SwitchBookmarkViewEvent(SwitchBookmarkViewEvent.View.TEXT));
        } else if (btn == switchToTextEditViewBtn) {
            switchToTreeEditViewBtn.setVisible(true);
            switchToTextEditViewBtn.setVisible(false);
            eventBus.post(new SwitchBookmarkViewEvent(SwitchBookmarkViewEvent.View.TREE));
        }
    }
}
