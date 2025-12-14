# JavaFX WebView 中 Tab 键处理与焦点控制问题总结

## 1. 问题描述

在将 TOC 编辑功能迁移至 Svelte Web 组件 `SimpleEditor.svelte` 后，遇到了一个棘手的键盘事件处理问题：当用户在编辑器（`<textarea>`）中按下 `Tab` 键时，预期是插入缩进，但实际效果是焦点跳到了 Web 页面内部的下一个可输入框（例如“Offset”输入框），而不是在编辑器内部处理 `Tab` 键。

## 2. 问题根源及排查过程

### a. 初步判断：JS `e.preventDefault()` 未生效

`SimpleEditor.svelte` 中的 `handleKeydown` 函数已明确调用 `e.preventDefault()` 来阻止 `Tab` 键的默认行为（焦点切换）。然而，焦点仍然跳转，这强烈暗示 `e.preventDefault()` 要么没有被执行，要么没有成功阻止默认行为。

### b. 调试发现：`e.key` 属性在 WebView 中可能不标准

通过在 `handleKeydown` 中添加日志（`window.debugBridge.log`），发现 `Keydown:` 后面的 `e.key` 值为空。这说明在 JavaFX 的 WebView 环境下，`KeyboardEvent.key` 属性对于 `Tab` 键可能存在兼容性问题，未正确传递标准值 `"Tab"`。

### c. JavaFX Focus Traversal 的干扰

最初怀疑是 JavaFX 自身的焦点遍历机制（Focus Traversal）在底层拦截了 `Tab` 键，阻止其传递到 WebView 内部的 JavaScript 层。我们尝试在 JavaFX 侧添加 `addEventHandler(KeyEvent.KEY_PRESSED, event -> event.consume())` 来阻止 JavaFX 的焦点切换。

### d. 关键洞察：焦点未跳出 WebView，而是在内部跳转

用户反馈焦点是跳到了 Web 页面内部的“Offset”编辑框，而不是跳出 WebView 到 JavaFX 的原生控件。这提供了决定性的证据：
*   问题并非 JavaFX 劫持了焦点，而是焦点在 WebView **内部**跳转。
*   这意味着 WebKit (WebView 的内核) 收到了 `Tab` 键事件，但 JS 层的 `e.preventDefault()` 没能阻止其内部的默认焦点遍历行为。
*   JavaFX 层的 `event.consume()` 反而可能进一步干扰了 WebKit 内部事件的正常分发，使其无法传递给 JS。

## 3. 最终解决方案与原理

通过迭代和验证，最终的解决方案是移除 JavaFX 端的干预，并强化前端 JS 侧的兼容性判断。

### a. 移除 JavaFX 对 `Tab` 键的拦截

`TocGeneratorTabController.java` 中移除以下代码块：

```java
// 移除这一段，让 Tab 键事件能完全传递到 WebView 内部
// previewWebView.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
//     if (event.getCode() == KeyCode.TAB) {
//         event.consume();
//     }
// });
```
**原理**：既然焦点在 WebView 内部跳转，问题出在 Web 层。JavaFX 的拦截可能干扰了 WebKit 的事件分发，阻止了 JS 的 `preventDefault()` 生效。让 JavaFX 不再干预，确保事件能完整地传递给 WebKit。

### b. 强化前端 `SimpleEditor.svelte` 的 `Tab` 键识别

在 `SimpleEditor.svelte` 的 `handleKeydown` 函数中，修改 `Tab` 键的判断逻辑，使其更具兼容性：

```typescript
// 原始：if (e.key === 'Tab')
// 修复后：
if (e.key === 'Tab' || e.code === 'Tab' || e.keyCode === 9) {
    e.preventDefault();
    // ... 缩进逻辑 ...
}
```
**原理**：通过增加对 `e.code` 和 `e.keyCode` 的检查，确保即使 `e.key` 在 JavaFX WebView 中表现不标准，也能正确识别 `Tab` 键，并执行 `e.preventDefault()`。`e.keyCode === 9` 是 `Tab` 键的传统按键码，兼容性广。

### c. 修正事件绑定方式

将 `SimpleEditor.svelte` 中的 `onMount` 手动 `addEventListener` 方式**改回** Svelte 5 推荐的**属性绑定方式 `onkeydown={handleKeydown}`**。

**原理**：Svelte 的属性绑定 (`onkeydown={...}`) 旨在与组件生命周期和响应式系统更好地集成，并且是 Svelte 5 默认且推荐的事件处理方式。虽然 `addEventListener` 更底层，但在 Svelte 的上下文中使用属性绑定通常更可靠，它能更好地与 Svelte 的内部机制协调。

## 4. 总结与启示

这个 Tab 键问题揭示了在混合应用（JavaFX WebView）开发中，跨技术栈的事件处理复杂性：

*   **平台差异性**：不同 WebView 实现（WebKit, Chromium）对 `KeyboardEvent` 对象的填充可能不尽相同。
*   **事件流模型**：原生应用框架（JavaFX）和嵌入的 Web 引擎（WebKit）各自有一套事件处理机制，两者之间的交互可能产生意想不到的行为。
*   **兼容性与防御性编程**：在 Web 层处理按键事件时，应考虑使用 `e.key`, `e.code`, `e.keyCode` 等多个属性进行判断，以提高兼容性。
*   **调试的挑战**：在无头或嵌入式浏览器环境中，传统的浏览器开发者工具可能受限，需要借助 Bridge 提供的日志工具进行调试。

通过这次修复，`SimpleEditor` 现在能够正确拦截并处理 `Tab` 键，提供了流畅的编辑体验。这一问题的解决，是混合应用开发中深入理解平台行为和事件机制的典型案例。