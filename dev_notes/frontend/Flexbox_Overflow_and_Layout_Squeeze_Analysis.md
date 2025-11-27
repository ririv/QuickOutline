# Flexbox 布局溢出与侧边栏挤压问题复盘

## 问题描述

在 `QuickOutline` Web 版集成过程中，发现定宽 50px 的左侧侧边栏 (`LeftPane`) 在 Page Label 页面被右侧内容区域 (`Content Area`) 挤压至约 41px，导致图标贴边。

## 排查过程

1.  **初步怀疑**：Flexbox 默认的 `flex-shrink: 1` 导致侧边栏被压缩。
2.  **定位罪魁祸首**：
    *   使用“删除元素法”，发现通过开发者工具删除 `ThumbnailPane` 内部的带有`.scroll-area` class 的HTML元素后布局恢复正常。

## 根本原因分析

问题的核心在于 **"刚性宽度" (Rigid Width) 的传递链条**。

### 1. 内容端的刚性 (`ThumbnailPane`)
缩略图元素 `.thumbnail-wrapper` 使用了：
```css
width: calc(100px * var(--zoom, 1));
flex: 0 0 auto; /* 禁止收缩 */
```
这创建了一个**不可压缩的实体**。当父容器变窄（不足以容纳一个缩略图+padding）时，这个元素拒绝缩小，强行撑开了 `.scroll-area` 和 `.thumbnail-pane`。

### 2. 容器端的刚性 (`SplitPane`)
`SplitPane` 通过显式设置 `width` 百分比来控制布局：
```html
<div class="pane" style="width: {split}%">
```
在 Flex 布局中，显式 `width` 的优先级往往高于隐式的 Flex 收缩逻辑。当内部内容（如上述刚性缩略图）撑开 `.pane` 时，由于 `.pane` 本身被设定了一个基于父容器百分比的宽度，这种双向约束（父容器定宽 vs 内容撑开）在浏览器计算中导致了“向外溢出”。相比之下，使用 `flex: 1` 的普通 DIV 能更好地处理内容溢出（通常会触发 `min-width: auto` 规则或者直接截断，而不是撑大容器）。

### 3. 牺牲品 (`LeftPane`)
由于右侧区域（`content-area` -> `SplitPane`）变得比视口剩余空间更宽，且 `LeftPane` 未设置 `flex-shrink: 0`，Flexbox 算法为了放下右侧的“刚性”内容，只能压缩左侧面板。

### 4. 尝试通过增加 `min-width: 0` 解决
再各个元素通过加上 `min-width: 0` 均未解决

### 5. 尝试通过修改 `SplitPane` 解决
- **数学修正无效**：尝试修复 `SplitPane` 的 5px 溢出（`calc(100% - 5px)`），问题依旧。这表明问题不在于简单的像素溢出，而在于**布局的弹性机制**失效。 
- **对比测试**：将 `SplitPane` 替换为普通的 `<div style="flex: 1; display: flex;">`，问题消失。这说明 `SplitPane` 的特定实现方式放大了内部内容的宽度压力。

## 最终解决方案

我们采取了**“打破刚性”**的策略，从源头和架构两端入手：

### 1. 源头治理：柔化内容宽度
将缩略图的刚性 `width` 转换为弹性的 `flex-basis`，并允许收缩。
**修改前**：
```css
width: calc(100px * var(--zoom, 1));
flex: 0 0 auto;
```
**修改后**：
```css
/* 使用 flex-basis 设定基准尺寸，但允许 shrink */
flex: 0 1 calc(100px * var(--zoom, 1));
```
这样，在空间不足时，缩略图会温和地缩小，而不是撑爆容器。这是解决问题的关键一击。
1. 如果设置 `flex: 0 0 calc(100px * var(--zoom, 1));` 即不允许收缩，问题是依旧存在的
2. 如果保留 `width: calc(100px * var(--zoom, 1));` 但设置 `flex: 0 1 auto;` 允许收缩，问题也依旧存在

### 2. 架构防御：侧边栏加固
给 `LeftPane` 或者 `.nav-btn` 添加 `flex-shrink: 0`。
```css
.left-pane {
    width: 50px;
    flex-shrink: 0;
}
```
或者
```css
.nav-btn {
    width: 40px;
    flex-shrink: 0;
}
```

这是定宽组件的标准防御措施，确保无论右侧发生什么（哪怕布局炸了），侧边栏永远保持 50px。

## 经验总结

导致 LeftPane 被挤压的真凶链条是：
1. `ThumbnailWrapper` 设置了硬性的 width。
2. `Flex-Wrap` 布局机制尊重了这个 width，即使空间不足也不收缩它。
3. `ScrollArea` 被撑大。
4. `SplitPane` 被撑大。
5. `LeftPane` 被挤压。

*   **警惕 `width: calc(100px * var(--zoom, 1));` 这会创造出极其顽固的布局元素，极易引发溢出。在响应式布局中，尽量使用 `flex-basis` 并允许 `flex-shrink: 1`(即 flex-basis 中的第二个属性)。
*   **SplitPane 的局限**：基于 `width` 百分比的 SplitPane 在处理溢出内容时不如纯 Flex 容器健壮。在使用此类组件时，必须严格控制其子内容的宽度行为。
*   **删除法是调试神器**：在复杂的嵌套布局中，直接删除疑似有问题的 DOM 节点是验证猜想的最快路径。
