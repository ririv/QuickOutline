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
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.Pane;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.ResourceBundle;

import static com.ririv.quickoutline.view.MyAlert.showAlert;

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
        eventBus.subscribe(GetContentsEvent.class, this::handleGetBookmarks);
        eventBus.subscribe(SetContentsEvent.class, this::handleSetBookmarks);
        eventBus.subscribe(DeleteContentsEvent.class, this::handleDeleteBookmarks);
        eventBus.subscribe(ExtractTocEvent.class, this::handleExtractToc);

        currentFileState.srcFileProperty().addListener((obs, old, nu) -> {
            if (nu != null) {
                loadBookmarksFromPdf();
            }
        });
    }

    private void handleGetBookmarks(GetContentsEvent event) {
        loadBookmarksFromPdf();
    }

    private void handleSetBookmarks(SetContentsEvent event) {
        saveBookmarksToPdf(event.getViewScaleType());
    }

    private void handleDeleteBookmarks(DeleteContentsEvent event) {
        Path srcFile = currentFileState.getSrcFile();
        if (srcFile == null) {
            eventBus.publish(new ShowMessageEvent(bundle.getString("message.choosePDFFile"), Message.MessageType.WARNING));
            return;
        }
        pdfOutlineService.deleteOutline(srcFile.toString(), currentFileState.getDestFile().toString());
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
        textTabController.setContents(contents);
    }


    public void initialize() {
        // Default to showing the tree tab
        treeTab.setVisible(false);
        textTab.setVisible(true);
    }

    public void handleSwitchBookmarkViewEvent(SwitchBookmarkViewEvent event) {
        if (event.getView() == SwitchBookmarkViewEvent.View.TEXT) { // Switching TO Text View
            resetContentsByTree();
            // Switch visibility
            textTab.setVisible(true);
            treeTab.setVisible(false);
        } else { // Switching TO Tree View
            // The text view is currently visible. ReconstructTree will get its contents and update the state.
            reconstructTreeByContents();
            // Switch visibility
            treeTab.setVisible(true);
            textTab.setVisible(false);
        }
    }

    private void resetContentsByTree() {
        Bookmark rootBookmark = bookmarkSettingsState.getRootBookmark();
        if (rootBookmark != null) {
            textTabController.setContents(rootBookmark.toTreeText());
        }
    }

    //    This will parse the text and update the shared state
    private void reconstructTreeByContents() {
        Bookmark rootBookmark = pdfOutlineService.convertTextToBookmarkTreeByMethod(
                getContents(), 0,
                textTabController.getSelectedMethod()
        );
        bookmarkSettingsState.setRootBookmark(rootBookmark);
    }

    public void loadBookmarksFromPdf() {
        if (!getContents().isEmpty()) {
            ButtonType keepContentsTextBtnType = new ButtonType(bundle.getString("btnType.keepContents"), ButtonBar.ButtonData.OK_DONE);
            ButtonType noKeepContentsTextBtnType = new ButtonType(bundle.getString("btnType.noKeepContents"), ButtonBar.ButtonData.OK_DONE);
            ButtonType cancelBtnType = new ButtonType(bundle.getString("btnType.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
            Optional<ButtonType> result = showAlert(
                    Alert.AlertType.CONFIRMATION,
                    bundle.getString("alert.unsavedConfirmation"),
                    textTab.getScene().getWindow(),
                    keepContentsTextBtnType, noKeepContentsTextBtnType, cancelBtnType);
            if (result.isPresent() && result.get() == cancelBtnType) {
                return;
            }
        }

        if (currentFileState.getSrcFile() == null) {
            eventBus.publish(new ShowMessageEvent(bundle.getString("message.choosePDFFile"), Message.MessageType.WARNING));
            return;
        }

        try {
            Bookmark rootBookmark = pdfOutlineService.getOutlineAsBookmark(currentFileState.getSrcFile().toString(), 0);
            if (rootBookmark != null) {
                bookmarkSettingsState.setRootBookmark(rootBookmark);
                textTabController.setContents(rootBookmark.toTreeText());
            }
        } catch (NoOutlineException e) {
            e.printStackTrace();
            eventBus.publish(new ShowMessageEvent(bundle.getString("message.noBookmarks"), Message.MessageType.WARNING));
        }
    }

    public void saveBookmarksToPdf(ViewScaleType viewScaleType) {
        Path srcFile = currentFileState.getSrcFile();
        if (srcFile == null) {
            eventBus.publish(new ShowMessageEvent(bundle.getString("message.choosePDFFile"), Message.MessageType.WARNING));
            return;
        }

        // Ensure the state is in sync with the active view before saving
        if (textTab.isVisible()) {
            reconstructTreeByContents();
        }

        String srcFilePath = srcFile.toString();
        String destFilePath = currentFileState.getDestFile().toString();
        try {
            Bookmark rootBookmark = bookmarkSettingsState.getRootBookmark();

            if (rootBookmark == null || rootBookmark.getChildren().isEmpty()) {
                eventBus.publish(new ShowMessageEvent(bundle.getString("message.noContentToSet"), Message.MessageType.WARNING));
                return;
            }

            rootBookmark.updateLevelByStructureLevel();
            pdfOutlineService.setOutline(rootBookmark, srcFilePath, destFilePath, viewScaleType);

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

    public String getContents() {
        if (textTab.isVisible()) {
            return textTabController.getContents();
        } else {
            return treeTabController.getContents();
        }
    }
}
