# Paged.js 双缓冲模式下的样式隔离问题 (Style Isolation in Double Buffering)

**日期**: 2026-02-02
**类别**: 前端渲染 / 性能优化
**状态**: 已解决 (Resolved)

## 问题背景 (Background)

在 QuickOutline 的 TOC (目录) 预览功能中，我们使用了 Paged.js 进行分页排版。为了提供丝滑的预览体验，`PagedEngine` 实现了双缓冲机制（Double Buffering）：
1. **Buffer A**: 当前正在显示的预览。
2. **Buffer B**: 在后台静默渲染新生成的 HTML 和 CSS。
3. 渲染完成后，交换 A 和 B，并清理旧的样式。

## 问题描述 (The Bug)

当用户修改分栏设置（例如从 1 栏切换到 2 栏）时，预览会出现严重的排版错乱（内容重叠、分页异常、布局抖动）。然而，点击“刷新”按钮或再次手动触发更新，排版通常会恢复正常。

## 根本原因分析 (Root Cause Analysis)

问题的根源在于 **CSS 全局作用域与异步渲染的竞争条件 (Style Contention)**：

1. **样式共存**: Paged.js 在 `targetBuffer` (后台 Buffer) 开始渲染时，为了防止当前预览 (Buffer A) 闪烁，我们不能立即删除旧的 CSS。
2. **选择器冲突**: 之前生成的 CSS 使用的是通用的类选择器（如 `.toc-content`）。
3. **计算错误**: Paged.js 在执行测量（Measuring）和分块（Chunking）时，会读取 DOM 元素的计算样式。此时，`<head>` 中同时存在旧的 CSS 和新的 CSS。
4. **优先级干扰**: 如果新旧 CSS 的选择器优先级相同，浏览器可能会基于旧样式或混合样式进行布局计算。导致 Paged.js 的分页算法基于错误的参数运行。
5. **结果**: 渲染出来的 DOM 结构虽然应用了新样式，但其内部的分页逻辑（如 `break-after` 位置、列宽计算）却是基于过期状态的，导致最终展示错乱。

## 解决方案 (Solution: Scoped CSS)

引入 **样式作用域隔离 (Style Scoping)**，确保每一帧渲染都是独立且互不干扰的。

### 实施细节

1. **动态生成 Scope ID**: 在每次 `generateTocHtml` 调用时，生成一个唯一的 `scopeId`（如 `toc-x7y2z9`）。
   ```typescript
   const scopeId = `toc-${Math.random().toString(36).substring(2, 8)}`;
   ```
2. **HTML 绑定**: 将该 ID 作为类名添加到根元素上：
   ```html
   <div class="toc-content toc-x7y2z9">...</div>
   ```
3. **CSS 注入**: 动态生成的样式规则全部限定在该 ID 下：
   ```css
   .toc-x7y2z9.toc-content { column-count: 2; }
   ```

### 效果

即使 `<head>` 中暂时残留了旧的样式标签（针对旧的 Scope ID），它们也绝对不会匹配到当前正在渲染的 DOM 元素。Paged.js 能够在一个“干净”的 CSS 环境中进行精确的布局计算。

## 经验教训 (Lessons Learned)

- 在涉及重量级异步渲染引擎（如 Paged.js）和动态 CSS 注入的场景中，**双缓冲不仅需要 DOM 层面的隔离，更需要 CSS 层面的隔离**。
- 手动刷新能修复的问题通常暗示了状态残留或竞态条件。
- 模仿 Svelte/Vue 的 Scoped CSS 机制是解决此类全局 CSS 污染最简单有效的手段。
