package com.ririv.quickoutline.view.utils;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

public class WebViewEditorSupport {

    private final WebView webView;
    private final WebEngine webEngine;

    public WebViewEditorSupport(WebView webView) {
        this.webView = webView;
        this.webEngine = webView.getEngine();
    }

    public void install() {
        // 1. Disable default context menu
        webView.setContextMenuEnabled(false);

        // 2. Create custom context menu
        ContextMenu contextMenu = new ContextMenu();

        MenuItem cutItem = new MenuItem("\u526a\u5207");
        MenuItem copyItem = new MenuItem("\u590d\u5236");
        MenuItem pasteItem = new MenuItem("\u7c98\u8d34");

        cutItem.setOnAction(e -> performCut());
        copyItem.setOnAction(e -> performCopy());
        pasteItem.setOnAction(e -> performPaste());

        contextMenu.getItems().addAll(cutItem, copyItem, new SeparatorMenuItem(), pasteItem);

        // 3. Mouse event for context menu
        webView.setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.SECONDARY) {
                boolean hasSelection = false;
                try {
                    String sel = (String) webEngine.executeScript("window.getSelection().toString()");
                    hasSelection = sel != null && !sel.isEmpty();
                } catch (Exception ignore) {}

                cutItem.setDisable(!hasSelection);
                copyItem.setDisable(!hasSelection);
                pasteItem.setDisable(!Clipboard.getSystemClipboard().hasString());

                contextMenu.show(webView, e.getScreenX(), e.getScreenY());
            } else {
                contextMenu.hide();
            }
        });

        // 4. Keyboard shortcuts
        webView.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (isPasteShortcut(event)) {
                performPaste();
                event.consume();
            } else if (isCopyShortcut(event)) {
                performCopy();
                event.consume();
            } else if (isCutShortcut(event)) {
                performCut();
                event.consume();
            }
        });
    }

    private void performPaste() {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        if (clipboard.hasString()) {
            String text = clipboard.getString();
            if (text == null) return;

            String safeText = text.replace("\\", "\\\\")
                    .replace("'", "\\'")
                    .replace("\n", "\\n")
                    .replace("\r", "");

            // Try custom insertContent (for Vditor) or fallback to execCommand
            webEngine.executeScript(
                "if (window.insertContent) { window.insertContent('" + safeText + "'); } " +
                "else { document.execCommand('insertText', false, '" + safeText + "'); }"
            );
        }
    }

    private void performCopy() {
        try {
            Object selection = webEngine.executeScript("window.getSelection().toString()");
            if (selection != null) {
                String selectedText = selection.toString();
                if (!selectedText.isEmpty()) {
                    ClipboardContent content = new ClipboardContent();
                    content.putString(selectedText);
                    Clipboard.getSystemClipboard().setContent(content);
                }
            }
        } catch (Exception ignore) {}
    }

    private void performCut() {
        performCopy();
        webEngine.executeScript("document.execCommand('delete')");
    }

    private boolean isCutShortcut(KeyEvent event) {
        return (event.isShortcutDown() && event.getCode() == KeyCode.X);
    }

    private boolean isPasteShortcut(KeyEvent event) {
        return (event.isShortcutDown() && event.getCode() == KeyCode.V);
    }

    private boolean isCopyShortcut(KeyEvent event) {
        return (event.isShortcutDown() && event.getCode() == KeyCode.C);
    }
}
