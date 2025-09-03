package com.ririv.quickoutline.view;

import com.google.inject.Inject;
import com.ririv.quickoutline.pdfProcess.PdfPreview;
import com.ririv.quickoutline.view.state.CurrentFileState;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;

import java.io.File;
import java.io.IOException;

public class PdfPreviewController {

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

    private PdfPreview pdfPreview;
    private int currentPageIndex = 0;

    private final CurrentFileState currentFileState;

    @Inject
    public PdfPreviewController(CurrentFileState currentFileState) {
        this.currentFileState = currentFileState;
    }

    @FXML
    public void initialize() {
        currentFileState.srcFileProperty().addListener((obs, oldPath, newPath) -> {
            if (newPath != null) {
                loadPdf(newPath.toFile());
            } else {
                closePreview();
                imageView.setImage(null);
                updateControls();
            }
        });
        updateControls();
    }

    public void loadPdf(File pdfFile) {
        try {
            closePreview(); // Close previous document if open

            this.pdfPreview = new PdfPreview(pdfFile);
            this.currentPageIndex = 0;
            showPage(this.currentPageIndex);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showPage(int index) {
        if (pdfPreview == null || pdfPreview.getPageCount() == 0) {
            imageView.setImage(null);
            pageLabel.setText("N/A");
            return;
        }

        if (index < 0 || index >= pdfPreview.getPageCount()) {
            return;
        }

        try {
            pdfPreview.renderPage(index, image -> {
                imageView.setImage(image);
                currentPageIndex = index;
                updateControls(); // Must update controls after image is set
            });
        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("渲染错误");
            alert.setHeaderText("无法渲染PDF页面");
            alert.setContentText("渲染页面 " + (index + 1) + " 时发生错误:\n" + e.getMessage());
            alert.showAndWait();
            updateControls(); // Also update controls on error
        }
    }

    private void updateControls() {
        boolean isPdfLoaded = pdfPreview != null && pdfPreview.getPageCount() > 0;

        prevButton.setDisable(!isPdfLoaded || currentPageIndex <= 0);
        nextButton.setDisable(!isPdfLoaded || currentPageIndex >= pdfPreview.getPageCount() - 1);
        goButton.setDisable(!isPdfLoaded);
        pageField.setDisable(!isPdfLoaded);

        if (isPdfLoaded) {
            int pageCount = pdfPreview.getPageCount();
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
        if (pdfPreview != null && currentPageIndex < pdfPreview.getPageCount() - 1) {
            showPage(currentPageIndex + 1);
        }
    }

    @FXML
    private void handleGoToPage() {
        if (pdfPreview == null) return;
        try {
            int pageNum = Integer.parseInt(pageField.getText().trim()) - 1;
            if (pageNum >= 0 && pageNum < pdfPreview.getPageCount()) {
                showPage(pageNum);
            } else {
                pageField.setText(String.valueOf(currentPageIndex + 1));
            }
        } catch (NumberFormatException e) {
            pageField.setText(String.valueOf(currentPageIndex + 1));
        }
    }

    /**
     * This method should be called by the owner of this controller's stage
     * when the stage is closing to ensure resources are released.
     */
    public void closePreview() {
        try {
            if (pdfPreview != null) {
                pdfPreview.close();
                pdfPreview = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
