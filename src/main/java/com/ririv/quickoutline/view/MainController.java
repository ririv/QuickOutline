package com.ririv.quickoutline.view;

import com.ririv.quickoutline.exception.BookmarkFormatException;
import com.ririv.quickoutline.exception.EncryptedPdfException;
import com.ririv.quickoutline.exception.NoOutlineException;
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

    public TextField filepathTF;
    public Button browseFileBtn;

    public Button getContentsBtn;
    public Button setContentsBtn;
    public TextField offsetTF;
    public Button deleteBtn;

    public RadioButton seqRBtn;
    public RadioButton indentRBtn;
    public ToggleGroup methodToggleGroup;

    public StackPane root;

    public PdfViewScaleType viewScaleType = PdfViewScaleType.None;


    //必须映射到textModeController，否则会无法报错
    //与下面的textMode区分，其映射的是<fx:include>绑定的控件
    public TextModeController textTabViewController;
    public TreeModeController treeTabViewController;
//    public TreeWebVIewController treeModeController;



    public ToggleButton textTabBtn;
    public ToggleButton treeTabBtn;
    public ToggleGroup tabToggleGroup;
    public Button helpBtn;

    public ToggleButton tocBtn;

    public MessageContainer messageManager;

    PdfService pdfService = new PdfService();

    @FXML
    private Node textTabView;  // <fx:include> 实际上对应的HBox类型

    @FXML
    private Node treeTabView;


    public enum FnTab{
        text, tree, setting
    }

    private FnTab currenTab;

    private Stage helpStage;


    public void switchTab(FnTab targetTab) {
        if (targetTab == FnTab.text) {
            textTabView.setVisible(true);
            treeTabView.setVisible(false);

        } else if (targetTab == FnTab.tree) {
            treeTabView.setVisible(true);
            textTabView.setVisible(false);
            reconstructTree();
        }
        currenTab = targetTab;
    }



    public void initialize() {

        textTabViewController.setPdfService(this.pdfService);
        treeTabViewController.setPdfService(this.pdfService);

        seqRBtn.setUserData(Method.SEQ);
        indentRBtn.setUserData(Method.INDENT);
        seqRBtn.setSelected(true);

        textTabBtn.setSelected(true);
        currenTab = FnTab.text;


        offsetTF.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.isEmpty() && !newValue.matches("^-?[1-9]\\d*$|^0$|^-$")) {
                if (newValue.charAt(0) == '-') {
                    newValue = newValue.substring(1);
                    newValue = newValue.replaceAll("[^0-9]", "");
                    newValue = '-'+newValue;
                }
                else newValue = newValue.replaceAll("[^0-9]", "");
                offsetTF.setText(newValue);
            }
        });

//        拖拽文件，必须下面两个方法配合完成
//        鼠标悬浮在结点时，会不停执行
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
            openFile(file);
        });


        browseFileBtn.setOnAction(event -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF 文档", "*.pdf"));
            File file = fileChooser.showOpenDialog(null);
            openFile(file);
        });

        tabToggleGroup.selectedToggleProperty().addListener((event,oldValue,newValue) -> {
            // 保持选中状态，防止取消选中
            if (newValue == null){
                switch (currenTab){
                    case text -> tabToggleGroup.selectToggle(textTabBtn);
                    case tree -> tabToggleGroup.selectToggle(treeTabBtn);
                }
            }
            if (textTabBtn.isSelected()) {
                switchTab(FnTab.text);
            } else if (treeTabBtn.isSelected()) {
                switchTab(FnTab.tree);
            }
        });

        methodToggleGroup.selectedToggleProperty().addListener(event -> {
            if (currenTab == FnTab.tree) reconstructTree();
        });


        GetContentsPopupView getContentsPopupView = new GetContentsPopupView(this);
        getContentsPopupView.filepathProperty().bind(filepathTF.textProperty());
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
            if (!textTabViewController.contentsTextArea.getText().isEmpty()) {
                Optional<ButtonType> buttonType = showAlert(
                        Alert.AlertType.CONFIRMATION,
                        "正在获取文件的目录文本，文本域中含有可能未保存的内容，是否确认?",
                        root.getScene().getWindow());
                if (buttonType.isPresent() && buttonType.get().getButtonData().isCancelButton()) {
                    return;
                }
            }

            if (filepathTF.getText().isEmpty()) {
                messageManager.showMessage("请选择PDF文件", Message.MessageType.WARNING);
                return;
            }

            getContents();
            if (currenTab == FnTab.tree) reconstructTree();
    }

    private String destFilePath(){
        String srcFilePath = filepathTF.getText();
        srcFilePath = srcFilePath.replaceAll("\\\\", "/");
        String srcFileName = srcFilePath.substring(srcFilePath.lastIndexOf("/") + 1);
        String ext = srcFileName.substring(srcFileName.lastIndexOf("."));
        return srcFilePath.substring(0, srcFilePath.lastIndexOf(srcFileName)) + srcFileName.substring(0, srcFileName.lastIndexOf(".")) + "_含目录" + ext;
    }



    private void openFile(File file){
        if (file == null) return;

        String newFilePath = file.getPath();
        String oldFilePath = filepathTF.getText();
        if (newFilePath.equals(oldFilePath)) return;

        try {
            pdfService.checkOpenFile(newFilePath);
        } catch (IOException e) {
            messageManager.showMessage("无法打开文档\n"+e.getMessage(), Message.MessageType.ERROR);
            return;
        } catch (EncryptedPdfException e) {
            messageManager.showMessage("该文档已加密", Message.MessageType.WARNING);
        } catch (com.itextpdf.io.exceptions.IOException e){
            e.printStackTrace();
            logger.info(String.valueOf(e));
            messageManager.showMessage("文档可能已损坏\n"+e.getMessage(), Message.MessageType.ERROR);
            return;
        }


        if (!textTabViewController.contentsTextArea.getText().isEmpty()) {
            ButtonType keepContentsTextBtnType = new ButtonType("保留", ButtonBar.ButtonData.OK_DONE);
            ButtonType noKeepContentsTextBtnType = new ButtonType("否", ButtonBar.ButtonData.OK_DONE);
            ButtonType cancelBtnType = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
            Optional<ButtonType> result = showAlert(
                    Alert.AlertType.CONFIRMATION,
                    "正在打开新文件，是否保留文本域中的内容?",
                    root.getScene().getWindow(),
                    keepContentsTextBtnType,noKeepContentsTextBtnType,cancelBtnType);

            if (result.isPresent() && result.get() == cancelBtnType) {
                return;
            } else {
                filepathTF.setText(newFilePath);
                resetState(result.isPresent() && result.get() == keepContentsTextBtnType);
            }
        } else {
            filepathTF.setText(newFilePath);
            resetState(false);
        }

    }


    @FXML
    private void setContentsBtnAction(ActionEvent event) {
            if (filepathTF.getText().isEmpty()) {
                messageManager.showMessage("请选择PDF文件", Message.MessageType.WARNING);
                return;
            }

            String text = textTabViewController.contentsTextArea.getText();

            if (text == null || text.isEmpty()) {
                return;
            }
            String srcFilePath = filepathTF.getText();
            String destFilePath = destFilePath();
            try {
                pdfService.setContents(text, srcFilePath, destFilePath, offset(),
                        (Method) methodToggleGroup.getSelectedToggle().getUserData(),
                        viewScaleType);
            } catch (BookmarkFormatException e) {
                e.printStackTrace();
                File file = new File(destFilePath);
                boolean deleteSuccess = file.delete();  //删除损坏的文件
                logger.info("删除文件成功: {}", deleteSuccess);
                messageManager.showMessage(e.getMessage(), Message.MessageType.ERROR);
                return;
            } catch (IOException e) {
//                e.printStackTrace();
                messageManager.showMessage(e.getMessage(), Message.MessageType.ERROR);
                return;
            } catch (EncryptedPdfException e) {
                messageManager.showMessage("该文档已加密，请先解密", Message.MessageType.ERROR);
                return;
            }

        showSuccessDialog();
    }

    private void showSuccessDialog() {
        ButtonType openDirAndSelectFileButtonType = new ButtonType("打开文件所在位置", ButtonBar.ButtonData.OK_DONE);
        ButtonType openFileButtonType = new ButtonType("打开文件", ButtonBar.ButtonData.OK_DONE);
        var result = showAlert(Alert.AlertType.INFORMATION,
                "文件存储在\n" + destFilePath(), root.getScene().getWindow(),
                openDirAndSelectFileButtonType, openFileButtonType, new ButtonType("OK", ButtonBar.ButtonData.CANCEL_CLOSE));
        try {
            String destFilePath = destFilePath();
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
                Desktop.getDesktop().browse(new File(destFilePath()).toURI()); //打开文件
            }
        } catch (IOException e) {
        e.printStackTrace();
    }
    }

    //重置，并获得目录
    private void resetState(boolean keepContents){
        offsetTF.setText(null); //必须在前，否则缓存的offset会影响下面的函数
        if (!keepContents){
            getContents();
            if (currenTab == FnTab.tree) reconstructTree();
        }
    }


    private void getContents() {
//        这里原本传入offset是用来相减，原本该功能未获取目录，而不是重置目录
//        因为比较迷惑，现在不再支持，设为0
        try {
            String contents = pdfService.getContents(filepathTF.getText(), 0);
            textTabViewController.contentsTextArea.setText(contents);
        } catch (NoOutlineException e) {
            e.printStackTrace();
            messageManager.showMessage("该文档没有书签目录", Message.MessageType.WARNING);
        }
    }

    public int offset() {
        String offsetText = this.offsetTF.getText();

        try {
            return Integer.parseInt(offsetText != null && !offsetText.isEmpty() && !offsetText.equals("-") ? offsetText : "0");
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public void reconstructTree() {
        Bookmark rootBookmark = pdfService.convertTextToBookmarkTreeByMethod(
                textTabViewController.contentsTextArea.getText(), 0,
                (Method) methodToggleGroup.getSelectedToggle().getUserData()
        );
        treeTabViewController.reconstructTree(rootBookmark);
    }

    public void deleteBtnAction(ActionEvent event) {
        if (filepathTF.getText().isEmpty()) {
            messageManager.showMessage("请选择PDF文件", Message.MessageType.WARNING);
            return;
        }
        pdfService.deleteContents(filepathTF.getText(), destFilePath());
        showSuccessDialog();
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
