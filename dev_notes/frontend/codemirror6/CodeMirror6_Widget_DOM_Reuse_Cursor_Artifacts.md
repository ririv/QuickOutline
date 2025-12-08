# CodeMirror 6 Widget DOM 复用缺失导致的光标伪影问题记录 (Cursor Artifacts)

**日期**: 2025年12月8日
**环境**: CodeMirror 6, Svelte, Tauri (WebKit/Blink)

## 问题描述

在实现 Markdown 列表的即时渲染（Live Preview）时，使用了 `WidgetType` 替换列表标记（如 `- `）。当用户在列表项前按下 `Backspace` 退格键时，虽然内容被正确删除，但浏览器界面上会出现一个“光标残影”（或“幽灵光标”）现象：
1.  一个不闪烁的光标停留在旧的位置。
2.  一个新的、正常闪烁的光标出现在当前光标位置。
3.  页面上同时存在视觉上的两个光标，直到进行其他操作强制重绘。

## 原因分析：DOM 抖动与 Widget 的 `eq()` 方法

这是一个经典的 **DOM Thrashing (DOM 抖动)** 导致的浏览器渲染引擎 Bug，其根源在于 CodeMirror 6 的 `WidgetType` 没有正确地被复用。

1.  **Widget 实现缺陷**：
    最初的 `BulletWidget` **没有实现 `eq(other)` 方法**。
    ```typescript
    export class BulletWidget extends WidgetType {
        toDOM() { ... }
        // 缺少 eq(other: BulletWidget) 方法
    }
    ```

2.  **CodeMirror 的更新机制与 `eq()` 方法**：
    *   CodeMirror 6 在每次文档内容或 Decorator（装饰器，包括 Widget）发生变化后，会重新计算并应用所有 Decorators。
    *   `WidgetType` 的 `eq(other: WidgetType)` 方法用于告诉 CodeMirror，当前的 Widget 实例是否和上次的 Widget 实例“相等”。
    *   如果 `eq()` 方法返回 `false`（或者未实现，默认行为是比较对象引用，即 `this !== other`），CodeMirror 会认为这是一个全新的 Widget。
    *   **后果**：CodeMirror 会强制进行 DOM 操作：销毁旧的 Widget DOM 元素，然后创建一个新的 DOM 元素并重新插入到文档中。

3.  **浏览器渲染引擎的渲染伪影**：
    *   当光标紧挨着一个正在被“销毁并立即重建”的 DOM 元素时，浏览器的渲染引擎（尤其是 Webkit/Blink 内核）在处理光标绘制（Caret Painting）时可能会出现 Bug。
    *   旧的光标层（Layer）没有被及时清除，而新的光标层已经绘制出来，导致页面上出现视觉上的“光标分身”或“残影”。

## 解决方案：强制 Widget DOM 复用

通过为 `BulletWidget` 实现 `eq()` 方法，显式告诉 CodeMirror 只要 Widget 的类型和属性相同，就**复用现有 DOM 元素**。

### 修复代码

在 `src/lib/editor/widgets.ts` 中，为 `BulletWidget` 添加 `eq()` 方法：

```typescript
export class BulletWidget extends WidgetType {
    // 关键修复：告诉 CodeMirror 所有的无状态圆点 Widget 都是相同的，请复用 DOM！
    // 这样每次更新时，CodeMirror 会直接复用已存在的 <span>•</span> DOM 元素，而不会销毁重建。
    eq(other: BulletWidget) { 
        return true; 
    }

    ignoreEvent() { return false; }

    toDOM() {
        const span = document.createElement("span");
        span.className = "cm-bullet-widget";
        span.textContent = "•";
        // ... 样式 ...
        return span;
    }
}
```

### 效果

修复后，CodeMirror 在更新 Decorations 时会尽可能地保留并复用 `BulletWidget` 对应的 DOM 元素。DOM 结构保持稳定，减少了不必要的 DOM 抖动。浏览器光标渲染恢复正常，光标残影问题彻底消失。

## 最佳实践总结

在 CodeMirror 6 中编写自定义 `WidgetType` 时，**务必仔细考虑并实现 `eq()` 方法**。对于无状态或状态稳定的 Widget，返回 `true` 可以显著提升性能，减少 DOM 操作，并避免各类由 DOM 抖动导致的渲染伪影和 Bug。此方法是实现高性能、稳定 CodeMirror 自定义渲染的关键。
