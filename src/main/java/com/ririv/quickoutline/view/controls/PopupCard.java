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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 一个功能强大且可配置的弹出卡片组件。
 *
 * <p><b>设计理念:</b></p>
 * <p>
 * 此类的设计遵循“关注点分离”原则，将“要显示的内容”与“触发显示它的节点”分离开来。
 * </p>
 * <ul>
 *   <li><b>内容 (Content):</b> 在构造函数中传入 ({@code new PopupCard(content)})，它定义了弹窗内部应该显示什么。</li>
 *   <li><b>触发器 (Trigger):</b> 通过 {@link #attachTo(Node)} 方法附加，它定义了哪个UI节点将负责触发弹窗的显示和隐藏。</li>
 * </ul>
 *
 * <p><b>基本用法:</b></p>
 * <pre>{@code
 *   // 1. 创建弹窗要显示的内容
 *   Label popupContent = new Label("这是一个弹窗！");
 *   popupContent.setPadding(new Insets(10));
 *
 *   // 2. 创建 PopupCard 实例
 *   PopupCard card = new PopupCard(popupContent);
 *
 *   // 3. 配置行为 (可选)
 *   card.setTriggers(PopupCard.TriggerType.DELAYED_ON_HOVER); // 设置为延迟显示
 *   card.setPosition(PopupCard.PopupPosition.RIGHT_OF); // 设置位置
 *   card.setHideDelay(Duration.millis(1)); // 设置为准立即隐藏
 *
 *   // 4. 将其附加到一个触发节点上
 *   Button myButton = new Button("悬浮在我上面");
 *   card.attachTo(myButton);
 * }</pre>
 *
 * @see #attachTo(Node)
 * @see #setTriggers(TriggerType...)
 * @see #setPosition(PopupPosition)
 */
public class PopupCard extends Popup {
    private static final Logger logger = LoggerFactory.getLogger(PopupCard.class);

    //--- 核心属性 ---
    private final Parent contentNode; // 弹窗显示的内容节点
    private Node ownerNode;           // 触发此弹窗的UI节点

    //--- 行为配置 ---
    private final Set<TriggerType> triggers = new HashSet<>(); // 激活的触发器集合
    private PopupPosition position = PopupPosition.TOP_CENTER;   // 弹出位置

    //--- 内部状态与计时器 ---
    private final PauseTransition showTimer; // 控制延迟显示的计时器
    private final PauseTransition hideTimer; // 控制延迟隐藏的计时器

    /**
     * 触发器类型枚举
     */
    public enum TriggerType {
        INSTANT_ON_HOVER, // 悬浮时立即显示
        DELAYED_ON_HOVER, // 悬浮一段时间后延迟显示
        CTRL_ON_HOVER     // 按住Ctrl(Command)并悬浮时立即显示
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
     * 将弹窗逻辑附加到目标节点上。这是使用此组件的主要入口。
     * @param node 目标节点
     */
    public void attachTo(Node node) {
        this.ownerNode = node;
        // 核心逻辑：为目标节点和弹窗内容本身都添加鼠标事件监听
        // 这样可以确保鼠标在两者之间移动时，弹窗不会意外消失
        addHoverListeners(this.ownerNode);
        addHoverListeners(this.contentNode);
    }

    /**
     * 设置一个或多个触发器类型。
     * @param types 一个或多个TriggerType
     */
    public void setTriggers(TriggerType... types) {
        this.triggers.clear();
        this.triggers.addAll(Arrays.asList(types));
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
        node.addEventHandler(MouseEvent.MOUSE_ENTERED, this::handleMouseEntered);
        node.addEventHandler(MouseEvent.MOUSE_EXITED, this::handleMouseExited);
    }

    /**
     * 处理鼠标进入事件（进入触发节点或弹窗内容时调用）
     */
    private void handleMouseEntered(MouseEvent event) {
        // 核心：立刻停止任何待隐藏的计划
        stopHideTimer();

        // 如果弹窗已显示，或“显示”计划已在进行中，则无需任何操作
        if (isShowing() || showTimer.getStatus() == javafx.animation.Animation.Status.RUNNING) {
            return;
        }

        // 检查是否满足任何一个立即显示的触发条件
        boolean shouldShowInstantly = (triggers.contains(TriggerType.CTRL_ON_HOVER) && event.isShortcutDown()) ||
                                      triggers.contains(TriggerType.INSTANT_ON_HOVER);

        if (shouldShowInstantly) {
            display();
        } else if (triggers.contains(TriggerType.DELAYED_ON_HOVER)) {
            // 如果是延迟显示模式，则启动显示计时器
            showTimer.setOnFinished(e -> display());
            showTimer.playFromStart();
        }
    }

    /**
     * 处理鼠标移出事件（移出触发节点或弹窗内容时调用）
     */
    private void handleMouseExited(MouseEvent event) {
        // 核心：立刻停止任何待显示的计划
        stopShowTimer();
        // 并启动“延迟隐藏”计划
        hideAfterDelay();
    }

    /**
     * 执行显示和定位的核心逻辑
     */
    private void display() {
        if (isShowing() || ownerNode == null) return;

        // 技巧：使用 setOnShown 来确保在弹窗完成渲染、尺寸已知后，再进行定位
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

        // 调用父类的show方法来显示窗口
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