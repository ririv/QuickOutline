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
*   **高性能并发**：利用 Rust 的 `rayon` 库实现多核并行处理，大幅缩短大文件的分析时间。
*   **线程安全**：通过资源隔离策略，规避底层 PDF 库（PDFium）的线程安全限制。
*   **Unicode 完备**：完整支持 Unicode 数学符号、全角标点及特殊的 TitleCase 字符判定。

### 1.2 核心组件
*   **`TocExtractor`**: 流程控制器，负责任务分发和并行调度。
*   **`PdfProcessor`**: 页面级处理器，负责字符流到文本行（Line）再到文本块（Block）的转换。
*   **`TocAnalyser`**: 语义级分析器，负责识别正文样式并筛选目录条目。
*   **`TextMetrics`**: 排版度量工具，负责计算字符宽度和空格逻辑。

---

## 2. 核心算法流程

### 2.1 阶段一：并行文本块提取 (Parallel Block Extraction)

由于 PDF 解析是计算密集型与 I/O 密集型混合任务，且底层 PDF 引擎不支持多线程共享文档句柄，我们采用了 **分块并行 (Chunk-based Parallelism)** 架构。

1.  **初始化**: 主线程加载文档，仅获取总页数 `num_pages`。
2.  **分块 (Chunking)**: 将页码范围 `0..num_pages` 划分为 $N$ 个 Chunk（$N$ 为 CPU 逻辑核心数）。
3.  **并行执行**:
    *   使用 `rayon::par_iter` 并行处理每个 Chunk。
    *   **资源隔离**: 每个线程内部调用 `crate::pdf::init_pdfium()` 初始化独立的 PDFium 绑定，并加载独立的文档实例。
    *   **提取**: 对 Chunk 内的每一页调用 `PdfProcessor::extract_blocks_from_page`。
4.  **结果合并**: 所有线程的结果被收集并展平为 `Vec<PdfBlock>`。

### 2.2 阶段二：页面级处理 (Page Processing)

在 `PdfProcessor::extract_blocks_from_page` 中，我们执行以下步骤：

#### A. 字符排序与清洗
*   获取页面所有字符对象。
*   **排序规则**:
    1.  **Y 轴 (Top to Bottom)**: 只有当两字符基线 Y 坐标完全一致时，才比较 X 轴。
    2.  **X 轴 (Left to Right)**: 标准阅读顺序。

#### B. 物理行构建 (Line Construction)
*   **分行阈值**: 遍历排序后的字符，计算 `abs(当前字符.BaselineY - 上一字符.BaselineY)`。
    *   如果差值 `> 1.0 pt`，判定为新的一行。
*   **元数据记录**:
    *   对于每一行，记录其 `BoundingBox` (包围盒)、`BaselineY` (基线位置)、`AvgFontSize` (平均字号)、`Style` (字体名与大小)。
    *   **TextChunks**: 完整保留行内每个字符/词元的原始位置信息，用于后续的精确空格重构。

#### C. 语义块合并 (Block Aggregation)
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
再次并行扫描每一页的 Block，筛选符合目录特征的候选者：

*   **特征 1 (引导符)**: 包含目录常见的点线引导符 (Dot Leaders)，正则：`.*([.]\s*|\s{2,}){4,}\s*\d+\s*$`。
*   **特征 2 (数字结尾)**: 以数字结尾，且字体大小显著大于正文样式 (`> DominantSize + 0.5pt`)，且长度适中 (`< 80` 字符)。
*   **长度过滤**: 剔除过短 (`< 3`) 或过长 (`> 150`) 的文本块。

只有当单个页面中包含 **至少 3 个** 符合上述特征的 Block 时，该页才被认定为“目录页”，其包含的候选块会被提取为最终结果。

---

## 3. 实现细节与特性

### 3.1 文本重构 (Text Reconstruction)
在输出最终文本时，我们不使用简单的字符串拼接，而是基于 `TextChunk` 的位置进行**视觉重构**：

*   **空格计算**:
    *   优先读取字体文件中的空格宽度。
    *   **兜底策略**: 如果字体未提供（返回 0），则使用 `FontSize * 0.25` 作为估算值。
*   **插入逻辑**:
    *   如果是大间隙 (`Gap > 5 * SpaceWidth`)：插入 5 个空格（模拟 Tab/对齐）。
    *   如果是小间隙 (`Gap > 0.3 * SpaceWidth`)：插入 1 个空格。

### 3.2 Unicode 深度支持
*   **数学符号**: 使用正则 `\p{Sm}` 精确匹配 Unicode 数学符号类别，确保公式中的符号不会干扰分词或样式统计。
*   **TitleCase**: 使用正则 `\p{Lt}` 支持 Unicode Titlecase 字母（如连字 `ǅ`），将其视为大写字母处理，确保样式统计的准确性。

### 3.3 视觉坐标系统
*   **宽度计算**: 采用 `BoundingBox.Right - BoundingBox.Left` 计算行宽，而非字符宽度的简单累加。这在处理分散对齐（Justified Alignment）或存在字间距调整（Kerning/Tracking）的文本时更加准确。
*   **基线对齐**: 所有垂直间距计算均基于 **Baseline Y**，而非包围盒底部。这符合排版学原理，避免了因字符下伸部（Descenders，如 g, p, y）导致的行距抖动。

---

## 4. 依赖项

*   `rayon`: 并行迭代器。
*   `pdfium-render`: PDF 解析引擎绑定。
*   `regex`: 正则表达式（含 Unicode 支持）。
*   `serde`: 数据序列化。

---