package com.ririv.quickoutline.view;

import com.google.inject.Inject;
import com.ririv.quickoutline.model.Bookmark;
import com.ririv.quickoutline.state.BookmarkSettingsState;
import com.ririv.quickoutline.utils.LocalizationManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTreeTableCell;
import javafx.scene.input.MouseButton;

import java.util.Optional;
import java.util.ResourceBundle;

public class TreeTabController {
    public TreeTableView<Bookmark> treeTableView;
    public TreeTableColumn<Bookmark, String> titleColumn;
    public TreeTableColumn<Bookmark, String> offsetPageColumn;

    private final ResourceBundle bundle = LocalizationManager.getResourceBundle();
    private final BookmarkSettingsState bookmarkSettingsState;
    private MenuItem promoteMenuItem;
    private MenuItem demoteMenuItem;

    @Inject
    public TreeTabController(BookmarkSettingsState bookmarkSettingsState) {
        this.bookmarkSettingsState = bookmarkSettingsState;
    }

    public void initialize() {
        treeTableView.setEditable(true);
        treeTableView.setColumnResizePolicy(TreeTableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        titleColumn.prefWidthProperty().bind(treeTableView.widthProperty().multiply(0.9));
        offsetPageColumn.prefWidthProperty().bind(treeTableView.widthProperty().multiply(0.1));
        setupRowFactory();

        bookmarkSettingsState.rootBookmarkProperty().addListener((obs, oldRoot, newRoot) -> {
            if (newRoot == null) {
                treeTableView.setRoot(null);
                return;
            }
            TreeItem<Bookmark> rootItem = new RecursiveTreeItem(newRoot);
            expandAllNodes(rootItem);

            titleColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getValue().getTitle()));
            offsetPageColumn.setCellValueFactory(param -> {
                String pageNumStr = param.getValue().getValue().getOffsetPageNum()
                        .map(String::valueOf).orElse("");
                return new SimpleStringProperty(pageNumStr);
            });

            titleColumn.setCellFactory(TextFieldTreeTableCell.forTreeTableColumn());
            titleColumn.setOnEditCommit(event -> event.getRowValue().getValue().setTitle(event.getNewValue()));

            offsetPageColumn.setCellFactory(TextFieldTreeTableCell.forTreeTableColumn());
            offsetPageColumn.setOnEditCommit(event -> {
                try {
                    event.getRowValue().getValue().setOffsetPageNum(Integer.parseInt(event.getNewValue()));
                } catch (NumberFormatException e) {
                    treeTableView.refresh();
                }
            });

            treeTableView.setShowRoot(false);
            treeTableView.setRoot(rootItem);
        });
    }

    private void setupRowFactory() {
        ContextMenu contextMenu = new ContextMenu();
        MenuItem addSiblingItem = new MenuItem(bundle.getString("contextMenu.addBookmark"));
        addSiblingItem.setOnAction(event -> addBookmark(false));
        MenuItem addChildItem = new MenuItem(bundle.getString("contextMenu.addChildBookmark"));
        addChildItem.setOnAction(event -> addBookmark(true));
        MenuItem deleteItem = new MenuItem(bundle.getString("contextMenu.deleteBookmark"));
        deleteItem.setOnAction(event -> deleteSelectedBookmark());

        promoteMenuItem = new MenuItem(bundle.getString("contextMenu.promote"));
        promoteMenuItem.setOnAction(event -> promoteSelection());
        demoteMenuItem = new MenuItem(bundle.getString("contextMenu.demote"));
        demoteMenuItem.setOnAction(event -> demoteSelection());

        contextMenu.getItems().addAll(addSiblingItem, addChildItem, new SeparatorMenuItem(), deleteItem, new SeparatorMenuItem(), promoteMenuItem, demoteMenuItem);

        treeTableView.setRowFactory(tv -> {
            TreeTableRow<Bookmark> row = new TreeTableRow<>();

            row.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.SECONDARY && !row.isEmpty()) {
                    updateContextMenuStatus(row.getTreeItem());
                    contextMenu.show(row, event.getScreenX(), event.getScreenY());
                } else {
                    contextMenu.hide();
                }
            });

            return row;
        });
    }

    private void updateContextMenuStatus(TreeItem<Bookmark> selectedItem) {
        if (selectedItem == null) {
            promoteMenuItem.setDisable(true);
            demoteMenuItem.setDisable(true);
            return;
        }
        TreeItem<Bookmark> parent = selectedItem.getParent();
        promoteMenuItem.setDisable(parent == null || parent == treeTableView.getRoot());

        if (parent != null) {
            int index = parent.getChildren().indexOf(selectedItem.getValue());
            demoteMenuItem.setDisable(index == 0);
        } else {
            demoteMenuItem.setDisable(true);
        }
    }

    private void promoteSelection() {
        TreeItem<Bookmark> itemToPromote = treeTableView.getSelectionModel().getSelectedItem();
        if (itemToPromote == null) return;
        TreeItem<Bookmark> parent = itemToPromote.getParent();
        if (parent == null || parent == treeTableView.getRoot()) return;
        TreeItem<Bookmark> grandParent = parent.getParent();
        if (grandParent == null) return;

        Bookmark bookmarkToPromote = itemToPromote.getValue();
        Bookmark oldParentBookmark = parent.getValue();
        Bookmark newParentBookmark = grandParent.getValue();

        oldParentBookmark.getChildren().remove(bookmarkToPromote);
        int parentIndex = newParentBookmark.getChildren().indexOf(oldParentBookmark);
        newParentBookmark.addChild(parentIndex + 1, bookmarkToPromote);
    }

    private void demoteSelection() {
        TreeItem<Bookmark> itemToDemote = treeTableView.getSelectionModel().getSelectedItem();
        if (itemToDemote == null) return;
        TreeItem<Bookmark> parent = itemToDemote.getParent();
        if (parent == null) return;
        int currentIndex = parent.getChildren().indexOf(itemToDemote.getValue());
        if (currentIndex < 1) return;

        TreeItem<Bookmark> newParentItem = parent.getChildren().get(currentIndex - 1);

        Bookmark bookmarkToDemote = itemToDemote.getValue();
        Bookmark oldParentBookmark = parent.getValue();
        Bookmark newParentBookmark = newParentItem.getValue();

        oldParentBookmark.getChildren().remove(bookmarkToDemote);
        newParentBookmark.addChild(bookmarkToDemote);
    }

    private void addBookmark(boolean asChild) {
        TreeItem<Bookmark> selectedItem = treeTableView.getSelectionModel().getSelectedItem();
        Bookmark newBookmark;

        if (selectedItem == null) {
            Bookmark rootBookmark = bookmarkSettingsState.getRootBookmark();
            if (rootBookmark == null) return; // Cannot add to a non-existent tree
            newBookmark = new Bookmark("New Bookmark", null, 1);
            rootBookmark.addChild(newBookmark);
        } else if (asChild) {
            Bookmark parentBookmark = selectedItem.getValue();
            newBookmark = new Bookmark("New Child", null, parentBookmark.getLevel() + 1);
            parentBookmark.addChild(newBookmark);
        } else {
            Bookmark parentBookmark = selectedItem.getParent().getValue();
            Bookmark siblingBookmark = selectedItem.getValue();
            int index = parentBookmark.getChildren().indexOf(siblingBookmark);
            newBookmark = new Bookmark("New Sibling", null, siblingBookmark.getLevel());
            parentBookmark.addChild(index + 1, newBookmark);
        }
    }

    private void deleteSelectedBookmark() {
        TreeItem<Bookmark> selectedItem = treeTableView.getSelectionModel().getSelectedItem();
        if (selectedItem == null || selectedItem.getParent() == null) return;

        Optional<ButtonType> result = MyAlert.showAlert(Alert.AlertType.CONFIRMATION,
                bundle.getString("alert.deleteConfirmation"), treeTableView.getScene().getWindow());

        if (result.isPresent() && result.get() == ButtonType.OK) {
            Bookmark parentBookmark = selectedItem.getParent().getValue();
            Bookmark bookmarkToRemove = selectedItem.getValue();
            parentBookmark.getChildren().remove(bookmarkToRemove);
        }
    }

    private void expandAllNodes(TreeItem<?> item) {
        if (item != null) {
            item.setExpanded(true);
            for (TreeItem<?> child : item.getChildren()) {
                expandAllNodes(child);
            }
        }
    }

    public String getContents() {
        Bookmark rootBookmark = bookmarkSettingsState.getRootBookmark();
        if (rootBookmark != null) {
            return rootBookmark.toOutlineString();
        }
        return "";
    }
}
