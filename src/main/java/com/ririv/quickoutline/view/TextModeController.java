package com.ririv.quickoutline.view;

import com.ririv.quickoutline.service.PdfService;
import com.ririv.quickoutline.service.syncWithExternelEditor.SyncWithExternalEditorService;
import com.ririv.quickoutline.utils.Pair;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static com.ririv.quickoutline.view.MyAlert.showAlert;

public class TextModeController {
    public TextArea contentsText;
    public Button externalEditorBtn;
    public Button autoFormat;

    private final SyncWithExternalEditorService syncWithExternalEditorService = new SyncWithExternalEditorService();
    public StackPane shade;
    public Label label;

    public static TextModeController textModelController;
    public HBox root;
    PdfService pdfService;

    public void setPdfService(PdfService pdfService) {
        this.pdfService = pdfService;
    }

    public void initialize() {
        textModelController = this;
        label.setText("正在等待VSCode关闭文件\n期间可使用\"自动缩进\"功能");

/*
        \n没用，只好用\r了
        https://stackoverflow.com/questions/51698600/how-to-add-line-breaks-to-prompt-text-in-javafx
*/
        contentsText.setPromptText("""
                请输入目录文本，格式如下\r
                \r
                按序号：\r
                1  我是标题  1\r
                1.1  我是子标题  2\r
                1.1.1  我是子子标题 3\r
                (此方式如有缩进将会自动去除，不会影响，最终生成的PDF中标题中也会带序号)\r
                \r
                按缩进（推荐使用制表符Tab键）：\r
                我是标题  1\r
                \t我是子标题  2\r
                \t\t我是子子标题  3\r
                (此方式如有序号将会视作标题，不会影响)\r
                """);

        autoFormat.setOnAction(event -> {
            contentsText.setText(pdfService.autoFormatBySeq(contentsText.getText()));
            syncWithExternalEditorService.writeTemp(contentsText.getText());
            //自动格式化后，将方式切换为"indent",由于操作较为隐蔽，使用者不易发现变化，容易迷惑使用者，所以关闭
//            methodGroup.selectToggle(indentRBtn);
        });


        externalEditorBtn.setOnAction(event ->
                syncWithExternalEditorService.exec(

                        getCoordinate(),

                        fileText -> Platform.runLater(()-> contentsText.setText(fileText)),

                        () -> {
                            syncWithExternalEditorService.writeTemp(contentsText.getText());
                            Platform.runLater(() -> {
                                contentsText.setDisable(true);
                                externalEditorBtn.setDisable(true);
                                externalEditorBtn.setText("已连接...");
                                label.setVisible(true);
                            });
                        },

                        () -> Platform.runLater(() -> {
                            contentsText.setDisable(false);
                            externalEditorBtn.setDisable(false);
                            externalEditorBtn.setText("VSCode");
                            label.setVisible(false);

                        }),
                        () -> Platform.runLater(() -> {
                            ButtonType gotoButton = new ButtonType("前往官网下载", ButtonBar.ButtonData.OK_DONE);

                            //必须加取消类型按钮，否则对话框右上角的"×"不起作用
                            var result = showAlert(
                                    Alert.AlertType.WARNING, "未找到VSCode\n请确保安装并正确添加环境变量至PATH\n详情请查阅右上角帮助-使用说明", root.getScene().getWindow(),
                                    gotoButton, new ButtonType("OK", ButtonBar.ButtonData.CANCEL_CLOSE));
                            if (result.isPresent() && result.get() == gotoButton) {
                                Desktop desktop = Desktop.getDesktop();
                                try {
                                    URI uri = new URI("https://code.visualstudio.com/");
                                    desktop.browse(uri); //使用默认浏览器打开超链接
                                } catch (IOException | URISyntaxException e) {
                                    e.printStackTrace();
                                }
                            }
                        })
                )
        );
    }


    private Pair<Integer,Integer> getCoordinate() {

        int x = 0;
        int y = 0;
        int pos = contentsText.getCaretPosition() + 1;
//        System.out.println("The caret post is"+pos);

        for (String line : contentsText.getText().split("\n")) {
            x++;
            y = 0;

            for (int l = line.length(); l >= 0 && pos > 0; pos--, l--) {
                y++;
            }
            if (pos == 0) break;
        }

        return new Pair<Integer,Integer> (x, y);
    }

    //暂时有bug
    public int getPosition(int x,int y) {
        y += 1;
        int pos = 0;

        for (String line : contentsText.getText().split("\n")) {
            y--;
            pos += line.length();
            if (y <= 0) break;
        }
        return pos;
    }

}
