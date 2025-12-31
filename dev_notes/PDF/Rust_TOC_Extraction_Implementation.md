---
title: Rust TOC Extraction Implementation Guide
date: 2025-12-31
author: Gemini Agent
status: Completed
module: src-tauri/src/pdf_analysis
---

# Rust PDF 目录提取模块技术文档

本文档详细描述了 `src-tauri/src/pdf_analysis/` 模块的设计与实现。该模块旨在提供一个高性能、高精度的 PDF 视觉文本块提取与目录（TOC）识别引擎。

## 1. 模块概览

### 1.1 设计目标
*   **纯视觉分析**：不依赖 PDF 内部已有的 Outline/Bookmark 结构，完全通过分析页面上的文本布局（位置、字体、间距）来重构语义块。
*   **高性能并发**：利用手动线程池实现多核并行处理，大幅缩短大文件的分析时间。
*   **线程安全与稳定**：通过全局单例和串行化加载策略，彻底规避底层 PDF 库（PDFium）的线程安全限制及 `dlopen` 死锁风险。
*   **Unicode 完备**：完整支持 Unicode 数学符号、全角标点及特殊的 TitleCase 字符判定。

### 1.2 核心组件
*   **`TocExtractor`**: 流程控制器，负责任务分发和并行调度。
*   **`PdfProcessor`**: 页面级处理器，负责字符流到文本行（Line）再到文本块（Block）的转换，以及高精度的排版参数推导。
*   **`TocAnalyser`**: 语义级分析器，负责识别正文样式并筛选目录条目。
*   **`TextMetrics`**: （已废弃）排版度量工具，原用于计算字符宽度，现已由 `PdfProcessor` 的全页推导逻辑取代。

---

## 2. 核心算法流程

### 2.1 阶段一：并行文本块提取 (Parallel Block Extraction)

为了在保证绝对稳定性的前提下复刻 Java 版的高并发性能，我们采用了 **手动线程池 + 全局单例 + 串行加载** 的架构。这是为了解决 Rust FFI 与 PDFium 在多线程环境下的复杂交互问题（如 `!Send` 约束和 `dlopen` 锁竞争）。

1.  **初始化**: 主线程初始化 **全局单例 PDFium 绑定** (`crate::pdf::get_pdfium()`)。确保动态库只加载一次，避免重复 `dlopen` 导致的死锁。
2.  **分块 (Chunking)**: 将页码范围 `0..num_pages` 划分为 $N$ 个 Chunk（$N$ 为 CPU 逻辑核心数）。
3.  **手动线程池**: 使用 `std::thread::spawn` 启动 $N$ 个 Worker 线程。
4.  **资源隔离与串行化**:
    *   每个 Worker 线程获取全局 PDFium 单例。
    *   在调用 `load_pdf_from_file` 加载文档时，使用全局互斥锁 **`PDF_LOAD_MUTEX`** 进行串行化保护。这是为了规避 PDFium 某些底层实现可能存在的非重入风险。
    *   一旦文档加载完成（得到线程独立的 `PdfDocument`），互斥锁立即释放。
    *   后续的 **页面提取 (`extract_blocks_from_page`)** 是完全并行执行的，互不干扰。
5.  **结果合并**: 主线程等待所有 Worker 完成 (`join`)，并合并结果。

### 2.2 阶段二：页面级处理 (Page Processing)

在 `PdfProcessor::extract_blocks_from_page` 中，我们执行以下步骤：

#### A. 字符排序与清洗
*   获取页面所有字符对象。
*   **内容清洗**: 移除 `char_info.unicode_string()` 中自带的 `\r` 或 `\n` 字符，确保输出的换行逻辑完全由后续的几何坐标差驱动，防止产生双倍空行。
*   **排序规则**:
    1.  **Y 轴 (Top to Bottom)**: 只有当两字符基线 Y 坐标完全一致时，才比较 X 轴。
    2.  **X 轴 (Left to Right)**: 标准阅读顺序。

#### B. 排版参数全页推导 (Page-Level Metrics Inference)
为了解决 `tight_bounds`（墨水宽度）无法准确反映排版间距（Advance Width）的问题，我们实现了一套 **数据驱动的推导算法**：

1.  **Advance Width 推导**:
    *   对于页面上的每一个字符 $C_i$，检查其下一个字符 $C_{i+1}$。
    *   如果两者位于同一行（基线 Y 差值 < 1.0pt）且水平距离合理（$< 1.5 	imes FontSize$），则推导 $C_i$ 的 **逻辑宽度 (Advance Width)** 为 $X_{i+1} - X_i$。这自动包含了字间距。
    *   如果不满足条件（如行尾），则回退到 $LooseBounds$ 宽度。
    *   **结果**: 所有 `TextChunk` 的 `width` 字段都填充为这个推导出的逻辑宽度，而非视觉宽度。

2.  **全局空格宽度推导 ($S_{page}$)**:
    *   **优先法**: 如果页面上存在真实的空格字符 `' '`，取其推导宽度。
    *   **兜底法**: 如果无空格，计算全页所有字符推导宽度的 **平均值 (AvgWidth)**。
    *   这完美复刻了 iText 的逻辑（优先用空格宽，否则用字体平均宽），但在无法直接访问字体元数据的情况下，使用了更准确的实测统计值。

#### C. 物理行构建 (Line Construction)
*   **分行阈值**: 遍历排序后的字符，计算 `abs(当前字符.BaselineY - 上一字符.BaselineY)`。
    *   如果差值 `> 1.0 pt`，判定为新的一行。
*   **元数据记录**:
    *   对于每一行，记录其 `BoundingBox` (包围盒)、`BaselineY` (基线位置)、`AvgFontSize` (平均字号)、`Style` (字体名与大小)。
    *   **TextChunks**: 完整保留行内每个字符/词元的原始位置信息，用于后续的精确空格重构。

#### D. 语义块合并 (Block Aggregation)
将物理行合并为逻辑段落（Block）。对于相邻的两行（Line A 和 Line B），只有满足**所有**以下条件才进行合并：

1.  **同页**: 必须位于同一页。
2.  **字体一致**: Line B 的字体名称和大小必须与 Line A 完全一致。
3.  **左对齐**: Line B 的左边缘与 Line A 的左边缘水平差距 `abs(A.left - B.left) <= 5.0 pt`。
4.  **行距合理**: 垂直基线间距 `abs(A.y - B.y)` 必须小于 `A.avg_font_size * 1.8`。
5.  **标点阻断**: Line A **不能**以结束性标点结尾（`. ! ? :` 及对应的全角符号 `。 ！ ？ ： ．`）。
6.  **编号阻断**: Line B **不能**以列表编号开头（正则匹配：数字+点、罗马数字、字母+点等，如 `1.` `IV.` `a.`）。
7.  **大小写接续**: 如果 Line B 以小写字母开头，则放宽上述限制（通常意味着句子未完待续）。

### 2.3 阶段三：目录语义分析 (TOC Analysis)

#### A. 全局样式统计 (Global Style Analysis)
*   统计全文档所有 Block 的首行样式。
*   频率最高的样式被定义为 **Dominant Style**（正文样式）。这是识别“异常”目录项（如字号加大的章标题）的基准。

#### B. 目录项筛选 (Filtering)
再次并行扫描每一页的 Block（此阶段使用 `rayon`），筛选符合目录特征的候选者：

*   **特征 1 (引导符)**: 包含目录常见的点线引导符 (Dot Leaders)，正则：`.*([.]
	*|
	{2,}){4,}
	*
	+
	*$`。
*   **特征 2 (数字结尾)**: 以数字结尾，且字体大小显著大于正文样式 (`> DominantSize + 0.5pt`)，且长度适中 (`< 80` 字符)。
*   **长度过滤**: 剔除过短 (`< 3`) 或过长 (`> 150`) 的文本块。

只有当单个页面中包含 **至少 3 个** 符合上述特征的 Block 时，该页才被认定为“目录页”，其包含的候选块会被提取为最终结果。

---

## 3. 实现细节与特性

### 3.1 文本重构 (Text Reconstruction)
在输出最终文本时，我们不使用简单的字符串拼接，而是基于 `TextChunk` 的位置进行**视觉重构**：

*   **空格计算**: 使用前述推导出的 $S_{page}$ 作为基准。
*   **阈值判定**:
    *   计算 Gap: `Next.X - (Curr.X + Curr.AdvanceWidth)`。由于使用了 Advance Width，紧邻字符的 Gap 接近 0。
    *   **插入逻辑**:
        *   `Gap > 6.0 * SpaceWidth`: 插入 5 个空格（大间隙）。
        *   `Gap > 0.5 * SpaceWidth`: 插入 1 个空格（小间隙）。
*   **换行符规范化 (Normalization)**:
    *   **现象**: PDF 内部文本流可能包含来自源文档的 `\r` (CR) 或 `\r\n` (CRLF) 字符（常见于 Windows 平台生成的 PDF）。
    *   **处理**: 在输出前，所有文本块均统一规范化为 `\n` (LF)。
    *   **目的**: 防止前端编辑器 (CodeMirror) 接收到“脏”文本后自动执行内部清洗，从而避免与 Svelte 5 的响应式状态产生死循环冲突（Effect Update Depth Exceeded）。

### 3.2 Unicode 深度支持
*   **数学符号**: 使用正则 `
	{Sm}` 精确匹配 Unicode 数学符号类别，确保公式中的符号不会干扰分词或样式统计。
*   **TitleCase**: 使用正则 `
	{Lt}` 支持 Unicode Titlecase 字母（如连字 `ǅ`），将其视为大写字母处理，确保样式统计的准确性。

### 3.3 视觉坐标系统
*   **宽度计算**: 采用 `LooseBounds` 作为回退，并优先使用位置推导。
*   **基线对齐**: 所有垂直间距计算均基于 **Baseline Y**，而非包围盒底部。这符合排版学原理，避免了因字符下伸部（Descenders，如 g, p, y）导致的行距抖动。

---

## 4. 依赖项

*   `rayon`: 用于第二阶段（纯计算）的并行迭代。
*   `std::thread`: 用于第一阶段（IO/FFI 混合）的手动并行控制。
*   `pdfium-render`: PDF 解析引擎绑定。
*   `regex`: 正则表达式（含 Unicode 支持）。
*   `serde`: 数据序列化。

---
