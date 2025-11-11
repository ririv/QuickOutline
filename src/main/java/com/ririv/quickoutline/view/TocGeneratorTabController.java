package com.ririv.quickoutline.view;

import com.ririv.quickoutline.service.PdfOutlineService;
import com.ririv.quickoutline.textProcess.methods.Method;
import com.ririv.quickoutline.view.controls.EditorTextArea;
import jakarta.inject.Inject;
import com.ririv.quickoutline.view.event.AppEventBus;
import com.ririv.quickoutline.view.event.ShowMessageEvent;
import com.ririv.quickoutline.model.Bookmark;
import com.ririv.quickoutline.view.state.BookmarkSettingsState;
import com.ririv.quickoutline.service.PdfTocPageGeneratorService;
import com.ririv.quickoutline.view.state.CurrentFileState;
import com.ririv.quickoutline.view.controls.message.Message;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;

import java.io.IOException;
import java.util.ResourceBundle;

public class TocGeneratorTabController {

    private final PdfTocPageGeneratorService pdfTocPageGeneratorService;
    private final CurrentFileState currentFileState;
    private final BookmarkSettingsState bookmarkSettingsState;
    private final AppEventBus eventBus;
    private final PdfOutlineService pdfOutlineService;
    private final ResourceBundle bundle = LocalizationManager.getResourceBundle();

    @FXML
    private EditorTextArea tocContentTextArea;
    @FXML
    private TextArea titleTextArea;

    @Inject
    public TocGeneratorTabController(PdfTocPageGeneratorService pdfTocPageGeneratorService, CurrentFileState currentFileState, BookmarkSettingsState bookmarkSettingsState, AppEventBus eventBus, PdfOutlineService pdfOutlineService) {
        this.pdfTocPageGeneratorService = pdfTocPageGeneratorService;
        this.currentFileState = currentFileState;
        this.bookmarkSettingsState = bookmarkSettingsState;
        this.eventBus = eventBus;
        this.pdfOutlineService = pdfOutlineService;
    }

    @FXML
    public void initialize() {
        titleTextArea.setText("Table of Contents");
        Bookmark rootBookmark = bookmarkSettingsState.getRootBookmark();
        if (rootBookmark != null) {
            tocContentTextArea.setText(rootBookmark.toOutlineString());
        }
    }

    @FXML
    void generateTocPageAction(ActionEvent event) {
        if (currentFileState.getSrcFile() == null) {
            eventBus.post(new ShowMessageEvent(bundle.getString("message.choosePDFFile"), Message.MessageType.WARNING));
            return;
        }

        String tocContent = tocContentTextArea.getText();
        if (tocContent == null || tocContent.isBlank()) {
            eventBus.post(new ShowMessageEvent(bundle.getString("message.noContentToSet"), Message.MessageType.WARNING));
            return;
        }

        String title = titleTextArea.getText();
        if (title == null || title.isBlank()) {
            title = "Table of Contents"; // Default title
        }

        String srcFile = currentFileState.getSrcFile().toString();
        String destFile = currentFileState.getDestFile().toString();
        
        // The user might have edited the text, so we need to parse it back into bookmarks.
        // We assume the "indent" method for parsing, as it's the most common for TOCs.
        Bookmark rootBookmark = pdfOutlineService.convertTextToBookmarkTreeByMethod(tocContent, Method.INDENT);

        if (rootBookmark == null || rootBookmark.getChildren().isEmpty()) {
            eventBus.post(new ShowMessageEvent(bundle.getString("message.noContentToSet"), Message.MessageType.WARNING));
            return;
        }

        try {
            pdfTocPageGeneratorService.createTocPage(srcFile, destFile, title, rootBookmark.getChildren());
            eventBus.post(new ShowMessageEvent(bundle.getString("alert.FileSavedAt") + destFile, Message.MessageType.SUCCESS));
        } catch (IOException e) {
            eventBus.post(new ShowMessageEvent("Failed to generate TOC page: " + e.getMessage(), Message.MessageType.ERROR));
            e.printStackTrace();
        }
    }
}
