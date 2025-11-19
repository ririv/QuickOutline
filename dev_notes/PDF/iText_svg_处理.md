## 相关类
com/itextpdf/html2pdf/attach/impl/tags/SvgTagWorker.java
com/itextpdf/html2pdf/util/SvgProcessingUtil.java
com/itextpdf/svg/SvgConstants.java
com/itextpdf/svg/utils/SvgCssUtils.java
com/itextpdf/styledxmlparser/css/util/CssDimensionParsingUtils.java

## 流程

### size处理
SvgTagWorker.processEnd()
SvgProcessingUtil.createXObjectFromProcessingResult()
SvgCssUtils.extractWidthAndHeight() 这里包含了viewBox的处理 SvgCssUtils.parseViewBox
SvgCssUtils.calculateFinalSvgRendererLength()
CssTypesValidationUtils.parseRelativeValue()|parseAbsoluteLength()

## ex单位处理

当然可以，很高兴能为你深入讲解。

### 1. 什么是 `ex` 单位？

`ex` 是 CSS 中的一个**相对长度单位**。它的定义是基于当前字体的 **"x-height"**。

*   **什么是 "x-height"？**
    简单来说，"x-height" 就是当前字体中，小写字母 'x' 的高度。它代表了小写字母主体部分的高度，不包括向上延伸的部分（如 'h', 'd'）和向下延伸的部分（如 'p', 'g'）。

*   **为什么 MathJax 要用它？**
    `ex` 单位对于排版数学公式非常有用。因为它与字体的小写字母主体高度相关，所以使用 `ex` 作为单位，可以让公式中的符号（如分数线、根号等）与周围的文字在视觉上对得更整齐，看起来更和谐。

*   **`ex` 和 `em` 的关系**
    在 CSS 渲染中，精确获取一个字体的 "x-height" 有时比较困难。因此，行业内有一个普遍接受的、非常通用的**近似换算标准**：

    **`1ex ≈ 0.5em`**

    这里的 `em` 单位就是当前元素的 `font-size`（字号）。所以，如果一个元素的 `font-size` 是 `16pt`，那么 `1em` 就是 `16pt`，而 `1ex` 就约等于 `8pt`。

### 2. `parseRelativeValue` 方法深度解析

现在我们来逐行分析 iText 的这个工具方法，看看它是如何实现单位换算的：

```java
public static float parseRelativeValue(final String relativeValue, final float baseValue) {
    // 1. 分离数值和单位
    // 例如，对于 "5.8ex"，它会找到 'e' 的位置 pos=3
    int pos = CssDimensionParsingUtils.determinePositionBetweenValueAndUnit(relativeValue);
    if (pos == 0) {
        return 0f;
    }

    // 2. 解析出数值部分
    // 例如，f = Double.parseDouble("5.8")，结果是 5.8
    double f = Double.parseDouble(relativeValue.substring(0, pos));

    // 3. 获取单位部分
    // 例如，unit = "ex"
    String unit = relativeValue.substring(pos);

    // 4. 根据单位进行计算
    if (unit.startsWith(CommonCssConstants.PERCENTAGE)) { // 如果是 "%"
        f = baseValue * f / 100; // 用基准值乘以百分比
    } else if (unit.startsWith(CommonCssConstants.EM) || unit.startsWith(CommonCssConstants.REM)) { // 如果是 "em" 或 "rem"
        f = baseValue * f; // 直接用基准值（通常是 font-size）乘以数值
    } else if (unit.startsWith(CommonCssConstants.EX)) { // 如果是 "ex"
        // 关键在这里！
        f = baseValue * f / 2;
    }

    return (float) f;
}
```

**重点分析 `f = baseValue * f / 2;` 这一行：**

*   `baseValue`：是我们传入的当前元素的 `font-size`，单位是 `pt`。这其实就是 `1em` 的长度。
*   `f`：是我们从字符串中解析出的 `ex` 前面的数值（例如 `5.8`）。
*   所以，`baseValue * f` 就是 `(1em的长度) * 5.8`。
*   最后再 `/ 2`，就变成了 `(1em的长度) * 5.8 / 2`，等价于 `5.8 * (1em的长度 / 2)`。
*   根据我们前面说的 `1ex ≈ 0.5em`，这里的 `(1em的长度 / 2)` 正好就是 `1ex` 的长度。

**结论**：iText 的 `parseRelativeValue` 方法，完全正确地实现了 `1ex ≈ 0.5em` 这个标准的近似换算。

### 3. 我们的实现

见 MjxSvgTagWorker

## 行内公式对齐处理

### 行内处理
MathJax 自定义的标签 `<mjx-container>` 使用 MjxContainerTagWorker 处理，继承自 SpanTagWorker。注意，如果不继承自 SpanTagWorker，那么 `<mjx-container>` 的内容默认会占一行。我之前的的问题就是，`<mjx-container>` 的内容默认会占一行，会在上面一行，但左边的文字会在下方一行，但文字和公式左右时衔接的（正确），上下也是衔接的（不正确，不在同一行）。

### 垂直向下偏移处理
MathJax 行内公式默认是垂直向下偏移的，这个偏移量在 style 属性中定义了，如 `vertical-align: -0.691ex;`。

这个偏移量处理本该在 BlockCssApplier.apply() 中调用 VerticalAlignmentApplierUtil.applyVerticalAlignmentForBlocks(cssProps, container, isInlineItem(tagWorker));处理

BlockCssApplier.java
```java
    private static boolean isInlineItem(ITagWorker tagWorker) {
        return tagWorker instanceof SpanTagWorker ||
                tagWorker instanceof ImgTagWorker;
    }
```

VerticalAlignmentApplierUtil.java
```java

    /**
     * Apply vertical alignment to inline elements.
     *
     * @param cssProps the CSS properties
     * @param element the styles container
     * @param isInlineTag whether the origin is a tag that defaults to inline
     */
    public static void applyVerticalAlignmentForBlocks(Map<String, String> cssProps, IPropertyContainer element,
            boolean isInlineTag ) {
        String display = cssProps.get(CssConstants.DISPLAY);
        if (isInlineTag || CssConstants.INLINE_BLOCK.equals(display)) {
            String vAlignVal = cssProps.get(CssConstants.VERTICAL_ALIGN);
            if (CssConstants.MIDDLE.equals(vAlignVal)) {
                element.setProperty(Property.INLINE_VERTICAL_ALIGNMENT,
                        new InlineVerticalAlignment(InlineVerticalAlignmentType.MIDDLE));
            } else if (CssConstants.BOTTOM.equals(vAlignVal)) {
                element.setProperty(Property.INLINE_VERTICAL_ALIGNMENT, 
                        new InlineVerticalAlignment(InlineVerticalAlignmentType.BOTTOM));
            } else if (CssConstants.TOP.equals(vAlignVal)) {
                element.setProperty(Property.INLINE_VERTICAL_ALIGNMENT, 
                        new InlineVerticalAlignment(InlineVerticalAlignmentType.TOP));
            } else if (CssConstants.TEXT_BOTTOM.equals(vAlignVal)) {
                element.setProperty(Property.INLINE_VERTICAL_ALIGNMENT, 
                        new InlineVerticalAlignment(InlineVerticalAlignmentType.TEXT_BOTTOM));
            } else if (CssConstants.TEXT_TOP.equals(vAlignVal)) {
                element.setProperty(Property.INLINE_VERTICAL_ALIGNMENT, 
                        new InlineVerticalAlignment(InlineVerticalAlignmentType.TEXT_TOP));
            } else if ( CssConstants.SUPER.equals((vAlignVal))) {
                element.setProperty(Property.INLINE_VERTICAL_ALIGNMENT, 
                        new InlineVerticalAlignment(InlineVerticalAlignmentType.SUPER));
            } else if ( CssConstants.SUB.equals((vAlignVal))) {
                element.setProperty(Property.INLINE_VERTICAL_ALIGNMENT, 
                        new InlineVerticalAlignment(InlineVerticalAlignmentType.SUB));
            } else if ( CssTypesValidationUtils.isPercentageValue(vAlignVal) ) {
                element.setProperty(Property.INLINE_VERTICAL_ALIGNMENT, 
                        new InlineVerticalAlignment(InlineVerticalAlignmentType.FRACTION,
                        CssDimensionParsingUtils.parseRelativeValue(vAlignVal,1)));
            } else if ( CssTypesValidationUtils.isValidNumericValue(vAlignVal) ) {
                element.setProperty(Property.INLINE_VERTICAL_ALIGNMENT, 
                        new InlineVerticalAlignment(InlineVerticalAlignmentType.FIXED,
                        CssDimensionParsingUtils.parseAbsoluteLength(vAlignVal)));
            } else {
                element.setProperty(Property.INLINE_VERTICAL_ALIGNMENT, 
                        new InlineVerticalAlignment(InlineVerticalAlignmentType.BASELINE));
            }
        }
    }
```

首先目前的 `isInlineTag` 没处理 svg tag 为 false，其次 display 为没设置，所以为 `null`，不满足条件。

此外，即使满足了条件进入了 `if ( CssTypesValidationUtils.isValidNumericValue(vAlignVal)`
这里也只调用了 `CssDimensionParsingUtils.parseAbsoluteLength(vAlignVal)` ，没法处理 ex 单位的方法。

修复
错误 1
`svgImage.setRelativePosition(0, 0, 0, offsetPt);`
这个会修复偏移，但底部会截断

错误 2
`svgImage.setProperty(com.itextpdf.layout.properties.Property.TEXT_RISE, offsetPt);`
这个不起作用

正确
`svgImage.setProperty(com.itextpdf.layout.properties.Property.INLINE_VERTICAL_ALIGNMENT, new InlineVerticalAlignment(InlineVerticalAlignmentType.FIXED, offsetPt));`

注意offset值为负数
