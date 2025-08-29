package com.ririv.quickoutline.view.controls;

import javafx.animation.PauseTransition;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.input.MouseEvent;
import javafx.stage.Popup;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 一个功能强大且可配置的弹出卡片组件。
 * 支持立即显示或延迟显示，并能处理鼠标在触发节点和弹窗内容之间的移动，避免意外关闭。
 */
public class PopupCard extends Popup {
    private static final Logger logger = LoggerFactory.getLogger(PopupCard.class);

    // 定时器
    private final PauseTransition showTimer; // 控制延迟显示的计时器
    private final PauseTransition hideTimer; // 控制延迟隐藏的计时器

    // 属性
    private final Parent contentNode; // 弹窗显示的内容
    private Node ownerNode; // 触发此弹窗的节点
    private TriggerMode triggerMode = TriggerMode.INSTANT_ON_HOVER; // 默认触发模式
    private PopupPosition position = PopupPosition.TOP_CENTER;   // 默认弹出位置

    /**
     * 触发模式枚举
     */
    public enum TriggerMode {
        INSTANT_ON_HOVER, // 悬浮时立即显示
        DELAYED_ON_HOVER  // 悬浮一段时间后延迟显示
    }

    /**
     * 弹窗的显示位置枚举
     */
    public enum PopupPosition {
        TOP_CENTER, // 在宿主节点上方居中
        RIGHT_OF    // 在宿主节点右侧
    }

    public PopupCard(Parent content) {
        this.contentNode = content;
        this.contentNode.getStylesheets().add(getClass().getResource("PopupCard.css").toExternalForm());
        this.contentNode.getStyleClass().add("card");
        this.getContent().add(this.contentNode);
        this.setAutoHide(true); // 点击外部时自动隐藏

        // 初始化默认延迟时间
        this.showTimer = new PauseTransition(Duration.seconds(2));
        this.hideTimer = new PauseTransition(Duration.seconds(0.3));
    }

    // --- 公共API ---

    /**
     * 将弹窗逻辑附加到目标节点上。
     * 这是使用此组件的主要入口。
     * @param node 目标节点
     */
    public void attachTo(Node node) {
        this.ownerNode = node;
        // 核心逻辑：为目标节点和弹窗内容本身都添加鼠标事件监听
        // 这样可以确保鼠标在两者之间移动时，弹窗不会意外消失
        addHoverListeners(this.ownerNode);
        addHoverListeners(this.contentNode);
    }

    public void setTriggerMode(TriggerMode mode) {
        this.triggerMode = mode;
    }

    public void setPosition(PopupPosition position) {
        this.position = position;
    }

    public void setShowDelay(Duration duration) {
        this.showTimer.setDuration(duration);
    }

    public void setHideDelay(Duration duration) {
        this.hideTimer.setDuration(duration);
    }

    // --- 私有方法 ---

    private void addHoverListeners(Node node) {
        node.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> handleMouseEntered());
        node.addEventHandler(MouseEvent.MOUSE_EXITED, e -> handleMouseExited());
    }

    private void handleMouseEntered() {
        // 鼠标进入时，最主要的操作是取消任何将要执行的“隐藏”计划
        stopHideTimer();

        // 如果弹窗已显示，或“显示”计划已在进行中，则无需任何操作
        if (isShowing() || showTimer.getStatus() == javafx.animation.Animation.Status.RUNNING) {
            return;
        }

        // 根据不同的模式，执行显示逻辑
        if (triggerMode == TriggerMode.DELAYED_ON_HOVER) {
            showTimer.setOnFinished(e -> display());
            showTimer.playFromStart();
        } else { // INSTANT_ON_HOVER
            display();
        }
    }

    private void handleMouseExited() {
        // 鼠标移出时，取消任何将要执行的“显示”计划
        stopShowTimer();
        // 并启动“隐藏”计划
        hideAfterDelay();
    }

    private void display() {
        if (isShowing() || ownerNode == null) return;

        // 使用 setOnShown 来确保在弹窗完成渲染、尺寸已知后，再进行定位
        this.setOnShown(e -> {
            Bounds nodeBounds = ownerNode.localToScreen(ownerNode.getBoundsInLocal());
            double x, y;

            if (position == PopupPosition.TOP_CENTER) {
                x = nodeBounds.getCenterX() - this.getWidth() / 2;
                y = nodeBounds.getMinY() - this.getHeight() - 5;
            } else { // RIGHT_OF
                x = nodeBounds.getMaxX() + 5;
                y = nodeBounds.getMinY();
            }
            this.setX(x);
            this.setY(y);
        });

        super.show(ownerNode.getScene().getWindow());
    }

    private void hideAfterDelay() {
        hideTimer.setOnFinished(event -> this.hide());
        hideTimer.playFromStart();
    }

    private void stopShowTimer() {
        showTimer.stop();
    }

    private void stopHideTimer() {
        hideTimer.stop();
    }
}
