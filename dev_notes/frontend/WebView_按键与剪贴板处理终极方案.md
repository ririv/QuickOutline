# JavaFX WebView 中按键与剪贴板处理的最终方案

## 1. 之前的问题总结

我们在 JavaFX WebView 中集成 Web 前端时，遇到了以下挑战：

1.  **Tab 键焦点跳转**：在 `SimpleEditor` (基于 `<textarea>`) 和 `MdEditor` (Vditor 编辑器) 中，Tab 键行为异常，不插入缩进反而导致焦点在 Web 页面内部跳转。
2.  **快捷键失效**：Cmd/Ctrl+B 等格式化快捷键在 `MdEditor` (Vditor) 中无效。
3.  **剪贴板操作异常**：Cmd/Ctrl+C/X/V 在 `MdEditor` 中“看似有效，实则无效”（剪贴板内容未更新或粘贴的是旧内容）。
4.  **右键菜单干扰**：`MdEditor` 区域会弹出 JavaFX 默认的右键菜单，而非 Web 应用预期的行为。

## 2. 问题根源深入分析

这些问题最终都被归结为 JavaFX WebView 与嵌入的 WebKit 引擎在**事件模型**和**安全沙箱**上的“阻抗不匹配”：

*   **键盘事件属性缺失 (`e.key`)**：JavaFX 的 WebView 在转发 `KeyboardEvent` 给 JS 时，对于 `e.key` 属性可能未正确填充（导致其为空）。Vditor 等许多 JS 库高度依赖 `e.key` 进行按键识别。
*   **浏览器默认行为 vs JS `preventDefault()`**：即使 JS 内部调用了 `e.preventDefault()`，在某些情况下（尤其是在 `contenteditable` 或 `iframe` 这种复杂 Web 元素中），WebView 的默认焦点遍历或快捷键处理行为仍可能抢占或绕过 JS 的阻止。
*   **剪贴板安全限制**：出于安全考虑，浏览器环境下的 JS 访问系统剪贴板受到严格限制。JavaFX WebView 同样继承了这些限制，导致 JS 无法直接读写系统剪贴板。
*   **JavaFX 默认菜单覆盖**：`webView.setContextMenuEnabled(false)` 对 `<textarea>` 等简单元素有效，但对 `contenteditable` 区域，JavaFX 可能有更强的默认右键菜单接管逻辑。

## 3. 最终解决方案

为了彻底解决上述问题，我们采取了在 **JS 端通过 Polyfill 和全局捕获拦截**，结合 **Java 端提供原生能力** 的混合方案。

### a. 键盘事件 `e.key` 兼容性修复 (Polyfill)

*   **问题**：JavaFX WebView 在 `KeyboardEvent` 对象中，`e.key` 属性可能为空，导致依赖 `e.key` 的 JS 库（如 Vditor）无法正确识别按键。
*   **修复**：在 `web/src/lib/bridge/index.ts` 中注入一个全局 `keydown` 事件监听器（捕获阶段）。如果 `e.key` 为空，则根据 `e.keyCode` 填充 `e.key`。

    ```typescript
    // web/src/lib/bridge/index.ts
    (function polyfillKeyboardEvent() {
        // ... (省略 keyMap 定义) ...
        window.addEventListener('keydown', (e) => {
            if (!e.key) {
                let key = keyMap[e.keyCode];
                // ... (根据 keyCode 尝试推导 key) ...
                if (key) {
                    try { Object.defineProperty(e, 'key', { get: () => key }); } catch(err){}
                }
            }
        }, true); // `true` 表示在捕获阶段执行
    })();
    ```
*   **效果**：确保了所有键盘事件都能被 JS 库正确识别，使得 Vditor 的 Tab 缩进、Cmd+B 等快捷键在 WebView 中正常工作。

### b. 剪贴板操作的 Java Bridge 代理

*   **问题**：JS 无法直接访问系统剪贴板，导致复制/粘贴失效。
*   **修复**：将剪贴板操作委托给 Java 端执行，并通过 Bridge 进行双向通信。
    1.  **`JsBridge.java`**：
        *   新增 `public void copyText(String text)` 方法：将文本写入系统剪贴板。**注意：移除了 `Platform.runLater`，确保同步写入，解决了竞态条件。**
        *   新增 `public String getClipboardText()` 方法：从系统剪贴板读取文本。
    2.  **`web/src/components/MdEditor.svelte`**：
        *   在 `onMount` 中，通过 `window.addEventListener('copy', ..., true)`, `window.addEventListener('cut', ..., true)`, `window.addEventListener('paste', ..., true)` 在捕获阶段拦截剪贴板事件。
        *   在事件处理函数中，调用 `window.javaBridge.copyText(selection)` 和 `vditorInstance.insertValue(window.javaBridge.getClipboardText())`。
        *   `e.preventDefault()` 和 `e.stopPropagation()` 用于阻止默认的浏览器行为和事件冒泡。

*   **效果**：`MdEditor` 现在可以稳定、可靠地进行复制、剪切、粘贴操作，且与系统剪贴板完全同步。

### c. 禁用 JavaFX 默认右键菜单

*   **问题**：`webView.setContextMenuEnabled(false)` 对 `contenteditable` 区域可能无效，导致弹出不符合预期的 JavaFX 右键菜单。
*   **修复**：在 `web/src/lib/bridge/index.ts` 中添加全局事件监听器。

    ```typescript
    // web/src/lib/bridge/index.ts
    window.addEventListener('contextmenu', e => e.preventDefault());
    ```
*   **效果**：彻底禁用了所有 Web 内容的默认右键菜单，为 Vditor 等编辑器未来定制自己的 Web 右键菜单扫清了障碍。

### d. 弃用 `WebViewEditorSupport`

*   **原因**：由于所有相关功能都已在前端或通过 Bridge 得到更优雅的解决，JavaFX 侧的 `WebViewEditorSupport.java` (其职责包括自定义右键菜单和拦截 Ctrl+C/V/X) 已被删除。
*   **效果**：简化了 JavaFX 代码，避免了 JavaFX 与 Web 之间不必要的事件冲突和逻辑耦合。

## 4. 总结与启示

这次按键和剪贴板问题的解决，是混合应用开发中**深入理解平台差异、事件模型和跨语言通信**的典型案例。通过在正确的时间、正确的层级（捕获阶段的 JS 事件、Java Bridge）进行介入，我们成功地为 WebView 内的 Web 应用带来了近乎原生的体验。核心原则是：**尽可能地将复杂逻辑下推到 Web 前端处理，而 Java 后端只提供必要的原生能力桥接**。