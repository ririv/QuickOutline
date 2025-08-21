package com.ririv.quickoutline.view;

import com.google.inject.Inject;
import com.ririv.quickoutline.event.*;
import com.ririv.quickoutline.exception.BookmarkFormatException;
import com.ririv.quickoutline.exception.EncryptedPdfException;
import com.ririv.quickoutline.exception.NoOutlineException;
import com.ririv.quickoutline.model.Bookmark;
import com.ririv.quickoutline.pdfProcess.ViewScaleType;
import com.ririv.quickoutline.service.PdfTocService;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

import static com.ririv.quickoutline.view.MyAlert.showAlert;

public class MainController {

    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(MainController.class);
    private final ResourceBundle bundle = LocalizationManager.getResourceBundle();

    public TextField filepathTF;
    public Button browseFileBtn;
    public StackPane root;
    private final ObjectProperty<ViewScaleType> viewScaleTypeProperty = new SimpleObjectProperty<>(ViewScaleType.NONE);

    // Child controllers
    public TextTabController textTabViewController;
    public TreeTabController treeTabViewController;
    public LeftPaneController leftPaneController;
    public PdfPreviewController pdfPreviewTabViewController;

    // Included controller
    @FXML
    private BottomPaneController bottomPaneController;

    public MessageContainer messageManager;
    public BorderPane leftPane;

    private final PdfOutlineService pdfOutlineService;
    private final PdfTocService pdfTocService;
    private final CurrentFileState currentFileState;
    private final AppEventBus eventBus;

    @Inject
    public MainController(PdfOutlineService pdfOutlineService, PdfTocService pdfTocService, CurrentFileState currentFileState, AppEventBus eventBus) {
        this.pdfOutlineService = pdfOutlineService;
        this.pdfTocService = pdfTocService;
        this.currentFileState = currentFileState;
        this.eventBus = eventBus;
    }

    @FXML
    private Node textTabView;
    @FXML
    private Node treeTabView;
    @FXML
    private Node tocGeneratorTabView;
    @FXML
    private Node pageLabelTabView;
    @FXML
    private Node pdfPreviewTabView;

    public enum FnTab {
        text, tree, toc, setting, label, preview
    }

    private final ObjectProperty<FnTab> currentTabProperty = new SimpleObjectProperty<>(FnTab.text);

    @FXML
    public void initialize() {
        // Register event listeners
        eventBus.subscribe(GetContentsEvent.class, this::handleGetContents);
        eventBus.subscribe(SetContentsEvent.class, this::handleSetContents);
        eventBus.subscribe(DeleteContentsEvent.class, this::handleDeleteContents);
        eventBus.subscribe(ExtractTocEvent.class, this::handleExtractToc);
        eventBus.subscribe(AutoToggleToIndentEvent.class, event -> autoToggleToIndentMethod());
        eventBus.subscribe(ViewScaleChangedEvent.class, event -> viewScaleTypeProperty.set(event.viewScaleType));
        eventBus.subscribe(ReconstructTreeEvent.class, this::handleReconstructTree);
        eventBus.subscribe(SwitchTabEvent.class, event -> currentTabProperty.set(event.targetTab));

        // Bind tab visibility to currentTabProperty
        textTabView.visibleProperty().bind(currentTabProperty.isEqualTo(FnTab.text));
        treeTabView.visibleProperty().bind(currentTabProperty.isEqualTo(FnTab.tree));
        tocGeneratorTabView.visibleProperty().bind(currentTabProperty.isEqualTo(FnTab.toc));
        pageLabelTabView.visibleProperty().bind(currentTabProperty.isEqualTo(FnTab.label));
        pdfPreviewTabView.visibleProperty().bind(currentTabProperty.isEqualTo(FnTab.preview));

        // Add listener for tab-specific logic
        currentTabProperty.addListener((obs, oldTab, newTab) -> {
            if (newTab == FnTab.tree) {
                reconstructTree();
            } else if (newTab == FnTab.text) {
                Bookmark rootBookmark = treeTabViewController.getRootBookmark();
                if (rootBookmark != null) {
                    rootBookmark.updateLevelByStructureLevel();
                    textTabViewController.contentsTextArea.setText(rootBookmark.toTreeText());
                }
            } else if (newTab == FnTab.preview) {
                Path srcFile = currentFileState.getSrcFile();
                if (srcFile != null) {
                    pdfPreviewTabViewController.loadPdf(srcFile.toFile());
                } else {
                    pdfPreviewTabViewController.closePreview();
                }
            }
        });

        currentFileState.srcFileProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                filepathTF.setText(newValue.toString());
                resetState(false);
            } else {
                filepathTF.clear();
            }
        });

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

    private void handleGetContents(GetContentsEvent event) {
        if (!textTabViewController.contentsTextArea.getText().isEmpty()) {
            Optional<ButtonType> buttonType = showAlert(
                    Alert.AlertType.CONFIRMATION,
                    bundle.getString("alert.unsavedConfirmation"),
                    root.getScene().getWindow());
            if (buttonType.isPresent() && buttonType.get().getButtonData().isCancelButton()) {
                return;
            }
        }

        if (currentFileState.getSrcFile() == null) {
            messageManager.showMessage(bundle.getString("message.choosePDFFile"), Message.MessageType.WARNING);
            return;
        }

        getContents();
        if (currentTabProperty.get() == FnTab.tree) reconstructTree();
    }

    private void handleSetContents(SetContentsEvent event) {
        Path srcFile = currentFileState.getSrcFile();
        if (srcFile == null) {
            messageManager.showMessage(bundle.getString("message.choosePDFFile"), Message.MessageType.WARNING);
            return;
        }

        String srcFilePath = srcFile.toString();
        String destFilePath = currentFileState.getDestFile().toString();
        try {
            Bookmark rootBookmark = treeTabViewController.getRootBookmark();
            if (rootBookmark == null || rootBookmark.getChildren().isEmpty()) {
                String text = textTabViewController.contentsTextArea.getText();
                if (text == null || text.isEmpty()) {
                    messageManager.showMessage(bundle.getString("message.noContentToSet"), Message.MessageType.WARNING);
                    return;
                }
                pdfOutlineService.setContents(text, srcFilePath, destFilePath, offset(),
                        bottomPaneController.getSelectedMethod(),
                        viewScaleTypeProperty.get());
            } else {
                rootBookmark.updateLevelByStructureLevel();
                pdfOutlineService.setContents(rootBookmark, srcFilePath, destFilePath, viewScaleTypeProperty.get());
            }
        } catch (BookmarkFormatException e) {
            e.printStackTrace();
            File file = new File(destFilePath);
            boolean deleteSuccess = file.delete();
            logger.info("删除文件成功: {}", deleteSuccess);
            messageManager.showMessage(e.getMessage(), Message.MessageType.ERROR);
            return;
        } catch (IOException e) {
            messageManager.showMessage(e.getMessage(), Message.MessageType.ERROR);
            return;
        } catch (EncryptedPdfException e) {
            messageManager.showMessage(bundle.getString("message.decryptionPrompt"), Message.MessageType.ERROR);
            return;
        }

        showSuccessDialog();
    }

    private void handleDeleteContents(DeleteContentsEvent event) {
        Path srcFile = currentFileState.getSrcFile();
        if (srcFile == null) {
            messageManager.showMessage(bundle.getString("message.choosePDFFile"), Message.MessageType.WARNING);
            return;
        }
        pdfOutlineService.deleteContents(srcFile.toString(), currentFileState.getDestFile().toString());
        showSuccessDialog();
    }

    private void handleReconstructTree(ReconstructTreeEvent event) {
        if (currentTabProperty.get() == FnTab.tree) {
            reconstructTree();
        }
    }

    private void handleExtractToc(ExtractTocEvent event) {
        Path srcFile = currentFileState.getSrcFile();
        if (srcFile == null) {
            messageManager.showMessage(bundle.getString("message.choosePDFFile"), Message.MessageType.WARNING);
            return;
        }
        String contents;
        if (event.startPage == null || event.endPage == null) {
            contents = pdfTocService.extract(srcFile.toString());
        } else {
            contents = pdfTocService.extract(srcFile.toString(), event.startPage, event.endPage);
        }
        textTabViewController.contentsTextArea.setText(contents);
    }

    private void openFile(File file) {
        if (file == null) return;

        Path newFilePath = file.toPath();
        Path oldFile = currentFileState.getSrcFile();
        if (oldFile != null && oldFile.equals(newFilePath)) return;

        if (!textTabViewController.contentsTextArea.getText().isEmpty()) {
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

    private void showSuccessDialog() {
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

    private void resetState(boolean keepContents) {
        bottomPaneController.offsetTF.setText(null);
        if (!keepContents) {
            getContents();
            if (currentTabProperty.get() == FnTab.tree) reconstructTree();
        }
    }

    private void getContents() {
        try {
            String contents = pdfOutlineService.getContents(currentFileState.getSrcFile().toString(), 0);
            textTabViewController.contentsTextArea.setText(contents);
            autoToggleToIndentMethod();
        } catch (NoOutlineException e) {
            e.printStackTrace();
            messageManager.showMessage(bundle.getString("message.noBookmarks"), Message.MessageType.WARNING);
        }
    }

    public void autoToggleToIndentMethod() {
        eventBus.publish(new AutoToggleToIndentEvent());
    }

    public int offset() {
        return bottomPaneController.getOffset();
    }

    public void reconstructTree() {
        Bookmark rootBookmark = pdfOutlineService.convertTextToBookmarkTreeByMethod(
                textTabViewController.contentsTextArea.getText(), 0,
                bottomPaneController.getSelectedMethod()
        );
        treeTabViewController.reconstructTree(rootBookmark);
    }
}
