package com.ririv.quickoutline.view;

import com.ririv.quickoutline.model.Bookmark;
import com.ririv.quickoutline.service.PdfService;
import com.ririv.quickoutline.textProcess.methods.Method;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.*;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;

import java.util.HashMap;
import java.util.Map;

//import static com.ririv.contents.view.MainController.mainController;


public class TreeModeController {
    public TreeTableView<Bookmark> treeTableView;

    public TreeTableColumn<Bookmark, String> titleColumn;
    public TreeTableColumn<Bookmark, String> offsetPageColumn;

    private final Map<String, TreeItem<Bookmark>> itemCache = new HashMap<>();




    private PdfService pdfService;
    private MainController mainController;


    public void setMainController(MainController mainController) {
        this.mainController = mainController;
        this.pdfService = mainController.pdfService;
    }


    //不要在此方法中访问mainController，因此此时还没有产生此实例，得到null
    public void initialize() {
        treeTableView.setEditable(true); // 设置 TreeTableView 可编辑

        // 仅显示两列且撑满整个view
        // 注意两个值的和需要为1
        treeTableView.setColumnResizePolicy(TreeTableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        titleColumn.prefWidthProperty().bind(treeTableView.widthProperty().multiply(0.9));
        offsetPageColumn.prefWidthProperty().bind(treeTableView.widthProperty().multiply(0.1));


//        setupDragAndDrop(treeTableView);

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
        rootBookmark = pdfService.convertTextToBookmarkTreeByMethod(sb.toString(), 0, Method.INDENT
//                ,true
        );

        reconstructTree(rootBookmark);

        treeTableView.getTreeItem(i).setExpanded(true);
        treeTableView.getSelectionModel().select(treeTableView.getSelectionModel().getModelItem(i));
        treeTableView.refresh();
    }

    private void setupDragAndDrop(TreeTableView<Bookmark> treeTableView) {
        // 为行添加拖拽功能
        treeTableView.setRowFactory(tv -> {
            TreeTableRow<Bookmark> row = new TreeTableRow<>();

            // 检测拖拽操作
            row.setOnDragDetected(event -> {
                if (!row.isEmpty()) {
                    Dragboard db = row.startDragAndDrop(TransferMode.MOVE);

                    ClipboardContent content = new ClipboardContent();
                    content.putString(row.getItem().getId()); // 使用唯一标识符
                    db.setContent(content);

                    event.consume();
                }
            });
//
//            // 处理拖拽进入目标节点
//            row.setOnDragOver(event -> {
//                Dragboard db = event.getDragboard();
//
//                // 检查是否是合法的拖拽行为
//                if (event.getGestureSource() != row && db.hasString() && !row.isEmpty()) {
//                    String draggedItemId = db.getString(); // 获取唯一标识符
//                    TreeItem<Bookmark> draggedItem = findTreeItemById(treeTableView.getRoot(), draggedItemId); // 根据 ID 查找
//                    TreeItem<Bookmark> targetItem = row.getTreeItem();
//
//                    if (draggedItem != null && targetItem != null) {
//                        // 检查目标节点是否是拖拽节点的子节点或自身节点
//                        if (isChildOrSelf(draggedItem, targetItem)) {
//                            event.acceptTransferModes(); // 禁止拖拽
//                            event.consume();
//                            return;
//                        }
//
//                        // 如果合法，允许拖拽
//                        event.acceptTransferModes(TransferMode.MOVE);
//                    }
//                }
//
//                event.consume();
//            });
//
//
//            // 处理拖拽释放到目标节点
//            row.setOnDragDropped(event -> {
//                Dragboard db = event.getDragboard();
//
//                if (db.hasString() && !row.isEmpty()) {
//                    String draggedItemId = db.getString(); // 获取唯一标识符
//                    TreeItem<Bookmark> draggedItem = findTreeItemById(treeTableView.getRoot(), draggedItemId); // 根据 ID 查找
//                    TreeItem<Bookmark> targetItem = row.getTreeItem();
//
//                    if (draggedItem != null && targetItem != null) {
//
//                        // 移动节点：从原父节点中删除
//                        draggedItem.getParent().getChildren().remove(draggedItem);
//
//                        // 添加到目标节点
//                        targetItem.getChildren().add(draggedItem);
//
//                        // 刷新视图（可选，确保视图更新）
//                        treeTableView.refresh(); // 刷新整个视图
//
//                        // 设置选中项为拖拽的 TreeItem
//                        treeTableView.getSelectionModel().select(draggedItem);
//                    }
//
//                    event.setDropCompleted(true);
//                } else {
//                    event.setDropCompleted(false);
//                }
//
//                event.consume();
//            });
            row.setOnDragOver(event -> {
                Dragboard db = event.getDragboard();

                if (event.getGestureSource() != row && db.hasString() && !row.isEmpty()) {
                    String draggedItemId = db.getString(); // 获取唯一标识符
                    TreeItem<Bookmark> draggedItem = findTreeItemById(treeTableView.getRoot(), draggedItemId); // 根据 ID 查找
                    TreeItem<Bookmark> targetItem = row.getTreeItem();

                    if (draggedItem != null && targetItem != null) {
                        // 检查目标节点是否是拖拽节点的子节点或自身节点
                        if (isChildOrSelf(draggedItem, targetItem)) {
                            event.acceptTransferModes(); // 禁止拖拽
                            event.consume();
                            return;
                        }

                        // 根据鼠标位置判断插入行为（上方/下方/作为子节点）
                        double y = event.getY();
                        double height = row.getHeight();
                        if (y < height * 0.33) {
                            // 鼠标在上三分之一处，表示插入到目标节点上方
                            row.setStyle("-fx-border-color: blue; -fx-border-width: 2 0 0 0;"); // 高亮上边框
                            row.getProperties().put("drag-insert-position", "above");
                        } else if (y > height * 0.66) {
                            // 鼠标在下三分之一处，表示插入到目标节点下方
                            row.setStyle("-fx-border-color: blue; -fx-border-width: 0 0 2 0;"); // 高亮下边框
                            row.getProperties().put("drag-insert-position", "below");
                        } else {
                            // 鼠标在中间区域，表示作为子节点
                            row.setStyle("-fx-background-color: lightblue;"); // 高亮整个行
                            row.getProperties().put("drag-insert-position", "child");
                        }

                        event.acceptTransferModes(TransferMode.MOVE);
                    }
                }

                event.consume();
            });

            row.setOnDragExited(event -> {
                row.setStyle(""); // 清除所有样式
                row.getProperties().remove("drag-insert-position"); // 移除插入位置标记
            });

            row.setOnDragDropped(event -> {
                Dragboard db = event.getDragboard();

                if (db.hasString() && !row.isEmpty()) {
                    String draggedItemId = db.getString(); // 获取唯一标识符
                    TreeItem<Bookmark> draggedItem = findTreeItemById(treeTableView.getRoot(), draggedItemId); // 根据 ID 查找
                    TreeItem<Bookmark> targetItem = row.getTreeItem();

                    if (draggedItem != null && targetItem != null) {
                        // 检查插入位置
                        String position = (String) row.getProperties().get("drag-insert-position");

                        if ("above".equals(position)) {
                            // 插入到目标节点上方
                            TreeItem<Bookmark> parent = targetItem.getParent();
                            if (parent != null) {
                                int index = parent.getChildren().indexOf(targetItem);
                                parent.getChildren().remove(draggedItem);
                                parent.getChildren().add(index, draggedItem);
                            }
                        } else if ("below".equals(position)) {
                            // 插入到目标节点下方
                            TreeItem<Bookmark> parent = targetItem.getParent();
                            if (parent != null) {
                                int index = parent.getChildren().indexOf(targetItem);
                                parent.getChildren().remove(draggedItem);
                                parent.getChildren().add(index + 1, draggedItem);
                            }
                        } else {
                            // 作为目标节点的子节点
                            draggedItem.getParent().getChildren().remove(draggedItem);
                            targetItem.getChildren().add(draggedItem);
                        }
//                         刷新视图（可选，确保视图更新）
                        treeTableView.refresh(); // 刷新整个视图
                        // 设置选中项为拖拽的 TreeItem
                        treeTableView.getSelectionModel().select(draggedItem);
                    }

                    event.setDropCompleted(true);
                } else {
                    event.setDropCompleted(false);
                }

                event.consume();
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

//        titleColumn.setCellFactory(TextFieldTreeTableCell.forTreeTableColumn());

//        titleColumn.setCellFactory(param -> new EditableTreeTableCell());




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


    // 自定义 TreeTableCell，使其可编辑
    @SuppressWarnings("unchecked")
    private static class EditableTreeTableCell<S, T> extends javafx.scene.control.TreeTableCell<S, T> {
        @Override
        public void startEdit() {
            super.startEdit();

            if (getTableColumn().getText().equals("标题")) {
                TextField textField = new TextField(getItem() != null ? getItem().toString() : "");
                textField.setOnAction(event -> commitEdit((T) textField.getText())); // 提交编辑的文本
                setGraphic(textField);
                textField.selectAll();
            } else if (getTableColumn().getText().equals("Age")) {
                TextField textField = new TextField(getItem() != null ? getItem().toString() : "");
                textField.setOnAction(event -> {
                    try {
                        commitEdit((T) Integer.valueOf(textField.getText())); // 提交编辑的整数值
                    } catch (NumberFormatException e) {
                        // 如果输入无效，提示用户并不进行提交
                        Alert alert = new Alert(Alert.AlertType.WARNING);
                        alert.setHeaderText("Invalid input");
                        alert.setContentText("Please enter a valid number for Age.");
                        alert.showAndWait();
                    }
                });
                setGraphic(textField);
                textField.selectAll();
            }
        }

        @Override
        public void cancelEdit() {
            super.cancelEdit();
            setText(getItem().toString());
            setGraphic(null);
        }

        @Override
        public void updateItem(T item, boolean empty) {
            super.updateItem(item, empty);

            if (empty) {
                setText(null);
                setGraphic(null);
            } else {
                if (isEditing()) {
                    setText(null);
                } else {
                    setText(item != null ? item.toString() : "");
                }
            }
        }
    }
}
