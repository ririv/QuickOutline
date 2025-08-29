# 缩略图窗格加载机制重构

本文档概述了重构缩略图加载机制以解决UI渲染错误和严重性能问题的过程。

## 1. 初始问题

最初使用 `ListView` 的实现存在两个主要问题：

1.  **渲染问题**：缩略图经常无法显示，尤其是在滚动之后。这很可能是由于 `ListView` 的单元格虚拟化机制与图片的异步加载之间的复杂交互导致的错误。
2.  **UI卡死**：在打开大型PDF文件时，应用程序会变得没有响应。

## 2. 解决方案的演进

最终的解决方案经过了三个迭代阶段的重构。

### 第一阶段：用 `TilePane` 替换 `ListView`

-   **操作**：将 `ListView` 替换为托管在 `ScrollPane` 内的 `TilePane`。
-   **原因**：这为显示项目网格提供了一种更健壮和常规的方法，避免了 `ListView` 单元格复用逻辑的复杂性。
-   **新问题**：此更改引入了严重的性能问题。所有的 `ThumbnailViewController` 实例都会被一次性创建并添加到 `TilePane` 中，导致打开有很多页的PDF时UI冻结。

### 第二阶段：实现“无限滚动”

-   **操作**：实现了一种批量加载机制，而不是一次性创建所有缩略图控件。
    -   只加载初始的一批缩略图（例如20个）。
    -   在 `ScrollPane` 的垂直滚动条上添加一个监听器 (`vvalueProperty`)，当用户滚动到底部时触发下一批的加载。
-   **原因**：这避免了在同一时间创建过多的UI节点，保持了UI的响应性。

```java
// 示例：用于批量加载的滚动监听器
scrollPane.vvalueProperty().addListener((obs, oldValue, newValue) -> {
    if (newValue.doubleValue() >= 0.98 && !isLoading) {
        loadMoreThumbnails();
    }
});
```

### 第三阶段：后台文件加载

-   **操作**：将初始的文件I/O和解析（`new PdfPreview(file)`）从JavaFX应用线程移至后台 `Task` 中。
-   **原因**：即使实现了无限滚动，UI在打开文件时仍然会瞬间冻结。这被追溯到 `PdfPreview` 构造函数阻塞了主线程。通过将此操作移到后台，UI在初始文件加载阶段保持响应。
-   **工作流程**：
    1.  提交一个后台 `Task` 来处理 `new PdfPreview(file)`。
    2.  当任务成功完成时，通知主UI线程。
    3.  UI线程接着调用 `loadMoreThumbnails()` 来加载*第一批*缩略图。

```java
// 示例：使用Task进行后台加载
Task<PdfPreview> loadFileTask = new Task<>() {
    @Override
    protected PdfPreview call() throws Exception {
        // 后台执行耗时操作
        return new PdfPreview(pdfFile);
    }
};

loadFileTask.setOnSucceeded(event -> {
    // 在FX线程上更新UI
    currentPreview = loadFileTask.getValue();
    loadMoreThumbnails();
});

fileLoadExecutor.submit(loadFileTask);
```

## 3. 最终架构

最终的稳定解决方案采用了多层异步方法：

1.  **后台文件I/O**：一个 `Task` 在不阻塞UI的情况下加载和解析PDF文件。
2.  **批量UI创建**：在UI线程上，通过用户滚动触发，分批次地创建缩略图并将其添加到 `TilePane` 中。
3.  **后台图像渲染**：每个单独缩略图图像的渲染由一个专用的 `ExecutorService` 处理，确保即使在填充缩略图时UI也保持流畅。
