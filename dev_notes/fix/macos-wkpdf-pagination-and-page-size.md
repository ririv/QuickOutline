# macOS WKPDF 分页与页面尺寸修复记录

## 背景

在 macOS Native PDF 生成路径中，QuickOutline 使用 `WKWebView.createPDFWithConfiguration` 生成目录/Markdown 的 PDF。预览使用 Paged.js，预览中分页、页眉页脚和内容位置看起来正确，但实际生成 PDF 时出现两个核心问题：

1. 生成结果只有一页，其它页内容丢失。
2. 页面尺寸不符合预期，尤其在默认继承原 PDF 第一页尺寸时，生成页大小和参考页不一致。

后续修复过程中又暴露出第三个问题：虽然可以把内容切成多页，但切片后的文字起始位置存在偏移，表现为顶部大块空白或内容被向上裁掉。

## 现象

最初现象：

- 使用 WKPDF 生成时，PDF 只有第一页。
- 其它 Paged.js 页面没有出现在最终 PDF 中。
- 输出页面尺寸和用户选择/自动继承的页面尺寸不一致。

中间修复过程中的现象：

- 使用“长页 PDF 后处理切分”后，确实可以得到多页。
- 但切分内容位置不稳定：
  - 一版第一页顶部出现大块空白。
  - 另一版内容明显偏到上方，导致部分内容被裁掉。
  - 后续改用 WKPDF 生成后的 `MediaBox/CropBox` 坐标继续修正后，偏移变小，但顶部仍残留空白。

最终确认：

- 长页后处理切分方案本身不可靠。
- 逐页 WKPDF 导出可以得到正确的页面内容拆分。
- 逐页导出后还需要统一页面物理尺寸，否则单页 PDF 的 `MediaBox/CropBox` 会沿用 WKPDF 基于 WebView/CSS rect 推导出来的尺寸。

## 根因分析

### 1. WKPDF 的 `rect` 不是分页设置

`WKPDFConfiguration.rect` 表示要导出的 WebView 内容矩形，不是纸张大小，也不会触发自动分页。

因此当代码把 `rect` 设置成 A4/参考页大小时，WKPDF 只导出了这个矩形区域，相当于只截取第一页大小的内容，后续页面自然丢失。

这解释了“只有一页”的问题。

### 2. Paged.js 预览正确不代表 WKPDF 会按 Paged.js 页面分页

Paged.js 会在 DOM 中生成多个 `.pagedjs_page` 元素。预览正确，是因为浏览器正常显示这些分页 DOM。

但 WKPDF 只按传入的 `rect` 导出 WebView 的某个矩形区域。它不会理解“这些 `.pagedjs_page` 应该成为 PDF 的多个页面”。

所以 WKPDF 路径必须显式处理每个 `.pagedjs_page`。

### 3. 长页 PDF 后处理切分不可靠

曾尝试过一个中间方案：

1. 等 Paged.js 渲染完成。
2. 导出包含所有页面的长 PDF。
3. 用 `lopdf` 按页面高度切分。

这个方案能把单页切成多页，但坐标偏移一直无法完全消除。原因是它混用了两套坐标：

- DOM / Paged.js 的 `getBoundingClientRect()`，单位是 CSS px。
- WKPDF 生成后 PDF 内容流和 `MediaBox/CropBox`，单位是 PDF points，且内容流内部坐标由 WebKit 自己生成。

直接用 DOM 高度推导 PDF 内容流平移量，会出现系统性偏移。改用 WKPDF 原始 `MediaBox/CropBox` 后有所改善，但仍无法完全消除顶部空白，说明 WKPDF 生成的内容流和 DOM rect 之间不是一个可稳定手工推导的简单线性切片模型。

因此这条路被放弃。

### 4. 页面尺寸自动继承曾经有两个事实来源

另一个独立但相关的问题是页面尺寸默认继承原 PDF 第一页时，状态栏显示和生成参数曾经不一致。

旧逻辑中：

- 状态栏显示可以直接使用 `layoutDetection.actualDimensions`。
- PDF 生成实际使用 `store.pageLayout.pageSize`。
- 自动检测结果写回 `store.pageLayout.pageSize` 的逻辑曾经放在 `PageSizePopup.svelte` 内，只有打开 Page Size 弹窗时才执行。

结果是不打开弹窗直接生成时，状态栏显示参考页尺寸，但生成仍可能使用默认 A4。

这个问题已通过把自动检测写回逻辑移动到 `usePdfPageSizeDetection` 源头解决，并单独提交。

## 调查与尝试过程

### 尝试 1：继续使用 WKPDF，设置单页 rect

做法：

- 给 `WKPDFConfiguration` 设置目标纸张大小的 `rect`。

结果：

- 页面尺寸看似可控。
- 但只导出第一页矩形，其它 Paged.js 页面丢失。

结论：

- `WKPDFConfiguration.rect` 不能作为分页机制使用。

### 尝试 2：切到 `NSPrintOperation`

做法：

- 使用 `WKWebView.printOperationWithPrintInfo`。
- 配置 `NSPrintInfo`：
  - `NSPrintSaveJob`
  - `NSPrintJobSavingURL`
  - 自定义 paper size
  - 0 margin
  - 自动分页

结果：

- 理论上更符合 macOS 打印模型。
- 但在 Tauri/WebView 环境中，`runOperation()` 会同步阻塞，用户点击生成后程序卡住。

结论：

- 不能直接在当前路径使用同步 `NSPrintOperation.runOperation()`。
- 除非后续能找到可靠的异步/回调式 AppKit 打印方案，否则该方案不适合作为当前修复。

### 尝试 3：WKPDF 导出长页，再用 `lopdf` 切分

做法：

- 等 Paged.js 渲染完成。
- 采集 `.pagedjs_page` 数量和单页高度。
- 让 WKPDF 导出一个包含所有页面的长 rect。
- 使用 `lopdf` 重写页面树和内容流，把长页切成多个页面。

结果：

- 成功得到多页。
- 但内容位置不稳定：
  - 使用 PDF 底部坐标推导时，顶部出现大块空白。
  - 使用视觉顶部顺序推导时，内容被向上裁掉。
  - 改用 WKPDF 原始 `MediaBox/CropBox` 后，偏移减小但仍有顶部空白。

结论：

- 长页后处理切分无法稳定复原 WKPDF 内容坐标。
- 不应继续通过猜测平移量修正。

### 尝试 4：逐页 WKPDF 导出，再合并

做法：

- 等 Paged.js 渲染完成。
- 读取所有 `.pagedjs_page` 的 `getBoundingClientRect()`。
- 对每个页面 rect 单独调用 `WKWebView.createPDFWithConfiguration`。
- 每次只导出一页对应的 DOM rect。
- 使用 PDFium 将这些单页 PDF 合并。

结果：

- 页面拆分位置正确。
- 不再需要手动切分 content stream。
- 解决了顶部空白和内容裁切问题。

剩余问题：

- 单页 WKPDF 导出的 PDF 物理尺寸不一定等于目标纸张尺寸。

### 尝试 5：合并后归一化页面尺寸

做法：

- 合并逐页 WKPDF 输出后，再用 `lopdf` 遍历每一页。
- 读取当前页实际 `MediaBox/CropBox`。
- 计算目标纸张尺寸和当前页面尺寸的缩放比例。
- 包一层 PDF 内容矩阵：

```pdf
q
scale_x 0 0 scale_y 0 0 cm
...original content...
Q
```

- 将每页 `MediaBox/CropBox` 重设为目标纸张尺寸。

结果：

- 保留逐页 WKPDF 的正确内容拆分。
- 页面物理尺寸恢复为目标尺寸。
- 内容按比例缩放到目标页面，不再因为重设页面盒子被裁掉。

结论：

- 这是当前可靠方案。

## 最终方案

最终 macOS Native WKPDF 生成流程：

1. 创建隐藏的 `WKWebView`。
2. 加载 HTML/URL。
3. 等待 Paged.js 渲染完成：
   - `document.body.classList.contains('pagedjs_ready')`
   - 采集 `.pagedjs_page` 列表和每页 rect。
4. 对每个 `.pagedjs_page`：
   - 使用该页的 `left/top/width/height` 作为 `WKPDFConfiguration.rect`。
   - 调用 `createPDFWithConfiguration` 导出单页 PDF bytes。
5. 使用 PDFium 创建新 PDF，并按顺序导入所有单页 PDF。
6. 使用 `lopdf` 归一化最终 PDF 每页尺寸：
   - 读取每页原始 `MediaBox/CropBox`。
   - 缩放内容流到目标尺寸。
   - 重设 `MediaBox/CropBox` 为目标页面尺寸。

对应核心函数：

- `wait_for_wkpdf_metrics(...)`
  - 等待 Paged.js 完成并采集页面 rect。
- `create_wkpdf_for_rect(...)`
  - 用 WKPDF 导出单个页面 rect。
- `merge_wkpdf_pages(...)`
  - 使用 PDFium 合并逐页导出的 PDF。
- `normalize_pdf_page_sizes(...)`
  - 使用 `lopdf` 归一化最终页面尺寸。

## 关键经验

1. **`WKPDFConfiguration.rect` 是截图/导出矩形，不是分页配置。**
2. **Paged.js 生成的分页 DOM 不会自动变成 WKPDF 的 PDF 页面。**
3. **不要用 DOM/CSS px 坐标去手动推导 PDF content stream 的切片平移。**
4. **对 WKPDF 这种输出，逐页导出比长页后处理切分更稳定。**
5. **逐页导出解决内容位置，页面物理尺寸需要单独归一化。**
6. **UI 显示尺寸和生成参数必须来自同一个 source of truth。**

## 验证

已验证：

- Paged.js 预览不再因隐藏容器触发 `offsetParent` 空指针错误。
- macOS WKPDF 逐页导出后，分页位置正确。
- 合并后页面尺寸通过 `MediaBox/CropBox` 归一化。
- 页面尺寸自动继承逻辑已移动到 `usePdfPageSizeDetection` 源头，避免显示尺寸和生成尺寸不一致。

已运行过的检查：

```bash
npm run check -- --threshold error
cargo check -p quickoutline --quiet
cargo test -p quickoutline split_wkpdf_long_page --quiet
```

## 后续注意

- `NSPrintOperation` 方案暂时保留代码，但不要直接切回同步 `runOperation()`，否则可能再次导致 UI 卡住。
- 如果未来要重启 `NSPrintOperation` 方案，应优先研究异步/回调式运行方式，并确认保存路径和 UI 线程不会互相阻塞。
- 当前逐页 WKPDF 方案依赖 Paged.js 输出的 `.pagedjs_page` rect；如果未来更换分页引擎，需要同步调整页面 rect 采集逻辑。
