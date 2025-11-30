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
        } else if (event.getCode() == KeyCode.TAB && (isMultipleLinesSelected() || isSingleLineFullySelected())) {
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
        selectRange(startLineStartPos, startLineStartPos + String.join("\n", selectedLines).length());
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
            selectRange(startLineStartPos, startLineStartPos + String.join("\n", selectedLines).length());
        } else { // Single line or no selection
            int selectionStart = getSelection().getStart();
            int selectionEnd = getSelection().getEnd();
            boolean hasSelection = selectionEnd > selectionStart;

            int lineNo = getLineNumber(selectionStart);
            String[] lines = getText().split("\n");
            if (lineNo > lines.length) return; // Should not happen

            String currentLine = lines[lineNo - 1];
            Matcher matcher = INDENT_PATTERN.matcher(currentLine);

            if (matcher.find()) {
                int indentSize = matcher.group(1).length();
                String trimmedLine = currentLine.substring(indentSize);
                int lineStartPos = getLineStartPos(lineNo - 1);

                replaceText(lineStartPos, lineStartPos + currentLine.length(), trimmedLine);

                if (hasSelection) {
                    // Reselect the text, adjusted for the removed indent
                    selectRange(Math.max(lineStartPos, selectionStart - indentSize), selectionEnd - indentSize);
                } else {
                    // Just move the caret
                    positionCaret(Math.max(lineStartPos, selectionStart - indentSize));
                }
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

    private boolean isSingleLineFullySelected() {
        int start = getSelection().getStart();
        int end = getSelection().getEnd();
        if (start == end) return false;

        String text = getText();
        // Find the start of the line
        int lineStart = text.lastIndexOf('\n', start - 1) + 1;

        // Find the end of the line
        int lineEnd = text.indexOf('\n', start);
        if (lineEnd == -1) {
            lineEnd = text.length();
        }

        // Check if the selection covers the entire line
        return start == lineStart && end == lineEnd;
    }
}
