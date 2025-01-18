package com.ririv.quickoutline.view;

import com.ririv.quickoutline.exception.BookmarkFormatException;
import com.ririv.quickoutline.model.Bookmark;
import com.ririv.quickoutline.pdfProcess.PdfViewScaleType;
import com.ririv.quickoutline.service.PdfService;
import com.ririv.quickoutline.textProcess.methods.Method;
import com.ririv.quickoutline.utils.InfoUtil;
import com.ririv.quickoutline.view.controls.Message;
import com.ririv.quickoutline.view.controls.MessageContainer;
import com.ririv.quickoutline.view.controls.PopupCard;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;

import java.awt.*;
import java.io.*;
import java.util.Objects;
import java.util.Optional;

import static com.ririv.quickoutline.view.MyAlert.showAlert;

public class MainController {

    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(MainController.class);

    public TextField filepathText;
    public Button browseFileBtn;


    public Button getContentsBtn;

    public Button setContentsBtn;
    public TextField offsetTextField;

    public RadioButton seqRBtn;
    public RadioButton indentRBtn;
    public ToggleGroup methodToggleGroup;

    public StackPane root;

    public PdfViewScaleType viewScaleType = PdfViewScaleType.None;


    //必须映射到textModeController，否则会无法报错
    //与下面的textMode区分，其映射的是<fx:include>绑定的控件
    public TextModeController textModeController;
    public TreeModeController treeModeController;
//    public TreeWebVIewController treeModeController;


    public Button helpBtn;
    public HBox topPane;
    public ToggleButton textModeBtn;
    public ToggleButton treeModeBtn;
    public ToggleGroup tabToggleGroup;
    public ToggleButton tocBtn;
    public Button deleteBtn;
    public MessageContainer messageDialog;
//    public ChoiceBox<String> scaleChoice;
//    public ObservableList<String> scaleOptions = FXCollections.observableArrayList("适合宽度", "适合高度", "适合页面");


    PdfService pdfService = new PdfService();

    @FXML
    private Node textMode;  // <fx:include> 实际上对应的HBox类型

    @FXML
    private Node treeMode;



    public enum FnTab{
        text, tree, setting
    }

    private FnTab currenTab;

    private Stage helpStage;

    private String srcFilePath;

    private String destFilePath;


    public void switchTab(FnTab targetTab) {
        if (targetTab == FnTab.text) {
            textMode.setVisible(true);
            treeMode.setVisible(false);

        } else if (targetTab == FnTab.tree) {
            treeMode.setVisible(true);
            textMode.setVisible(false);
            reconstructTree();
        }
        currenTab = targetTab;
    }



    public void initialize() {
//        scaleChoice.setItems(scaleOptions);
//        scaleChoice.setValue(scaleOptions.getFirst());

        textModeController.setPdfService(this.pdfService);
        treeModeController.setPdfService(this.pdfService);

        seqRBtn.setUserData(Method.SEQ);
        indentRBtn.setUserData(Method.INDENT);
        seqRBtn.setSelected(true);

        textModeBtn.setSelected(true);
        currenTab = FnTab.text;


        offsetTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.isEmpty() && !newValue.matches("^-?[1-9]\\d*$|^0$|^-$")) {
                if (newValue.charAt(0) == '-') {
                    newValue = newValue.substring(1);
                    newValue = newValue.replaceAll("[^0-9]", "");
                    newValue = '-'+newValue;
                }
                else newValue = newValue.replaceAll("[^0-9]", "");
                offsetTextField.setText(newValue);
            }
        });


        filepathText.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!oldValue.equals(newValue)) {
                resetState();
            }
            srcFilePath = newValue.replaceAll("\\\\", "/");
            String srcFileName = srcFilePath.substring(srcFilePath.lastIndexOf("/") + 1);
            String ext = srcFileName.substring(srcFileName.lastIndexOf("."));
            destFilePath = srcFilePath.substring(0, srcFilePath.lastIndexOf(srcFileName)) + srcFileName.substring(0, srcFileName.lastIndexOf(".")) + "_含目录" + ext;

        });

        //拖拽文件，必须下面两个方法配合完成
        //鼠标悬浮在结点时，会不停执行
        root.setOnDragOver(event -> {
            String pdfFormatPattern = ".+\\.[pP][dD][fF]$";
            Dragboard dragboard = event.getDragboard();
            if (dragboard.hasFiles()) {
                File file = dragboard.getFiles().getFirst();
                if (file.getName().matches(pdfFormatPattern)) { //用来过滤拖入类型
                    event.acceptTransferModes(TransferMode.LINK);//用于接受文件和设定以何种方式接受，必须有，否则接受不了
                }
            }
        });

        //鼠标送开时，会执行且只执行一遍
        root.setOnDragDropped(e -> {
            Dragboard dragboard = e.getDragboard();
            File file = dragboard.getFiles().getFirst();

            if (!textModeController.contentsTextArea.getText().isEmpty() && !filepathText.getText().equals(file.getAbsolutePath())) {

                ButtonType keepContentsText = new ButtonType("保留", ButtonBar.ButtonData.OK_DONE);
                ButtonType noKeepContentsText = new ButtonType("否", ButtonBar.ButtonData.OK_DONE);
                Optional<ButtonType> result = showAlert(
                        Alert.AlertType.CONFIRMATION,
                        "正在打开新文件，是否保留文本域中的内容?",
                        root.getScene().getWindow(),
                        keepContentsText,noKeepContentsText,new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE));
                if (result.isPresent() && result.get().getButtonData().isCancelButton()) {
                    return;
                }
                else if (result.isPresent() && result.get() == keepContentsText) {
                    String tempText = textModeController.contentsTextArea.getText();
                    Toggle tempToggle = methodToggleGroup.getSelectedToggle();
                    filepathText.setText(file.getPath());
                    methodToggleGroup.selectToggle(tempToggle);
                    textModeController.contentsTextArea.setText(tempText);
                }
            }
            filepathText.setText(file.getPath());

        });


        browseFileBtn.setOnAction(event -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF 文档", "*.pdf"));
            File file = fileChooser.showOpenDialog(null);
            if (file != null) {
                filepathText.setText(file.getPath());
            }
        });


        tabToggleGroup.selectedToggleProperty().addListener((event,oldValue,newValue) -> {
            // 保持选中状态，防止取消选中
            if (newValue == null){
                switch (currenTab){
                    case text -> tabToggleGroup.selectToggle(textModeBtn);
                    case tree -> tabToggleGroup.selectToggle(treeModeBtn);
                }
            }
            if (textModeBtn.isSelected()) {
                switchTab(FnTab.text);
            } else if (treeModeBtn.isSelected()) {
                switchTab(FnTab.tree);
            }
        });

        methodToggleGroup.selectedToggleProperty().addListener(event -> {
            if (currenTab == FnTab.tree) reconstructTree();
        });


        GetContentsPopupView getContentsPopupView = new GetContentsPopupView(this);
        getContentsPopupView.filepathProperty().bind(filepathText.textProperty());
        getContentsPopupView.setPrefHeight(120);
        getContentsPopupView.setPrefWidth(240);


        PopupCard popup1 = new PopupCard(getContentsPopupView);
        getContentsBtn.addEventHandler(MouseEvent.MOUSE_ENTERED, popup1::showEventHandler);

        SetContentsPopupView setContentsPopupView = new SetContentsPopupView(this);
        PopupCard popup2 = new PopupCard(setContentsPopupView);
        setContentsBtn.addEventHandler(MouseEvent.MOUSE_ENTERED, popup2::showEventHandler);
    }

    @FXML
    private void getContentsBtnAction(ActionEvent event) {
            if (!textModeController.contentsTextArea.getText().isEmpty()) {
                Optional<ButtonType> buttonType = showAlert(
                        Alert.AlertType.CONFIRMATION,
                        "正在获取文件的目录文本，文本域中含有可能未保存的内容，是否确认?",
                        root.getScene().getWindow());
                if (buttonType.isPresent() && buttonType.get().getButtonData().isCancelButton()) {
                    return;
                }
            }

            if (filepathText.getText().isEmpty()) {
                messageDialog.showMessage("请选择PDF文件", Message.MessageType.WARNING);
                return;
            }

            getContents();
            if (currenTab == FnTab.tree) reconstructTree();

    }




    @FXML
    private void setContentsBtnAction(ActionEvent event) {
            if (filepathText.getText().isEmpty()) {
                messageDialog.showMessage("请选择PDF文件", Message.MessageType.WARNING);
                return;
            }

            String text = textModeController.contentsTextArea.getText();

            if (text == null || text.isEmpty()) {
                return;
            }

            try {

                pdfService.setContents(text, srcFilePath, destFilePath, offset(),
                        (Method) methodToggleGroup.getSelectedToggle().getUserData(),
                        viewScaleType);
            } catch (BookmarkFormatException e) {
                e.printStackTrace();
                File file = new File(destFilePath);
                boolean deleteSuccess = file.delete();  //删除损坏的文件
                logger.info("删除文件成功: {}", deleteSuccess);
//                ButtonType buttonType = new ButtonType("定位");
//                messageDialog.showMessage(e.getMessage(), Message.MessageType.WARN);
                var result = showAlert(Alert.AlertType.ERROR, e.getMessage(), root.getScene().getWindow());
//                if(result.isPresent()&&result.get()==buttonType){
//                    textModeController.contentsText.requestFocus(); //获得焦点
//                    textModeController.contentsText.positionCaret(
//                            textModeController.getPosition(0,e.getIndex()));
//                }
                return;
            }

        showSuccessWindow();
    }

    private void showSuccessWindow() {
        ButtonType openDirAndSelectFileButtonType = new ButtonType("打开文件所在位置", ButtonBar.ButtonData.OK_DONE);
        ButtonType openFileButtonType = new ButtonType("打开文件", ButtonBar.ButtonData.OK_DONE);
        var result = showAlert(Alert.AlertType.INFORMATION,
                "文件存储在\n" + destFilePath, root.getScene().getWindow(),
                openDirAndSelectFileButtonType, openFileButtonType, new ButtonType("OK", ButtonBar.ButtonData.CANCEL_CLOSE));
        try {
            if (result.isPresent() && result.get() == openDirAndSelectFileButtonType) {

                //打开文件所在文件夹并选择文件
                String[] command;
                if (InfoUtil.isWindows()) {
                    destFilePath = destFilePath.replaceAll("/", "\\\\");
                    //实测该命令会在文件路径下因为检测到参数包含空格而未被引号包裹，于是主动加了双引号，形成的命令在powershell的确是有效的，但在cmd中无效
                    //但默认是用cmd执行的,导致非预期结果
                    //参考 ProcessImpl.java下 createCommandLine 和 needsEscaping 源码
//                        command = new String[]{"explorer.exe", "/select,\"%s\"".formatted(destFilePath)};
                    // 故采用如下方式执行
                    command = new String[]{"cmd.exe", "/C", "explorer.exe /select,\"%s\"".formatted(destFilePath)};
                }
                else if (InfoUtil.isMacOS()) {
                    command = new String[]{"open", "-R", destFilePath}; //macos
                }
                else {
                    command = new String[]{"nautilus", destFilePath}; // 打开文件可以使用 xdg-open
                }
                Process p = Runtime.getRuntime().exec(command);

                logger.info("Executing command: {}", String.join(" ", command));

                InputStream is = p.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                String s;
                while ((s = reader.readLine()) != null) {
                    logger.info("执行命令外部输出: {}", s);
                }
            } else if (result.isPresent() && result.get().equals(openFileButtonType)) {
                Desktop.getDesktop().browse(new File(destFilePath).toURI()); //打开文件
            }
        } catch (IOException e) {
        e.printStackTrace();
    }
    }

    //重置，并获得目录
    private void resetState(){
        offsetTextField.setText(null); //必须在前，否则缓存的offset会影响下面的函数
        getContents();
        if (currenTab == FnTab.tree) reconstructTree();
    }


    private void getContents() {
//        这里原本传入offset是用来相减，原本该功能未获取目录，而不是重置目录
//        因为比较迷惑，现在不再支持，设为0
        String contents = pdfService.getContents(filepathText.getText(), 0);
        textModeController.contentsTextArea.setText(contents);
    }

    public int offset() {
        String offsetText = this.offsetTextField.getText();

        try {
            return Integer.parseInt(offsetText != null && !offsetText.isEmpty() && !offsetText.equals("-") ? offsetText : "0");
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public void reconstructTree() {
        Bookmark rootBookmark = pdfService.convertTextToBookmarkTreeByMethod(
                textModeController.contentsTextArea.getText(), 0,
                (Method) methodToggleGroup.getSelectedToggle().getUserData()
        );
        treeModeController.reconstructTree(rootBookmark);
    }

    public void deleteBtnAction(ActionEvent event) {
        if (filepathText.getText().isEmpty()) {
            messageDialog.showMessage("请选择PDF文件", Message.MessageType.WARNING);
            return;
        }
        pdfService.deleteContents(srcFilePath, destFilePath);
        showSuccessWindow();
    }

    @FXML
    public void createHelpWindowAction(ActionEvent actionEvent) throws IOException {
        if (helpStage == null) {
            helpStage = new Stage();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("HelpWindow.fxml"));
            Parent helpWinRoot = loader.load();

            helpStage.setTitle("帮助");
            helpStage.setScene(new Scene(helpWinRoot, 400, 300));

            helpStage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("icon/help_black.png"))));
            helpStage.setResizable(false); //不可调整大小，且使最大化不可用
            helpStage.initOwner(root.getScene().getWindow());//可以使最小化不可用，配合上一条语句，可以使最小化最大化隐藏，只留下"×"
            helpStage.show();
//            helpStage.focusedProperty().addListener((observable, oldValue, newValue) -> {
//                if (!observable.getValue()) {
//                    helpStage.hide();
//                    ((Stage) root.getScene().getWindow()).toFront();
//                }
//            });
        }
        else {
            // 如果窗口已经创建但被隐藏，重新显示
            // 如果窗口已存在，将其聚焦到前台
            helpStage.show();
//            helpStage.toFront();
            helpStage.requestFocus();
        }
    }

}
