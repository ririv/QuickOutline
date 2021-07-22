package com.ririv.quickoutline.view;

import com.ririv.quickoutline.entity.Bookmark;
import com.ririv.quickoutline.enums.Method;
import com.ririv.quickoutline.service.PdfService;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.control.*;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.scene.paint.Paint;

import java.util.ArrayList;
import java.util.List;

//import static com.ririv.contents.view.MainController.mainController;


public class TreeModeController {
    public TreeTableView<Bookmark> tree;

    public TreeTableColumn<Bookmark, String> titleColumn;
    public TreeTableColumn<Bookmark, String> offsetPageColumn;

    private static final DataFormat SERIALIZED_MIME_TYPE = new DataFormat("application/x-java-serialized-object");
    public Button gradeBtn;
    public Button degradeBtn;

    TreeItem<Bookmark> dragItem;
    TreeTableRow<Bookmark> lastDragRow;


    //不要在此方法中访问mainController，因此此时还没有产生此实例，得到null
    public void initialize() {
//        titleColumn.prefWidthProperty().bind(tree.widthProperty().multiply(.5));
//        titleColumn.prefWidthProperty().bind(tree.widthProperty().multiply(.1));


//        drag();
//        gradeBtn.setOnAction(event -> grade(true));
//        degradeBtn.setOnAction(event -> grade(false));


    }

    //TODO,没法自动展开
    public void grade(boolean isGrade) {
        int i = tree.getSelectionModel().getFocusedIndex();
        Bookmark rootBookmark = tree.getRoot().getValue();
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
        rootBookmark = PdfService.textToBookmarkByMethod(sb.toString(), 0, Method.INDENT
//                ,true
        );

        reconstructTree(rootBookmark);

        tree.getTreeItem(i).setExpanded(true);
        tree.getSelectionModel().select(tree.getSelectionModel().getModelItem(i));
        tree.refresh();
    }


    //TODO,拖拽排序功能，实现了一半，有点bug，太难了，不打算做了,哪个来大神实现下
    void drag() {
//        PauseTransition pt = new PauseTransition(Duration.millis(1000));
//
//        pt.setOnFinished((e) -> {
//            if (!dragItem.isLeaf()) dragItem.setExpanded(false);
//        });


        tree.setRowFactory(tv -> {
            //注意cell与row的区别，cell指某行某列的那一小个
            TreeTableRow<Bookmark> row = new TreeTableRow<>();

            row.setOnDragDetected(event -> {
                if (!row.isEmpty()) {
                    Dragboard db = row.startDragAndDrop(TransferMode.MOVE);
                    db.setDragView(row.snapshot(null, null));
                    ClipboardContent cc = new ClipboardContent();
                    cc.put(SERIALIZED_MIME_TYPE, row.getTreeItem().getValue());
                    db.setContent(cc);

                    dragItem = row.getTreeItem();
                    row.setOpacity(0.3);
                    event.consume();


                }
            });


            row.setOnDragOver(event -> {
                Dragboard db = event.getDragboard();
                if (db.hasContent(SERIALIZED_MIME_TYPE)) { //一定要加，否则会与拖入pdf文件冲突

                    int dragIndex = dragItem.getParent().getChildren().indexOf(dragItem); //拖拽中的行号

                    if (dragIndex != row.getIndex()) {

                        event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
                    }

                    if (lastDragRow != null) {
                        lastDragRow.setBorder(null);

                    }
                    lastDragRow = row;


//                    if(pt.getStatus() == Animation.Status.RUNNING) {
//                        pt.stop();
//                        if (!dragItem.isLeaf()) dragItem.setExpanded(false);
//                    }


                    //设置底边
                    if (event.getY() > row.getHeight() - 10 && event.getY() <= row.getHeight()) {
                        BorderStroke bs = new BorderStroke(
                                null, null, Paint.valueOf("#71C671"), null,
                                BorderStrokeStyle.SOLID, null, null, null, null,
                                new BorderWidths(0, 0, 2, 0), null);
                        Border border = new Border(bs);
                        row.setBorder(border);
                    }
                }

            });


            row.setOnDragDropped(event -> {
                Dragboard db = event.getDragboard();
                int dropIndex; //新行号
                if (db.hasContent(SERIALIZED_MIME_TYPE)) {
                    if (event.getY() > row.getHeight() - 10 && event.getY() <= row.getHeight()) {


                        int dragIndex = dragItem.getParent().getChildren().indexOf(dragItem); //拖拽中的行号
//                    TreeItem<Bookmark> item = tree.getTreeItem(dragIndex);


                        if (row.isEmpty()) {
                            dropIndex = tree.getExpandedItemCount();
                        } else {
                            dropIndex = row.getIndex();
                        }

                        int subIndex = row.getTreeItem().getParent().getChildren().indexOf(row.getTreeItem()) + 1;

                        dragItem.getParent().getChildren().remove(dragItem);
                        row.getTreeItem().getParent().getChildren().add(subIndex, dragItem);
                        tree.getSelectionModel().select(dropIndex);
                    }
                }
                event.setDropCompleted(true);
                event.consume();
                tree.refresh(); //词方法会重置透明度
            });
            dragItem = null;
            return row;
        });
    }


    //注意不要新建TreeTableView实例传递给tree，因为fxml已经有一个此控件实例并绑定到tree了，新建实例仅会导致fxml视图中的真实实例丢失指针
    void reconstructTree(Bookmark rootBookmark) {

        TreeItem<Bookmark> rootItem = new TreeItem<>(rootBookmark);

        convert(rootBookmark, rootItem);


        titleColumn.setCellValueFactory((TreeTableColumn.CellDataFeatures<Bookmark, String> p) ->
                new ReadOnlyStringWrapper(p.getValue().getValue().getTitle()));


        offsetPageColumn.setCellValueFactory((TreeTableColumn.CellDataFeatures<Bookmark, String> p) -> {
            var pageNum = p.getValue().getValue().getPageNum();
            String pageNumStr;
            pageNumStr = pageNum.map(integer -> Integer.toString(integer)).orElse("");
            return new ReadOnlyStringWrapper(pageNumStr);
        });
        rootItem.setExpanded(true);

        tree.setShowRoot(false);
        tree.setRoot(rootItem);
        tree.refresh();
    }

    //实际应用中此功能没有需求场景
    public void updatePageNum(int offset) {
        offsetPageColumn.setCellValueFactory((TreeTableColumn.CellDataFeatures<Bookmark, String> p) -> {
            var pageNum = p.getValue().getValue().getPageNum();
            String offsetPageNumStr = pageNum.map(integer -> Integer.toString(integer+offset)).orElse("");

            String offsetCellValue;
            if (offset == 0) {
                offsetCellValue = offsetPageNumStr;
            } else {
                offsetCellValue = pageNum + "  (" + offsetPageNumStr + ")";
            }
            return new ReadOnlyStringWrapper(offsetCellValue);
        });
        tree.refresh();
    }

    private void convert(Bookmark bookmark, TreeItem<Bookmark> item) {
        for (Bookmark i : bookmark.getChildren()) {
            TreeItem<Bookmark> j = new TreeItem<>(i);
            item.getChildren().add(j);
            convert(i, j);
        }
    }

    private void expandAllNodes() {
    }
}
