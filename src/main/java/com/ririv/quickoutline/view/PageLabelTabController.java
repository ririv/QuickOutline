package com.ririv.quickoutline.view;

import com.google.inject.Inject;
import com.ririv.quickoutline.exception.InvalidPageLabelRuleException;
import com.ririv.quickoutline.service.PageLabelRule;
import com.ririv.quickoutline.pdfProcess.PageLabel.PageLabelNumberingStyle;
import com.ririv.quickoutline.service.PdfPageLabelService;
import com.ririv.quickoutline.view.controls.message.Message;
import com.ririv.quickoutline.view.controls.select.StyledSelect;
import com.ririv.quickoutline.view.event.AppEventBus;
import com.ririv.quickoutline.view.event.PageLabelsChangedEvent;
import com.ririv.quickoutline.view.event.ShowMessageEvent;
import com.ririv.quickoutline.view.event.ShowSuccessDialogEvent;
import com.ririv.quickoutline.view.state.CurrentFileState;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PageLabelTabController {

    // The UI display strings and their mapping to the domain model (Enum)
    // are now properly confined to the view layer.
    private static final Map<String, PageLabelNumberingStyle> STYLE_MAP = new LinkedHashMap<>();
    static {
        STYLE_MAP.put("无", PageLabelNumberingStyle.NONE);
        STYLE_MAP.put("1, 2, 3, ...", PageLabelNumberingStyle.DECIMAL_ARABIC_NUMERALS);
        STYLE_MAP.put("i, ii, iii, ...", PageLabelNumberingStyle.LOWERCASE_ROMAN_NUMERALS);
        STYLE_MAP.put("I, II, III, ...", PageLabelNumberingStyle.UPPERCASE_ROMAN_NUMERALS);
        STYLE_MAP.put("a, b, c, ...", PageLabelNumberingStyle.LOWERCASE_LETTERS);
        STYLE_MAP.put("A, B, C, ...", PageLabelNumberingStyle.UPPERCASE_LETTERS);
    }

    public ScrollPane labelRuleListLayout;
    @FXML
    private StyledSelect<String> numberingStyleChoiceBox;
    @FXML
    private TextField prefixTextField;
    @FXML
    private TextField startTextField;
    @FXML
    private TextField fromPageTextField;

    private final List<PageLabelRule> pageLabelRules = new ArrayList<>();
    private final PdfPageLabelService pdfPageLabelService;
    private final CurrentFileState fileService;
    private final VBox ruleVBox = new VBox(5);
    private final AppEventBus appEventBus;
    private List<String> originalPageLabels;

    @Inject
    public PageLabelTabController(PdfPageLabelService pdfPageLabelService, CurrentFileState fileService, AppEventBus appEventBus) {
        this.pdfPageLabelService = pdfPageLabelService;
        this.fileService = fileService;
        this.appEventBus = appEventBus;
    }

    public void initialize() {
        labelRuleListLayout.setContent(ruleVBox);
        numberingStyleChoiceBox.setItems(FXCollections.observableArrayList(STYLE_MAP.keySet()));
        numberingStyleChoiceBox.setValue("1, 2, 3, ...");

        fileService.srcFileProperty().addListener((obs, oldFile, newFile) -> {
            if (newFile != null) {
                try {
                    originalPageLabels = Arrays.asList(pdfPageLabelService.getPageLabels(newFile.toString()));
                } catch (IOException e) {
                    e.printStackTrace();
                    originalPageLabels = null;
                }
            } else {
                originalPageLabels = null;
            }
        });
    }

    @FXML
    void addRule() {
        try {
            String selectedStyleString = numberingStyleChoiceBox.getValue();
            PageLabelNumberingStyle selectedStyleEnum = STYLE_MAP.get(selectedStyleString);

            // The service is now called with the pure Enum, not the display string.
            PageLabelRule rule = pdfPageLabelService.validateAndCreateRule(
                    fromPageTextField.getText(),
                    prefixTextField.getText(),
                    startTextField.getText(),
                    selectedStyleEnum, // Pass the pure enum
                    pageLabelRules
            );

            pageLabelRules.add(rule);
            // The view needs the display string to show the rule, so we create a temporary view model or just format it here.
            addRuleToView(rule, selectedStyleString);
            simulate();

            fromPageTextField.clear();
            prefixTextField.clear();
            startTextField.clear();

        } catch (InvalidPageLabelRuleException e) {
            appEventBus.post(new ShowMessageEvent(e.getMessage(), Message.MessageType.ERROR));
        }
    }

    private void addRuleToView(PageLabelRule rule, String styleString) {
        HBox ruleBox = new HBox(10);
        ruleBox.setAlignment(Pos.CENTER_LEFT);

        String text = String.format("从第 %d 页开始: 样式=%s, 前缀='%s', 起始于=%d",
                rule.fromPage(), styleString, rule.prefix(), rule.start());
        Label ruleLabel = new Label(text);
        HBox.setHgrow(ruleLabel, Priority.ALWAYS);
        ruleLabel.setMaxWidth(Double.MAX_VALUE);

        Button deleteButton = new Button();
        deleteButton.getStyleClass().addAll("graph-button", "graph-button-important");
        deleteButton.setPrefWidth(30);
        Node graphic = new Region();
        graphic.getStyleClass().addAll("icon", "delete-item-icon");
        deleteButton.setGraphic(graphic);
        deleteButton.setOnAction(event -> {
            pageLabelRules.remove(rule);
            ruleVBox.getChildren().remove(ruleBox);
            simulate();
        });

        ruleBox.getChildren().addAll(ruleLabel, deleteButton);
        ruleVBox.getChildren().add(ruleBox);
    }

    private void simulate() {
        if (fileService.getSrcFile() == null) {
            return;
        }

        if (pageLabelRules.isEmpty()) {
            if (originalPageLabels != null) {
                appEventBus.post(new PageLabelsChangedEvent(originalPageLabels));
            }
            return;
        }

        int totalPages;
        try {
            totalPages = pdfPageLabelService.getPageLabels(fileService.getSrcFile().toString()).length;
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        List<String> simulatedLabels = pdfPageLabelService.simulatePageLabels(pageLabelRules, totalPages);
        appEventBus.post(new PageLabelsChangedEvent(simulatedLabels));
    }

    @FXML
    void apply() {
        if (fileService.getSrcFile() == null) {
            appEventBus.post(new ShowMessageEvent("请先选择一个PDF文件", Message.MessageType.WARNING));
            return;
        }

        List<com.ririv.quickoutline.pdfProcess.PageLabel> finalPageLabels = pdfPageLabelService.convertRulesToPageLabels(pageLabelRules);

        String srcFilePath = fileService.getSrcFile().toString();
        String destFilePath = fileService.getDestFile().toString();
        try {
            String[] pageLabels = pdfPageLabelService.setPageLabels(srcFilePath, destFilePath, finalPageLabels);
            appEventBus.post(new PageLabelsChangedEvent(Arrays.asList(pageLabels)));
            appEventBus.post(new ShowSuccessDialogEvent());
        } catch (IOException e) {
            appEventBus.post(new ShowMessageEvent("应用页码标签失败: " + e.getMessage(), Message.MessageType.ERROR));
            throw new RuntimeException(e);
        }
    }
}
