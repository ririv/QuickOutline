package com.ririv.quickoutline.view;

import com.google.inject.Inject;
import com.ririv.quickoutline.event.*;
import com.ririv.quickoutline.event.ShowSuccessDialogEvent;
import com.ririv.quickoutline.exception.BookmarkFormatException;
import com.ririv.quickoutline.exception.EncryptedPdfException;
import com.ririv.quickoutline.exception.NoOutlineException;
import com.ririv.quickoutline.model.Bookmark;
import com.ririv.quickoutline.pdfProcess.ViewScaleType;
import com.ririv.quickoutline.service.PdfOutlineService;
import com.ririv.quickoutline.service.PdfTocExtractorService;
import com.ririv.quickoutline.state.BookmarkSettingsState;
import com.ririv.quickoutline.state.CurrentFileState;
import com.ririv.quickoutline.utils.LocalizationManager;
import com.ririv.quickoutline.view.controls.Message;
import com.ririv.quickoutline.view.controls.MessageContainer;
import javafx.beans.property.ObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.Pane;
import javafx.stage.Window;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.ResourceBundle;

public class BookmarkTabController {
    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(BookmarkTabController.class);

    @FXML
    private Pane textTab;
    @FXML
    public TextTabController textTabController;

    @FXML
    private Pane treeTab;
    @FXML
    public TreeTabController treeTabController;

    private final AppEventBus eventBus;
    private final PdfOutlineService pdfOutlineService;
    private final CurrentFileState currentFileState;
    private final PdfTocExtractorService pdfTocExtractorService;
    private final BookmarkSettingsState bookmarkSettingsState;
    private final ResourceBundle bundle;

    @Inject
    public BookmarkTabController(AppEventBus eventBus, PdfOutlineService pdfOutlineService, CurrentFileState currentFileState, PdfTocExtractorService pdfTocExtractorService, BookmarkSettingsState bookmarkSettingsState) {
        this.eventBus = eventBus;
        this.pdfOutlineService = pdfOutlineService;
        this.currentFileState = currentFileState;
        this.pdfTocExtractorService = pdfTocExtractorService;
        this.bookmarkSettingsState = bookmarkSettingsState;
        this.bundle = LocalizationManager.getResourceBundle();
        eventBus.subscribe(SwitchBookmarkViewEvent.class, this::handleSwitchBookmarkViewEvent);
        eventBus.subscribe(GetContentsEvent.class, this::handleGetContents);
        eventBus.subscribe(SetContentsEvent.class, this::handleSetContents);
        eventBus.subscribe(DeleteContentsEvent.class, this::handleDeleteContents);
        eventBus.subscribe(ExtractTocEvent.class, this::handleExtractToc);
        eventBus.subscribe(ReconstructTreeEvent.class, event -> reconstructTree());

        currentFileState.srcFileProperty().addListener((obs, old, nu) -> {
            if (nu != null) {
                loadBookmarksFromPdf();
            }
        });
    }

    private void reconstructTree() {
        Bookmark rootBookmark = pdfOutlineService.convertTextToBookmarkTreeByMethod(
                getContents(), 0,
                textTabController.getSelectedMethod()
        );
        bookmarkSettingsState.setRootBookmark(rootBookmark);
    }

    

    private void handleGetContents(GetContentsEvent event) {
        loadBookmarksFromPdf();
    }

    private void handleSetContents(SetContentsEvent event) {
        saveBookmarksToPdf(event.getViewScaleType());
    }

    private void handleDeleteContents(DeleteContentsEvent event) {
        Path srcFile = currentFileState.getSrcFile();
        if (srcFile == null) {
            eventBus.publish(new ShowMessageEvent(bundle.getString("message.choosePDFFile"), Message.MessageType.WARNING));
            return;
        }
        pdfOutlineService.deleteContents(srcFile.toString(), currentFileState.getDestFile().toString());
        eventBus.publish(new ShowSuccessDialogEvent());
    }

    private void handleExtractToc(ExtractTocEvent event) {
        Path srcFile = currentFileState.getSrcFile();
        if (srcFile == null) {
            eventBus.publish(new ShowMessageEvent(bundle.getString("message.choosePDFFile"), Message.MessageType.WARNING));
            return;
        }
        String contents;
        if (event.startPage == null || event.endPage == null) {
            contents = pdfTocExtractorService.extract(srcFile.toString());
        } else {
            contents = pdfTocExtractorService.extract(srcFile.toString(), event.startPage, event.endPage);
        }
        setContents(contents);
    }


    public void initialize() {
        // Default to showing the tree tab
        treeTab.setVisible(false);
        textTab.setVisible(true);
    }

    public void handleSwitchBookmarkViewEvent(SwitchBookmarkViewEvent event) {
        if (event.getView() == SwitchBookmarkViewEvent.View.TEXT) { // Switching TO Text View
            Bookmark rootBookmark = bookmarkSettingsState.getRootBookmark();
            if (rootBookmark != null) {
                textTabController.setContents(rootBookmark.toTreeText());
            }
            // Switch visibility
            textTab.setVisible(true);
            treeTab.setVisible(false);
        } else { // Switching TO Tree View
            // The text view is currently visible. ReconstructTree will get its contents and update the state.
            reconstructTree();
            // Switch visibility
            treeTab.setVisible(true);
            textTab.setVisible(false);
        }
    }

    public void loadBookmarksFromPdf() {
        if (!getContents().isEmpty()) {
            Optional<ButtonType> buttonType = MyAlert.showAlert(
                    Alert.AlertType.CONFIRMATION,
                    bundle.getString("alert.unsavedConfirmation"),
                    textTab.getScene().getWindow());
            if (buttonType.isPresent() && buttonType.get().getButtonData().isCancelButton()) {
                return;
            }
        }

        if (currentFileState.getSrcFile() == null) {
            eventBus.publish(new ShowMessageEvent(bundle.getString("message.choosePDFFile"), Message.MessageType.WARNING));
            return;
        }

        try {
            String contents = pdfOutlineService.getContents(currentFileState.getSrcFile().toString(), 0);
            setContents(contents);
        } catch (NoOutlineException e) {
            e.printStackTrace();
            eventBus.publish(new ShowMessageEvent(bundle.getString("message.noBookmarks"), Message.MessageType.WARNING));
        }
        eventBus.publish(new ReconstructTreeEvent());
    }

    public void saveBookmarksToPdf(ViewScaleType viewScaleType) {
        Path srcFile = currentFileState.getSrcFile();
        if (srcFile == null) {
            eventBus.publish(new ShowMessageEvent(bundle.getString("message.choosePDFFile"), Message.MessageType.WARNING));
            return;
        }

        String srcFilePath = srcFile.toString();
        String destFilePath = currentFileState.getDestFile().toString();
        try {
            Bookmark rootBookmark = bookmarkSettingsState.getRootBookmark();
            if (rootBookmark == null || rootBookmark.getChildren().isEmpty()) {
                String text = getContents();
                if (text == null || text.isEmpty()) {
                    eventBus.publish(new ShowMessageEvent(bundle.getString("message.noContentToSet"), Message.MessageType.WARNING));
                    return;
                }
                Integer offset = bookmarkSettingsState.getOffset();
                pdfOutlineService.setContents(text, srcFilePath, destFilePath, (offset == null) ? 0 : offset,
                        textTabController.getSelectedMethod(),
                        viewScaleType);
            } else {
                rootBookmark.updateLevelByStructureLevel();
                pdfOutlineService.setContents(rootBookmark, srcFilePath, destFilePath, viewScaleType);
            }
        } catch (BookmarkFormatException e) {
            e.printStackTrace();
            File file = new File(destFilePath);
            boolean deleteSuccess = file.delete();
            logger.info("删除文件成功: {}", deleteSuccess);
            eventBus.publish(new ShowMessageEvent(e.getMessage(), Message.MessageType.ERROR));
            return;
        } catch (IOException e) {
            eventBus.publish(new ShowMessageEvent(e.getMessage(), Message.MessageType.ERROR));
            return;
        } catch (EncryptedPdfException e) {
            eventBus.publish(new ShowMessageEvent(bundle.getString("message.decryptionPrompt"), Message.MessageType.ERROR));
            return;
        }

        eventBus.publish(new ShowSuccessDialogEvent());
    }

    public void setContents(String text) {
        textTabController.setContents(text);
        reconstructTree(); // This will parse the text and update the shared state
    }

    public String getContents() {
        if (textTab.isVisible()) {
            return textTabController.getContents();
        } else {
            return treeTabController.getContents();
        }
    }
}
