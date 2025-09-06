package com.ririv.quickoutline.view.viewmodel;

import com.ririv.quickoutline.model.Bookmark;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.stream.Collectors;

/**
 * ViewModel for a Bookmark, acting as an adapter for the TreeView.
 * It wraps a Bookmark model and provides an ObservableList of its children
 * for automatic UI updates.
 */
public class BookmarkViewModel {

    private final Bookmark model;
    private final ObservableList<BookmarkViewModel> children;

    public BookmarkViewModel(Bookmark model) {
        this.model = model;
        this.children = FXCollections.observableArrayList(
                model.getChildren().stream()
                        .map(BookmarkViewModel::new)
                        .collect(Collectors.toList())
        );
    }

    public Bookmark getModel() {
        return model;
    }

    public ObservableList<BookmarkViewModel> getChildren() {
        return children;
    }

    // Delegate methods for properties used by the TreeTableView
    public String getTitle() {
        return model.getTitle();
    }

    public void setTitle(String title) {
        model.setTitle(title);
    }

    public String getOffsetPageNumAsString() {
        return model.getOffsetPageNum().map(String::valueOf).orElse("");
    }

    public void setOffsetPageNum(Integer pageNum) {
        model.setOffsetPageNum(pageNum);
    }

    // Methods to manipulate the model and sync the view model

    public void addChild(int index, Bookmark child) {
        model.addChild(index, child);
        children.add(index, new BookmarkViewModel(child));
    }

    public void addChild(Bookmark child) {
        model.addChild(child);
        children.add(new BookmarkViewModel(child));
    }

    public void removeChild(BookmarkViewModel childViewModel) {
        model.getChildren().remove(childViewModel.getModel());
        children.remove(childViewModel);
    }
}
