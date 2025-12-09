# CodeMirror 6 任务列表 (Task List) 的渲染架构与源码揭示方案

**创建日期:** 2025-12-09
**状态:** 已解决 (Solved)

本文档记录了在 QuickOutline 中实现 GFM 任务列表 (`- [ ] task`) 时遇到的复杂渲染冲突、CodeMirror 架构限制以及最终的解决方案。

## 1. 核心问题

在实现“所见即所得”的 Markdown 编辑器时，我们希望 `- [ ]` 被渲染为一个漂亮的复选框 (`<input type="checkbox">`)。

### 1.1 初始现象：重复渲染 (UI Conflict)
CodeMirror 的 Markdown 解析器 (Lezer) 将 `- [ ]` 解析为两个独立的节点：
*   `ListMark`: 对应字符 `-`
*   `TaskMarker`: 对应字符 `[ ]`

我们的装饰器 (`listProvider` 和 `taskListProvider`) 分别独立工作：
*   `listProvider`: 遇到 `ListMark` -> 渲染圆点 Widget (`•`)。
*   `taskListProvider`: 遇到 `TaskMarker` -> 渲染复选框 Widget (`☑`)。

**结果**: 用户看到的是 `• ☑ task`，即圆点和复选框同时存在。

### 1.2 架构限制：RangeSetBuilder 排序错误 (Crash)
为了解决上述 UI 冲突，我们尝试在 `taskListProvider` (处理 `[ ]` 时) 给整行添加一个 CSS 类名 `cm-task-list-item`，企图用 CSS 隐藏圆点。

**代码尝试**:
```typescript
// taskListProvider
builder.add(node.from, node.to, Decoration.replace(CheckboxWidget)); // Offset: 2
const line = state.doc.lineAt(node.from);
builder.add(line.from, line.from, Decoration.line({class: 'cm-task-list-item'})); // Offset: 0
```

**报错**: `Error: Ranges must be added sorted by from position and startSide`。
**原因**: CodeMirror 的 `RangeSetBuilder` 要求装饰必须严格按位置顺序添加。我们处理到了 Offset 2，就不能回头去添加 Offset 0 的装饰。

### 1.3 交互体验：源码揭示 (Source Reveal) 不一致
解决了渲染问题后，出现了“幽灵复选框”问题：
*   当光标移入 `[ ]` 区域时，复选框消失，显示源码。
*   但当光标左移，越过 `[ ]` 到达 `-` 和 `[` 之间的空格时，复选框又重新渲染出来了。
*   **期望**: 只要光标在标记区域（`- [ ]`），整个区域都应该显示源码，不应有 Widget 干扰。

---

## 2. 最终解决方案架构

我们采用了 **"三位一体"** 的分层处理方案，完美解决了上述所有问题。

### 层级 1: Block Decoration (处理行样式)
**职责**: 负责给任务列表项的整行添加 CSS 类名。
**Provider**: `taskListClassProvider`
**实现逻辑**:
*   作为 `BlockProvider` 运行（优先于 Inline）。
*   监听 `ListItem` 节点。
*   使用 `hasDescendant(node, 'TaskMarker')` 递归检查该 Item 是否包含任务标记。
*   如果是，添加 `Decoration.line({ class: 'cm-task-list-item' })`。
*   **解决问题**: 避开了 `RangeSetBuilder` 的排序限制（因为 ListItem 从行首开始）。

### 层级 2: Inline Decoration (处理 Widget 渲染与隐藏)
**职责**: 负责渲染 Widget，并决定何时“让路”给源码。
**Provider**: `listProvider` & `taskListProvider`

**核心逻辑 (Hotspot Detection)**:
为了实现 Typora 风格的交互，我们定义了一个 **"编辑敏感区 (Hotspot)"**：从 `ListItem` 的起始位置 (`-`) 到 `TaskMarker` 的结束位置 (`]`)。

*   **`listProvider`**:
    *   如果光标在 Hotspot 内 -> **不渲染**圆点 (显示源码 `-`)。
    *   如果光标在 Content 内 -> 渲染圆点。
*   **`taskListProvider`**:
    *   如果光标在 Hotspot 内 -> **不渲染**复选框 (显示源码 `[ ]`)。
    *   如果光标在 Content 内 -> 渲染复选框。

**代码片段**:
```typescript
// 寻找敏感区终点
let curr = node.node.nextSibling;
while (curr) {
    if (curr.name === 'TaskMarker') { hotspotEnd = curr.to; break; }
    // ...
}
// 判断光标
if (hasFocus && selection.from >= parent.from && selection.to <= hotspotEnd) {
     return; // 放弃渲染，显示源码
}
```

### 层级 3: CSS Override (处理视觉冲突)
**职责**: 当 Widget 被渲染出来时，确保只有一个 Widget 可见。
**样式**:
```css
/* 当一行被标记为任务列表时，强制隐藏普通的列表圆点 */
.cm-task-list-item .cm-bullet-widget {
    display: none;
}
```
**逻辑闭环**:
*   非编辑态：`listProvider` 渲染圆点，`taskListProvider` 渲染复选框。CSS 隐藏圆点。**用户只看到复选框。**
*   编辑态（光标在 Hotspot）：JS 逻辑阻止两个 Provider 渲染。**用户只看到源码 `- [ ]`。**

---

## 3. 关键经验 (Takeaways)

1.  **不要与 `RangeSetBuilder` 对抗**: 如果需要添加位置靠前的 Decoration，必须在遍历到那个位置之前就处理，或者将逻辑移到更上层的节点（如 `ListItem` 或 `Line`）处理。
2.  **CSS > JS**: 解决 Widget 冲突时，CSS (`display: none`) 往往比复杂的 JS 节点关系判断（`nextSibling` 可能会遇到意想不到的空格节点）更稳健。
3.  **Hotspot 概念**: 对于混合编辑模式，精确定义“何时显示源码”至关重要。简单的 `node.from/to` 往往不够，需要计算语义上的关联区域。
