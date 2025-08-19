package com.ririv.quickoutline.view;

import com.ririv.quickoutline.model.Bookmark;
import com.ririv.quickoutline.service.PdfOutlineService;
import com.ririv.quickoutline.textProcess.methods.Method;
import com.ririv.quickoutline.utils.LocalizationManager;
import javafx.beans.property.ReadOnlyStringWrapper;
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

//import static com.ririv.contents.view.MainController.mainController;


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


    //不要在此方法中访问mainController，因此此时还没有产生此实例，得到null
    public void initialize() {
        treeTableView.setEditable(true); // 设置 TreeTableView 可编辑

        // 仅显示两列且撑满整个view
        // 注意两个值的和需要为1
        treeTableView.setColumnResizePolicy(TreeTableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        titleColumn.prefWidthProperty().bind(treeTableView.widthProperty().multiply(0.9));
        offsetPageColumn.prefWidthProperty().bind(treeTableView.widthProperty().multiply(0.1));


        setupRowFactory();

//        gradeBtn.setOnAction(event -> grade(true));
//        degradeBtn.setOnAction(event -> grade(false));


    }

    //TODO,没法自动展开
    public void grade(boolean isGrade) {
        int i = treeTableView.getSelectionModel().getFocusedIndex();
        Bookmark rootBookmark = treeTableView.getRoot().getValue();
        String[] lines = rootBookmark.toString().split("\n");
        String focusedLine = lines[i];
        if (isGrade) {
            lines[i] = focusedLine.replaceFirst("\t", ""); //升级
        } else {
            lines[i] = "\t" + focusedLine; //降级
        }
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            sb.append(line).append("\n");
        }
        System.out.println(sb);
        rootBookmark = pdfOutlineService.convertTextToBookmarkTreeByMethod(sb.toString(), 0, Method.INDENT
//                ,true
        );

        reconstructTree(rootBookmark);

        treeTableView.getTreeItem(i).setExpanded(true);
        treeTableView.getSelectionModel().select(treeTableView.getSelectionModel().getModelItem(i));
        treeTableView.refresh();
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

            // 1. Context Menu Logic
            row.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.SECONDARY && (!row.isEmpty())) {
                    contextMenu.show(row, event.getScreenX(), event.getScreenY());
                } else {
                    contextMenu.hide();
                }
            });

            // 2. Drag and Drop Logic
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
                    TreeItem<Bookmark> targetItem = row.getTreeItem();

                    if (draggedItem != null && targetItem != null && !isChildOrSelf(draggedItem, targetItem)) {
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
                        Bookmark oldParentBookmark = draggedItemUI.getParent().getValue();
                        
                        // Data Model Update
                        oldParentBookmark.getChildren().remove(draggedBookmark);
                        Bookmark newParentBookmark = targetItemUI.getParent().getValue();
                        int targetIndexInData = newParentBookmark.getChildren().indexOf(targetBookmark);
                        newParentBookmark.getChildren().add(targetIndexInData + 1, draggedBookmark);
                        draggedBookmark.setParent(newParentBookmark);

                        // UI Update
                        draggedItemUI.getParent().getChildren().remove(draggedItemUI);
                        TreeItem<Bookmark> newParentUI = targetItemUI.getParent();
                        int targetIndexInUI = newParentUI.getChildren().indexOf(targetItemUI);
                        newParentUI.getChildren().add(targetIndexInUI + 1, draggedItemUI);
                        
                        treeTableView.getSelectionModel().select(draggedItemUI);
                        success = true;
                    }
                }
                event.setDropCompleted(success);
                event.consume();
                if(success) {
                    syncTextTabView();
                }
            });

            return row;
        });
    }

    private TreeItem<Bookmark> findTreeItemById(TreeItem<Bookmark> root, String id) {
        // 检查缓存
        if (itemCache.containsKey(id)) {
            return itemCache.get(id);
        }

        if (root.getValue().getId().equals(id)) {
            itemCache.put(id, root); // 存入缓存
            return root;
        }

        for (TreeItem<Bookmark> child : root.getChildren()) {
            TreeItem<Bookmark> result = findTreeItemById(child, id);
            if (result != null) {
                return result;
            }
        }

        return null;
    }

    /**
     * 检查目标节点是否是拖拽节点的子节点或自身节点（迭代实现）
     * 转化为问题:拖拽节点是否为目标节点的父节点来检查父节点链,以避免原问题会使用递归检查子节点链
     *
     * @param draggedItem 被拖拽的节点
     * @param targetItem  目标节点
     * @return true 如果目标节点是拖拽节点的子节点或自身节点
     */
    private boolean isChildOrSelf(TreeItem<Bookmark> draggedItem, TreeItem<Bookmark> targetItem) {
        TreeItem<Bookmark> temp = targetItem;

        // 使用循环逐级向上检查
        while (temp != null) {
            if (temp == draggedItem) {
                return true;
            }
            temp = temp.getParent(); // 向上移动到父节点
        }

        return false;
    }


    //注意不要新建TreeTableView实例传递给tree，因为fxml已经有一个此控件实例并绑定到tree了，新建实例仅会导致fxml视图中的真实实例丢失指针
    void reconstructTree(Bookmark rootBookmark) {

        TreeItem<Bookmark> rootItem = new TreeItem<>(rootBookmark);

        convert(rootBookmark, rootItem);


        titleColumn.setCellValueFactory(( param) ->
                new SimpleStringProperty(param.getValue().getValue().getTitle()));


        offsetPageColumn.setCellValueFactory((param) -> {
            var pageNum = param.getValue().getValue().getOffsetPageNum();
            String pageNumStr;
            pageNumStr = pageNum.map(integer -> Integer.toString(integer)).orElse("");
            return new SimpleStringProperty(pageNumStr);
        });

        titleColumn.setCellFactory(TextFieldTreeTableCell.forTreeTableColumn());
        titleColumn.setOnEditCommit(event -> {
            TreeItem<Bookmark> currentEditingItem = treeTableView.getTreeItem(event.getTreeTablePosition().getRow());
            currentEditingItem.getValue().setTitle(event.getNewValue());
        });

        offsetPageColumn.setCellFactory(TextFieldTreeTableCell.forTreeTableColumn());
        offsetPageColumn.setOnEditCommit(event -> {
            TreeItem<Bookmark> currentEditingItem = treeTableView.getTreeItem(event.getTreeTablePosition().getRow());
            try {
                currentEditingItem.getValue().setOffsetPageNum(Integer.parseInt(event.getNewValue()));
            } catch (NumberFormatException e) {
                //
            }
        });




        expandAllNodes(rootItem);

        treeTableView.setShowRoot(false);
        treeTableView.setRoot(rootItem);
        treeTableView.refresh();
        itemCache.clear();
    }

    //实际应用中此功能没有需求场景
    public void updatePageNum(int offset) {
        offsetPageColumn.setCellValueFactory((TreeTableColumn.CellDataFeatures<Bookmark, String> p) -> {
            var pageNum = p.getValue().getValue().getOffsetPageNum();
            String offsetPageNumStr = pageNum.map(integer -> Integer.toString(integer+offset)).orElse("");

            String offsetCellValue;
            if (offset == 0) {
                offsetCellValue = offsetPageNumStr;
            } else {
                offsetCellValue = pageNum + "  (" + offsetPageNumStr + ")";
            }
            return new ReadOnlyStringWrapper(offsetCellValue);
        });
        treeTableView.refresh();
    }

    private void convert(Bookmark bookmark, TreeItem<Bookmark> item) {
        for (Bookmark i : bookmark.getChildren()) {
            TreeItem<Bookmark> j = new TreeItem<>(i);
            item.getChildren().add(j);
            convert(i, j);
        }
    }

    // 递归展开所有节点
    private void expandAllNodes(TreeItem<?> item) {
        if (item != null) {
            item.setExpanded(true); // 展开当前节点
            for (TreeItem<?> child : item.getChildren()) {
                expandAllNodes(child); // 递归展开子节点
            }
        }
    }


    
    

    private void addBookmark(boolean asChild) {
        TreeItem<Bookmark> selectedItem = treeTableView.getSelectionModel().getSelectedItem();

        // Case 1: No item selected, add to root
        if (selectedItem == null) {
            TreeItem<Bookmark> rootItem = treeTableView.getRoot();
            if (rootItem == null) return;
            Bookmark rootBookmark = rootItem.getValue();
            Bookmark newBookmark = new Bookmark("New Bookmark", null, 1);
            newBookmark.setParent(rootBookmark); // Set parent for data model consistency
            rootBookmark.getChildren().add(newBookmark); // Data model update
            TreeItem<Bookmark> newItem = new TreeItem<>(newBookmark);
            rootItem.getChildren().add(newItem); // UI update
            syncTextTabView();
            return;
        }

        // Case 2: Add as a child of the selected item
        if (asChild) {
            Bookmark parentBookmark = selectedItem.getValue();
            Bookmark newBookmark = new Bookmark("New Child", null, parentBookmark.getLevelByStructure() + 1);
            newBookmark.setParent(parentBookmark); // Set parent for data model consistency
            parentBookmark.getChildren().add(newBookmark); // Data model update
            TreeItem<Bookmark> newItem = new TreeItem<>(newBookmark);
            selectedItem.getChildren().add(newItem); // UI update
            selectedItem.setExpanded(true);
        }
        // Case 3: Add as a sibling after the selected item
        else {
            TreeItem<Bookmark> parentItem = selectedItem.getParent();
            if (parentItem == null) return; // Should not happen for visible items

            Bookmark parentBookmark = parentItem.getValue();
            Bookmark siblingBookmark = selectedItem.getValue();
            int index = parentItem.getChildren().indexOf(selectedItem);

            Bookmark newBookmark = new Bookmark("New Sibling", null, siblingBookmark.getLevelByStructure());
            newBookmark.setParent(parentBookmark); // Set parent for data model consistency
            parentBookmark.getChildren().add(index + 1, newBookmark); // Data model update

            TreeItem<Bookmark> newItem = new TreeItem<>(newBookmark);
            parentItem.getChildren().add(index + 1, newItem); // UI update
        }
        syncTextTabView();
    }

    private void deleteSelectedBookmark() {
        TreeItem<Bookmark> selectedItem = treeTableView.getSelectionModel().getSelectedItem();
        if (selectedItem == null || selectedItem.getParent() == null) { // Cannot delete the (hidden) root
            return;
        }

        ResourceBundle bundle = LocalizationManager.getResourceBundle();
        Optional<ButtonType> result = MyAlert.showAlert(
                Alert.AlertType.CONFIRMATION,
                bundle.getString("alert.deleteConfirmation"),
                treeTableView.getScene().getWindow()
        );

        if (result.isPresent() && result.get() == ButtonType.OK) {
            TreeItem<Bookmark> parentItem = selectedItem.getParent();
            Bookmark parentBookmark = parentItem.getValue();
            Bookmark bookmarkToRemove = selectedItem.getValue();

            // Data model update
            parentBookmark.getChildren().remove(bookmarkToRemove);

            // UI update
            parentItem.getChildren().remove(selectedItem);
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

    public Bookmark getRootBookmark() {
        if (treeTableView.getRoot() != null) {
            return treeTableView.getRoot().getValue();
        }
        return null;
    }
}
