package com.ririv.quickoutline.view;

import com.google.inject.Inject;
import com.ririv.quickoutline.event.AppEventBus;
import com.ririv.quickoutline.model.Bookmark;
import com.ririv.quickoutline.state.BookmarkSettingsState;
import com.ririv.quickoutline.utils.LocalizationManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTreeTableCell;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseButton;
import javafx.scene.input.TransferMode;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

public class TreeTabController {
    public TreeTableView<Bookmark> treeTableView;
    public TreeTableColumn<Bookmark, String> titleColumn;
    public TreeTableColumn<Bookmark, String> offsetPageColumn;

    private final ResourceBundle bundle = LocalizationManager.getResourceBundle();
    private final AppEventBus eventBus;
    private final BookmarkSettingsState bookmarkSettingsState;
    private final Map<String, TreeItem<Bookmark>> itemCache = new HashMap<>();
    private MenuItem promoteMenuItem;
    private MenuItem demoteMenuItem;

    @Inject
    public TreeTabController(AppEventBus eventBus, BookmarkSettingsState bookmarkSettingsState) {
        this.eventBus = eventBus;
        this.bookmarkSettingsState = bookmarkSettingsState;
    }

    public void initialize() {
        treeTableView.setEditable(true);
        treeTableView.setColumnResizePolicy(TreeTableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        titleColumn.prefWidthProperty().bind(treeTableView.widthProperty().multiply(0.9));
        offsetPageColumn.prefWidthProperty().bind(treeTableView.widthProperty().multiply(0.1));
        setupRowFactory();

        // Cleanup placeholder on drag exit
        treeTableView.setOnDragExited(event -> removePlaceholder());

        bookmarkSettingsState.rootBookmarkProperty().addListener((obs, oldRoot, newRoot) -> {
            if (newRoot == null) {
                treeTableView.setRoot(null);
                return;
            }
            TreeItem<Bookmark> rootItem = new RecursiveTreeItem(newRoot);
            expandAllNodes(rootItem);

            // Rebuild cache
            itemCache.clear();
            buildCache(rootItem);

            titleColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getValue().getTitle()));
            offsetPageColumn.setCellValueFactory(param -> {
                String pageNumStr = param.getValue().getValue().getOffsetPageNum()
                        .map(String::valueOf).orElse("");
                return new SimpleStringProperty(pageNumStr);
            });

            titleColumn.setCellFactory(TextFieldTreeTableCell.forTreeTableColumn());
            titleColumn.setOnEditCommit(event -> {
                event.getRowValue().getValue().setTitle(event.getNewValue());
            });

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
            treeTableView.refresh();
        });
    }

    private void buildCache(TreeItem<Bookmark> item) {
        if (item != null && item.getValue() != null) {
            itemCache.put(item.getValue().getId(), item);
            for (TreeItem<Bookmark> child : item.getChildren()) {
                buildCache(child);
            }
        }
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
            TreeTableRow<Bookmark> row = new TreeTableRow<>() {
                @Override
                protected void updateItem(Bookmark item, boolean empty) {
                    super.updateItem(item, empty);
                    getStyleClass().remove("placeholder-row");
                    if (!empty && item == PLACEHOLDER) {
                        getStyleClass().add("placeholder-row");
                    }
                }
            };

            row.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.SECONDARY && !row.isEmpty()) {
                    updateContextMenuStatus(row.getTreeItem());
                    contextMenu.show(row, event.getScreenX(), event.getScreenY());
                } else {
                    contextMenu.hide();
                }
            });

            row.setOnDragDetected(event -> {
                if (!row.isEmpty() && row.getItem() != PLACEHOLDER) {
                    Dragboard db = row.startDragAndDrop(TransferMode.MOVE);
                    db.setDragView(row.snapshot(null, null));
                    ClipboardContent content = new ClipboardContent();
                    content.putString(row.getTreeItem().getValue().getId());
                    db.setContent(content);
                    // Crucial for cleanup: remove placeholder when drag is done, no matter how it ends.
                    row.setOnDragDone(e -> removePlaceholder());
                    event.consume();
                }
            });

            row.setOnDragExited(event -> row.getStyleClass().removeAll("drop-hint-child"));

            row.setOnDragOver(event -> {
                Dragboard db = event.getDragboard();
                if (event.getGestureSource() != row && db.hasString() && !row.isEmpty()) {
                    TreeItem<Bookmark> draggedItem = findTreeItemById(db.getString());
                    if (draggedItem != null && !isChildOrSelf(draggedItem, row.getTreeItem())) {
                        event.acceptTransferModes(TransferMode.MOVE);

                        // Placeholder logic
                        removePlaceholder();

                        final double dropZoneHeight = row.getHeight() * 0.25;
                        if (event.getY() < dropZoneHeight) {
                            row.getStyleClass().removeAll("drop-hint-child");
                            addPlaceholder(row.getTreeItem(), true); // Insert before
                        } else if (event.getY() > row.getHeight() - dropZoneHeight) {
                            row.getStyleClass().removeAll("drop-hint-child");
                            addPlaceholder(row.getTreeItem(), false); // Insert after
                        } else {
                            row.getStyleClass().add("drop-hint-child");
                        }
                    }
                }
                event.consume();
            });

            row.setOnDragDropped(event -> {
                Dragboard db = event.getDragboard();
                boolean success = false;
                if (db.hasString() && !row.isEmpty()) {
                    TreeItem<Bookmark> draggedItemUI = findTreeItemById(db.getString());
                    Bookmark draggedBookmark = draggedItemUI.getValue();

                    // Find placeholder index
                    int placeholderIndex = -1;
                    Bookmark parentOfPlaceholder = null;
                    java.util.List<Bookmark> allNodes = bookmarkSettingsState.getRootBookmark().flattenToList();
                    allNodes.add(0, bookmarkSettingsState.getRootBookmark()); // Add root to the list to check its children

                    for (Bookmark parentCand : allNodes) {
                        int idx = parentCand.getChildren().indexOf(PLACEHOLDER);
                        if (idx != -1) {
                            placeholderIndex = idx;
                            parentOfPlaceholder = parentCand;
                            break;
                        }
                    }

                    removePlaceholder();

                    if (placeholderIndex != -1) {
                        // Case 1: Dropped on a placeholder line
                        draggedBookmark.getSiblingsList().remove(draggedBookmark);
                        parentOfPlaceholder.addChild(placeholderIndex, draggedBookmark);
                        success = true;
                    } else {
                        // Case 2: Dropped on a node to make it a child
                        TreeItem<Bookmark> targetItemUI = row.getTreeItem();
                        if (draggedItemUI != null && targetItemUI != null && draggedItemUI != targetItemUI) {
                            draggedBookmark.getSiblingsList().remove(draggedBookmark);
                            Bookmark newParentBookmark = targetItemUI.getValue();
                            newParentBookmark.addChild(draggedBookmark);
                            targetItemUI.setExpanded(true);
                            success = true;
                        }
                    }

                    if(success) {
//                        bookmarkSettingsState.getRootBookmark().updateLevelByStructureLevel();
                        treeTableView.refresh();
                    }
                }
                event.setDropCompleted(success);
                event.consume();
            });
            return row;
        });
    }

    private static final Bookmark PLACEHOLDER = new Bookmark("---placeholder---", null, -1);

    

    private void addPlaceholder(TreeItem<Bookmark> targetItem, boolean before) {
        if (targetItem == null) return;
        Bookmark targetBookmark = targetItem.getValue();
        if (targetBookmark == PLACEHOLDER) return;

        Bookmark parentBookmark = targetItem.getParent().getValue();
        if (parentBookmark == null) return; // Should not happen for non-root items

        ObservableList<Bookmark> siblings = parentBookmark.getChildren();
        int targetIndex = siblings.indexOf(targetBookmark);

        if (before) {
            siblings.add(targetIndex, PLACEHOLDER);
        } else {
            siblings.add(targetIndex + 1, PLACEHOLDER);
        }
    }

    private void removePlaceholder() {
        // Search the entire tree for the placeholder and remove it.
        if (bookmarkSettingsState.getRootBookmark() == null) return;
        java.util.List<Bookmark> allNodes = bookmarkSettingsState.getRootBookmark().flattenToList();
        for (Bookmark node : allNodes) {
            node.getChildren().remove(PLACEHOLDER);
        }
        // Also check the root's direct children, as genLinearList might not include the root itself.
        bookmarkSettingsState.getRootBookmark().getChildren().remove(PLACEHOLDER);
    }

    private void updateContextMenuStatus(TreeItem<Bookmark> selectedItem) {
        if (selectedItem == null || selectedItem.getValue() == PLACEHOLDER) {
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

//        bookmarkSettingsState.getRootBookmark().updateLevelByStructureLevel();
        treeTableView.refresh();
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

//        bookmarkSettingsState.getRootBookmark().updateLevelByStructureLevel();
        treeTableView.refresh();
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
            newBookmark = new Bookmark("New Child", null, parentBookmark.calculateLevelByStructure() + 1);
            parentBookmark.addChild(newBookmark);
        } else {
            Bookmark parentBookmark = selectedItem.getParent().getValue();
            Bookmark siblingBookmark = selectedItem.getValue();
            int index = parentBookmark.getChildren().indexOf(siblingBookmark);
            newBookmark = new Bookmark("New Sibling", null, siblingBookmark.calculateLevelByStructure());
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

    private TreeItem<Bookmark> findTreeItemById(String id) {
        return itemCache.get(id);
    }

    private boolean isChildOrSelf(TreeItem<Bookmark> draggedItem, TreeItem<Bookmark> targetItem) {
        if (targetItem.getValue() == PLACEHOLDER) return true; // Prevent dropping onto placeholder
        TreeItem<Bookmark> temp = targetItem;
        while (temp != null) {
            if (temp == draggedItem) return true;
            temp = temp.getParent();
        }
        return false;
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