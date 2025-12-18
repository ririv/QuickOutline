# 双向同步架构与死循环阻断机制分析

**日期:** 2025-12-18
**涉及模块:** `BookmarkEditor` (CodeMirror), `TreeSubView`, `TextSubView`, `bookmarkStore`
**Tags:** Architecture, Svelte 5, CodeMirror 6, Reactivity, State Management

## 1. 总体架构：菱形同步模型

`QuickOutline` 采用了一种“菱形”同步架构，以 `bookmarkStore` 作为中心枢纽，连接两个异构视图：

```text
       [ PDF 后端 / 文件 ]
               ↑
      [ bookmarkStore ] <---------- 中心枢纽 (State)
        /           \
 [Text View]     [Tree View] <---- 异构视图
      |               |
 [BookmarkEditor] [BookmarkNode]
```

同步不仅包含状态的传递，还包含了**数据格式的转换**：
*   **文本侧**：处理的是原始字符串。
*   **树形侧**：处理的是嵌套的对象数组 (`BookmarkUI[]`)。

## 2. 数据转换逻辑 (Transformation)

为了保持两个视图的一致性，我们在 `src/lib/outlineParser` 中定义了两套核心转换算法：

### A. 文本转树 (Parsing: Text -> Tree)
发生在 `TextSubView` 中，当用户编辑文本或切换解析模式时触发。
*   **核心方法**: `processText(text, method)`。
*   **逻辑**: 
    1. 预处理（去空行、标准化空格）。
    2. 根据 `Method` (SEQ/INDENT) 调用对应的 `Parser`。
    3. 生成线性列表并转换为嵌套树结构。
*   **触发时机**: 编辑器 `onchange` (防抖) 或 `bookmarkStore.method` 变化。

### B. 树转文本 (Serialization: Tree -> Text)
发生在 `TreeSubView` 中，当用户在 GUI 中修改标题、页码或调整层级时触发。
*   **核心方法**: `serializeBookmarkTree(root)`。
*   **逻辑**: 
    1. 深度优先遍历 (DFS) 树形结构。
    2. 根据节点层级计算缩进量 (`\t`)。
    3. 拼接标题和页码，生成符合规范的 Markdown 文本。
*   **触发时机**: 本地 `bookmarks` 状态深度变化 (防抖)。

## 3. 状态保持策略 (Identity Preservation)

在文本解析为树的过程中，物理上会生成全新的对象树（新的内存引用）。为了防止 DOM 闪烁和丢失用户的折叠/展开状态，我们在 `BookmarkStore` 中实现了基于**编辑距离（Levenshtein Distance）**的状态嫁接机制。

### 核心机制：Reconcile Trees
每次调用 `setTree(newTree)` 时，Store 不会直接粗暴替换，而是先执行 `reconcileTrees(oldTree, newTree)`：

1.  **拍平 (Flatten)**: 将新旧两棵树拍平成线性列表。
2.  **动态规划 (DP)**: 计算最小编辑距离，找到旧节点和新节点的最佳匹配关系。
    *   **匹配规则**: 优先匹配层级相同、内容（标题/页码）相似的节点。
3.  **状态迁移 (Transfer)**: 对于匹配成功的节点，将旧节点的 **`id`** 和 **`expanded`** 状态复制给新节点。

```typescript
function transferState(oldNode, newNode) {
    newNode.id = oldNode.id;          // 保持 DOM 身份，避免 Svelte 销毁重建 DOM
    newNode.expanded = oldNode.expanded; // 保持 UI 折叠状态
}
```

**收益**:
*   **DOM 复用**: Svelte 的 `{#each ... (id)}` 键值索引机制能识别出 ID 未变，从而复用现有 DOM，性能极佳。
*   **体验无缝**: 用户在左侧编辑文本时，右侧树形视图不会因为重新解析而折叠所有节点，体验流畅。

---

## 4. 解决方案一：编辑器侧的“来源标记”阻断 (`BookmarkEditor`)

在 `TextSubView` 中，我们使用 CodeMirror 6 作为编辑器。

### 问题场景
1.  Store 更新文本。
2.  Svelte 传入新 `value` 给 `BookmarkEditor`。
3.  `BookmarkEditor` 调用 `view.dispatch({ changes: ... })` 更新编辑器内容。
4.  CodeMirror 触发 `updateListener`。
5.  `updateListener` 发现 `docChanged` 为 true，调用组件的 `onchange` 回调。
6.  `onchange` 将新文本写回 Store。
7.  **死循环形成**。

### 架构修复：CodeMirror Annotation
我们利用 CodeMirror 的 **Annotation（注解）** 机制来区分“用户输入”和“程序同步”。

1.  **定义标记**:
    ```typescript
    const ExternalUpdate = Annotation.define<boolean>();
    ```

2.  **标记程序更新**:
    当 Svelte `$effect` 监听到 `value` 属性变化（来自 Store）时，我们在 dispatch 事务时打上标记：
    ```typescript
    view.dispatch({
        changes: { ... },
        annotations: ExternalUpdate.of(true) // <--- 关键：我是外部程序更新
    });
    ```

3.  **拦截回调**:
    在 `updateListener` 中检查是否存在该标记：
    ```typescript
    EditorView.updateListener.of((update) => {
        if (update.docChanged) {
            // 检查事务是否携带 ExternalUpdate 标记
            const isExternal = update.transactions.some(tr => tr.annotation(ExternalUpdate));
            
            // 只有当不是外部更新（即它是用户键盘输入）时，才通知父组件
            if (!isExternal) {
                onchange?.(newVal, ...);
            }
        }
    })
    ```

**结果**: 切断了 `Store -> Editor -> onchange -> Store` 的回环。

## 3. 解决方案二：树形视图侧的“深度一致性”检查 (`TreeSubView`)

在 `TreeSubView` 中，数据是复杂的嵌套对象数组 (`BookmarkUI[]`)。

### 问题场景
早期的实现尝试使用 `isUpdatingFromStore` 标志位来防止循环，但这种命令式的状态管理容易在复杂的异步更新或快速交互中产生竞态条件（Race Condition），或者在某些情况下锁死状态。

### 架构修复：纯粹的数据一致性检查
我们移除了所有标志位，回归到数据驱动的本源：**只有数据真的变了，才进行同步。**

利用 Svelte 5 的 `untrack` 和 `JSON.stringify` 深度对比：

1.  **Store -> Local**:
    ```typescript
    $effect(() => {
        const storeTree = bookmarkStore.tree;
        untrack(() => {
             // 只有当 Store 的内容真的和本地不一样时，才更新本地
             if (JSON.stringify(storeTree) !== JSON.stringify(bookmarks)) {
                 bookmarks = storeTree;
             }
        });
    });
    ```

2.  **Local -> Store**:
    ```typescript
    $effect(() => {
        JSON.stringify(bookmarks); // 追踪本地变化
        const currentBookmarks = bookmarks;
        
        untrack(() => {
            // 只有当本地内容真的和 Store 不一样时，才写回 Store
            const isDifferent = JSON.stringify(bookmarkStore.tree) !== JSON.stringify(currentBookmarks);
            
            if (isDifferent) {
                bookmarkStore.setTree(currentBookmarks);
            }
        });
    });
    ```

**原理**:
当 Store 更新同步到 Local (`bookmarks`) 时，Local 的变更会触发第二个 `$effect`。但在第二个 `$effect` 中，我们立即对比了 `bookmarkStore.tree` 和 `currentBookmarks`。由于前者刚刚传给后者，它们是深度相等的。因此，`if (isDifferent)` 为 `false`，写回操作被拦截。

**结果**: 循环自然终止，且不依赖脆弱的状态标志位。

## 4. 附录：类型系统重构

在本次优化中，我们还将 `bookmarkStore.method` 从字符串字面量 (`'sequential' | 'indent'`) 重构为 TypeScript 枚举 (`Method.SEQ | Method.INDENT`)。

*   **IconSwitch 组件**: 升级为泛型组件 `<script lang="ts" generics="T">`，使其能完美支持 Enum 类型的值绑定。
*   **收益**: 
    *   消除了 `TextSubView` 中繁琐的 `method === 'indent' ? Method.INDENT : ...` 转换逻辑。
    *   确保了 UI 层 (`IconSwitch`) 和逻辑层 (`Parser`) 使用完全一致的类型源（Single Source of Truth）。

## 5. 总结

双向绑定的核心在于**打破回环**。
*   对于 **DOM/Editor** 类组件：使用事务标记（Annotation）或事件来源判断是最佳实践。
*   对于 **数据/State** 类组件：严格的“变更检测”（Dirty Check / Deep Compare）是防止死循环的最终防线。
