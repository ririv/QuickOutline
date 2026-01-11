# Portal Popup Architecture & Click Handling Refactoring
**Date:** 2026-01-11
**Category:** Frontend / Svelte / Architecture

## 背景与问题 (Context & Problem)

在开发 `StatusBar` 和 `PageNumberHint` 等组件时，我们遇到了两个相互冲突的核心问题：

### A. 遮挡问题 (The Clipping Issue)
弹窗（Popup）经常因为父容器（如 `StatusBar` 或 `SplitPane`）设置了 `overflow: hidden` 或 `z-index` 层级较低而被裁剪或遮挡。
*   **解决方案**：引入 **Portal (传送门)** 技术，将弹窗 DOM 元素直接挂载到 `<body>` 根节点下，突破所有父级容器的几何限制。

### B. 点击判定问题 (The Click Detection Issue)
一旦启用 Portal，弹窗的 DOM 元素在物理上脱离了原来的组件树。
*   **后果**：
    *   `clickOutside` 逻辑失效：父组件监听点击事件时，发现点击目标（Target）不在自己内部（`this.contains(target) === false`），于是误判为“点击外部”，导致弹窗刚打开就立刻关闭。
*   **旧的权宜之计**：
    *   在弹窗上使用 `e.stopPropagation()` 阻止事件冒泡。
    *   **致命缺陷**：这会导致弹窗内部的子组件（如 `StyledSelect`）无法接收到 `document` 级别的事件委托，导致下拉菜单无法交互。

## 新架构原理 (Architecture Overview)

为了既能使用 Portal 解决遮挡，又能保留完整的事件冒泡和点击交互，我们重构了一套 **“自动注册、智能感知”** 的机制。这套机制借鉴了 Melt UI / Headless UI 的设计思想，但采用了更轻量的实现。

### 核心组件

#### 1. 全局注册表 (Global Registry)
*   **位置**: `src/lib/actions/clickOutside.ts`
*   **机制**: 导出一个全局共享的 `Set<HTMLElement>`，名为 `portalElements`。
*   **作用**: 存储所有当前活跃的、被 Portal 出去的弹窗 DOM 节点引用。

#### 2. 智能 ClickOutside Action
*   **位置**: `src/lib/actions/clickOutside.ts`
*   **增强逻辑**: 在判断点击是否发生在“外部”时，它不仅检查绑定的节点，还**自动遍历全局注册表**。
*   **判定规则**:
    ```typescript
    if (node.contains(target)) return; // 1. 在组件内部 -> 不关
    if (excludeList.contains(target)) return; // 2. 在显式白名单内 -> 不关
    if (portalElements.has(target)) return; // 3. [新] 在任何 Portal 弹窗内 -> 不关
    ```

#### 3. 自动注册的 ArrowPopup
*   **位置**: `src/components/controls/ArrowPopup.svelte`
*   **行为**:
    *   当 `usePortal={true}` 时，组件在 `onMount` 生命周期中自动调用 `registerPortal(thisElement)`。
    *   组件销毁时自动注销。
    *   **结果**: 使用者完全不需要手动传递 `popupEl` 或配置 `exclude`，一切全自动完成。

#### 4. 智能定位 (Auto Positioning)
*   **位置**: `src/lib/actions/autoPosition.ts`
*   **增强**: 引入了 `getScrollParent` 检测。
*   **效果**: 即使不使用 Fixed 定位（Absolute 模式），弹窗也能感知最近的滚动容器边界（如左侧边栏），并在被遮挡时自动向反方向偏移，确保内容可见。

---

## 使用指南 (Usage Guide)

`ArrowPopup` 支持两种工作模式，系统会根据 `usePortal` 属性自动调整行为。

### 场景 A：Portal Mode (默认 / 推荐)
这是解决遮挡问题的标准模式。

*   **配置**: `usePortal={true}` (默认值)
*   **行为**: 弹窗 DOM 元素被直接挂载到 `<body>` 根节点下。
*   **适用场景**:
    *   下拉菜单、复杂设置面板。
    *   父容器有 `overflow: hidden` 或 `z-index` 限制的场景。
*   **代码示例**:
    无需任何特殊配置，直接使用即可。
    ```svelte
    <ArrowPopup triggerEl={btn}>
        Content
    </ArrowPopup>
    ```

### 场景 B：Inline Mode (非 Portal 模式)
这是保留原生 DOM 结构的简单模式。

*   **配置**: `usePortal={false}`
*   **行为**: 弹窗 DOM 元素保留在原来的组件树中，作为父元素的直接子节点。
*   **适用场景**:
    *   **CSS Hover 交互**: 如果你需要依赖父元素的 `:hover` 状态来显示弹窗（纯 CSS 实现），必须使用此模式。因为 Portal 会打断 CSS 选择器的父子关系。
    *   **不需要突破容器**: 弹窗很小，或者父容器没有 overflow 限制。
*   **原理**:
    *   **物理嵌套**: 依赖原生的 DOM 父子关系。
    *   **点击保护**: `clickOutside` Action 通过原生的 `node.contains(target)` 即可正确识别点击，**不需要**全局注册表的介入。
    *   **防遮挡**: 即使在 Inline 模式下，`autoPosition` 也会智能检测最近的滚动容器边界，并自动向反方向偏移以防止被切断（例如左侧被挡住时向右移）。

### 场景 C：Centralized Management (集中管理模式，在父组件管理多个弹窗)
这是处理一组互斥弹窗（如状态栏）的最佳实践。

*   **配置**: 在父容器上使用 `use:clickOutside`。
*   **行为**: 点击父容器外部（且不在任何 Portal 弹窗内）时，触发回调清理状态。
*   **适用场景**:
    *   状态栏 (StatusBar)、工具栏。
    *   多 Tab 切换、互斥菜单。
    *   需要点击空白处“重置”或“关闭所有”的场景。
*   **原理**:
    *   **父级监听**: 监听器挂载在父容器上，负责全局状态（如 `activePopup`）的清理。
    *   **自动白名单**: 即使子弹窗（Portal）在物理上位于父容器之外，`clickOutside` 也会通过全局注册表识别它们，判定为“内部点击”，从而防止因误判导致状态被重置。
*   **代码示例**:
    无需关心子弹窗的位置，只需管理状态。
    ```svelte
    <script>
      import { clickOutside } from '@/lib/actions/clickOutside';
      let activePopup = null;
    </script>

    <!-- 点击任何非内部、非Portal区域时，重置状态 -->
    <div class="bar" use:clickOutside={() => activePopup = null}>
        <button onclick={() => activePopup = 'A'}>Open A</button>
        
        {#if activePopup === 'A'}
            <!-- 子弹窗自动注册，父级自动识别 -->
            <ArrowPopup ... />
        {/if}
    </div>
    ```

---

## FAQ

**Q: 既然现在的代码中也没有手动绑定 `popupEl`，为什么点击弹窗内部不会误触发关闭？**

**A**: 这是因为我们实现了 **“自动注册机制”**。
*   **过去（手动模式）**：我们需要通过 `bind:popupEl` 将弹窗的 DOM 引用手动“拎”出来，再传给 `clickOutside` 的白名单。这虽然解决了 Portal 导致的 DOM 断裂问题，但代码非常啰嗦。
*   **现在（智能模式）**：`ArrowPopup` 会在挂载时**自动**向全局注册表（`portalElements`）登记自己的身份。而增强后的 `clickOutside` 已经具备了“感应”这张表的能力。虽然你在代码层面看不见引用的传递，但底层逻辑已经通过全局注册完成了“自发现”，实现了真正的零配置。

---

**Q: 为什么最原始的版本（没有 Portal 时）不需要这一套复杂的逻辑？**

**A**: 因为在最原始的“嵌套架构”中，弹窗在 DOM 树上就是父组件的亲生子节点。原生的 `contains()` 检查能够通过物理嵌套关系直接找到它。引入 Portal 相当于让子节点“离家出走”挂到了 body 下，我们这一套逻辑本质上是给“离家出走”的孩子建立了一套**自动化的寻人回执系统**。

---

**Q: 如果我显式设置 `usePortal={false}`，这套自动注册机制还会工作吗？还有必要吗？**

**A**: 当 `usePortal={false}` 时，`ArrowPopup` **不会**将自己添加到全局注册表。
此时弹窗的 DOM 节点会保留在父容器内部（物理嵌套）。即使没有自动注册，`clickOutside` 依然能通过原生的 DOM 父子关系（`node.contains(target)`）正确识别点击。所以，不使用 Portal 时，这套机制**不需要介入**，一切也能完美工作（回归到最原始的简单模式）。这保证了系统在两种模式下的行为是一致且高效的。

---

**Q: 为什么不使用 `stopPropagation`？**

**A**: `stopPropagation` 是一种破坏性的做法。它会阻止事件冒泡，导致弹窗内部的复杂组件（如 `StyledSelect`，它依赖 document 级的点击监听来关闭下拉菜单）失效。目前的方案保留了完整的事件流。

---

**Q: 这种全局注册会有副作用吗？**

**A**: 理论上，如果你点击了页面上**另一个**毫不相关的 Portal 弹窗，`clickOutside` 也会认为这是“内部点击”而不触发关闭。但在桌面应用场景下，这是一个可以接受的特性（点击弹窗 A 不应该导致弹窗 B 意外关闭，除非业务逻辑强制要求）。如果需要更严格的隔离，未来可以引入 Scope ID 机制（参考 Melt UI）。
