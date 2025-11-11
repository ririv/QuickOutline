package com.ririv.quickoutline.view.bookmarktab;

import com.google.common.eventbus.Subscribe;
import com.ririv.quickoutline.service.PdfOutlineService;
import com.ririv.quickoutline.service.syncWithExternelEditor.SyncWithExternalEditorService;
import com.ririv.quickoutline.textProcess.methods.Method;
import com.ririv.quickoutline.utils.OsDesktopUtil;
import com.ririv.quickoutline.utils.Pair;
import com.ririv.quickoutline.view.LocalizationManager;
import com.ririv.quickoutline.view.controls.EditorTextArea;
import com.ririv.quickoutline.view.controls.Remind;
import com.ririv.quickoutline.view.event.AppEventBus;
import com.ririv.quickoutline.view.event.AutoToggleToIndentEvent;
import com.ririv.quickoutline.view.event.BookmarksChangedEvent;
import jakarta.inject.Inject;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import org.slf4j.Logger;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

import static com.ririv.quickoutline.view.MyAlert.showAlert;

public class TextSubViewController {
    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(TextSubViewController.class);

    private final ResourceBundle bundle = LocalizationManager.getResourceBundle();
    private final PdfOutlineService pdfOutlineService;

    private final SyncWithExternalEditorService syncWithExternalEditorService;
    private final AppEventBus eventBus;

    public EditorTextArea contentsTextArea;
    public Button externalEditorBtn;
    public Button autoFormatBtn;
    public Label mask;
    public HBox root;

    @FXML public RadioButton seqRBtn;
    @FXML public RadioButton indentRBtn;
    @FXML public ToggleGroup methodToggleGroup;
    @FXML public Remind indentRBtnRemind;
    @FXML public Remind seqRBtnRemind;

    private static final Pattern INDENT_PATTERN = Pattern.compile("^(\\t|\\s{1,4})");

    @Inject
    public TextSubViewController(PdfOutlineService pdfOutlineService,
                                 SyncWithExternalEditorService syncWithExternalEditorService,
                                 AppEventBus eventBus) {
        this.pdfOutlineService = pdfOutlineService;
        this.syncWithExternalEditorService = syncWithExternalEditorService;
        this.eventBus = eventBus;
        this.eventBus.register(this);
    }

    public void initialize() {
        seqRBtn.setUserData(Method.SEQ);
        indentRBtn.setUserData(Method.INDENT);
        seqRBtn.setSelected(true);

        if (OsDesktopUtil.isMacOS()) {
            externalEditorBtn.setVisible(false);
            externalEditorBtn.setManaged(false);
        }
    }

    @Subscribe
    public void onAutoToggleToIndent(AutoToggleToIndentEvent event) {
        autoToggleToIndentMethod();
    }

    @Subscribe
    public void onBookmarksChanged(BookmarksChangedEvent event) {
        Platform.runLater(() -> contentsTextArea.setText(event.getRootBookmark().toOutlineString()));
    }

    @FXML
    private void externalEditorBtnAction() {
        externalEditorBtn.setOnAction(event ->
            syncWithExternalEditorService.exec(
                getCoordinate(),
                fileText -> Platform.runLater(() -> contentsTextArea.setText(fileText)),
                () -> {
                    syncWithExternalEditorService.writeTemp(contentsTextArea.getText());
                    Platform.runLater(() -> {
                        contentsTextArea.setDisable(true);
                        externalEditorBtn.setDisable(true);
                        externalEditorBtn.setText(bundle.getString("btn.externalEditorConnected"));
                        mask.setVisible(true);
                    });
                },
                () -> Platform.runLater(() -> {
                    contentsTextArea.setDisable(false);
                    externalEditorBtn.setDisable(false);
                    externalEditorBtn.setText("VSCode");
                    mask.setVisible(false);
                }),
                () -> Platform.runLater(() -> {
                    ButtonType gotoButton = new ButtonType(bundle.getString("btnType.gotoDownload"), ButtonBar.ButtonData.OK_DONE);
                    ButtonType cancelButton = new ButtonType("OK", ButtonBar.ButtonData.CANCEL_CLOSE);
                    String contentText = bundle.getString("alert.NotFoundVSCodePrompt");
                    Optional<ButtonType> result = showAlert(
                            Alert.AlertType.WARNING, contentText, root.getScene().getWindow(),
                            gotoButton, cancelButton);
                    if (result.isPresent() && result.get() == gotoButton) {
                        try {
                            Desktop.getDesktop().browse(new URI("https://code.visualstudio.com/"));
                        } catch (IOException | URISyntaxException e) {
                            e.printStackTrace();
                        }
                    }
                })
            )
        );
    }

    @FXML
    private void autoFormatBtnAction(ActionEvent event) {
        contentsTextArea.setText(pdfOutlineService.autoFormat(contentsTextArea.getText()));
        syncWithExternalEditorService.writeTemp(contentsTextArea.getText());
        eventBus.post(new AutoToggleToIndentEvent());
    }

    private Pair<Integer, Integer> getCoordinate() {
        return contentsTextArea.getCoordinate();
    }

    public Method getSelectedMethod() {
        return (Method) methodToggleGroup.getSelectedToggle().getUserData();
    }

    public String getContents() {
        return contentsTextArea.getText();
    }

    public void setContents(String contents) {
        contentsTextArea.setText(contents);
    }

    public void autoToggleToIndentMethod() {
        if (methodToggleGroup.getSelectedToggle() != indentRBtn) {
            methodToggleGroup.selectToggle(indentRBtn);
            indentRBtnRemind.play();
        }
    }
}