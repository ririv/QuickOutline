# SplitPane 分割条实现分析

**日期:** 2025-12-04
**组件:** `src/components/SplitPane.svelte`

## 概述
本文档详细阐述了 `SplitPane` 分割条的优化 CSS 实现，旨在模仿 Visual Studio Code 分屏调整条的行为和视觉风格。

## 问题
分割条的朴素实现（例如，一个 `width: 5px` 的 `div`）存在两个主要缺点：
1.  **布局偏移**：可见的宽度会在 Flex 布局中占用空间，可能导致相邻面板出现“挤压”问题。
2.  **交互与视觉冲突**：将线条在视觉上做得太细（1px）会很难点击。如果做得太粗（5px），又会显得笨重。

## 解决方案：“零宽度锚点”策略

该解决方案利用 CSS 伪元素和巧妙的定位，将布局逻辑、交互逻辑和视觉呈现分离为三个不同的层。

### 1. 锚点 (`.resizer`)
这个元素本身在布局中不占用任何空间，同时作为定位伪元素的锚点。

```css
.resizer {
    width: 0;               /* 不占用布局空间 */
    position: relative;     /* 作为伪元素的定位基准 */
    z-index: 100;           /* 确保在最上层，优先响应鼠标事件 */
    flex-shrink: 0;         /* 防止 Flex 布局收缩 */
    user-select: none;
}
```

### 2. 点击热区 (`::after`)
创建一个透明但宽阔的区域，以便用户轻松捕获鼠标事件。这解决了可用性问题。

```css
.resizer::after {
    content: '';
    position: absolute;
    top: 0; 
    bottom: 0;
    /* 将 10px 宽的区域在 0px 锚点上居中 */
    left: -5px;             
    width: 10px;            
    background: transparent; /* 透明，不可见 */
    cursor: col-resize;      /* 调整大小的光标 */
    z-index: 10;             /* 确保在视觉线条之上，捕获点击 */
}
```

### 3. 视觉线条 (`::before`)
一个独立的元素负责渲染实际可见的分割线。这解决了视觉美观性问题。

**默认状态：**
一条细微的 1px 灰色线条。

```css
.resizer::before {
    content: '';
    position: absolute;
    top: 0; 
    bottom: 0;
    /* 将 1px 线条在 0px 锚点上居中 */
    left: -0.5px;           
    width: 1px;
    background-color: #e5e5e5; /* 默认颜色 */
    transition: all 0.15s ease-out; /* 平滑过渡 */
    z-index: 9;             /* 在点击热区之下 */
    pointer-events: none;   /* 允许点击穿透到 ::after */
}
```

**悬停/激活状态：**
当鼠标悬停在**点击热区**（即 `.resizer`）上时，我们会改变**视觉线条**（`::before`）的样式，使其更粗并变为蓝色。

```css
.resizer:hover::before, .resizer:active::before {
    background-color: #1677ff; /* VS Code 激活时的蓝色 */
    width: 4px;                /* 变宽到 4px */
    left: -2px;                /* 重新居中 4px 的线条 */
}
```

## 视觉示意图

```text
           |<---- 点击热区 (10px) ---->|
           |      (::after)            |
           |                           |
           |      |<-- 线条 (1px) -->| |
           |      |    (::before)    | |
           |      |                  | |
布局 -------+------+------------------+------
锚点       ^ (0px 宽度)
(.resizer)
```

## 核心优势
1.  **零布局影响**：面板在视觉上紧密相连（数学上），不会因为分割线而产生额外的布局空间。
2.  **符合费茨定律 (Fitts's Law)**：尽管视觉线条纤细，但提供了宽阔（10px）且易于点击的目标区域，大大提升了用户体验。
3.  **精细的 UI**：通过平滑过渡和状态反馈（颜色/宽度变化），实现了专业 IDE 般的用户界面效果。