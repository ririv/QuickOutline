package com.ririv.quickoutline.view;

import com.ririv.quickoutline.model.Bookmark;
import javafx.collections.ListChangeListener;
import javafx.scene.control.TreeItem;

import java.util.stream.Collectors;

public class RecursiveTreeItem extends TreeItem<Bookmark> {

    public RecursiveTreeItem(Bookmark bookmark) {
        super(bookmark);

        // Add a listener to the children of the bookmark.
        // This listener will automatically update the TreeItem's children
        // whenever the Bookmark's children change.
        bookmark.getChildren().addListener((ListChangeListener<Bookmark>) c -> {
            while (c.next()) {
                if (c.wasRemoved()) {
                    // If bookmarks were removed from the model, remove the corresponding TreeItems from the UI.
                    getChildren().removeIf(item -> c.getRemoved().contains(item.getValue()));
                }
                if (c.wasAdded()) {
                    // If bookmarks were added to the model, create new RecursiveTreeItems for them
                    // and add them to the UI at the correct index.
                    getChildren().addAll(c.getFrom(), c.getAddedSubList().stream()
                            .map(RecursiveTreeItem::new)
                            .toList());
                }
            }
        });

        // Recursively build the initial tree structure for existing children
        getChildren().setAll(bookmark.getChildren().stream()
                .map(RecursiveTreeItem::new)
                .collect(Collectors.toList()));
    }
}
