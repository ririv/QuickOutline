package com.ririv.quickoutline.view;

import com.ririv.quickoutline.pdfProcess.PageLabel;
import com.ririv.quickoutline.service.PdfLabelService;
import com.ririv.quickoutline.service.FileService;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class PageLabelController {

    public ScrollPane labelRuleListLayout;
    @FXML
    private ChoiceBox<String> numberingStyleChoiceBox;
    @FXML
    private TextField prefixTextField;
    @FXML
    private TextField startTextField;

    @FXML
    private TextField fromPageTextField;

    @FXML
    private TextField toPageTextField;

    // A record to hold rule data
    private record PageLabelRule(int fromPage, int toPage, PageLabel.PageLabelNumberingStyle style, String styleString, String prefix, int start) {}

    private final List<PageLabelRule> pageLabelRules = new ArrayList<>();

    private final PdfLabelService pdfLabelService = new PdfLabelService();
    private final FileService fileService = FileService.getInstance();

    private final VBox ruleVBox = new VBox(5); // Add some spacing

    public void initialize() {
        labelRuleListLayout.setContent(ruleVBox);
        // Set default value for choice box if not set in FXML
        if (numberingStyleChoiceBox.getValue() == null) {
            numberingStyleChoiceBox.setValue("1, 2, 3, ...");
        }
    }

    @FXML
    void addRule() {
        try {
            // 1. Parse and validate input
            if (fromPageTextField.getText().isEmpty() || toPageTextField.getText().isEmpty()) {
                showAlert("Invalid Input", "Page range fields cannot be empty.");
                return;
            }
            int fromPage = Integer.parseInt(fromPageTextField.getText());
            int toPage = Integer.parseInt(toPageTextField.getText());

            if (fromPage <= 0 || toPage <= 0) {
                showAlert("Invalid Input", "Page numbers must be positive.");
                return;
            }

            if (fromPage > toPage) {
                showAlert("Invalid Input", "'From' page must be less than or equal to 'To' page.");
                return;
            }

            String prefix = prefixTextField.getText();
            int start = 1; // Default value
            if (!startTextField.getText().isEmpty()) {
                start = Integer.parseInt(startTextField.getText());
                if (start < 1) {
                    showAlert("Invalid Input", "Start number must be 1 or greater.");
                    return;
                }
            }

            String styleString = numberingStyleChoiceBox.getValue();
            PageLabel.PageLabelNumberingStyle style = getNumberingStyle(styleString);

            // 2. Create and store the rule
            PageLabelRule rule = new PageLabelRule(fromPage, toPage, style, styleString, prefix, start);
            pageLabelRules.add(rule);

            // 3. Update UI
            addRuleToView(rule);

            // 4. Clear input fields for next entry
            fromPageTextField.clear();
            toPageTextField.clear();
            prefixTextField.clear();
            startTextField.clear();

        } catch (NumberFormatException e) {
            showAlert("Invalid Input", "Please enter valid numbers for page ranges and start number.");
        }
    }

    private void addRuleToView(PageLabelRule rule) {
        HBox ruleBox = new HBox(10);
        ruleBox.setAlignment(Pos.CENTER_LEFT);

        String text = String.format("页码 %d 到 %d: 样式=%s, 前缀='%s', 起始于=%d",
                rule.fromPage(), rule.toPage(), rule.styleString(), rule.prefix(), rule.start());
        Label ruleLabel = new Label(text);
        HBox.setHgrow(ruleLabel, Priority.ALWAYS);
        ruleLabel.setMaxWidth(Double.MAX_VALUE);


        Button deleteButton = new Button("删除");
        deleteButton.setOnAction(event -> {
            pageLabelRules.remove(rule);
            ruleVBox.getChildren().remove(ruleBox);
        });

        ruleBox.getChildren().addAll(ruleLabel, deleteButton);
        ruleVBox.getChildren().add(ruleBox);
    }


    @FXML
    void apply() {
        if (fileService.getSrcFile() == null) {
            showAlert("Error", "Please select a PDF file first.");
            return;
        }

        // Generate PageLabel list from rules, handling overlaps
        // Using a map ensures that later rules for the same page override earlier ones.
        Map<Integer, PageLabel> pageLabelsMap = new TreeMap<>();
        for (PageLabelRule rule : pageLabelRules) {
            for (int i = rule.fromPage(); i <= rule.toPage(); i++) {
                int logicalPage = rule.start() + (i - rule.fromPage());
                PageLabel pageLabel = new PageLabel(i, rule.style(), rule.prefix(), logicalPage);
                pageLabelsMap.put(i, pageLabel);
            }
        }
        List<PageLabel> finalPageLabels = new ArrayList<>(pageLabelsMap.values());


        String srcFilePath = fileService.getSrcFile().getPath();
        String destFilePath = fileService.getDestFile().getPath();
        try {
            pdfLabelService.setPageLabels(srcFilePath, destFilePath, finalPageLabels);
            showAlert("Success", "Page labels applied successfully.");
        } catch (IOException e) {
            showAlert("Error", "Failed to apply page labels: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private PageLabel.PageLabelNumberingStyle getNumberingStyle(String styleString) {
        return switch (styleString) {
            case "1, 2, 3, ..." -> PageLabel.PageLabelNumberingStyle.DECIMAL_ARABIC_NUMERALS;
            case "i, ii, iii, ..." -> PageLabel.PageLabelNumberingStyle.LOWERCASE_ROMAN_NUMERALS;
            case "I, II, III, ..." -> PageLabel.PageLabelNumberingStyle.UPPERCASE_ROMAN_NUMERALS;
            case "a, b, c, ..." -> PageLabel.PageLabelNumberingStyle.LOWERCASE_LETTERS;
            case "A, B, C, ..." -> PageLabel.PageLabelNumberingStyle.UPPERCASE_LETTERS;
            default -> null; // "无" maps to null
        };
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}