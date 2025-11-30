# Radio Button Group 边框实现原理 (边框合并技术)

本文档解释了 `StyledRadioButtonGroup.svelte` 中所使用的技术，用于实现类似 Ant Design 或 Bootstrap 按钮组（Button Group）那种无缝衔接、边框合并的外观。

## 面临的问题
当我们将多个带边框的按钮紧挨着水平排列时，会遇到两个主要问题：
1.  **双重边框**：相邻按钮的边框宽度叠加（例如 1px + 1px = 2px），导致中间的分隔线看起来比周围的边框更粗。
2.  **选中状态遮挡**：当选中中间某个按钮并改变其边框颜色（例如变为蓝色）时，该按钮的蓝色边框可能会被相邻未选中按钮的灰色边框部分遮挡。

## 解决方案：Flexbox + 负 Margin + Z-Index

这套方案的核心思想是让按钮像瓦片一样稍微重叠。

### 1. 容器设置
- **`display: flex`**: 让按钮水平排列。
- **`isolate`**: 创建一个新的层叠上下文（Stacking Context），确保内部的 `z-index` 设置不会影响到组件外部。

### 2. 按钮布局
- **`flex-1`**: 确保所有按钮平分容器宽度，无论按钮数量是 2 个、3 个还是更多，都能保持等宽。
- **`border`**: 每个按钮初始都拥有完整的上下左右边框。

### 3. 合并边框 (负 Margin 技巧)
除了第一个按钮外，我们给所有按钮添加一个等于边框宽度的左侧负 margin。

```css
.-ml-[1px] { margin-left: -1px; }
.first\:ml-0:first-child { margin-left: 0; }
```

- **效果**：
    - 按钮 2 向左移动 1px，其左边框正好盖在按钮 1 的右边框上。
    - 按钮 3 向左移动 1px，其左边框盖在按钮 2 的右边框上。
    - 以此类推。
- **结果**：相邻按钮之间的视觉边框宽度保持为 1px（合并后的），而不是 2px。无论有多少个按钮，这个逻辑都适用。

### 4. 处理选中状态 (Z-Index 技巧)
当某个按钮被选中时，我们通常会将边框颜色改为高亮色（如蓝色）。由于负 margin 导致的重叠，相邻的灰色边框可能会盖住蓝色的边框。

为了修复这个问题，我们提升选中按钮的层级：

```css
.z-10 { z-index: 10; } /* 应用于 选中 状态的按钮 */
.z-0  { z-index: 0; }  /* 应用于 默认 状态的按钮 */
```

- **效果**：选中的按钮会渲染在所有邻居的**上层**。
- **结果**：蓝色的边框能够完整显示，覆盖住相邻的灰色边框。

### 5. 圆角处理
为了保持“整体组件”的外观，我们只给最外侧的角落加圆角。

```css
/* 第一个按钮：只加左上、左下圆角 */
.first\:rounded-l-md:first-child { border-top-left-radius: ...; border-bottom-left-radius: ...; }

/* 最后一个按钮：只加右上、右下圆角 */
.last\:rounded-r-md:last-child   { border-top-right-radius: ...; border-bottom-right-radius: ...; }
```

中间的所有按钮都没有圆角。

## Tailwind 实现示例

```html
<!-- 容器 -->
<div class="flex w-full isolate">
  
  <!-- 按钮 (循环渲染) -->
  <button class="
      flex-1 border 
      -ml-[1px] first:ml-0 
      first:rounded-l-md last:rounded-r-md
      
      ${isSelected 
         ? 'border-blue-500 z-10'  /* 选中：蓝色边框，浮在最上层 */
         : 'border-gray-300 z-0'   /* 默认：灰色边框，在下层 */
      }
  ">
    选项
  </button>

</div>
```