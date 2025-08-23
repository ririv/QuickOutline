package com.ririv.quickoutline.view;

import com.google.inject.Inject;
import com.ririv.quickoutline.event.AppEventBus;
import com.ririv.quickoutline.event.SwitchBookmarkViewEvent;
import javafx.fxml.FXML;
import javafx.scene.layout.Pane;

public class BookmarkTabController {

    @FXML
    private Pane textTab;
    @FXML
    public TextTabController textTabController;

    @FXML
    private Pane treeTab;
    @FXML
    public TreeTabController treeTabController;

    @Inject
    public BookmarkTabController(AppEventBus eventBus) {
        eventBus.subscribe(SwitchBookmarkViewEvent.class, this::handleSwitchBookmarkViewEvent);
    }

    public void initialize() {
        // Default to showing the tree tab
        treeTab.setVisible(false);
        textTab.setVisible(true);
    }

    public void handleSwitchBookmarkViewEvent(SwitchBookmarkViewEvent event) {
        if (event.getView() == SwitchBookmarkViewEvent.View.TEXT) {
            textTab.setVisible(true);
            treeTab.setVisible(false);
        } else {
            treeTab.setVisible(true);
            textTab.setVisible(false);
        }
    }

    // Methods to delegate to sub-controllers, which will be filled in later
    public void setContents(String text) {
        textTabController.setContents(text);
        treeTabController.setContents(text);
    }

    public String getContents() {
        if (textTab.isVisible()) {
            return textTabController.getContents();
        } else {
            return treeTabController.getContents();
        }
    }
}
