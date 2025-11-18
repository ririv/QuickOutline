# PopupCard 组件设计与使用文档

本文档旨在详细说明 `PopupCard` 组件的设计理念、功能、API及其内部实现的关键技术，以便于在项目中更好地复用和维护。

## 1. 组件目标

`PopupCard` 是一个功能强大且可配置的通用弹出卡片组件。它的目标是提供一个统一的解决方案，以处理各种需要“弹出”显示的场景，例如鼠标悬浮后延迟显示预览、或立即显示一个功能面板等。

## 2. 设计理念：关注点分离

`PopupCard` 的核心设计思想是**“关注点分离”**，它将“要显示的内容”与“触发其显示的UI元素”明确地区分开来。

-   **内容 (Content)**：在构造 `PopupCard` 实例时，通过构造函数 `new PopupCard(content)` 传入。它定义了弹窗内部应该**显示什么**。
-   **触发器 (Trigger)**：在创建实例后，通过 `attachTo(node)` 方法进行附加。它定义了**哪个UI节点**将负责触发弹窗的显示和隐藏。

这种分离的设计带来了巨大的灵活性和清晰性，使得组件的创建和使用在逻辑上完全解耦。

## 3. 主要功能

-   **多种触发模式**：支持“立即显示” (`INSTANT_ON_HOVER`) 和“延迟显示” (`DELAYED_ON_HOVER`) 两种模式。
-   **多种定位选项**：支持在触发节点的“上方居中” (`TOP_CENTER`) 或“右侧” (`RIGHT_OF`) 等多种位置弹出。
-   **健壮的隐藏逻辑**：完美处理了鼠标在“触发节点”和“弹窗内容”之间移动的场景，只有当鼠标完全离开这两个区域后，才会启动延迟隐藏计时器，避免了弹窗的意外关闭。
-   **可配置的延迟**：显示和隐藏的延迟时间均可通过 `setShowDelay()` 和 `setHideDelay()` 方法自定义。

## 4. API 与使用示例

### 主要API

-   `PopupCard(Parent content)`: 构造函数，传入弹窗要显示的内容。
-   `attachTo(Node node)`: 将弹窗的触发逻辑附加到指定的节点上。
-   `setTriggers(TriggerMode mode)`: 设置触发模式（立即/延迟）。
-   `setPosition(PopupPosition position)`: 设置弹出位置（上方/右侧）。
-   `setShowDelay(Duration duration)`: 设置显示延迟时间。
-   `setHideDelay(Duration duration)`: 设置隐藏延迟时间。

### 完整使用示例

```java
// 1. 创建一个节点作为弹窗的触发器
Button myButton = new Button("悬浮在我上面");

// 2. 创建弹窗要显示的内容
Label popupContent = new Label("这是一个延迟2秒后在右侧弹出的窗口！");
popupContent.setPadding(new Insets(20));

// 3. 创建 PopupCard 实例，并传入内容
PopupCard card = new PopupCard(popupContent);

// 4. 配置其行为
card.setTriggers(PopupCard.TriggerMode.DELAYED_ON_HOVER);
card.setPosition(PopupCard.PopupPosition.RIGHT_OF);
card.setShowDelay(Duration.seconds(2));

// 5. 将其附加到触发节点上
card.attachTo(myButton);
```

## 5. 核心实现机制详解

### 5.1. 健壮的悬浮逻辑：双重事件监听

为了实现“鼠标可以在触发节点和弹窗之间自由移动而弹窗不消失”的健壮体验，我们为 **触发节点 (`ownerNode`)** 和 **弹窗内容 (`contentNode`)** 同时添加了 `MOUSE_ENTERED` 和 `MOUSE_EXITED` 事件监听。

```java
// attachTo 方法是关键
public void attachTo(Node node) {
    this.ownerNode = node;
    addHoverListeners(this.ownerNode);   // 为触发节点添加监听
    addHoverListeners(this.contentNode); // 为弹窗内容也添加监听
}
```

-   当鼠标进入**任何一个**区域时，都会触发 `handleMouseEntered()`，此方法会**取消所有待执行的隐藏计时器**。
-   只有当鼠标同时离开了**两个**区域时，`handleMouseExited()` 才会最终成功启动隐藏计时器。

### 5.2. 状态管理：双计时器模型

内部通过两个 `PauseTransition` 实例来管理状态，避免了复杂的布尔标记位。

-   `showTimer`: 负责“延迟显示”。在 `handleMouseEntered` 中，如果模式是 `DELAYED_ON_HOVER`，则启动此计时器。计时结束后，调用 `display()` 方法显示弹窗。
-   `hideTimer`: 负责“延迟隐藏”。在 `handleMouseExited` 中启动。计时结束后，调用 `this.hide()` 关闭弹窗。

任何鼠标进入/移出事件都会先停止所有计时器 (`stopAllTimers()`)，然后再根据情况启动其中一个，这确保了状态的唯一和正确性。

### 5.3. 精确定位：`setOnShown` 技巧

`Popup` 在显示之前，其宽度和高度通常为0，直接获取会导致定位错误。为了解决此问题，我们利用了 `setOnShown` 事件。

```java
private void display() {
    if (isShowing() || ownerNode == null) return;

    // 1. 注册一个一次性的 setOnShown 事件监听器
    this.setOnShown(e -> {
        // 3. 此处的代码在弹窗已显示、尺寸已知后才执行
        Bounds nodeBounds = ownerNode.localToScreen(ownerNode.getBoundsInLocal());
        double x, y;
        // ... 根据 position 计算 x, y ...
        this.setX(x);
        this.setY(y);
    });

    // 2. 先调用 show 方法，触发上述监听器
    super.show(ownerNode.getScene().getWindow());
}
```
这个“先注册、后显示”的技巧，确保了我们总能基于弹窗的最终实际尺寸，来计算其精确的显示位置。