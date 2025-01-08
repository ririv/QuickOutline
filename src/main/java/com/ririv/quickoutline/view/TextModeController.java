package com.ririv.quickoutline.view;

import com.ririv.quickoutline.service.PdfService;
import com.ririv.quickoutline.service.syncWithExternelEditor.SyncWithExternalEditorService;
import com.ririv.quickoutline.utils.Pair;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Pattern;

import static com.ririv.quickoutline.view.MyAlert.showAlert;

public class TextModeController {
    public TextArea contentsTextArea;
    public Button externalEditorBtn;
    public Button autoFormatBtn;

    private final SyncWithExternalEditorService syncWithExternalEditorService = new SyncWithExternalEditorService();
    public Label mask;
//    public ScalePane

    public static TextModeController textModelController;
    public HBox root;
    PdfService pdfService;

    // 定义正则表达式表示一个缩进（默认一个制表符或4个空格）
    private static final Pattern INDENT_PATTERN = Pattern.compile("^(\\t|\\s{1,4})");


    public void setPdfService(PdfService pdfService) {
        this.pdfService = pdfService;
    }

    public void initialize() {
        textModelController = this;
        mask.setText("正在等待VSCode关闭文件\n期间可使用\"自动缩进\"功能");

/*
        \n没用，只好用\r了
        https://stackoverflow.com/questions/51698600/how-to-add-line-breaks-to-prompt-text-in-javafx
*/
        contentsTextArea.setPromptText("""
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

        // 设置处理键盘事件
        // 按下SHIFT+TAB将自动去掉一格缩进（\t或者4个空格）
        contentsTextArea.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            // 检查是否是 Shift+Tab
            if (event.getCode() == KeyCode.TAB && event.isShiftDown()) {
                // 获取当前光标所在行
                int caretPosition = contentsTextArea.getCaretPosition();
                int lineNumber = getLineNumber(contentsTextArea, caretPosition);

                // 获取光标所在行的文本
                String lineText = contentsTextArea.getText().split("\n")[lineNumber - 1];
                // 如果该行没有缩进，则不需要处理，直接返回
                if (INDENT_PATTERN.matcher(lineText).find()) {
                    // 去除行首的缩进（制表符或4个空格）
                    String trimmedLine = lineText.replaceFirst(INDENT_PATTERN.pattern(), "");
                    // 替换当前行
                    StringBuilder newText = new StringBuilder(contentsTextArea.getText());
                    newText.replace(contentsTextArea.getText().indexOf(lineText), contentsTextArea.getText().indexOf(lineText) + lineText.length(), trimmedLine);
                    // 设置修改后的文本
                    contentsTextArea.setText(newText.toString());

                    // 设置光标位置
                    contentsTextArea.positionCaret(caretPosition - 1);
                }

                event.consume(); // 消耗事件，防止默认行为
            }
        });


        autoFormatBtn.setOnAction(event -> {
            contentsTextArea.setText(pdfService.autoFormat(contentsTextArea.getText()));
            syncWithExternalEditorService.writeTemp(contentsTextArea.getText());
            //自动格式化后，将方式切换为"indent",由于操作较为隐蔽，使用者不易发现变化，容易迷惑使用者，所以关闭
//            methodGroup.selectToggle(indentRBtn);
        });


        externalEditorBtn.setOnAction(event ->
                syncWithExternalEditorService.exec(

                        getCoordinate(),

                        fileText -> Platform.runLater(()-> contentsTextArea.setText(fileText)),

                        () -> {
                            syncWithExternalEditorService.writeTemp(contentsTextArea.getText());
                            Platform.runLater(() -> {
                                contentsTextArea.setDisable(true);
                                externalEditorBtn.setDisable(true);
                                externalEditorBtn.setText("已连接...");
//                                externalEditorBtn.setGraphic();
                                mask.setVisible(true);
                            });
                        },

                        () -> Platform.runLater(() -> {
                            contentsTextArea.setDisable(false);
                            externalEditorBtn.setDisable(false);
                            externalEditorBtn.setText("VSCode");
                            mask.setVisible(false);

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


    // 行号计算
    // 使用 String.chars() 方法统计光标之前文本中 \n 的数量。因为换行符的数量等于行号减一，因此需要加 1。
    private int getLineNumber(TextArea textArea, int caretPosition) {
        String textBeforeCaret = textArea.getText(0, caretPosition);
        return (int) textBeforeCaret.chars().filter(ch -> ch == '\n').count() + 1;
    }

    // 列号计算
    // 查找光标之前最近一次出现 \n 的位置，计算从该位置到光标的字符数即可。
    private int getColumnNumber(TextArea textArea, int caretPosition) {
        String textBeforeCaret = textArea.getText(0, caretPosition);
        int lastNewLineIndex = textBeforeCaret.lastIndexOf('\n');
        return caretPosition - (lastNewLineIndex + 1) + 1;
    }

    private Pair<Integer,Integer> getCoordinate() {
        int pos = contentsTextArea.getCaretPosition();
        int x = getLineNumber(contentsTextArea, pos);
        int y = getColumnNumber(contentsTextArea, pos);
        return new Pair<> (x,y);
    }

}
