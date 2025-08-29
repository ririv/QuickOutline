package com.ririv.quickoutline.view;

import com.google.inject.Inject;
import com.ririv.quickoutline.event.AppEventBus;
import com.ririv.quickoutline.event.ShowMessageEvent;
import com.ririv.quickoutline.pdfProcess.PageLabel;
import com.ririv.quickoutline.service.PdfPageLabelService;
import com.ririv.quickoutline.state.CurrentFileState;
import com.ririv.quickoutline.view.controls.select.StyledSelect;
import com.ririv.quickoutline.view.controls.message.Message;
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
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class PageLabelController {

    // 代码风格优化: 将所有样式字符串定义为公共静态常量
    public static final String STYLE_NONE = "无";
    public static final String STYLE_DECIMAL = "1, 2, 3, ...";
    public static final String STYLE_ROMAN_LOWER = "i, ii, iii, ...";
    public static final String STYLE_ROMAN_UPPER = "I, II, III, ...";
    public static final String STYLE_LETTERS_LOWER = "a, b, c, ...";
    public static final String STYLE_LETTERS_UPPER = "A, B, C, ...";

    public ScrollPane labelRuleListLayout;
    @FXML
    private StyledSelect<String> numberingStyleChoiceBox;
    @FXML
    private TextField prefixTextField;
    @FXML
    private TextField startTextField;
    @FXML
    private TextField fromPageTextField;

    private record PageLabelRule(int fromPage, PageLabel.PageLabelNumberingStyle style, String styleString, String prefix, int start) {}

    private final List<PageLabelRule> pageLabelRules = new ArrayList<>();
    private final PdfPageLabelService pdfPageLabelService;
    private final CurrentFileState fileService;
    private final VBox ruleVBox = new VBox(5);
    private final AppEventBus appEventBus;

    @FXML
    private ThumbnailPaneController thumbnailPaneController; // Injected by FXML loader

    @Inject
    public PageLabelController(PdfPageLabelService pdfPageLabelService, CurrentFileState fileService, AppEventBus appEventBus) {
        this.pdfPageLabelService = pdfPageLabelService;
        this.fileService = fileService;
        this.appEventBus = appEventBus;
    }

    public void initialize() {
        labelRuleListLayout.setContent(ruleVBox);
        numberingStyleChoiceBox.setItems(FXCollections.observableArrayList(
                STYLE_NONE, STYLE_DECIMAL, STYLE_ROMAN_LOWER, STYLE_ROMAN_UPPER, STYLE_LETTERS_LOWER, STYLE_LETTERS_UPPER
        ));
        // 使用常量来设置默认值
        if (numberingStyleChoiceBox.getValue() == null) {
            numberingStyleChoiceBox.setValue(STYLE_DECIMAL);
        }
    }

    @FXML
    void addRule() {
        try {
            if (fromPageTextField.getText().isEmpty()) {
                appEventBus.post(new ShowMessageEvent("输入无效，'起始页' 字段不能为空。", Message.MessageType.ERROR));
                return;
            }
            int fromPage = Integer.parseInt(fromPageTextField.getText());

            if (fromPage <= 0) {
                appEventBus.post(new ShowMessageEvent("输入无效，页码必须是正数。", Message.MessageType.ERROR));
                return;
            }

            String prefix = prefixTextField.getText();
            int start = 1;
            if (!startTextField.getText().isEmpty()) {
                start = Integer.parseInt(startTextField.getText());
                if (start < 1) {
                    appEventBus.post(new ShowMessageEvent("输入无效，起始数字必须大于或等于 1。", Message.MessageType.ERROR));
                    return;
                }
            }

            String styleString = numberingStyleChoiceBox.getValue();
            PageLabel.PageLabelNumberingStyle style = getNumberingStyle(styleString);

            PageLabelRule rule = new PageLabelRule(fromPage, style, styleString, prefix, start);
            pageLabelRules.add(rule);

            addRuleToView(rule);

            fromPageTextField.clear();
            prefixTextField.clear();
            startTextField.clear();

        } catch (NumberFormatException e) {
            appEventBus.post(new ShowMessageEvent("输入无效，请在页码和起始数字字段中输入有效的数字。", Message.MessageType.ERROR));
        }
    }

    private void addRuleToView(PageLabelRule rule) {
        HBox ruleBox = new HBox(10);
        ruleBox.setAlignment(Pos.CENTER_LEFT);

        String text = String.format("从第 %d 页开始: 样式=%s, 前缀='%s', 起始于=%d",
                rule.fromPage(), rule.styleString(), rule.prefix(), rule.start());
        Label ruleLabel = new Label(text);
        HBox.setHgrow(ruleLabel, Priority.ALWAYS);
        ruleLabel.setMaxWidth(Double.MAX_VALUE);

        Button deleteButton = new Button();
        deleteButton.getStyleClass().addAll("graph-button","graph-button-important");
        deleteButton.setPrefWidth(30);
        Node graphic = new Region();
        graphic.getStyleClass().addAll("icon", "delete-item-icon");
        deleteButton.setGraphic(graphic);
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
            appEventBus.post(new ShowMessageEvent("请先选择一个PDF文件", Message.MessageType.WARNING));
            return;
        }

        // ==============================================
        // 我们只需要为每条规则的 *起始页* 创建一个 PageLabel 条目。

        // 使用 Map (特别是 TreeMap) 可以确保对于同一个起始页，后添加的规则会覆盖先添加的规则。
        Map<Integer, PageLabel> pageLabelsMap = new TreeMap<>();
        for (PageLabelRule rule : pageLabelRules) {
            // 对于每条规则，只为其起始页创建一个 PageLabel 对象。
            // pageNum: 新编号开始的物理页码 (即 rule.fromPage())。
            // style: 这个区段的编号样式。
            // prefix: 这个区段的前缀。
            // firstPage: 起始页应当具有的逻辑页码 (即 rule.start())。
            PageLabel pageLabel = new PageLabel(rule.fromPage(), rule.style(), rule.prefix(), rule.start());

            // Map 的键 (key) 就是规则开始的物理页码。
            pageLabelsMap.put(rule.fromPage(), pageLabel);
        }

        // 最终的列表就是这些唯一的、按页码排序的起始页规则的集合。
        List<PageLabel> finalPageLabels = new ArrayList<>(pageLabelsMap.values());
        // ==============================================


        String srcFilePath = fileService.getSrcFile().toString();
        String destFilePath = fileService.getDestFile().toString();
        try {
            pdfPageLabelService.setPageLabels(srcFilePath, destFilePath, finalPageLabels);
            appEventBus.post(new ShowMessageEvent("页码标签已成功应用。", Message.MessageType.SUCCESS));
        } catch (IOException e) {
            appEventBus.post(new ShowMessageEvent("应用页码标签失败: " + "e.getMessage()", Message.MessageType.ERROR));
            throw new RuntimeException(e);
        }
    }

    private PageLabel.PageLabelNumberingStyle getNumberingStyle(String styleString) {
        // 在 switch 语句中使用常量，确保逻辑和定义一致
        return switch (styleString) {
            case STYLE_DECIMAL -> PageLabel.PageLabelNumberingStyle.DECIMAL_ARABIC_NUMERALS;
            case STYLE_ROMAN_LOWER -> PageLabel.PageLabelNumberingStyle.LOWERCASE_ROMAN_NUMERALS;
            case STYLE_ROMAN_UPPER -> PageLabel.PageLabelNumberingStyle.UPPERCASE_ROMAN_NUMERALS;
            case STYLE_LETTERS_LOWER -> PageLabel.PageLabelNumberingStyle.LOWERCASE_LETTERS;
            case STYLE_LETTERS_UPPER -> PageLabel.PageLabelNumberingStyle.UPPERCASE_LETTERS;
            default -> null; // STYLE_NONE 和其他未知情况都返回 null
        };
    }
}