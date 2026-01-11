# Svelte 5 双向绑定与状态同步问题总结

**日期:** 2025-12-08
**Tags:** Svelte 5, Runes, Reactivity, UX, Two-Way Binding

## 问题描述

在将 `BookmarkBottomPane.svelte` 迁移到 Svelte 5 Runes (`$state`, `$effect`) 后，发现 `Offset` 输入框**无法正常输入**。

**具体表现：**
*   当用户尝试输入（例如键入数字或负号）时，输入框的内容会立即被重置回之前的值（或 0）。
*   无法输入负数（负号刚打出来就被删除了）。

## 原因分析

这是由于 Svelte 5 的 `$effect` 自动追踪机制导致的**循环依赖**。

### 原始（问题）代码逻辑

```typescript
// 1. 本地输入处理
function handleOffsetInput(e) {
    offsetValue = e.target.value; // 更新本地 UI 状态
    bookmarkStore.setOffset(parseInt(offsetValue)); // 同步到全局 Store
}

// 2. 状态同步 (Store -> UI)
$effect(() => {
    // 错误点：这里直接读取了 offsetValue
    if (offsetValue === '-') return; 

    // 当 Store 变化时，强制更新本地 UI
    if (offsetValue !== String(bookmarkStore.offset)) {
        offsetValue = String(bookmarkStore.offset);
    }
});
```

### 循环死锁流程
1.  **用户输入**：用户键入 "1"，`handleOffsetInput` 将 `offsetValue` 更新为 `"1"`。
2.  **触发 Effect**：因为 `$effect` 内部读取了 `offsetValue`（为了判断是否为 `'-'`），Svelte 5 自动将其注册为依赖。因此，`offsetValue` 的变化触发了 Effect 重新运行。
3.  **冲突回滚**：
    *   Effect 运行时读取 `bookmarkStore.offset`。
    *   此时如果 Store 的更新有微小延迟，或者数据类型转换逻辑（String -> Number -> String）导致不一致（例如输入 "01" vs Store "1"）。
    *   Effect 判断值不匹配，执行 `offsetValue = String(bookmarkStore.offset)`。
    *   **结果**：用户的输入被强制覆盖回旧值。

## 解决方案：使用 `untrack`

Svelte 5 提供了 `untrack` 函数，用于在响应式上下文中读取值，但**不**建立依赖关系。

### 修复后的代码

```typescript
import { untrack } from 'svelte';

$effect(() => {
    // 1. 在 untrack 外部读取真正的依赖 (Store)
    // 这样只有当 bookmarkStore.offset 变化时，Effect 才会运行
    const currentStoreOffset = bookmarkStore.offset;

    untrack(() => {
        // 2. 在 untrack 内部读取或修改本地状态 offsetValue
        // 这样 offsetValue 的变化本身不会再次触发这个 Effect
        if (offsetValue === '-') return;

        const currentOffsetStr = currentStoreOffset === 0 ? '' : String(currentStoreOffset);
        if (offsetValue !== currentOffsetStr) {
            offsetValue = currentOffsetStr;
        }
    });
});
```

### 修复原理
通过 `untrack`，切断了 `Local Input -> Local State Change -> Effect Trigger -> Local State Reset` 的循环。
现在逻辑变成了单向流：只有 **Store** 变了，才会去更新 UI。UI 自己的变化由 `oninput` 事件自行管理，不再自扰。

## 架构思考：为什么需要两个状态？

为什么不能直接把输入框绑定到 `bookmarkStore.offset`，而非要维护一个 `offsetValue` (String) 和一个 Store (Number)？

这是为了**用户体验 (UX)** 和**数据类型安全**。

1.  **处理“非法”中间态 (Buffer)**
    *   用户输入 `-5` 时，第一步是打出 `-`。
    *   `-` 不是有效的 `Number`（解析为 NaN 或 0）。
    *   如果直接绑定 Store，Store 会变成 0，UI 立刻重绘成 `0`，用户永远打不出负号。
    *   **本地状态 (`offsetValue`)** 作为一个**脏缓冲区 (Dirty Buffer)**，允许暂时存储 `"-"`、`""`、`"00"` 等不完美的数据，等待用户完成输入。

2.  **数据类型差异**
    *   DOM Input 永远是 `String`。
    *   业务逻辑需要 `Number`。
    *   双向直接转换（String <-> Number）会导致 `0` vs `""` 或 `007` vs `7` 的格式丢失问题，导致输入框光标跳动或内容突变。

### 结论
在 MVVM 模式中，处理 `String` (UI) 到 `Number` (Model) 的绑定时：
1.  **必须**保留本地状态作为缓冲区。
2.  在同步回 Model 时，使用 `untrack` (Svelte 5) 或类似的机制打破循环依赖，防止 UI 状态的更新反向触发同步逻辑覆盖自己。
