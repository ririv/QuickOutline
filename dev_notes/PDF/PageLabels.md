在 PDF 标准 (ISO 32000) 中，**Page Labels (页面标签)** 是通过 **Document Catalog** 中的一个名为 **`PageLabels`** 的条目来记录的。

具体的结构如下：

1.  **Document Catalog**:
    *   PDF 文件的根对象（Root）。
    *   包含一个键 `PageLabels`。

2.  **Number Tree (数字树)**:
    *   `PageLabels` 的值是一个 **Number Tree** 结构。
    *   在最简单的情况下（没有分层），它包含一个名为 **`Nums`** 的数组。
    *   **`Nums` 数组结构**: `[Key, Value, Key, Value, ...]`
        *   **Key (Integer)**: 页面索引（从 0 开始）。表示一个新的标签规则从这一页开始生效。
        *   **Value (Dictionary)**: 一个描述标签规则的字典（Page Label Dictionary）。

3.  **Page Label Dictionary (标签规则字典)**:
    包含以下键（均可选）：
    *   **`Type`** (Name): 必须是 `/PageLabel`（如果在 PDF 1.3 之前可能没有）。
    *   **`S`** (Name, Optional): **Style (编号样式)**。
        *   `/D`: 阿拉伯数字 (1, 2, 3...)
        *   `/R`: 大写罗马数字 (I, II, III...)
        *   `/r`: 小写罗马数字 (i, ii, iii...)
        *   `/A`: 大写字母 (A, B, C...)
        *   `/a`: 小写字母 (a, b, c...)
        *   (缺省): 无数字编号，只显示前缀。
    *   **`P`** (String, Optional): **Prefix (标签前缀)**。例如 "A-"。
    *   **`St`** (Integer, Optional): **Start (起始数值)**。
        *   该范围内第一页的数字编号起始值。
        *   默认为 1。

**示例结构 (伪代码)**:

```
Catalog
  |
  +-- PageLabels
       |
       +-- Nums: [
             0, << /S /r >>,              // 第0页(物理第1页)开始：i, ii, iii...
             4, << /S /D >>,              // 第4页(物理第5页)开始：1, 2, 3...
             10, << /S /A /P (App-) /St 1 >> // 第10页(物理第11页)开始：App-A, App-B...
           ]
```

**工作原理**:
*   阅读器在渲染第 `n` 页的页码时，会在 `Nums` 数组中查找**小于等于 `n` 的最大 Key**。
*   找到对应的规则字典后，计算偏移量 `offset = n - Key`。
*   显示的数字 = `St + offset`。
*   最终标签 = `P` + `format(DisplayNumber, S)`。

**去重机制**:
*   因为 `Nums` 是一个映射表（Map），同一个 Key（页面索引）只能对应一个 Value（规则）。
*   如果在 `Nums` 数组中出现了重复的 Key（如 `[0, DictA, 0, DictB]`），这在语法上是无效的 Number Tree，解析器的行为未定义（通常后一个覆盖前一个，或者报错）。我们在生成 PDF 时必须保证 Key 唯一且有序。