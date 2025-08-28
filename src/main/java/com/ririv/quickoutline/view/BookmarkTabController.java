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
import com.ririv.quickoutline.view.controls.message.Message;
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
        String contents = pdfTocExtractorService.extract(srcFile.toString());
        textTabController.setContents(contents);
        reconstructTreeByContents();
    }


    public void initialize() {
        // Default to showing the tree tab
        treeTab.setVisible(false);
        textTab.setVisible(true);
    }

    public void handleSwitchBookmarkViewEvent(SwitchBookmarkViewEvent event) {
        if (event.getView() == SwitchBookmarkViewEvent.View.TEXT) { // Switching to Text View
            resetContentsByTree();
            textTab.setVisible(true);
            treeTab.setVisible(false);
        } else { // Switching to Tree View
            // The text view is currently visible. ReconstructTree will get its contents and update the state.
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

    //    This will parse the text and update the shared state
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
            eventBus.publish(new ShowMessageEvent(bundle.getString("message.choosePDFFile"), Message.MessageType.WARNING));
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
            eventBus.publish(new ShowMessageEvent(bundle.getString("message.noBookmarks"), Message.MessageType.WARNING));
        }
    }

    public void saveBookmarksToPdf(ViewScaleType viewScaleType) {
        Path srcFile = currentFileState.getSrcFile();
        if (srcFile == null) {
            eventBus.publish(new ShowMessageEvent(bundle.getString("message.choosePDFFile"), Message.MessageType.WARNING));
            return;
        }

 /*       让我们设想一下如果没有这段代码，会发生什么（即您说的“删除它”）：
        1. 用户打开了一个PDF，程序加载了书签，此时 BookmarkSettingsState 中的 rootBookmark
        和文本视图中的内容是同步的。
        2. 用户停留在文本视图，对书签文本进行了大量的编辑和修改。
        3. 用户没有切换视图，而是直接点击了“保存”按钮。
        4. saveBookmarksToPdf 方法被调用。由于我们删除了那段 if
        代码，程序不会去读取文本视图中刚刚被修改的内容。
        5. 程序会直接从 BookmarkSettingsState 中获取 rootBookmark。但这个 rootBookmark
        还是修改前的旧状态！
        6. 结果：用户在文本视图中的所有修改，在点击保存后，全部丢失了！
        这从用户的角度看，是一个非常严重的BUG。

        这段代码的核心作用，就是将“保存”这个动作，也定义为一个“提交点”。
        它确保了，无论用户在哪个视图，当他点击“保存”时，程序的第一反应是：“我必须先确保我将要保存的数据，和我屏幕上看到的是一致的。”
        * 如果用户在树状视图，他的修改是直接作用于 BookmarkSettingsState的，数据已经同步了，所以这个 if 不执行。
        * 如果用户在文本视图，这段 if 代码就会被触发，它会调用 reconstructTreeByContents()，强制用当前文本框里的内容去更新
        BookmarkSettingsState。完成同步后，再用这个最新的 rootBookmark 去执行保存。*/
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

//            rootBookmark.updateLevelByStructureLevel();
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

    
}
