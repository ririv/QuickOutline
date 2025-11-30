package com.ririv.quickoutline.view;

import com.ririv.quickoutline.pdfProcess.PdfRenderSession;
import com.ririv.quickoutline.view.controls.PagePreviewer;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

import java.io.IOException;

public class ThumbnailView extends VBox {

    private static final double BASE_WIDTH = 150;
    private static final double BASE_HEIGHT = 225;

    @FXML
    private ImageView thumbnailImageView;
    @FXML
    private Label pageLabel;

    private int pageIndex; // Store the page index to re-render high-res image
    private String[] pageLabels; // Store the page labels array
    private int totalPages; // Store total pages
    private PagePreviewer pagePreviewer;

    public ThumbnailView() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("ThumbnailView.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    @FXML
    public void initialize() {
        // Set base size
        thumbnailImageView.setFitWidth(BASE_WIDTH);
        thumbnailImageView.setFitHeight(BASE_HEIGHT);

        // Prepare the popup card and its content
        this.pagePreviewer = new PagePreviewer();
        pagePreviewer.attach(thumbnailImageView, () -> this.pageIndex);
    }

    public void setScale(double scale) {
        thumbnailImageView.setFitWidth(BASE_WIDTH * scale);
        thumbnailImageView.setFitHeight(BASE_HEIGHT * scale);
    }

    public void setThumbnailImage(Image image, int pageIndex, PdfRenderSession session, String[] pageLabels) {
        this.pageIndex = pageIndex;
        this.pageLabels = pageLabels;
        this.totalPages = session.getPageCount(); // Get total pages
        this.thumbnailImageView.setImage(image);

        // Set the renderer for the previewer
        this.pagePreviewer.setRenderSession(session);

        // Update page label with PageLabelService
        updatePageLabel();
    }

    public void updatePageLabel(String[] pageLabels) {
        this.pageLabels = pageLabels;
        updatePageLabel();
    }

    private void updatePageLabel() {
        String displayLabel = "";
        if (pageLabels != null && pageIndex < pageLabels.length) {
            String labelText = pageLabels[pageIndex];
            if (labelText != null) {
                displayLabel = labelText;
            } else {
                displayLabel = "第 " + (pageIndex + 1) + " 页";
            }
        } else {
            displayLabel = "第 " + (pageIndex + 1) + " 页";
        }
        pageLabel.setText(displayLabel);

        // Add Tooltip
        Tooltip tooltip = new Tooltip("页面" + (pageIndex + 1) + "/" + totalPages);
        pageLabel.setTooltip(tooltip);
    }

    
}
