#  `Message` 消息提示系统的实现原理

它主要由三个部分协同工作：**消息仓库 (`messageStore`)**、**消息容器 (`MessageContainer`)** 和 **单个消息 (`Message`)**。

---

## 1. 消息仓库 (`messageStore.ts`) - 全局状态管理器

这是整个系统的“大脑”，负责管理当前需要显示的所有消息。

*   **核心**：它是一个 Svelte 的 `writable` store，内部维护着一个数组，数组的每一项都是一个消息对象（如 `{ id, text, type, duration }`）。
*   **`add(text, type, duration)` 函数**：这是我们从应用中任何地方触发新消息的入口。当调用 `messageStore.add(...)` 时，它会创建一个带唯一 ID 的新消息对象，并将其添加到 store 的数组中。
*   **`remove(id)` 函数**：这个函数用于从数组中移除指定 ID 的消息。

关键在于，这个 Store 只负责维护“应该显示哪些消息”的**列表**，而不关心它们具体如何消失或动画。

---

## 2. 消息容器 (`MessageContainer.svelte`) - 布局与动画管理者

这个组件是所有消息的“家”，它决定了消息列表在屏幕上的位置和动态效果。

*   **订阅 Store**：它使用 Svelte 的自动订阅语法 `$`（即 `$messageStore`）来监听消息仓库的变化。
*   **渲染列表 (`{#each}` L)**：当 `messageStore` 中的数组发生变化（增加或删除消息）时，它会使用 `{#each ...}` 循环来动态地渲染或销毁对应的 `Message.svelte` 组件。
*   **FLIP 动画 (`animate:flip`)**：这是 Svelte 提供的一个强大功能。当列表顺序改变或有元素增删时（例如，一条消息消失，下面的消息向上移动），`animate:flip` 会自动计算元素移动前后的位置，并平滑地应用一个 CSS transform 动画。这完美地实现了 JavaFX 版本中 `relayoutMessages` 的平滑移动效果，且代码极其简洁。
*   **定位**：通过 CSS `position: fixed`，它将自身固定在屏幕的顶部中央，确保消息总是在用户能看到的地方弹出。
*   **可访问性 (`aria-live`)**：它被标记为 `aria-live="polite"`，这样当新消息被添加到这个容器时，屏幕阅读器会自动将新消息的内容读给用户，提升了可访问性。

---

## 3. 单个消息 (`Message.svelte`) - 外观与自身生命周期管理者

这是用户直接看到的单个消息条，它负责自己的外观和“生老病死”。

*   **外观**：
    *   它接收 `text` 和 `type` 作为属性。
    *   根据 `type`（SUCCESS, ERROR 等），它会应用不同的 CSS 类（如 `.message.success`），这些类定义了消息的背景色和**文本/图标颜色**。
    *   **图标颜色**：图标的 SVG 代码被直接用 `{@html}` 注入到 DOM 中。由于 SVG 代码内部有 `fill="currentColor"`，它的颜色会**自动继承**父元素 `.message` 的 `color` 属性，从而与文本颜色保持一致。
*   **生命周期（自动消失与暂停）**：
    *   **`onMount`**：当组件被创建并挂载到页面上时，它会启动一个 `setTimeout` 计时器，准备在 `duration`（默认 3 秒）后将自己从 `messageStore` 中移除。
    *   **`onmouseenter` (悬停暂停)**：当用户的鼠标移入消息条时，`pauseTimer` 函数会触发。它会清除当前的 `setTimeout` 计时器，并记录下已经过去了多少时间。
    *   **`onmouseleave` (继续计时)**：当鼠标移开时，`startTimer` 函数会再次被调用。但这次，它会用**剩余的时间**来设置一个新的 `setTimeout`，而不是从头开始计时。
*   **手动关闭**：组件包含一个 "×" 按钮，它的 `onclick` 事件直接调用 `messageStore.remove(id)`，立即将自己从全局 Store 中移除，从而触发销毁。
*   **出场/入场动画 (`transition:slide`)**：Svelte 的 `transition` 指令让它在创建和销毁时有一个平滑的滑入和淡出效果。

---

**总结：**
这个系统通过**职责分离**实现了清晰且可维护的架构：
*   **Store** 只管数据。
*   **Container** 只管布局和列表动画。
*   **Message** 只管自己的外观和生命周期。

这种模式充分利用了 Svelte 的响应式、动画和组件化特性，用非常少的代码实现了复杂且流畅的交互效果。