package com.ririv.quickoutline.view.controls;

import com.ririv.quickoutline.utils.Pair;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EditorTextArea extends TextArea {

    private static final Pattern INDENT_PATTERN = Pattern.compile("^(\\t|\\s{1,4})");

    public EditorTextArea() {
        super();
        this.addEventFilter(KeyEvent.KEY_PRESSED, this::handleTabKeyPress);
    }

    // --- Logic moved from TextSubViewController ---

    private void handleTabKeyPress(KeyEvent event) {
        if (event.getCode() == KeyCode.TAB && event.isShiftDown()) {
            removeIndent();
            event.consume();
        } else if (event.getCode() == KeyCode.TAB && isMultipleLinesSelected()) {
            addIndent();
            event.consume();
        }
    }

    private void addIndent() {
        int start = getSelection().getStart();
        int end = getSelection().getEnd();
        if (getText(start, start + 1).equals("\n")) {
            start++;
        }
        if (getText(end - 1, end).equals("\n")) {
            end--;
        }
        int startLineNumber = getLineNumber(start);
        int startLineStartPos = getLineStartPos(startLineNumber - 1);
        String selectedLinesText = getText().substring(startLineStartPos, end);
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
        replaceText(startLineStartPos, end, String.join("\n", selectedLines));
        selectRange(start + firstLineIndent, end + totalIndent);
    }

    private void removeIndent() {
        if (isMultipleLinesSelected()) { // 选中多行
            int start = getSelection().getStart();
            int end = getSelection().getEnd();
            if (getText(start, start + 1).equals("\n")) {
                start++;
            }
            if (getText(end - 1, end).equals("\n")) {
                end--;
            }
            int startLineNumber = getLineNumber(start);
            int startLineStartPos = getLineStartPos(startLineNumber - 1);
            String selectedLinesText = getText().substring(startLineStartPos, end);
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
            replaceText(startLineStartPos, end, String.join("\n", selectedLines));
            selectRange(start - firstLineIndent, end - totalIndent);
        } else { // 单行或未选中文字
            int caretPosition = getCaretPosition();
            if (getText(caretPosition - 1, caretPosition).equals("\n")) {
                caretPosition++;
            }
            int currentLineNumber = getLineNumber(caretPosition);
            if (getText().split("\n").length < currentLineNumber) {
                return;
            }
            String currentLine = getText().split("\n")[currentLineNumber - 1];
            if (INDENT_PATTERN.matcher(currentLine).find()) {
                String trimmedLine = currentLine.replaceFirst(INDENT_PATTERN.pattern(), "");
                int start = getLineStartPos(currentLineNumber - 1);
                int end = start + currentLine.length();
                replaceText(start, end, trimmedLine);
                positionCaret(caretPosition - 1);
            }
        }
    }

    private boolean isMultipleLinesSelected() {
        String selectedText = getSelectedText();
        if (selectedText == null || selectedText.isEmpty()) {
            return false;
        }
        return selectedText.contains("\n");
    }

    private int getLineStartPos(int lineNumber) {
        String text = getText();
        String[] lines = text.split("\n");
        int startPosition = 0;
        for (int i = 0; i < lineNumber; i++) {
            startPosition += lines[i].length() + 1;
        }
        return startPosition;
    }

    private int getLineNumber(int caretPosition) {
        String textBeforeCaret = getText(0, caretPosition);
        return (int) textBeforeCaret.chars().filter(ch -> ch == '\n').count() + 1;
    }

    private int getColumnNumber(int caretPosition) {
        String textBeforeCaret = getText(0, caretPosition);
        int lastNewLineIndex = textBeforeCaret.lastIndexOf('\n');
        return caretPosition - (lastNewLineIndex + 1) + 1;
    }

    public Pair<Integer, Integer> getCoordinate() {
        int pos = getCaretPosition();
        int x = getLineNumber(pos);
        int y = getColumnNumber(pos);
        return new Pair<>(x, y);
    }
}