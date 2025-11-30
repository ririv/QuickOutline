# Web 前端改进总结 (20251129)

本次迭代主要关注 QuickOutline Web 前端的用户体验、功能完整性及后端集成效率，尤其是在 PDF 文件处理和数据同步方面进行了深入优化和重构。

---

## 1. 文件打开与拖拽功能重构

### 1.1 问题
原 JavaFX 应用中的文件打开功能较为基础，且前端（Web 端）缺少统一的文件操作入口。同时，Web 前端在浏览器和 Tauri 环境下处理文件拖拽存在兼容性问题，尤其是在浏览器环境中无法直接获取文件完整路径。

### 1.2 解决方案
1.  **FileHeader 组件**：创建 `web/src/components/FileHeader.svelte` 作为应用顶部的统一文件操作入口。
    *   **显示**：居中显示当前打开的文件名（未打开时显示“Click to Open or Drop PDF”引导文本）。
    *   **打开操作**：右侧提供一个简洁的图标按钮，点击触发文件选择。
        *   在 Tauri 环境下，调用 `@tauri-apps/plugin-dialog` 弹出系统原生文件选择对话框。
        *   在浏览器开发环境下，提供 `prompt` Fallback 允许用户手动输入文件路径。
2.  **全局拖拽功能**：在 `web/src/pages/app/App.svelte` 中实现全屏拖拽打开 PDF。
    *   **环境兼容**：
        *   **Tauri 环境**：利用 Tauri 的事件系统（`tauri://drag-enter` / `tauri://drag-leave` / `tauri://drag-drop`）监听拖拽事件，直接获取文件绝对路径并调用后端 RPC。
        *   **浏览器环境**：使用 HTML5 原生 `ondragenter` / `ondragleave` / `ondrop` 事件处理 UI 反馈和文件拖放。由于浏览器安全限制无法获取完整路径，会弹出 `prompt` 允许用户手动输入路径。
    *   **视觉反馈**：拖拽文件进入应用区域时，显示全屏半透明覆盖层提示用户。
3.  **统一文件打开逻辑**：将 `rpc.openFile(path)` 和 `appStore.setCurrentFile(path)` 的核心逻辑封装到 `appStore.openFile(path)` action 中。所有触发文件打开的地方（按钮点击、拖拽）都统一调用此 action。

### 1.3 改进点
*   **提升用户体验**：提供了直观、现代的文件打开和拖拽交互。
*   **环境兼容性**：确保在 Tauri 和浏览器开发环境下都能正常工作。
*   **代码整洁**：通过 Store 和统一 Action，避免了逻辑重复，提高了可维护性。

---

## 2. 书签 Text-Tree 同步问题与后端解耦

### 2.1 问题
前端 `TextSubView` 和 `TreeSubView` 在切换时内容不同步，因为 `TreeSubView` 未与全局状态 (Store) 绑定。同时，后端 `Bookmark` 实体类因包含循环引用（`parent` 字段）而无法直接通过 Gson 序列化到前端，导致 `StackOverflowError`。

### 2.2 解决方案
1.  **后端 DTO 引入**：
    *   创建 `src/main/java/com/ririv/quickoutline/api/model/BookmarkDto.java` 作为书签数据传输对象。`BookmarkDto` 只包含前端所需的 `id`, `title`, `page`, `level`, `children` 字段，不含 `parent` 字段，从而避免循环引用。
    *   在 `BookmarkDto` 中提供 `fromDomain()` 和 `toDomain()` 静态方法，用于 `Bookmark` 实体和 DTO 之间的相互转换。
2.  **后端 RPC 接口扩展**：
    *   `ApiService.java` 暴露 `BookmarkDto parseTextToTree(String text)` 和 `String serializeTreeToText(Bookmark root)` RPC 接口。
    *   `ApiServiceImpl.java` 实现这些接口，`parseTextToTree` 调用 `PdfOutlineService` 进行文本解析，然后将 `Bookmark` 实体转换为 `BookmarkDto` 返回；`serializeTreeToText` 则将前端传来的 `BookmarkDto` 转换为 `Bookmark` 实体，再调用 `toOutlineString()` 序列化为文本。
    *   `RpcProcessor.java` 注册这些新的 RPC 命令，并负责 `BookmarkDto` 的序列化/反序列化。
3.  **前端视图同步**：
    *   `web/src/components/bookmark/TreeSubView.svelte` 在 `onMount` 时，从 `bookmarkStore.text` 获取文本，调用 `rpc.parseTextToTree` 解析为树状结构并显示。
    *   在 `onDestroy` (或未来修改树结构时)，调用 `rpc.serializeTreeToText` 将当前树结构序列化为文本并更新 `bookmarkStore.text`。

### 2.3 改进点
*   **数据同步**：实现了前端 Text-Tree 视图的数据双向同步。
*   **后端解耦**：通过 DTO 将领域模型与 API 传输模型解耦，解决了循环引用问题，提升了 API 的健壮性和安全性。
*   **代码整洁**：统一了数据转换逻辑，避免了前端重复实现后端复杂的解析逻辑。

---

## 3. PDF 缩略图懒加载与后端性能优化

### 3.1 问题
原 PDF 缩略图加载方式效率低下（无法按需加载，HTTP 服务/RPC 存在阻塞 Event Loop 的风险），且 `PdfImageService` 和 `PdfRenderSession` 之间存在功能重叠但未有效整合。

### 3.2 解决方案
1.  **后端 PDF 渲染服务重构**：
    *   **`PdfRenderSession` 增强**：将其改造为 PDF 渲染的核心组件。
        *   支持从 `File` 和 `RandomAccessRead` (内存流) 两种源创建会话。
        *   提供 `renderWithDPI` / `renderWithScale` 等同步渲染方法，以及 `renderToPngWithDPIAsync` / `renderToPngWithScaleAsync` 等异步渲染方法。
        *   定义统一的 DPI 和 Scale 常量 (`THUMBNAIL_DPI`, `PREVIEW_DPI`, `THUMBNAIL_SCALE`, `PREVIEW_SCALE`)。
    *   **`PdfImageService` 整合 Session**：
        *   管理一个 `PdfRenderSession currentSession`。在 `openFile` 时创建文件Session，`diffPdfToImages` 时创建临时流Session。
        *   `getImageData(index)` (用于文件浏览缩略图) 优先从内存缓存读取，缓存未命中时，则异步调用 `currentSession.renderToPngWithDPIAsync(index, PdfRenderSession.PREVIEW_DPI)` (使用 300 DPI)。
        *   `diffPdfToImages` (用于 TOC 预览) 现在使用临时的 `PdfRenderSession` 来同步渲染，并使用 `PdfRenderSession.PREVIEW_SCALE` (Scale 2.0) 保持原有行为。
    *   **后端 RPC 异步化**：
        *   `ApiService` 暴露 `CompletableFuture<byte[]> getPreviewImageDataAsync(int pageIndex)`。
        *   `ApiServiceImpl` 实现 `getPreviewImageDataAsync`，并修改 `getThumbnail` (RPC 接口) 在内部阻塞等待。
        *   `WebSocketRpcHandler`：将 RPC 处理逻辑迁移到 Vert.x 的 Worker 线程 (`executeBlocking`)，防止阻塞 Event Loop。
        *   `SidecarApp` (HTTP 服务)：更新 `/page_images/` 接口，使用 `apiService.getPreviewImageDataAsync` 异步处理图片请求，防止阻塞 Event Loop。
2.  **前端缩略图组件优化**：
    *   `appStore` 增加了 `serverPort` 状态，`RpcProvider` 连接成功后更新此状态。
    *   `ThumbnailPane.svelte` 不再依赖 RPC 传 Base64，而是利用 `IntersectionObserver` 结合 HTTP URL (`http://127.0.0.1:{$appStore.serverPort}/page_images/{index}.png`) 实现缩略图的懒加载。

### 3.3 改进点
*   **性能提升**：解决了后端 Event Loop 阻塞问题，极大地提升了处理大文件时的响应速度和并发能力。
*   **用户体验**：缩略图实现懒加载，应用打开文件时不再卡顿，滚动预览流畅。
*   **代码复用与整洁**：统一了 PDF 渲染的核心逻辑，减少了代码重复，提高了模块化和可维护性。

---

## 4. UI 细节优化

*   **`FileHeader.svelte`**：
    *   优化了未打开文件时的提示样式，使其更简洁、居中且具引导性（"Click to Open or Drop PDF"）。
    *   统一了文件名显示和打开文件的交互。
*   **`Preview.svelte`**：
    *   优化了底部工具栏和刷新按钮的显示逻辑，使其在非活动状态下低调，悬停时高亮，减少对文档内容的干扰。

---

通过以上改进，QuickOutline 在 Web 前端的交互体验、后端服务的性能和代码的可维护性上都得到了显著提升。
