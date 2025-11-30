package com.ririv.quickoutline.view.bookmarktab;

import jakarta.inject.Inject;
import com.ririv.quickoutline.model.Bookmark;
import com.ririv.quickoutline.view.controls.PagePreviewer;
import com.ririv.quickoutline.view.viewmodel.BookmarkViewModel;
import com.ririv.quickoutline.view.state.BookmarkSettingsState;
import com.ririv.quickoutline.view.utils.LocalizationManager;
import com.ririv.quickoutline.view.MyAlert;
import com.ririv.quickoutline.view.RecursiveTreeItem;
import com.ririv.quickoutline.view.controls.EditableTreeTableCell;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;

import java.util.Optional;
import java.util.ResourceBundle;

public class TreeSubViewController {
    public TreeTableView<BookmarkViewModel> treeTableView;
    public TreeTableColumn<BookmarkViewModel, String> titleColumn;
    public TreeTableColumn<BookmarkViewModel, String> offsetPageColumn;

    private final ResourceBundle bundle = LocalizationManager.getResourceBundle();
    private final BookmarkSettingsState bookmarkSettingsState;
    private final com.ririv.quickoutline.view.state.CurrentFileState currentFileState;
    private MenuItem promoteMenuItem;
    private MenuItem demoteMenuItem;
    private final PagePreviewer pagePreviewer;

    @Inject
    public TreeSubViewController(BookmarkSettingsState bookmarkSettingsState, com.ririv.quickoutline.view.state.CurrentFileState currentFileState) {
        this.bookmarkSettingsState = bookmarkSettingsState;
        this.currentFileState = currentFileState;
        this.pagePreviewer = new PagePreviewer();
    }

    public void initialize() {
        treeTableView.setEditable(true);
        treeTableView.setColumnResizePolicy(TreeTableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        // 为使用EditableTreeTableCell的TreeTableView添加专用样式类
        treeTableView.getStyleClass().add("editable-tree-table-view");

        titleColumn.prefWidthProperty().bind(treeTableView.widthProperty().multiply(0.9));
        offsetPageColumn.prefWidthProperty().bind(treeTableView.widthProperty().multiply(0.1));
        setupRowFactory();

        currentFileState.pageRenderSessionProperty().addListener((obs, oldSession, newSession) -> {
            pagePreviewer.setRenderSession(newSession);
        });

        bookmarkSettingsState.rootBookmarkProperty().addListener((obs, oldRoot, newRoot) -> {
            if (newRoot == null) {
                treeTableView.setRoot(null);
                return;
            }
            BookmarkViewModel rootBookmarkViewModel = new BookmarkViewModel(newRoot);
            TreeItem<BookmarkViewModel> rootItem = new RecursiveTreeItem(rootBookmarkViewModel);
            expandAllNodes(rootItem);

            titleColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getValue().getTitle()));
            offsetPageColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getValue().getOffsetPageNumAsString()));

            titleColumn.setCellFactory(EditableTreeTableCell.forTreeTableColumn());
            titleColumn.setOnEditCommit(event -> {
                event.getRowValue().getValue().setTitle(event.getNewValue());
                treeTableView.refresh();
            });

            offsetPageColumn.setCellFactory(EditableTreeTableCell.forTreeTableColumn());
            offsetPageColumn.setOnEditCommit(event -> {
                String newValue = event.getNewValue();
                try {
                    Integer pageNum = newValue.isEmpty() ? null : Integer.valueOf(newValue);
                    event.getRowValue().getValue().setOffsetPageNum(pageNum);
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
            TreeTableRow<BookmarkViewModel> row = new TreeTableRow<>();

            pagePreviewer.attach(row, () -> {
                if (row.isEmpty()) {
                    return null;
                }
                BookmarkViewModel bookmark = row.getItem();
                if (bookmark != null) {
                    Optional<Integer> pageNumOpt = bookmark.getModel().getPageNum();
                    if (pageNumOpt.isPresent()) {
                        int offset = bookmarkSettingsState.getOffset();
                        return pageNumOpt.get() + offset - 1;
                    }
                }
                return null;
            });

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

    private void updateContextMenuStatus(TreeItem<BookmarkViewModel> selectedItem) {
        if (selectedItem == null) {
            promoteMenuItem.setDisable(true);
            demoteMenuItem.setDisable(true);
            return;
        }
        TreeItem<BookmarkViewModel> parent = selectedItem.getParent();
        promoteMenuItem.setDisable(parent == null || parent == treeTableView.getRoot());

        if (parent != null) {
            int index = parent.getChildren().indexOf(selectedItem);
            demoteMenuItem.setDisable(index == 0);
        } else {
            demoteMenuItem.setDisable(true);
        }
    }

    private void promoteSelection() {
        TreeItem<BookmarkViewModel> itemToPromote = treeTableView.getSelectionModel().getSelectedItem();
        if (itemToPromote == null) return;
        TreeItem<BookmarkViewModel> parentItem = itemToPromote.getParent();
        if (parentItem == null || parentItem == treeTableView.getRoot()) return;
        TreeItem<BookmarkViewModel> grandParentItem = parentItem.getParent();
        if (grandParentItem == null) return;

        BookmarkViewModel bookmarkViewModelToPromote = itemToPromote.getValue();
        BookmarkViewModel oldParentBookmarkViewModel = parentItem.getValue();
        BookmarkViewModel newParentBookmarkViewModel = grandParentItem.getValue();

        int parentIndex = newParentBookmarkViewModel.getChildren().indexOf(oldParentBookmarkViewModel);

        oldParentBookmarkViewModel.removeChild(bookmarkViewModelToPromote);
        newParentBookmarkViewModel.addChild(parentIndex + 1, bookmarkViewModelToPromote.getModel());
    }

    private void demoteSelection() {
        TreeItem<BookmarkViewModel> itemToDemote = treeTableView.getSelectionModel().getSelectedItem();
        if (itemToDemote == null) return;
        TreeItem<BookmarkViewModel> parentItem = itemToDemote.getParent();
        if (parentItem == null) return;
        int currentIndex = parentItem.getChildren().indexOf(itemToDemote);
        if (currentIndex < 1) return;

        TreeItem<BookmarkViewModel> newParentItem = parentItem.getChildren().get(currentIndex - 1);

        BookmarkViewModel bookmarkViewModelToDemote = itemToDemote.getValue();
        BookmarkViewModel oldParentBookmarkViewModel = parentItem.getValue();
        BookmarkViewModel newParentBookmarkViewModel = newParentItem.getValue();

        oldParentBookmarkViewModel.removeChild(bookmarkViewModelToDemote);
        newParentBookmarkViewModel.addChild(bookmarkViewModelToDemote.getModel());
    }

    private void addBookmark(boolean asChild) {
        TreeItem<BookmarkViewModel> selectedItem = treeTableView.getSelectionModel().getSelectedItem();
        Bookmark newBookmark;

        if (selectedItem == null) { // Add to root
            BookmarkViewModel rootBookmarkViewModel = treeTableView.getRoot().getValue();
            if (rootBookmarkViewModel == null) return;
            newBookmark = new Bookmark("New Bookmark", null, 1);
            rootBookmarkViewModel.addChild(newBookmark);
        } else if (asChild) { // Add as a child of selected
            BookmarkViewModel parentBookmarkViewModel = selectedItem.getValue();
            newBookmark = new Bookmark("New Child", null, parentBookmarkViewModel.getModel().getLevel() + 1);
            parentBookmarkViewModel.addChild(newBookmark);
            selectedItem.setExpanded(true);
        } else { // Add as a sibling after selected
            BookmarkViewModel parentBookmarkViewModel = selectedItem.getParent().getValue();
            BookmarkViewModel siblingBookmarkViewModel = selectedItem.getValue();
            int index = parentBookmarkViewModel.getChildren().indexOf(siblingBookmarkViewModel);
            newBookmark = new Bookmark("New Sibling", null, siblingBookmarkViewModel.getModel().getLevel());
            parentBookmarkViewModel.addChild(index + 1, newBookmark);
        }
    }

    private void deleteSelectedBookmark() {
        TreeItem<BookmarkViewModel> selectedItem = treeTableView.getSelectionModel().getSelectedItem();
        if (selectedItem == null || selectedItem.getParent() == null) return;

        Optional<ButtonType> result = MyAlert.showAlert(Alert.AlertType.CONFIRMATION,
                bundle.getString("alert.deleteConfirmation"), treeTableView.getScene().getWindow());

        if (result.isPresent() && result.get() == ButtonType.OK) {
            BookmarkViewModel parentBookmarkViewModel = selectedItem.getParent().getValue();
            BookmarkViewModel bookmarkViewModelToRemove = selectedItem.getValue();
            parentBookmarkViewModel.removeChild(bookmarkViewModelToRemove);
        }
    }

    // --- Unused methods kept and adapted as per user request ---

    private void refreshTreeView() {
        Bookmark rootBookmark = bookmarkSettingsState.getRootBookmark();
        if (rootBookmark != null) {
            TreeItem<BookmarkViewModel> selectedItem = treeTableView.getSelectionModel().getSelectedItem();
            int selectedRowIndex = treeTableView.getSelectionModel().getSelectedIndex();

            Platform.runLater(() -> {
                treeTableView.refresh();

                if (selectedRowIndex >= 0 && selectedRowIndex < treeTableView.getExpandedItemCount()) {
                    treeTableView.getSelectionModel().select(selectedRowIndex);
                    treeTableView.getFocusModel().focus(selectedRowIndex);
                } else if (selectedItem != null) {
                    restoreSelectionWithoutScroll(selectedItem.getValue());
                }
            });
        }
    }

    private void refreshTreeItem(TreeItem<BookmarkViewModel> item) {
        if (item != null) {
            item.setValue(item.getValue());
            for (TreeItem<BookmarkViewModel> child : item.getChildren()) {
                refreshTreeItem(child);
            }
        }
    }

    private void restoreSelectionWithoutScroll(BookmarkViewModel targetBookmarkViewModel) {
        if (targetBookmarkViewModel == null) return;

        TreeItem<BookmarkViewModel> rootItem = treeTableView.getRoot();
        TreeItem<BookmarkViewModel> foundItem = findTreeItem(rootItem, targetBookmarkViewModel);
        if (foundItem != null) {
            treeTableView.getSelectionModel().select(foundItem);
            treeTableView.getFocusModel().focus(treeTableView.getRow(foundItem));
        }
    }

    private TreeItem<BookmarkViewModel> findTreeItem(TreeItem<BookmarkViewModel> parent, BookmarkViewModel target) {
        if (parent == null || target == null) return null;

        if (parent.getValue() == target) {
            return parent;
        }

        for (TreeItem<BookmarkViewModel> child : parent.getChildren()) {
            TreeItem<BookmarkViewModel> result = findTreeItem(child, target);
            if (result != null) {
                return result;
            }
        }

        return null;
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