package com.ririv.quickoutline.view.bookmarktab;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.ririv.quickoutline.exception.BookmarkFormatException;
import com.ririv.quickoutline.exception.EncryptedPdfException;
import com.ririv.quickoutline.exception.NoOutlineException;
import com.ririv.quickoutline.model.Bookmark;
import com.ririv.quickoutline.pdfProcess.ViewScaleType;
import com.ririv.quickoutline.service.PdfOutlineService;
import com.ririv.quickoutline.service.PdfTocExtractorService;
import com.ririv.quickoutline.view.state.BookmarkSettingsState;
import com.ririv.quickoutline.view.state.CurrentFileState;
import com.ririv.quickoutline.view.LocalizationManager;
import com.ririv.quickoutline.view.controls.message.Message;
import com.ririv.quickoutline.view.event.*;
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
        
        // 1. 注册自身为订阅者
        this.eventBus.register(this);

        // 2. 旧的 subscribe 调用已被移除

        currentFileState.srcFileProperty().addListener((obs, old, nu) -> {
            if (nu != null) {
                loadBookmarksFromPdf();
            }
        });
    }

    // 3. 将方法改为 public 并添加 @Subscribe 注解
    @Subscribe
    public void onGetBookmarks(GetContentsEvent event) {
        loadBookmarksFromPdf();
    }

    @Subscribe
    public void onSetBookmarks(SetContentsEvent event) {
        saveBookmarksToPdf(event.getViewScaleType());
    }

    @Subscribe
    public void onDeleteBookmarks(DeleteContentsEvent event) {
        Path srcFile = currentFileState.getSrcFile();
        if (srcFile == null) {
            eventBus.post(new ShowMessageEvent(bundle.getString("message.choosePDFFile"), Message.MessageType.WARNING));
            return;
        }
        pdfOutlineService.deleteOutline(srcFile.toString(), currentFileState.getDestFile().toString());
        eventBus.post(new ShowSuccessDialogEvent());
    }

    @Subscribe
    public void onExtractToc(ExtractTocEvent event) {
        Path srcFile = currentFileState.getSrcFile();
        if (srcFile == null) {
            eventBus.post(new ShowMessageEvent(bundle.getString("message.choosePDFFile"), Message.MessageType.WARNING));
            return;
        }
        String contents = pdfTocExtractorService.extract(srcFile.toString());
        textTabController.setContents(contents);
        reconstructTreeByContents();
    }


    public void initialize() {
        // Default to showing the tree tab
        treeTab.setVisible(false);
        textTab.setVisible(true);
    }

    @Subscribe
    public void onSwitchBookmarkViewEvent(SwitchBookmarkViewEvent event) {
        if (event.getView() == SwitchBookmarkViewEvent.View.TEXT) { // Switching to Text View
            resetContentsByTree();
            textTab.setVisible(true);
            treeTab.setVisible(false);
        } else { // Switching to Tree View
            reconstructTreeByContents();
            treeTab.setVisible(true);
            textTab.setVisible(false);
        }
    }

    private void resetContentsByTree() {
        Bookmark rootBookmark = bookmarkSettingsState.getRootBookmark();
        if (rootBookmark != null) {
            textTabController.setContents(rootBookmark.toOutlineString());
        }
    }

    private void reconstructTreeByContents() {
        Bookmark rootBookmark = pdfOutlineService.convertTextToBookmarkTreeByMethod(
                textTabController.getContents(), 0,
                textTabController.getSelectedMethod()
        );
        bookmarkSettingsState.setRootBookmark(rootBookmark);
    }

    public void loadBookmarksFromPdf() {
        Bookmark currentBookmark = bookmarkSettingsState.getRootBookmark();
        boolean treeHasContent = currentBookmark != null && !currentBookmark.getChildren().isEmpty();
        boolean textHasContent = !textTabController.getContents().isEmpty();

        if (treeHasContent || textHasContent) {
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
            eventBus.post(new ShowMessageEvent(bundle.getString("message.choosePDFFile"), Message.MessageType.WARNING));
            return;
        }

        try {
            Bookmark rootBookmark = pdfOutlineService.getOutlineAsBookmark(currentFileState.getSrcFile().toString(), 0);
            if (rootBookmark != null) {
                bookmarkSettingsState.setRootBookmark(rootBookmark);
                textTabController.setContents(rootBookmark.toOutlineString());
            }
        } catch (NoOutlineException e) {
            e.printStackTrace();
            eventBus.post(new ShowMessageEvent(bundle.getString("message.noBookmarks"), Message.MessageType.INFO));
        }
    }

    public void saveBookmarksToPdf(ViewScaleType viewScaleType) {
        Path srcFile = currentFileState.getSrcFile();
        if (srcFile == null) {
            eventBus.post(new ShowMessageEvent(bundle.getString("message.choosePDFFile"), Message.MessageType.WARNING));
            return;
        }

        if (textTab.isVisible()) {
            reconstructTreeByContents();
        }

        String srcFilePath = srcFile.toString();
        String destFilePath = currentFileState.getDestFile().toString();
        try {
            Bookmark rootBookmark = bookmarkSettingsState.getRootBookmark();

            if (rootBookmark == null || rootBookmark.getChildren().isEmpty()) {
                eventBus.post(new ShowMessageEvent(bundle.getString("message.noContentToSet"), Message.MessageType.WARNING));
                return;
            }

            pdfOutlineService.setOutline(rootBookmark, srcFilePath, destFilePath, viewScaleType);

        } catch (BookmarkFormatException e) {
            e.printStackTrace();
            File file = new File(destFilePath);
            boolean deleteSuccess = file.delete();
            logger.info("删除文件成功: {}", deleteSuccess);
            eventBus.post(new ShowMessageEvent(e.getMessage(), Message.MessageType.ERROR));
            return;
        } catch (IOException e) {
            eventBus.post(new ShowMessageEvent(e.getMessage(), Message.MessageType.ERROR));
            return;
        } catch (EncryptedPdfException e) {
            eventBus.post(new ShowMessageEvent(bundle.getString("message.decryptionPrompt"), Message.MessageType.ERROR));
            return;
        }

        eventBus.post(new ShowSuccessDialogEvent());
    }
}