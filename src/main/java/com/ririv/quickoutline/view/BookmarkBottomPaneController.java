package com.ririv.quickoutline.view;

import com.google.inject.Inject;
import com.ririv.quickoutline.event.*;
import com.ririv.quickoutline.utils.LocalizationManager;
import com.ririv.quickoutline.view.controls.PopupCard;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.input.MouseEvent;
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
        public BookmarkBottomPaneController(GetContentsPopupController getContentsPopupController, SetContentsPopupController setContentsPopupController, AppEventBus eventBus) {
        this.getContentsPopupController = getContentsPopupController;
        this.setContentsPopupController = setContentsPopupController;
        this.eventBus = eventBus;
    }

    @FXML
    public void initialize() {


        // Use a TextFormatter to allow only integer input
        UnaryOperator<TextFormatter.Change> integerFilter = change -> {
            String newText = change.getControlNewText();
            if (newText.matches("-?(\\d*)")) { // Allows empty, a minus sign, and digits
                return change;
            }
            return null; // Reject the change
        };

        offsetTF.setTextFormatter(new TextFormatter<>(integerFilter));

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
        String offsetText = offsetTF.getText();
        if (offsetText == null || offsetText.isEmpty() || "-".equals(offsetText)) {
            return 0;
        }
        try {
            return Integer.parseInt(offsetText);
        } catch (NumberFormatException e) {
            // This should rarely happen thanks to the TextFormatter, but as a fallback.
            logger.warn("Could not parse offset value: {}", offsetText, e);
            return 0;
        }
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
