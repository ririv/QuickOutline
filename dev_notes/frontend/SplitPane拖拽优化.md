# SplitPane 拖拽优化总结 (解决 WebView/iframe 交互干扰)

## 1. 问题背景

在 JavaFX WebView 环境中，实现 `SplitPane`（如编辑器与预览分栏）的拖拽调整大小时，遇到了一个典型问题：

1.  **拖拽中断**：当用户点击分割线（Resizer）开始拖拽，但鼠标移动速度过快，离开了分割线区域，进入了旁边的编辑器（Vditor 通常基于 `iframe` 或复杂 DOM 结构）或预览区域时，拖拽事件链会中断。
2.  **意外选中文字**：拖拽过程中，鼠标进入编辑器或预览区域后，可能会触发这些区域的默认文本选中行为，导致出现蓝色的选中框，严重干扰用户体验。
3.  **光标异常**：拖拽过程中，鼠标光标可能从 `col-resize` 变为默认光标（`arrow` 或 `text`），与拖拽操作不符。

这些问题使得 WebView 中的 `SplitPane` 体验不如原生应用，也与 VS Code 等成熟编辑器存在差距。

## 2. 问题根源分析

上述问题是由于浏览器事件模型和 CSS 属性的默认行为共同导致的：

*   **事件捕获**：当鼠标事件发生时，浏览器会从根元素向下传播事件（捕获阶段），再从目标元素向上冒泡（冒泡阶段）。如果鼠标进入了编辑器或预览区域，这些区域的 DOM 元素可能会捕获或响应 `mousemove` 事件，阻止它继续被 `SplitPane` 的 `handleMouseMove` 函数处理。
*   **`iframe` 的事件隔离**：Vditor 或其他富文本编辑器如果使用 `iframe` 实现，`iframe` 内部的文档是独立的上下文，它会完全捕获内部的鼠标事件，外部的 JavaScript 很难直接穿透管理。
*   **`user-select` 和 `pointer-events`**：默认情况下，文本是可选择的 (`user-select: text;`)，元素可以响应鼠标事件 (`pointer-events: auto;`)。当进行拖拽时，这些默认行为会产生干扰。

## 3. VS Code 及通用解决方案

VS Code 等高性能应用在处理这类拖拽问题时，采用的策略是**“全局遮罩（Overlay）”**或**“全局事件禁用”**：

在拖拽开始时：
1.  在整个窗口的**最顶层**（通常是 `body` 元素），添加一个 CSS 类或临时元素。
2.  这个类或元素会强制禁用所有**底层元素**的鼠标事件 (`pointer-events: none;`) 和文本选中行为 (`user-select: none;`)。
3.  同时，将 `cursor` 样式设置为与拖拽操作一致（如 `col-resize`），并应用于 `body`。

这样，在拖拽过程中，所有鼠标事件都被最顶层的 `body` 或遮罩层统一接管，确保拖拽逻辑能持续、流畅地进行，而底层内容不会响应鼠标，也不会出现意外的选中。

在拖拽结束时，移除这些全局样式或遮罩。

## 4. `QuickOutline` 中的实现与优化

我们在重构后的 Svelte 项目中，也采用了类似的机制来解决此问题：

### a. `web/src/components/SplitPane.svelte` 的变更

*   **`startResize(e: MouseEvent)` 函数**：
    *   `document.body.classList.add('is-resizing');`
    *   当用户点击分割线开始拖拽时，为 `body` 元素添加一个 `is-resizing` 的 CSS 类。
*   **`stopResize()` 函数**：
    *   `document.body.classList.remove('is-resizing');`
    *   拖拽结束后，移除 `is-resizing` 类。

### b. `web/src/assets/global.css` 的变更

我们添加了以下全局 CSS 规则：

```css
/* --- 拖拽时的全局保护层 --- */
/* 当 SplitPane 正在调整大小时，禁用页面上所有元素的交互 */
/* 这防止了 iframe (编辑器) 吞噬鼠标事件，也防止了意外选中文字 */
body.is-resizing {
    cursor: col-resize !important; /* 统一光标样式 */
}

body.is-resizing * {
    pointer-events: none !important; /* 禁用所有子元素鼠标交互 */
    user-select: none !important;   /* 禁用所有子元素文本选中 */
}
```
*   `.is-resizing` 类生效时，`body` 的光标会变为 `col-resize`。
*   `body.is-resizing *` 规则以 `!important` 确保了最高优先级，强制禁用所有子元素的鼠标事件和文本选中，有效地创建了一个“不可触碰”的全局遮罩。

### c. `web/src/components/SplitPane.svelte` 和 `global.css` 的其他优化

*   **`SplitPane.svelte` 中 `resizer` 元素的 `cursor` 样式**：
    ```css
    .resizer {
        width: 5px;
        background: #ddd;
        cursor: col-resize; /* 确保悬浮时光标正常 */
        z-index: 10;
        transition: background 0.2s;
    }
    ```
    这条规则确保了鼠标在悬停到分割线上时，即使没有开始拖拽，光标也会显示为 `col-resize`。
*   **移除冗余的 `user-select: none`**：在 `global.css` 中移除了 `body { user-select: none; }` 的规则，因为它与 `body.is-resizing *` 冲突，且在非拖拽模式下，我们希望编辑器和预览区域是可选中的。

### 5. 最终效果

通过这些优化，当用户拖动 `SplitPane` 的分割线时：
1.  鼠标光标会统一显示为 `col-resize`。
2.  拖拽过程流畅，不会被编辑器或预览区域的事件捕获中断。
3.  不会出现意外的文本选中。

这极大地提升了 `SplitPane` 的用户体验，使其达到了桌面应用应有的流畅和可靠性。

## 6. 启示

在 WebView/Electron 这种混合应用中，处理复杂 UI 交互时，有时需要结合 JS 状态管理和全局 CSS 规则，以弥补浏览器在原生桌面体验方面的不足。对于 `iframe` 内的复杂组件（如 Vditor），这种全局事件阻断机制是行之有效的方案。