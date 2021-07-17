package com.ririv.quickoutline.view;

import com.ririv.quickoutline.exception.BookmarkFormatException;
import com.ririv.quickoutline.entity.Bookmark;
import com.ririv.quickoutline.enums.Method;
import com.ririv.quickoutline.service.PdfService;
import com.ririv.quickoutline.utils.OsTypeUtil;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

import static com.ririv.quickoutline.view.MyAlert.showAlert;

public class MainController {

    public TextField filepathText;
    public Button browseFileBtn;


    public Button getCurrentContentsBtn;

    public Button addContentsBtn;
    public TextField offsetText;

    public RadioButton seqRBtn;
    public RadioButton indentRBtn;
    public ToggleGroup methodGroup;

    public AnchorPane root;

    public Tab treeTab;

    //fx:id="textMode"必须映射到textModeController，否则会报错
    @FXML
    public TextModeController textModeController;

    @FXML
    public TreeModeController treeModeController;
//    public TreeWebVIewController treeModeController;

    public AnchorPane shade;
    public Button helpBtn;


//    public static MainController mainController;


    public void initialize() {
//        mainController = this;


        seqRBtn.setToggleGroup(methodGroup);
        indentRBtn.setToggleGroup(methodGroup);
        seqRBtn.setUserData(Method.SEQ);
        indentRBtn.setUserData(Method.INDENT);
        seqRBtn.setSelected(true);

        offsetText.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.matches("\\d*")) offsetText.setText(newValue.replaceAll("[^\\d]", ""));
        });

//        pageNumOffset.focusedProperty().addListener((observable, oldValue, newValue) -> {
//
//                if (!observable.getValue()) {
//                String offset = pageNumOffset.getText();
//                if (offset != null && offset.length() > 0 && !offset.matches("^[0-9]+$")) {
//                    showAlert("错误", "页码偏移量格式错误", "页码偏移量只能为0或正整数", Alert.AlertType.ERROR);
//                }
//            }
//        });


        filepathText.textProperty().addListener((observable, oldValue, newValue) -> {
            //重置操作
            if (!oldValue.equals(newValue)) {
                offsetText.setText(null); //必须在前，否则缓存的offset会影响下面的函数
                getCurrentContents();
                if (treeTab.isSelected()) reconstructTree();
            }
        });

        //拖拽文件，必须下面两个方法配合完成
        //鼠标悬浮在结点时，会不停执行
        root.setOnDragOver(event -> {
            String pdfFormatPattern = ".+\\.[pP][dD][fF]$";
            Dragboard dragboard = event.getDragboard();
            if (dragboard.hasFiles()) {
                File file = dragboard.getFiles().get(0);
                if (file.getName().matches(pdfFormatPattern)) { //用来过滤拖入类型
                    event.acceptTransferModes(TransferMode.LINK);//用于接受文件和设定以何种方式接受，必须有，否则接受不了
                }
            }
        });

        //鼠标送开时，会执行且只执行一遍
        root.setOnDragDropped(e -> {
            if (!textModeController.contentsText.getText().isEmpty()) {
                Optional<ButtonType> buttonType = showAlert(
                        Alert.AlertType.CONFIRMATION,
                        "文本域中含有可能未保存的内容，是否确认?",
                        root.getScene().getWindow());
                if (buttonType.isPresent() && buttonType.get().getButtonData().isCancelButton()) {
                    return;
                }

            }

            Dragboard dragboard = e.getDragboard();
            File file = dragboard.getFiles().get(0);
            filepathText.setText(file.getPath());

        });


        browseFileBtn.setOnAction(event -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("pdf", "*.pdf"));
            File file = fileChooser.showOpenDialog(null);
            if (file != null) {
                filepathText.setText(file.getPath());
            }
        });

        getCurrentContentsBtn.setOnAction(event -> {
            if (!textModeController.contentsText.getText().isEmpty()) {
                Optional<ButtonType> buttonType = showAlert(
                        Alert.AlertType.CONFIRMATION,
                        "文本域中含有可能未保存的内容，是否确认?",
                        root.getScene().getWindow());
                if (buttonType.isPresent() && buttonType.get().getButtonData().isCancelButton()) {
                    return;
                }
            }

            if (filepathText.getText().isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "请选择pdf文件", root.getScene().getWindow());
                return;
            }

            getCurrentContents();
            if (treeTab.isSelected()) reconstructTree();

        });

        treeTab.setOnSelectionChanged(event -> {
            if (treeTab.isSelected()) reconstructTree();
        });
        methodGroup.selectedToggleProperty().addListener(event -> {
            if (treeTab.isSelected()) reconstructTree();

        });

        addContentsBtn.setOnAction(event -> {
            String fp = filepathText.getText();
            if (fp.isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "请选择pdf文件", root.getScene().getWindow());
                return;
            }

            String srcFilePath = fp.replaceAll("\\\\", "/");
            String srcFileName = srcFilePath.substring(srcFilePath.lastIndexOf("/") + 1);
            String ext = srcFileName.substring(srcFileName.lastIndexOf("."));
            String destFilePath = srcFilePath.substring(0, srcFilePath.lastIndexOf(srcFileName)) + srcFileName.substring(0, srcFileName.lastIndexOf(".")) + "_含目录" + ext;


            String text = textModeController.contentsText.getText();

            if (text == null || text.isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "PDF目录内容不能为空", root.getScene().getWindow());
                return;
            }

            try {
                PdfService.addContents(text, srcFilePath, destFilePath, offset(), (Method) methodGroup.getSelectedToggle().getUserData());
            } catch ( BookmarkFormatException e) {
                e.printStackTrace();
                File file = new File(destFilePath);
                boolean deleteSuccess = file.delete();  //删除损坏的文件
                System.out.println(deleteSuccess);
//                ButtonType buttonType = new ButtonType("定位");
                var result = showAlert(Alert.AlertType.ERROR, e.getMessage(), root.getScene().getWindow());
//                if(result.isPresent()&&result.get()==buttonType){
//                    textModeController.contentsText.requestFocus(); //获得焦点
//                    textModeController.contentsText.positionCaret(
//                            textModeController.getPosition(0,e.getIndex()));
//                }
                return;
            }
            ButtonType openButton = new ButtonType("打开文件所在位置",ButtonBar.ButtonData.OK_DONE);
            var result = showAlert(Alert.AlertType.INFORMATION,
                    "文件存储在\n" + destFilePath, root.getScene().getWindow(),
                    openButton,new ButtonType("OK", ButtonBar.ButtonData.CANCEL_CLOSE));
            if (result.isPresent()&&result.get()==openButton) {
                try {
//                    Desktop.getDesktop().browse(new File(filepathText.getText()).toURI()); //打开文件
                    //打开文件所在文件夹并选择文件
                    if (OsTypeUtil.isWindows()) Runtime.getRuntime().exec(
                            "explorer.exe /select, " + destFilePath.replaceAll("/", "\\\\")); //windows
                    else if (OsTypeUtil.isMacOS()) Runtime.getRuntime().exec("open -R " + destFilePath); //macos

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        });
    }


    private void getCurrentContents() {
//        这里传入offset是用来相减，以获得原始页码
        String contents = PdfService.getContents(filepathText.getText(), offset());


        textModeController.contentsText.setText(contents);

    }

    public int offset() {
        String offsetText = this.offsetText.getText();

        try {
            return Integer.parseInt(offsetText != null && !offsetText.isEmpty() ? offsetText : "0");
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public void reconstructTree() {
        if (textModeController.contentsText.getText().isEmpty()) return;

        Bookmark rootBookmark = PdfService.textToBookmarkByMethod(
                textModeController.contentsText.getText(), 0,
                (Method) methodGroup.getSelectedToggle().getUserData()
//                ,true
        );

        treeModeController.reconstructTree(rootBookmark);
    }

    @FXML
    public void createHelpWindow(ActionEvent actionEvent) throws IOException {
        final Stage helpStage = new Stage();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("HelpWindow.fxml"));
        Parent helpWinRoot = loader.load();

        helpStage.setTitle("帮助");
        helpStage.setScene(new Scene(helpWinRoot,400,300));

        helpStage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("icon/help_black.png"))));
        helpStage.setResizable(false); //不可调整大小，且使最大化不可用
        helpStage.initOwner(root.getScene().getWindow());//可以使最小化不可用，配合上一条语句，可以使最小化最大化隐藏，只留下"×"
        helpStage.show();
        helpStage.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!observable.getValue()) {
                helpStage.close();
            }
        });
    }

}
