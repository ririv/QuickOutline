package com.ririv.quickoutline.view;

import jakarta.inject.Inject;
import com.ririv.quickoutline.pdfProcess.PdfRenderSession;
import com.ririv.quickoutline.view.state.CurrentFileState;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class PdfViewerTabController {

    @FXML
    private ImageView imageView;
    @FXML
    private ScrollPane scrollPane;
    @FXML
    private Button prevButton;
    @FXML
    private Button nextButton;
    @FXML
    private Button goButton;
    @FXML
    private Label pageLabel;
    @FXML
    private TextField pageField;

    private PdfRenderSession renderSession; // 会话由 CurrentFileState 管理，本控制器仅监听和展示
    private int currentPageIndex = 0;

    private final CurrentFileState currentFileState;

    @Inject
    public PdfViewerTabController(CurrentFileState currentFileState) {
        this.currentFileState = currentFileState;
    }

    @FXML
    public void initialize() {
        currentFileState.pageRenderSessionProperty().addListener((obs, oldSession, newSession) -> {
            this.renderSession = newSession;
            this.currentPageIndex = 0;
            if (newSession != null) {
                showPage(this.currentPageIndex);
            } else {
                imageView.setImage(null);
                updateControls();
            }
        });
        updateControls();
    }

    private void showPage(int index) {
        if (renderSession == null || renderSession.getPageCount() == 0) {
            imageView.setImage(null);
            pageLabel.setText("N/A");
            return;
        }

        if (index < 0 || index >= renderSession.getPageCount()) {
            return;
        }
        renderSession.renderPreviewAsync(index, bufferedImage -> {
            Image image = SwingFXUtils.toFXImage(bufferedImage, null);
            Platform.runLater(() -> {
                imageView.setImage(image);
                currentPageIndex = index;
                updateControls();
            });
        });
    }

    private void updateControls() {
        boolean isPdfLoaded = renderSession != null && renderSession.getPageCount() > 0;

        prevButton.setDisable(!isPdfLoaded || currentPageIndex <= 0);
        nextButton.setDisable(!isPdfLoaded || currentPageIndex >= renderSession.getPageCount() - 1);
        goButton.setDisable(!isPdfLoaded);
        pageField.setDisable(!isPdfLoaded);

        if (isPdfLoaded) {
            int pageCount = renderSession.getPageCount();
            pageLabel.setText(String.format("第 %d / %d 页", currentPageIndex + 1, pageCount));
            pageField.setText(String.valueOf(currentPageIndex + 1));
        } else {
            pageLabel.setText("未加载文件");
            pageField.clear();
        }
    }

    @FXML
    private void handlePrevPage() {
        if (currentPageIndex > 0) {
            showPage(currentPageIndex - 1);
        }
    }

    @FXML
    private void handleNextPage() {
        if (renderSession != null && currentPageIndex < renderSession.getPageCount() - 1) {
            showPage(currentPageIndex + 1);
        }
    }

    @FXML
    private void handleGoToPage() {
        if (renderSession == null) return;
        try {
            int pageNum = Integer.parseInt(pageField.getText().trim()) - 1;
            if (pageNum >= 0 && pageNum < renderSession.getPageCount()) {
                showPage(pageNum);
            } else {
                pageField.setText(String.valueOf(currentPageIndex + 1));
            }
        } catch (NumberFormatException e) {
            pageField.setText(String.valueOf(currentPageIndex + 1));
        }
    }

}
