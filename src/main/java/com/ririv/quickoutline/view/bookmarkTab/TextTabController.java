package com.ririv.quickoutline.view.bookmarkTab;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.ririv.quickoutline.event.AppEventBus;
import com.ririv.quickoutline.event.AutoToggleToIndentEvent;
import com.ririv.quickoutline.event.BookmarksChangedEvent;
import com.ririv.quickoutline.service.PdfOutlineService;
import com.ririv.quickoutline.service.syncWithExternelEditor.SyncWithExternalEditorService;
import com.ririv.quickoutline.textProcess.methods.Method;
import com.ririv.quickoutline.utils.LocalizationManager;
import com.ririv.quickoutline.utils.OsDesktopUtil;
import com.ririv.quickoutline.utils.Pair;
import com.ririv.quickoutline.view.controls.Remind;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import org.slf4j.Logger;

import java.awt.Desktop;
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
    private final SyncWithExternalEditorService syncWithExternalEditorService = new SyncWithExternalEditorService();
    private final PdfOutlineService pdfOutlineService;
    private final AppEventBus eventBus;

    public TextArea contentsTextArea;
    public Button externalEditorBtn;
    public Button autoFormatBtn;
    public Label mask;
    public HBox root;

    @FXML public RadioButton seqRBtn;
    @FXML public RadioButton indentRBtn;
    @FXML public ToggleGroup methodToggleGroup;
    @FXML public Remind indentRBtnRemind;
    @FXML public Remind seqRBtnRemind;

    private static final Pattern INDENT_PATTERN = Pattern.compile("^(\\t|\\s{1,4})");

    @Inject
    public TextTabController(PdfOutlineService pdfOutlineService, AppEventBus eventBus) {
        this.pdfOutlineService = pdfOutlineService;
        this.eventBus = eventBus;
        this.eventBus.register(this);
    }

    public void initialize() {
        seqRBtn.setUserData(Method.SEQ);
        indentRBtn.setUserData(Method.INDENT);
        seqRBtn.setSelected(true);

        contentsTextArea.addEventFilter(KeyEvent.KEY_PRESSED, this::handleTabKeyPress);

        if (OsDesktopUtil.isMacOS()) {
            externalEditorBtn.setVisible(false);
            externalEditorBtn.setManaged(false);
        }
    }

    @Subscribe
    public void onAutoToggleToIndent(AutoToggleToIndentEvent event) {
        autoToggleToIndentMethod();
    }

    @Subscribe
    public void onBookmarksChanged(BookmarksChangedEvent event) {
        Platform.runLater(() -> contentsTextArea.setText(event.getRootBookmark().toOutlineString()));
    }

    private void handleTabKeyPress(KeyEvent event) {
        TextArea textArea = (TextArea) event.getSource();
       if (event.getCode() == KeyCode.TAB && event.isShiftDown()) {
            removeIndent(textArea);
            event.consume();
        } else  if (event.getCode() == KeyCode.TAB && isMultipleLinesSelected(textArea)) {
            addIndent(textArea);
            event.consume();
        }
    }

    private void addIndent(TextArea textArea){
        int start = textArea.getSelection().getStart();
        int end = textArea.getSelection().getEnd();
        if (textArea.getText(start, start+1).equals("\n")){
            start++;
        }
        if (textArea.getText(end-1, end).equals("\n")){
            end--;
        }
        int startLineNumber = getLineNumber(textArea, start);
        int startLineStartPos = getLineStartPos(textArea, startLineNumber - 1);
        String selectedLinesText = textArea.getText().substring(startLineStartPos, end);
        String[] selectedLines = selectedLinesText.split("\n");
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
        textArea.replaceText(startLineStartPos, end, String.join("\n", selectedLines));
        textArea.selectRange(start + firstLineIndent, end + totalIndent);
    }

    private void removeIndent(TextArea textArea){
        if (isMultipleLinesSelected(textArea)) { // 选中多行
            int start = textArea.getSelection().getStart();
            int end = textArea.getSelection().getEnd();
            if (textArea.getText(start, start+1).equals("\n")){
                start++;
            }
            if (textArea.getText(end-1, end).equals("\n")){
                end--;
            }
            int startLineNumber = getLineNumber(textArea, start);
            int startLineStartPos = getLineStartPos(textArea, startLineNumber - 1);
            String selectedLinesText = textArea.getText().substring(startLineStartPos, end);
            String[] selectedLines = selectedLinesText.split("\n");
            int firstLineIndent = 0;
            int totalIndent = 0;
            for (int i = 0; i < selectedLines.length; i++) {
                String currentLine = selectedLines[i];
                if (!INDENT_PATTERN.matcher(currentLine).find()) {
                    continue;
                }
                Matcher matcher = INDENT_PATTERN.matcher(currentLine);
                if (matcher.find()) {
                    if (i == 0) {
                        firstLineIndent += matcher.group(0).length();
                    }
                    totalIndent += matcher.group(0).length();
                }
                String trimmedLine = currentLine.replaceFirst(INDENT_PATTERN.pattern(), "");
                selectedLines[i] = trimmedLine;
            }
            textArea.replaceText(startLineStartPos, end, String.join("\n", selectedLines));
            textArea.selectRange(start - firstLineIndent, end - totalIndent);
        } else { // 单行或未选中文字
            int caretPosition = textArea.getCaretPosition();
            if (textArea.getText(caretPosition-1, caretPosition).equals("\n")){
                caretPosition++;
            }
            int currentLineNumber = getLineNumber(textArea, caretPosition);
            if (textArea.getText().split("\n").length < currentLineNumber) {
                return;
            }
            String currentLine = textArea.getText().split("\n")[currentLineNumber - 1];
            if (INDENT_PATTERN.matcher(currentLine).find()) {
                String trimmedLine = currentLine.replaceFirst(INDENT_PATTERN.pattern(), "");
                int start = getLineStartPos(textArea, currentLineNumber - 1);
                int end = start + currentLine.length();
                textArea.replaceText(start, end, trimmedLine);
                textArea.positionCaret(caretPosition - 1);
            }
        }
    }

    private boolean isMultipleLinesSelected(TextArea textArea) {
        int selectionStart = textArea.getSelection().getStart();
        int selectionEnd = textArea.getSelection().getEnd();
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
                    String contentText = bundle.getString("alert.NotFoundVSCodePrompt");
                    Optional<ButtonType> result = showAlert(
                            Alert.AlertType.WARNING, contentText, root.getScene().getWindow(),
                            gotoButton, cancelButton);
                    if (result.isPresent() && result.get() == gotoButton) {
                        try {
                            Desktop.getDesktop().browse(new URI("https://code.visualstudio.com/"));
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
        eventBus.post(new AutoToggleToIndentEvent());
    }

    private int getLineStartPos(TextArea textArea, int lineNumber) {
        String text = textArea.getText();
        String[] lines = text.split("\n");
        int startPosition = 0;
        for (int i = 0; i < lineNumber; i++) {
            startPosition += lines[i].length() + 1;
        }
        return startPosition;
    }

    private int getLineStartPosFromPos(TextArea textArea, int pos) {
        String text = textArea.getText();
        int lineStartPosition = text.lastIndexOf("\n", pos - 1);
        if (lineStartPosition == -1) {
            lineStartPosition = 0;
        } else {
            lineStartPosition += 1;
        }
        return lineStartPosition;
    }

    private int getLineNumber(TextArea textArea, int caretPosition) {
        String textBeforeCaret = textArea.getText(0, caretPosition);
        return (int) textBeforeCaret.chars().filter(ch -> ch == '\n').count() + 1;
    }

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

    public Method getSelectedMethod() {
        return (Method) methodToggleGroup.getSelectedToggle().getUserData();
    }

    public String getContents() {
        return contentsTextArea.getText();
    }

    public void setContents(String contents) {
        contentsTextArea.setText(contents);
    }

    public void autoToggleToIndentMethod() {
        if (methodToggleGroup.getSelectedToggle() != indentRBtn) {
            methodToggleGroup.selectToggle(indentRBtn);
            indentRBtnRemind.play();
        }
    }
}