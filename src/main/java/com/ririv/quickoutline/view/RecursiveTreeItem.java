package com.ririv.quickoutline.view;

import com.ririv.quickoutline.view.viewmodel.BookmarkViewModel;
import javafx.collections.ListChangeListener;
import javafx.scene.control.TreeItem;

import java.util.stream.Collectors;

public class RecursiveTreeItem extends TreeItem<BookmarkViewModel> {

    public RecursiveTreeItem(BookmarkViewModel bookmarkViewModel) {
        super(bookmarkViewModel);

        // Add a listener to the children of the bookmarkViewModel.
        // This listener will automatically update the TreeItem's children
        // whenever the BookmarkViewModel's children change.
        bookmarkViewModel.getChildren().addListener((ListChangeListener<BookmarkViewModel>) c -> {
            while (c.next()) {
                if (c.wasRemoved()) {
                    // If view models were removed, remove the corresponding TreeItems from the UI.
                    getChildren().removeIf(item -> c.getRemoved().contains(item.getValue()));
                }
                if (c.wasAdded()) {
                    // If view models were added, create new RecursiveTreeItems for them
                    // and add them to the UI at the correct index.
                    getChildren().addAll(c.getFrom(), c.getAddedSubList().stream()
                            .map(RecursiveTreeItem::new)
                            .toList());
                }
            }
        });

        // Recursively build the initial tree structure for existing children
        getChildren().setAll(bookmarkViewModel.getChildren().stream()
                .map(RecursiveTreeItem::new)
                .collect(Collectors.toList()));
    }
}