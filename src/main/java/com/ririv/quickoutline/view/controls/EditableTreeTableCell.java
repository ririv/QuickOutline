package com.ririv.quickoutline.view.controls;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.util.Callback;
import javafx.util.StringConverter;
import javafx.util.converter.DefaultStringConverter;

/**
 * 自定义TreeTableCell，解决编辑时UI不一致性问题
 * 主要特点：
 * 1. 编辑时不改变单元格大小
 * 2. 编辑框样式与普通显示保持一致
 * 3. 支持Enter确认、Esc取消编辑
 */
public class EditableTreeTableCell<S, T> extends TreeTableCell<S, T> {
    
    private TextField textField;
    private StringConverter<T> converter;
    private final ChangeListener<Boolean> focusListener = this::onFocusChanged;
    
    public EditableTreeTableCell() {
        this(null);
    }
    
    public EditableTreeTableCell(StringConverter<T> converter) {
        this.converter = converter != null ? converter : (StringConverter<T>) new DefaultStringConverter();
        getStyleClass().add("editable-tree-table-cell");
        
        // 加载专门的CSS样式文件
        String cssPath = getClass().getResource("EditableTreeTableCell.css").toExternalForm();
        getStylesheets().add(cssPath);
        
        // 设置编辑行为
        setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && !isEmpty() && isEditable()) {
                TreeTableView<S> tableView = getTreeTableView();
                TreeTableColumn<S, T> column = getTableColumn();
                if (tableView != null && column != null && 
                    tableView.isEditable() && column.isEditable()) {
                    startEdit();
                }
            }
        });
    }
    
    public static <S> Callback<TreeTableColumn<S, String>, TreeTableCell<S, String>> forTreeTableColumn() {
        return forTreeTableColumn(new DefaultStringConverter());
    }
    
    public static <S, T> Callback<TreeTableColumn<S, T>, TreeTableCell<S, T>> forTreeTableColumn(
            final StringConverter<T> converter) {
        return list -> new EditableTreeTableCell<>(converter);
    }
    
    @Override
    public void startEdit() {
        TreeTableView<S> tableView = getTreeTableView();
        TreeTableColumn<S, T> column = getTableColumn();
        
        if (!isEditable() || tableView == null || column == null || 
            !tableView.isEditable() || !column.isEditable()) {
            return;
        }
        
        super.startEdit();
        
        if (isEditing()) {
            if (textField == null) {
                textField = createTextField();
            }
            
            textField.setText(converter.toString(getItem()));
            setText(null);
            setGraphic(textField);
            
            // 严格控制编辑器高度
            textField.setPrefHeight(20);
            textField.setMaxHeight(20);
            textField.setMinHeight(20);
            
            // 延迟聚焦以确保渲染完成
            javafx.application.Platform.runLater(() -> {
                textField.requestFocus();
                textField.selectAll();
            });
        }
    }
    
    @Override
    public void cancelEdit() {
        super.cancelEdit();
        
        setText(converter.toString(getItem()));
        setGraphic(null);
        
        if (textField != null) {
            textField.focusedProperty().removeListener(focusListener);
        }
    }
    
    @Override
    public void updateItem(T item, boolean empty) {
        super.updateItem(item, empty);
        
        if (empty) {
            setText(null);
            setGraphic(null);
        } else {
            if (isEditing()) {
                if (textField != null) {
                    textField.setText(converter.toString(item));
                }
                setText(null);
                setGraphic(textField);
            } else {
                setText(converter.toString(item));
                setGraphic(null);
            }
        }
    }
    
    private TextField createTextField() {
        TextField textField = new TextField();
        
        // 设置样式类，使其与普通显示保持一致
        textField.getStyleClass().addAll("editable-tree-table-cell-editor");
        
        // 移除默认的边框和背景，保持与单元格一致的外观
        textField.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-border-color: transparent;" +
            "-fx-border-width: 0;" +
            "-fx-background-insets: 0;" +
            "-fx-padding: 0;" +
            "-fx-font-size: inherit;" +
            "-fx-font-family: inherit;" +
            "-fx-pref-height: 20px;" +
            "-fx-max-height: 20px;" +
            "-fx-min-height: 20px;"
        );
        
        // 键盘事件处理
        textField.setOnKeyPressed(this::onKeyPressed);
        
        // 失去焦点时提交编辑
        textField.focusedProperty().addListener(focusListener);
        
        return textField;
    }
    
    private void onKeyPressed(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            commitEdit();
            event.consume();
        } else if (event.getCode() == KeyCode.ESCAPE) {
            cancelEdit();
            event.consume();
        }
    }
    
    private void onFocusChanged(ObservableValue<? extends Boolean> obs, Boolean wasFocused, Boolean isNowFocused) {
        if (!isNowFocused && isEditing()) {
            commitEdit();
        }
    }
    
    private void commitEdit() {
        if (!isEditing()) {
            return;
        }
        
        try {
            T newValue = converter.fromString(textField.getText());
            super.commitEdit(newValue);
        } catch (Exception e) {
            // 转换失败时取消编辑
            cancelEdit();
        }
    }
}