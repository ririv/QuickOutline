package com.ririv.quickoutline.view;

import com.google.inject.Inject;
import com.ririv.quickoutline.event.AppEventBus;
import com.ririv.quickoutline.event.ShowMessageEvent;
import com.ririv.quickoutline.model.Bookmark;
import com.ririv.quickoutline.service.TocService;
import com.ririv.quickoutline.state.CurrentFileState;
import com.ririv.quickoutline.utils.LocalizationManager;
import com.ririv.quickoutline.view.controls.Message;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;

import java.io.IOException;
import java.util.ResourceBundle;

public class TocGeneratorTabController {

    private final TocService tocService;
    private final CurrentFileState currentFileState;
    private final TreeTabController treeTabController; // Injected to get bookmarks
    private final AppEventBus eventBus;
    private final ResourceBundle bundle = LocalizationManager.getResourceBundle();

    @Inject
    public TocGeneratorTabController(TocService tocService, CurrentFileState currentFileState, TreeTabController treeTabController, AppEventBus eventBus) {
        this.tocService = tocService;
        this.currentFileState = currentFileState;
        this.treeTabController = treeTabController;
        this.eventBus = eventBus;
    }

    @FXML
    void generateTocPageAction(ActionEvent event) {
        if (currentFileState.getSrcFile() == null) {
            eventBus.publish(new ShowMessageEvent(bundle.getString("message.choosePDFFile"), Message.MessageType.WARNING));
            return;
        }

        String srcFile = currentFileState.getSrcFile().toString();
        String destFile = currentFileState.getDestFile().toString();
        Bookmark rootBookmark = treeTabController.getRootBookmark();

        if (rootBookmark == null || rootBookmark.getChildren().isEmpty()) {
            eventBus.publish(new ShowMessageEvent(bundle.getString("message.noContentToSet"), Message.MessageType.WARNING));
            return;
        }

        try {
            tocService.createTocPage(srcFile, destFile, rootBookmark.getChildren());
            eventBus.publish(new ShowMessageEvent(bundle.getString("alert.FileSavedAt") + destFile, Message.MessageType.SUCCESS));
        } catch (IOException e) {
            eventBus.publish(new ShowMessageEvent("Failed to generate TOC page: " + e.getMessage(), Message.MessageType.ERROR));
            e.printStackTrace();
        }
    }
}
