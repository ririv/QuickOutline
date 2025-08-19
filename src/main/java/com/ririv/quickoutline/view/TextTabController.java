package com.ririv.quickoutline.view;

import com.ririv.quickoutline.event.AppEventBus;
import com.ririv.quickoutline.event.BookmarksChangedEvent;
import com.ririv.quickoutline.service.PdfOutlineService;
import com.ririv.quickoutline.service.syncWithExternelEditor.SyncWithExternalEditorService;
import com.ririv.quickoutline.utils.*;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import org.slf4j.Logger;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.ririv.quickoutline.view.MyAlert.showAlert;

public class TextTabController {
    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(TextTabController.class);

    private final ResourceBundle bundle = LocalizationManager.getResourceBundle();

    public TextArea contentsTextArea;
    public Button externalEditorBtn;
    public Button autoFormatBtn;

    private final SyncWithExternalEditorService syncWithExternalEditorService = new SyncWithExternalEditorService();

    public Label mask;
//    public ScalePane

    public static TextTabController textModelController;
    public HBox root;

    // 定义正则表达式表示一个缩进（默认一个制表符或4个空格）
    private static final Pattern INDENT_PATTERN = Pattern.compile("^(\\t|\\s{1,4})");


    private PdfOutlineService pdfOutlineService;
    private MainController mainController;

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
        this.pdfOutlineService = mainController.pdfOutlineService;
    }

    public void initialize() {
        textModelController = this;

        // Subscribe to BookmarksChangedEvent
        AppEventBus.getInstance().subscribe(BookmarksChangedEvent.class, event ->
                Platform.runLater(() -> contentsTextArea.setText(event.getRootBookmark().toTreeText())));

/*
        \n没用，只好用\r了
        https://stackoverflow.com/questions/51698600/how-to-add-line-breaks-to-prompt-text-in-javafx
*/


        // 设置处理键盘事件
        // 按下SHIFT+TAB将自动去掉一格缩进（\t或者4个空格）
        contentsTextArea.addEventFilter(KeyEvent.KEY_PRESSED, this::handleTabKeyPress);

        if (OsDesktopUtil.isMacOS()) {
            externalEditorBtn.setVisible(false);
            externalEditorBtn.setManaged(false);
        }
    }


    private void handleTabKeyPress(KeyEvent event) {
        TextArea textArea = (TextArea) event.getSource();
       if (event.getCode() == KeyCode.TAB && event.isShiftDown()) {
            // 按下Shift + Tab键减少缩进
            removeIndent(textArea);
            event.consume(); // 阻止默认行为
        } else  if (event.getCode() == KeyCode.TAB && isMultipleLinesSelected(textArea)) {
            // 按下Tab键添加缩进
            addIndent(textArea);
            event.consume(); // 阻止默认的Tab键行为（比如焦点切换等）
        }
    }

    private void addIndent(TextArea textArea){

                int start = textArea.getSelection().getStart();
                int end = textArea.getSelection().getEnd();

                // 如果选中的文本第一个字符是换行符，则在上一行，需要回到下一行
                if (textArea.getText(start, start+1).equals("\n")){
                    start++;
                }

                // 如果选中的文本最后一个字符是换行符，则已经在第二行，需要回到上一行
                if (textArea.getText(end-1, end).equals("\n")){
                    end--;
                }

                int startLineNumber = getLineNumber(textArea, start);

                int startLineStartPos = getLineStartPos(textArea, startLineNumber - 1);

                // 获取选中的文本
                String selectedLinesText = textArea.getText().substring(startLineStartPos, end);
                String[] selectedLines = selectedLinesText.split("\n");  // 按行分割选中的文本
                System.out.println("selectedLinesText: \n" + selectedLinesText);

                int firstLineIndent = 0;
                int totalIndent = 0;
                for (int i = 0; i < selectedLines.length; i++) {
                    String currentLine = selectedLines[i];
                    String defaultIndent = "\t";
                    if (i == 0) firstLineIndent += defaultIndent.length();
                    totalIndent += defaultIndent.length();
                    String trimmedLine = defaultIndent + currentLine;
                    selectedLines[i] = trimmedLine;
                }
                // 替换文本
                textArea.replaceText(startLineStartPos, end, String.join("\n", selectedLines));
                // 调整光标位置
                textArea.selectRange(start + firstLineIndent, end + totalIndent);

        }


    private void removeIndent(TextArea textArea){
            if (isMultipleLinesSelected(textArea)) { // 选中多行
                // 获取选中的文本范围
                int start = textArea.getSelection().getStart();
                int end = textArea.getSelection().getEnd();

                // 如果选中的文本第一个字符是换行符，则在上一行，需要回到下一行
                if (textArea.getText(start, start+1).equals("\n")){
                    start++;
                }

                // 如果选中的文本最后一个字符是换行符，则已经在第二行，需要回到上一行
                if (textArea.getText(end-1, end).equals("\n")){
                    end--;
                }

                int startLineNumber = getLineNumber(textArea, start);
                int startLineStartPos = getLineStartPos(textArea, startLineNumber - 1);

                // 获取选中的文本
                String selectedLinesText = textArea.getText().substring(startLineStartPos, end);
                String[] selectedLines = selectedLinesText.split("\n");  // 按行分割选中的文本
                System.out.println("selectedLinesText: \n" + selectedLinesText);

                int firstLineIndent = 0;
                int totalIndent = 0;
                for (int i = 0; i < selectedLines.length; i++) {
                    String currentLine = selectedLines[i];
                    // 检查该行是否有缩进，如果没有缩进，跳过此行
                    if (!INDENT_PATTERN.matcher(currentLine).find()) {
                        continue;
                    }

                    Matcher matcher = INDENT_PATTERN.matcher(currentLine);
                    // 去除行首的缩进
                    if (matcher.find()) {
                        if (i == 0) {
                            firstLineIndent += matcher.group(0).length();
                        }
                        totalIndent += matcher.group(0).length();
                    }
                    String trimmedLine = currentLine.replaceFirst(INDENT_PATTERN.pattern(), "");
                    selectedLines[i] = trimmedLine;
                }
                // 替换文本
                textArea.replaceText(startLineStartPos, end, String.join("\n", selectedLines));
                // 调整光标位置
                textArea.selectRange(start - firstLineIndent, end - totalIndent);
            } else { // 单行或未选中文字
                // 获取当前光标所在行
                int caretPosition = textArea.getCaretPosition();

                // 注意这里与多选的逻辑是反的
                if (textArea.getText(caretPosition-1, caretPosition).equals("\n")){
                    caretPosition++;
                }

                int currentLineNumber = getLineNumber(textArea, caretPosition);
//                    System.out.println("currentLineNumber: " + currentLineNumber);

                //最后一个字符为换行符时会发生这种情况，此时不处理
                if (textArea.getText().split("\n").length < currentLineNumber) {
                    return;
                }
                // 获取光标所在行的文本

                String currentLine = textArea.getText().split("\n")[currentLineNumber - 1];
//                    System.out.println("currentLine: " + currentLine);
                // 如果该行没有缩进，则不需要处理，直接返回
                if (INDENT_PATTERN.matcher(currentLine).find()) {
                    // 去除行首的缩进（制表符或4个空格）
                    String trimmedLine = currentLine.replaceFirst(INDENT_PATTERN.pattern(), "");

                    // 替换当前行
                    // 不要重新设置整个文本，否则会导致视图位置丢失
                    int start = getLineStartPos(textArea, currentLineNumber - 1);
                    int end = start + currentLine.length();
                    textArea.replaceText(start, end, trimmedLine);

                    // 设置光标位置
                    textArea.positionCaret(caretPosition - 1);
                }
            }
        }


    // 判断是否选中了多行
    private boolean isMultipleLinesSelected(TextArea textArea) {
        // 获取选中的文本的开始和结束位置
        int selectionStart = textArea.getSelection().getStart();
        int selectionEnd = textArea.getSelection().getEnd();

        // 如果选中的区域起始和结束位置在不同的行，则表示选中了多行
        String text = textArea.getText();
        String selectedText = text.substring(selectionStart, selectionEnd);
        String[] selectedLines = selectedText.split("\n");

        return selectedLines.length > 1;
    }

    @FXML
    private void externalEditorBtnAction() {
        externalEditorBtn.setOnAction(event ->
            syncWithExternalEditorService.exec(
                getCoordinate(),

                fileText -> Platform.runLater(() -> contentsTextArea.setText(fileText)),

                () -> {
                    syncWithExternalEditorService.writeTemp(contentsTextArea.getText());
                    Platform.runLater(() -> {
                        contentsTextArea.setDisable(true);
                        externalEditorBtn.setDisable(true);
                        externalEditorBtn.setText(bundle.getString("btn.externalEditorConnected"));
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
                    ButtonType gotoButton = new ButtonType(bundle.getString("btnType.gotoDownload"), ButtonBar.ButtonData.OK_DONE);
                    ButtonType cancelButton = new ButtonType("OK", ButtonBar.ButtonData.CANCEL_CLOSE);

                    //必须加取消类型按钮，否则对话框右上角的"×"不起作用
                    String contentText = bundle.getString("alert.NotFoundVSCodePrompt");

                    Optional<ButtonType> result;
                    result = showAlert(
                            Alert.AlertType.WARNING, contentText, root.getScene().getWindow(),
                            gotoButton, cancelButton);
//
//                    if (!InfoUtil.isMacOS()) {
//                        result = showAlert(
//                                Alert.AlertType.WARNING, contentText, root.getScene().getWindow(),
//                                gotoButton, cancelButton);
//                    }
//                    else {
//                        String cmd = "xattr -d com.apple.quarantine /Applications/QuickOutline.app";
//                        contentText += "\n\nMacOS请在VSCode中安装Path到环境变量后\n使用如下命令重新运行本软件：\n"+ cmd;
//                        ButtonType copyCmdButton = new ButtonType("复制命令", ButtonBar.ButtonData.OK_DONE);
//                        result = showAlert(
//                                Alert.AlertType.WARNING, contentText, root.getScene().getWindow(),
//                                gotoButton, copyCmdButton, cancelButton);
//                        if (result.isPresent() && result.get() == copyCmdButton) {
//                            Clipboard clipboard = Clipboard.getSystemClipboard();
//                            ClipboardContent content = new ClipboardContent();
//                            content.putString(cmd);
//                            clipboard.setContent(content);
//                        }
//                    }

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

    @FXML
    private void autoFormatBtnAction(ActionEvent event) {
        contentsTextArea.setText(pdfOutlineService.autoFormat(contentsTextArea.getText()));
        syncWithExternalEditorService.writeTemp(contentsTextArea.getText());
        //自动格式化后，将方式切换为"indent",由于操作较为隐蔽，使用者不易发现变化，容易迷惑使用者，所以关闭

        mainController.autoToggleToIndentMethod();
    }


    private int getLineStartPos(TextArea textArea, int lineNumber) {
        String text = textArea.getText();
        String[] lines = text.split("\n");
        int startPosition = 0;

        // 计算前面所有行的字符数总和
        for (int i = 0; i < lineNumber; i++) {
            startPosition += lines[i].length() + 1;  // +1 是为了包括换行符
        }

        return startPosition;
    }

    // 获取光标所在行的起始字符位置
    private int getLineStartPosFromPos(TextArea textArea, int pos) {
        String text = textArea.getText();

        // 查找光标位置之前的换行符
        int lineStartPosition = text.lastIndexOf("\n", pos - 1);

        // 如果光标在第一行，lineStartPosition 会返回 -1，因此需要调整为 0
        if (lineStartPosition == -1) {
            lineStartPosition = 0;
        } else {
            // 跳过换行符的长度，得到实际行的起始位置
            lineStartPosition += 1;
        }

        return lineStartPosition;
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

    private Pair<Integer, Integer> getCoordinate() {
        int pos = contentsTextArea.getCaretPosition();
        int x = getLineNumber(contentsTextArea, pos);
        int y = getColumnNumber(contentsTextArea, pos);
        return new Pair<>(x, y);
    }

}
