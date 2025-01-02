package com.ririv.quickoutline.view;

import com.ririv.quickoutline.entity.Bookmark;
import com.ririv.quickoutline.exception.BookmarkFormatException;
import com.ririv.quickoutline.service.PdfService;
import com.ririv.quickoutline.textProcess.methods.Method;
import com.ririv.quickoutline.utils.InfoUtil;
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

import java.io.*;
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
    PdfService pdfService = new PdfService();


//    public static MainController mainController;


    public void initialize() {
//        mainController = this;

        textModeController.setPdfService(this.pdfService);
        treeModeController.setPdfService(this.pdfService);

        seqRBtn.setToggleGroup(methodGroup);
        indentRBtn.setToggleGroup(methodGroup);
        seqRBtn.setUserData(Method.SEQ);
        indentRBtn.setUserData(Method.INDENT);
        seqRBtn.setSelected(true);

        offsetText.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.isEmpty() && !newValue.matches("^-?[1-9]\\d*$|^0$|^-$")) {
                if (newValue.charAt(0) == '-') {
                    newValue = newValue.substring(1);
                    newValue = newValue.replaceAll("[^0-9]", "");
                    newValue = '-'+newValue;
                }
                else newValue = newValue.replaceAll("[^0-9]", "");
                offsetText.setText(newValue);
            }
        });


        filepathText.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!oldValue.equals(newValue)) {
                resetState();
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
            Dragboard dragboard = e.getDragboard();
            File file = dragboard.getFiles().get(0);

            if (!textModeController.contentsText.getText().isEmpty() && !filepathText.getText().equals(file.getAbsolutePath())) {

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
                    String tempText = textModeController.contentsText.getText();
                    Toggle tempToggle = methodGroup.getSelectedToggle();
                    filepathText.setText(file.getPath());
                    methodGroup.selectToggle(tempToggle);
                    textModeController.contentsText.setText(tempText);
                }
            }
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
                        "正在获取文件的目录文本，文本域中含有可能未保存的内容，是否确认?",
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
                pdfService.addContents(text, srcFilePath, destFilePath, offset(), (Method) methodGroup.getSelectedToggle().getUserData());
            } catch (BookmarkFormatException e) {
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
            ButtonType openButton = new ButtonType("打开文件所在位置", ButtonBar.ButtonData.OK_DONE);
            var result = showAlert(Alert.AlertType.INFORMATION,
                    "文件存储在\n" + destFilePath, root.getScene().getWindow(),
                    openButton, new ButtonType("OK", ButtonBar.ButtonData.CANCEL_CLOSE));
            if (result.isPresent() && result.get() == openButton) {
                try {
//                    Desktop.getDesktop().browse(new File(filepathText.getText()).toURI()); //打开文件
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
                    System.out.println("Executing command: " + String.join(" ", command));
                    InputStream is = p.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                    String s;
                    while ((s = reader.readLine()) != null) {
                        System.out.println(s);
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        });
    }

    private void getCurrentState(){
    }

    //重置，并获得目录
    private void resetState(){
        offsetText.setText(null); //必须在前，否则缓存的offset会影响下面的函数
        getCurrentContents();
        if (treeTab.isSelected()) reconstructTree();
    }


    private void getCurrentContents() {
//        这里传入offset是用来相减，以获得原始页码
        String contents = pdfService.getContents(filepathText.getText(), offset());


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

        Bookmark rootBookmark = pdfService.textToBookmarkByMethod(
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
        helpStage.setScene(new Scene(helpWinRoot, 400, 300));

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
