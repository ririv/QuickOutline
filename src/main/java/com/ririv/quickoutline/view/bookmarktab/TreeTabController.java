package com.ririv.quickoutline.view.bookmarktab;

import com.google.inject.Inject;
import com.ririv.quickoutline.model.Bookmark;
import com.ririv.quickoutline.view.state.BookmarkSettingsState;
import com.ririv.quickoutline.view.LocalizationManager;
import com.ririv.quickoutline.view.MyAlert;
import com.ririv.quickoutline.view.RecursiveTreeItem;
import com.ririv.quickoutline.view.controls.EditableTreeTableCell;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.*;
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
        
        // 为使用EditableTreeTableCell的TreeTableView添加专用样式类
        treeTableView.getStyleClass().add("editable-tree-table-view");
        
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

            titleColumn.setCellFactory(EditableTreeTableCell.forTreeTableColumn());
            titleColumn.setOnEditCommit(event -> event.getRowValue().getValue().setTitle(event.getNewValue()));

            offsetPageColumn.setCellFactory(EditableTreeTableCell.forTreeTableColumn());
            offsetPageColumn.setOnEditCommit(event -> {
                String newValue = event.getNewValue();
                try {
                    Integer pageNum = newValue.isEmpty() ? null : Integer.valueOf(newValue);
                    event.getRowValue().getValue().setOffsetPageNum(pageNum);
                } catch (NumberFormatException e) {
                    // 如果转换失败，刷新显示原值
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
            int index = parent.getChildren().indexOf(selectedItem);
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

        // 修改数据模型
        oldParentBookmark.getChildren().remove(bookmarkToPromote);
        int parentIndex = newParentBookmark.getChildren().indexOf(oldParentBookmark);
        newParentBookmark.addChild(parentIndex + 1, bookmarkToPromote);
        
        // 刷新树视图显示
        refreshTreeView();
    }

    private void demoteSelection() {
        TreeItem<Bookmark> itemToDemote = treeTableView.getSelectionModel().getSelectedItem();
        if (itemToDemote == null) return;
        TreeItem<Bookmark> parent = itemToDemote.getParent();
        if (parent == null) return;
        int currentIndex = parent.getChildren().indexOf(itemToDemote);
        if (currentIndex < 1) return;

        TreeItem<Bookmark> newParentItem = parent.getChildren().get(currentIndex - 1);

        Bookmark bookmarkToDemote = itemToDemote.getValue();
        Bookmark oldParentBookmark = parent.getValue();
        Bookmark newParentBookmark = newParentItem.getValue();

        // 修改数据模型
        oldParentBookmark.getChildren().remove(bookmarkToDemote);
        newParentBookmark.addChild(bookmarkToDemote);
        
        // 刷新树视图显示
        refreshTreeView();
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

    // 添加刷新树视图的辅助方法
    private void refreshTreeView() {
        Bookmark rootBookmark = bookmarkSettingsState.getRootBookmark();
        if (rootBookmark != null) {
            // 保存当前状态
            TreeItem<Bookmark> selectedItem = treeTableView.getSelectionModel().getSelectedItem();
            int selectedRowIndex = treeTableView.getSelectionModel().getSelectedIndex();
            
            // 使用轻量级刷新，避免重建整个树
            Platform.runLater(() -> {
                // 刷新TreeTableView的数据显示
                treeTableView.refresh();
                
                // 恢复选择状态（优先使用行索引）
                if (selectedRowIndex >= 0 && selectedRowIndex < treeTableView.getExpandedItemCount()) {
                    treeTableView.getSelectionModel().select(selectedRowIndex);
                    treeTableView.getFocusModel().focus(selectedRowIndex);
                } else if (selectedItem != null) {
                    // 如果行索引无效，尝试按Bookmark查找
                    restoreSelectionWithoutScroll(selectedItem.getValue());
                }
            });
        }
    }
    
    // 递归刷新TreeItem及其子项
    private void refreshTreeItem(TreeItem<Bookmark> item) {
        if (item != null) {
            // 刷新当前项
            item.setValue(item.getValue()); // 触发值变化事件
            // 递归刷新子项
            for (TreeItem<Bookmark> child : item.getChildren()) {
                refreshTreeItem(child);
            }
        }
    }
    
    // 尝试恢复选择状态（不自动滚动）
    private void restoreSelectionWithoutScroll(Bookmark targetBookmark) {
        if (targetBookmark == null) return;
        
        TreeItem<Bookmark> rootItem = treeTableView.getRoot();
        TreeItem<Bookmark> foundItem = findTreeItem(rootItem, targetBookmark);
        if (foundItem != null) {
            // 只恢复选择，不滚动
            treeTableView.getSelectionModel().select(foundItem);
            treeTableView.getFocusModel().focus(treeTableView.getRow(foundItem));
        }
    }
    
    // 在树中查找特定的Bookmark对应的TreeItem
    private TreeItem<Bookmark> findTreeItem(TreeItem<Bookmark> parent, Bookmark target) {
        if (parent == null || target == null) return null;
        
        if (parent.getValue() == target) {
            return parent;
        }
        
        for (TreeItem<Bookmark> child : parent.getChildren()) {
            TreeItem<Bookmark> result = findTreeItem(child, target);
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
