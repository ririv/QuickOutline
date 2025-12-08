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

## 解决方案一：强制 Widget DOM 复用 (已实施)

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

修复后，CodeMirror 在更新 Decorations 时会尽可能地保留并复用 `BulletWidget` 对应的 DOM 元素。DOM 结构保持稳定，减少了不必要的 DOM 抖动。浏览器光标渲染恢复正常，光标残影问题得以缓解或消除。

---

## 解决方案二：使用 CodeMirror 自定义光标绘制 (drawSelection)

即使进行了 Widget DOM 复用优化，在某些极端场景或浏览器环境下（尤其是在 Widget 边界进行操作时），浏览器原生光标绘制仍然可能出现微小的伪影。CodeMirror 6 提供了更彻底的解决方案：完全接管光标的绘制。

### 场景：数学公式的光标残影

这个问题不仅出现在列表中，在实现 **即时渲染数学公式**（如 `$E=mc^2$`）时也会遇到，且更为棘手。

**场景**：
1.  用户输入 `$x$`，编辑器立即将其渲染为一个包含 KaTeX 公式的 Widget (`<span>...</span>`)。
2.  用户按下 `Backspace` 删除末尾的 `$`。
3.  CodeMirror 检测到语法不再匹配 `InlineMath`，于是决定**移除**该 Widget，恢复显示普通文本 `$x`。

**问题**：
由于 KaTeX 渲染生成的 DOM 结构非常复杂（包含多层嵌套的 span, svg 等），当这个复杂的 Widget 在一瞬间被从文档流中移除时，浏览器（尤其是 Webkit/Blink 内核）的光标重绘逻辑（Caret Painting）容易出现严重的滞后或计算错误，导致屏幕上残留一个“不闪烁的旧光标”，同时显示一个新的光标。

**解决方案**：
对于这种 **Widget 被销毁**（而非复用）导致的残影，`eq()` 方法无法解决（因为没有 Widget 可以复用了）。唯一的、彻底的解决方案是启用 **`drawSelection()`** 扩展。

通过 `drawSelection()`，CodeMirror 完全接管了光标和选区的绘制，不再依赖浏览器不稳定的原生光标渲染，从而彻底消除了此类残影。

### 原理

`drawSelection()` 扩展会禁用浏览器原生的光标，并使用 CodeMirror 自己的 DOM 元素来模拟绘制光标和选区。由于这些 DOM 元素是由 CodeMirror 精确控制的，它们能够更好地与编辑器的 Decorations 和 Widget 协调工作，从而彻底避免原生浏览器光标的渲染 Bug。

### 修复代码

在 `src/lib/editor/index.ts` 中，将 `drawSelection()` 扩展添加到编辑器的配置中：

```typescript
import { drawSelection } from '@codemirror/view'; // 引入 drawSelection

// ...

export class MarkdownEditor {
    // ...
    constructor(options: MarkdownEditorOptions) {
        const startState = EditorState.create({
            // ...
            extensions: [
                // ... 其他扩展 ...
                drawSelection(), // 启用 CodeMirror 自定义光标绘制
                // ...
            ]
        });
        // ...
    }
}
```

### 效果

通过 `drawSelection()`，CodeMirror 不再依赖浏览器原生的光标绘制，而是使用其高度优化的 DOM 模拟方案。这彻底解决了因浏览器渲染机制导致的各种光标残影问题，使得光标在复杂 Widget 场景下始终保持稳定和精准。

---

## 最佳实践总结

在 CodeMirror 6 中编写自定义 `WidgetType` 时，**务必仔细考虑并实现 `eq()` 方法**（减少 DOM 抖动）。同时，**启用 `drawSelection()` 扩展**是解决各类光标渲染问题（包括残影）的“银弹”，它确保了复杂编辑器场景下的光标视觉稳定性。