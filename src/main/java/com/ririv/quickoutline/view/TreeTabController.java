package com.ririv.quickoutline.view;

import com.ririv.quickoutline.model.Bookmark;
import com.ririv.quickoutline.service.PdfOutlineService;
import com.ririv.quickoutline.utils.LocalizationManager;
import javafx.beans.property.SimpleStringProperty;
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

    private final Map<String, TreeItem<Bookmark>> itemCache = new HashMap<>();
    private PdfOutlineService pdfOutlineService;
    private MainController mainController;

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
        this.pdfOutlineService = mainController.pdfOutlineService;
    }

    public void initialize() {
        treeTableView.setEditable(true);
        treeTableView.setColumnResizePolicy(TreeTableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        titleColumn.prefWidthProperty().bind(treeTableView.widthProperty().multiply(0.9));
        offsetPageColumn.prefWidthProperty().bind(treeTableView.widthProperty().multiply(0.1));
        setupRowFactory();
    }

    void reconstructTree(Bookmark rootBookmark) {
        TreeItem<Bookmark> rootItem = new RecursiveTreeItem(rootBookmark);
        expandAllNodes(rootItem);

        titleColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getValue().getTitle()));
        offsetPageColumn.setCellValueFactory(param -> {
            String pageNumStr = param.getValue().getValue().getOffsetPageNum()
                                   .map(String::valueOf).orElse("");
            return new SimpleStringProperty(pageNumStr);
        });

        titleColumn.setCellFactory(TextFieldTreeTableCell.forTreeTableColumn());
        titleColumn.setOnEditCommit(event -> {
            event.getRowValue().getValue().setTitle(event.getNewValue());
            syncTextTabView();
        });

        offsetPageColumn.setCellFactory(TextFieldTreeTableCell.forTreeTableColumn());
        offsetPageColumn.setOnEditCommit(event -> {
            try {
                event.getRowValue().getValue().setOffsetPageNum(Integer.parseInt(event.getNewValue()));
            } catch (NumberFormatException e) {
                treeTableView.refresh();
            }
            syncTextTabView();
        });

        treeTableView.setShowRoot(false);
        treeTableView.setRoot(rootItem);
        treeTableView.refresh();
        itemCache.clear();
    }

    private void setupRowFactory() {
        ResourceBundle bundle = LocalizationManager.getResourceBundle();
        ContextMenu contextMenu = new ContextMenu();
        MenuItem addSiblingItem = new MenuItem(bundle.getString("contextMenu.addBookmark"));
        addSiblingItem.setOnAction(event -> addBookmark(false));
        MenuItem addChildItem = new MenuItem(bundle.getString("contextMenu.addChildBookmark"));
        addChildItem.setOnAction(event -> addBookmark(true));
        MenuItem deleteItem = new MenuItem(bundle.getString("contextMenu.deleteBookmark"));
        deleteItem.setOnAction(event -> deleteSelectedBookmark());
        contextMenu.getItems().addAll(addSiblingItem, addChildItem, new SeparatorMenuItem(), deleteItem);

        treeTableView.setRowFactory(tv -> {
            TreeTableRow<Bookmark> row = new TreeTableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.SECONDARY && !row.isEmpty()) {
                    contextMenu.show(row, event.getScreenX(), event.getScreenY());
                } else {
                    contextMenu.hide();
                }
            });

            row.setOnDragDetected(event -> {
                if (!row.isEmpty()) {
                    Dragboard db = row.startDragAndDrop(TransferMode.MOVE);
                    db.setDragView(row.snapshot(null, null));
                    ClipboardContent content = new ClipboardContent();
                    content.putString(row.getItem().getId());
                    db.setContent(content);
                    event.consume();
                }
            });

            row.setOnDragOver(event -> {
                Dragboard db = event.getDragboard();
                if (event.getGestureSource() != row && db.hasString() && !row.isEmpty()) {
                    TreeItem<Bookmark> draggedItem = findTreeItemById(treeTableView.getRoot(), db.getString());
                    if (draggedItem != null && !isChildOrSelf(draggedItem, row.getTreeItem())) {
                        event.acceptTransferModes(TransferMode.MOVE);
                    }
                }
                event.consume();
            });

            row.setOnDragDropped(event -> {
                Dragboard db = event.getDragboard();
                boolean success = false;
                if (db.hasString() && !row.isEmpty()) {
                    TreeItem<Bookmark> draggedItemUI = findTreeItemById(treeTableView.getRoot(), db.getString());
                    TreeItem<Bookmark> targetItemUI = row.getTreeItem();
                    if (draggedItemUI != null && targetItemUI != null && draggedItemUI != targetItemUI) {
                        Bookmark draggedBookmark = draggedItemUI.getValue();
                        Bookmark targetBookmark = targetItemUI.getValue();
                        
                        draggedBookmark.getOwnerList().remove(draggedBookmark);
                        Bookmark newParentBookmark = targetBookmark.getParent();
                        int targetIndex = newParentBookmark.getChildren().indexOf(targetBookmark);
                        newParentBookmark.addChild(targetIndex + 1, draggedBookmark);
                        
                        treeTableView.getSelectionModel().select(draggedItemUI);
                        success = true;
                    }
                }
                event.setDropCompleted(success);
                event.consume();
                if (success) syncTextTabView();
            });
            return row;
        });
    }

    private void addBookmark(boolean asChild) {
        TreeItem<Bookmark> selectedItem = treeTableView.getSelectionModel().getSelectedItem();
        Bookmark newBookmark;

        if (selectedItem == null) {
            Bookmark rootBookmark = treeTableView.getRoot().getValue();
            newBookmark = new Bookmark("New Bookmark", null, 1);
            rootBookmark.addChild(newBookmark);
        } else if (asChild) {
            Bookmark parentBookmark = selectedItem.getValue();
            newBookmark = new Bookmark("New Child", null, parentBookmark.getLevelByStructure() + 1);
            parentBookmark.addChild(newBookmark);
        } else {
            Bookmark parentBookmark = selectedItem.getParent().getValue();
            Bookmark siblingBookmark = selectedItem.getValue();
            int index = parentBookmark.getChildren().indexOf(siblingBookmark);
            newBookmark = new Bookmark("New Sibling", null, siblingBookmark.getLevelByStructure());
            parentBookmark.addChild(index + 1, newBookmark);
        }
        syncTextTabView();
    }

    private void deleteSelectedBookmark() {
        TreeItem<Bookmark> selectedItem = treeTableView.getSelectionModel().getSelectedItem();
        if (selectedItem == null || selectedItem.getParent() == null) return;

        ResourceBundle bundle = LocalizationManager.getResourceBundle();
        Optional<ButtonType> result = MyAlert.showAlert(Alert.AlertType.CONFIRMATION,
                bundle.getString("alert.deleteConfirmation"), treeTableView.getScene().getWindow());

        if (result.isPresent() && result.get() == ButtonType.OK) {
            Bookmark parentBookmark = selectedItem.getParent().getValue();
            Bookmark bookmarkToRemove = selectedItem.getValue();
            parentBookmark.getChildren().remove(bookmarkToRemove);
            syncTextTabView();
        }
    }

    private void syncTextTabView() {
        Bookmark rootBookmark = getRootBookmark();
        if (rootBookmark != null) {
            rootBookmark.updateLevelByStructureLevel();
            mainController.textTabViewController.contentsTextArea.setText(rootBookmark.toTreeText());
        }
    }

    private TreeItem<Bookmark> findTreeItemById(TreeItem<Bookmark> root, String id) {
        if (root.getValue().getId().equals(id)) return root;
        for (TreeItem<Bookmark> child : root.getChildren()) {
            TreeItem<Bookmark> result = findTreeItemById(child, id);
            if (result != null) return result;
        }
        return null;
    }

    private boolean isChildOrSelf(TreeItem<Bookmark> draggedItem, TreeItem<Bookmark> targetItem) {
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

    public Bookmark getRootBookmark() {
        if (treeTableView.getRoot() != null) {
            return treeTableView.getRoot().getValue();
        }
        return null;
    }
}