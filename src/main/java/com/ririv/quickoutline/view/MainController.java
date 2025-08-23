package com.ririv.quickoutline.view;

import com.google.inject.Inject;
import com.ririv.quickoutline.event.*;
import com.ririv.quickoutline.event.ShowSuccessDialogEvent;
import com.ririv.quickoutline.exception.BookmarkFormatException;
import com.ririv.quickoutline.exception.EncryptedPdfException;
import com.ririv.quickoutline.exception.NoOutlineException;
import com.ririv.quickoutline.model.Bookmark;
import com.ririv.quickoutline.pdfProcess.ViewScaleType;
import com.ririv.quickoutline.service.PdfTocExtractorService;
import com.ririv.quickoutline.state.CurrentFileState;

import com.ririv.quickoutline.service.PdfOutlineService;
import com.ririv.quickoutline.utils.LocalizationManager;
import com.ririv.quickoutline.utils.OsDesktopUtil;
import com.ririv.quickoutline.view.controls.Message;
import com.ririv.quickoutline.view.controls.MessageContainer;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.ResourceBundle;

import static com.ririv.quickoutline.view.MyAlert.showAlert;

public class MainController {

    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(MainController.class);
    private final ResourceBundle bundle = LocalizationManager.getResourceBundle();

    public TextField filepathTF;
    public Button browseFileBtn;
    public StackPane root;

    // Child controllers
    public LeftPaneController leftPaneController;
    public PdfPreviewController pdfPreviewTabViewController;

    @FXML
    private BookmarkBottomPaneController bookmarkBottomPaneController;

    public MessageContainer messageManager;
    public BorderPane leftPane;

    
    private final CurrentFileState currentFileState;
    private final AppEventBus eventBus;

    @Inject
    public MainController(CurrentFileState currentFileState, AppEventBus eventBus) {
        this.currentFileState = currentFileState;
        this.eventBus = eventBus;
    }

    @FXML
    private Node bookmarkTabView;
    @FXML
    private BookmarkTabController bookmarkTabViewController;
    @FXML
    private Node tocGeneratorTabView;
    @FXML
    private Node pageLabelTabView;
    @FXML
    private Node pdfPreviewTabView;

    public enum FnTab {
        bookmark, toc, setting, label, preview, tocGenerator
    }

    private final ObjectProperty<FnTab> currentTabProperty = new SimpleObjectProperty<>(FnTab.bookmark);

    @FXML
    public void initialize() {
        // Register event listeners
        eventBus.subscribe(SwitchTabEvent.class, event -> currentTabProperty.set(event.targetTab));
        eventBus.subscribe(ShowMessageEvent.class, event -> messageManager.showMessage(event.message, event.messageType));
        eventBus.subscribe(ShowSuccessDialogEvent.class, this::handleShowSuccessDialog);

        // Bind tab visibility to currentTabProperty
        bookmarkTabView.visibleProperty().bind(currentTabProperty.isEqualTo(FnTab.bookmark));
        tocGeneratorTabView.visibleProperty().bind(currentTabProperty.isEqualTo(FnTab.tocGenerator));
        pageLabelTabView.visibleProperty().bind(currentTabProperty.isEqualTo(FnTab.label));
        pdfPreviewTabView.visibleProperty().bind(currentTabProperty.isEqualTo(FnTab.preview));

        // Drag and drop logic
        root.setOnDragOver(event -> {
            String pdfFormatPattern = ".+\\.[pP][dD][fF]$";
            Dragboard dragboard = event.getDragboard();
            if (dragboard.hasFiles()) {
                File file = dragboard.getFiles().getFirst();
                if (file.getName().matches(pdfFormatPattern)) {
                    event.acceptTransferModes(TransferMode.LINK);
                }
            }
        });

        root.setOnDragDropped(e -> {
            Dragboard dragboard = e.getDragboard();
            File file = dragboard.getFiles().getFirst();
            openFile(file);
        });

        browseFileBtn.setOnAction(event -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(bundle.getString("fileChooser.fileTypeText"), "*.pdf"));
            File file = fileChooser.showOpenDialog(null);
            openFile(file);
        });
    }

    private void openFile(File file) {
        if (file == null) return;

        Path newFilePath = file.toPath();
        Path oldFile = currentFileState.getSrcFile();
        if (oldFile != null && oldFile.equals(newFilePath)) return;

        if (!bookmarkTabViewController.getContents().isEmpty()) {
            ButtonType keepContentsTextBtnType = new ButtonType(bundle.getString("btnType.keepContents"), ButtonBar.ButtonData.OK_DONE);
            ButtonType noKeepContentsTextBtnType = new ButtonType(bundle.getString("btnType.noKeepContents"), ButtonBar.ButtonData.OK_DONE);
            ButtonType cancelBtnType = new ButtonType(bundle.getString("btnType.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
            Optional<ButtonType> result = showAlert(
                    Alert.AlertType.CONFIRMATION,
                    bundle.getString("alert.unsavedConfirmation"),
                    root.getScene().getWindow(),
                    keepContentsTextBtnType, noKeepContentsTextBtnType, cancelBtnType);

            if (result.isPresent() && result.get() == cancelBtnType) {
                return;
            }
        }

        try {
            currentFileState.setSrcFile(newFilePath);
        } catch (java.io.IOException e) { // Catch standard IO Exception
            messageManager.showMessage(bundle.getString("message.cannotOpenDoc") + e.getMessage(), Message.MessageType.ERROR);
        } catch (EncryptedPdfException e) {
            messageManager.showMessage(bundle.getString("message.encryptedDoc"), Message.MessageType.WARNING);
        } catch (com.itextpdf.io.exceptions.IOException e) { // Catch iText specific IO Exception
            e.printStackTrace();
            logger.info(String.valueOf(e));
            messageManager.showMessage(bundle.getString("message.corruptedDoc") + e.getMessage(), Message.MessageType.ERROR);
        }
    }


    private void handleShowSuccessDialog(ShowSuccessDialogEvent event) {
        ButtonType openDirAndSelectFileButtonType = new ButtonType(bundle.getString("btnType.openFileLocation"), ButtonBar.ButtonData.OK_DONE);
        ButtonType openFileButtonType = new ButtonType(bundle.getString("btnType.openFile"), ButtonBar.ButtonData.OK_DONE);
        var result = showAlert(Alert.AlertType.INFORMATION,
                bundle.getString("alert.FileSavedAt") + currentFileState.getDestFile().toString(), root.getScene().getWindow(),
                openDirAndSelectFileButtonType, openFileButtonType, new ButtonType("OK", ButtonBar.ButtonData.CANCEL_CLOSE));
        try {
            String destFilePath = currentFileState.getDestFile().toString();
            if (result.isPresent() && result.get() == openDirAndSelectFileButtonType) {
                OsDesktopUtil.openFileLocation(destFilePath);
            } else if (result.isPresent() && result.get().equals(openFileButtonType)) {
                OsDesktopUtil.openFile(destFilePath);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
}
