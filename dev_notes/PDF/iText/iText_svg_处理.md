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
MathJax 自定义的标签 `<mjx-container>` 使用 MjxInlineContainerTagWorker 处理，继承自 SpanTagWorker。注意，如果不继承自 SpanTagWorker，那么 `<mjx-container>` 的内容默认会占一行。我之前的的问题就是，`<mjx-container>` 的内容默认会占一行，会在上面一行，但左边的文字会在下方一行，但文字和公式左右时衔接的（正确），上下也是衔接的（不正确，不在同一行）。

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


## iText 处理 MathJax svg 整行公式 (Block) 的总结

在将 MathJax 生成的 SVG 转换为 PDF 时，我们需要区分 **行内公式**（Inline, 跟随文本）和 **整行公式**（Block, 独占一行并居中）。对于 `<mjx-container display="true">` 的整行公式，必须构建特定的 iText 对象结构以确保布局正确。

以下是技术复盘及两种有效的代码实现方案。

-----

### 1\. 技术复盘：为什么之前的 Span 混合写法失效？

在探索初期，我们曾尝试扩展 `SpanTagWorker` 来同时处理行内和整行公式，试图通过修改 CSS 属性来控制布局。这导致了“屏幕空白”或“样式失效”的问题，根本原因在于 iText 的底层对象模型限制：

1.  **`SpanTagWorker` 是“隐形”容器**：

    * 源码中 `SpanTagWorker.getElementResult()` 返回 **`null`**。它不生成实际的布局对象（如 `Div`），只负责传递子元素。当我们强行重写它返回 `Div` 时，父级布局上下文（通常是 `Paragraph`）无法处理，导致元素被丢弃或渲染为 0 高度。

2.  **`SpanTagCssApplier` 不支持块级样式**：

    * `SpanTagCssApplier` 的逻辑是将样式“穿透”应用给内部的叶子节点（如 Text 或 Image）。它**完全不支持** `text-align`（居中）逻辑，也会忽略垂直方向的 `margin`。因此，即使设置了居中样式，在 Span 模式下也会失效。

3.  **布局上下文冲突 (Context Violation)**：

    * `SpanTagWorker` 意味着该元素属于 **行内上下文**（会被放入 `Paragraph`）。在行内上下文中强行塞入一个 **块级元素**（`Div`）是非法的布局操作，导致渲染崩溃。

**结论**：必须在工厂层面对 `display="true"` 的元素进行**分流**，将其交给专门处理块级元素的 Worker，进入 iText 的块级渲染管线。

-----

### 2\. 预备工作：工厂分流策略

在 `CustomTagWorkerFactory` 中，必须根据 `display` 属性决定返回哪种 Worker。这决定了元素在 PDF 对象树中的层级。

```java
public class CustomTagWorkerFactory extends DefaultTagWorkerFactory {
    @Override
    public ITagWorker getCustomTagWorker(IElementNode tag, ProcessorContext context) {
        if ("mjx-container".equalsIgnoreCase(tag.name())) {
            // 根据 display 属性分流
            String display = tag.getAttribute("display");
            
            if ("true".equalsIgnoreCase(display)) {
                // 块级轨道 -> 使用 MjxBlockContainerTagWorker (生成 Div)
                return new MjxBlockContainerTagWorker(tag, context);
            } else {
                // 行内轨道 -> 使用之前的 MjxInlineContainerTagWorker (继承 SpanTagWorker)
                return new MjxInlineContainerTagWorker(tag, context);
            }
        }
        // ... 其他逻辑 (如 SVG 拦截) ...
        return super.getCustomTagWorker(tag, context);
    }
}
```

-----

### 3\. 方案一：Paragraph 包装法（官方推荐 🏆）

这是最符合 iText 语义的方案。利用 `Paragraph` 组件负责行内对齐（居中），利用 `Div` 组件负责块级布局（边距）。

**结构逻辑**：`Div` (控制边距) -\> `Paragraph` (控制居中) -\> `Image` (内容)

```java
package com.ririv.quickoutline.pdfProcess.itextImpl;

import com.itextpdf.html2pdf.attach.ITagWorker;
import com.itextpdf.html2pdf.attach.ProcessorContext;
import com.itextpdf.layout.IPropertyContainer;
import com.itextpdf.layout.element.Div;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.styledxmlparser.css.CommonCssConstants;
import com.itextpdf.styledxmlparser.css.util.CssDimensionParsingUtils;
import com.itextpdf.styledxmlparser.node.IElementNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class MjxBlockContainerTagWorker implements ITagWorker {
    private static final Logger log = LoggerFactory.getLogger(MjxBlockContainerTagWorker.class);

    private Div container;

    public MjxBlockContainerTagWorker(IElementNode element, ProcessorContext context) {
        this.container = new Div();
        // 1. 宽度设为 100%，这是块级居中的基础
        this.container.setWidth(UnitValue.createPercentValue(100));

        // 2. 动态读取样式
        // iText 已经将 "margin: 1em 0" 展开为 margin-top, margin-right, ...
        Map<String, String> styles = element.getStyles();

        // 获取当前基准字号 (用于计算 em)
        float emSize = context.getCssContext().getCurrentFontSize();
        if (emSize <= 0) emSize = 12f;

        // --- [上边距] (对应 1em) ---
        if (styles != null && styles.containsKey(CommonCssConstants.MARGIN_TOP)) {
            float val = CssDimensionParsingUtils.parseRelativeValue(
                    styles.get(CommonCssConstants.MARGIN_TOP), emSize);
            this.container.setMarginTop(val);
        } else {
            // 兜底：如果没解析到，给个默认值 1em
            this.container.setMarginTop(emSize);
        }

        // --- [下边距] (对应 1em) ---
        if (styles != null && styles.containsKey(CommonCssConstants.MARGIN_BOTTOM)) {
            float val = CssDimensionParsingUtils.parseRelativeValue(
                    styles.get(CommonCssConstants.MARGIN_BOTTOM), emSize);
            this.container.setMarginBottom(val);
        } else {
            // 兜底：默认值 1em
            this.container.setMarginBottom(emSize);
        }

        // --- [左右边距] (对应 0) ---
        // 虽然 Div 默认也是 0，但为了严谨，我们也读一下
        if (styles != null) {
            if (styles.containsKey(CommonCssConstants.MARGIN_LEFT)) {
                float val = CssDimensionParsingUtils.parseRelativeValue(
                        styles.get(CommonCssConstants.MARGIN_LEFT), emSize);
                this.container.setMarginLeft(val);
            }
            if (styles.containsKey(CommonCssConstants.MARGIN_RIGHT)) {
                float val = CssDimensionParsingUtils.parseRelativeValue(
                        styles.get(CommonCssConstants.MARGIN_RIGHT), emSize);
                this.container.setMarginRight(val);
            }
        }

        log.info("MjxBlock: Margins applied dynamically based on CSS.");
    }

    @Override
    public boolean processTagChild(ITagWorker childTagWorker, ProcessorContext context) {
        IPropertyContainer childResult = childTagWorker.getElementResult();

        if (childResult instanceof Image) {
            // 使用 Paragraph 包装以实现居中 (Method 1)
            Paragraph p = new Paragraph();
            p.add((Image) childResult);
            p.setTextAlignment(TextAlignment.CENTER);

            // 重要：清除 Paragraph 自身的默认 Margin，
            // 确保总边距完全由外层的 Div (我们刚才设置的那些) 控制
            p.setMargin(0);

            this.container.add(p);
        }
        return true;
    }

    @Override
    public IPropertyContainer getElementResult() {
        return container;
    }

    @Override public void processEnd(IElementNode element, ProcessorContext context) {}
    @Override public boolean processContent(String content, ProcessorContext context) { return false; }
}
```

-----

### 4\. 方案二：Image 直接对齐法（继承技巧）

通过继承 `DivTagWorker` 并使用**构造函数注入**技巧，直接操作容器并设置图片的水平对齐属性。

**结构逻辑**：`Div` (控制边距) -\> `Image` (自身设置水平居中)

```java
package com.ririv.quickoutline.pdfProcess.itextImpl;

import com.itextpdf.html2pdf.attach.ITagWorker;
import com.itextpdf.html2pdf.attach.ProcessorContext;
import com.itextpdf.html2pdf.attach.impl.tags.DivTagWorker;
import com.itextpdf.layout.element.Div;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.styledxmlparser.node.IElementNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MjxBlockContainerTagWorker extends DivTagWorker {
    private static final Logger log = LoggerFactory.getLogger(MjxBlockContainerTagWorker.class);

    // 自身持有引用，解决父类字段私有的问题
    private Div myContainer;

    /**
     * 公共构造函数
     */
    public MjxBlockContainerTagWorker(IElementNode element, ProcessorContext context) {
        // 1. 在这里创建 Div
        this(element, context, new Div());
    }

    /**
     * 私有构造函数 - 注入技巧
     */
    private MjxBlockContainerTagWorker(IElementNode element, ProcessorContext context, Div div) {
        // 2. 将 Div 传给父类
        super(element, context, div);
        
        // 3. 自己保留一份引用
        this.myContainer = div;

        // 初始化块级样式
        // 可根据需要动态读取样式，参考方案一
        this.myContainer.setWidth(UnitValue.createPercentValue(100));
        this.myContainer.setMarginTop(12f);
        this.myContainer.setMarginBottom(12f);
    }

    @Override
    public boolean processTagChild(ITagWorker childTagWorker, ProcessorContext context) {
        if (childTagWorker.getElementResult() instanceof Image) {
            Image img = (Image) childTagWorker.getElementResult();
            
            // 4. 【关键】设置图片自身在块级上下文中的水平居中
            img.setHorizontalAlignment(HorizontalAlignment.CENTER);
            
            // 5. 直接添加，绕过父类默认的 InlineHelper 逻辑
            this.myContainer.add(img);
            
            log.info("MjxBlock (Method 2): Added Image with HorizontalAlignment.CENTER directly.");
            return true;
        }
        
        return super.processTagChild(childTagWorker, context);
    }
}
```


## iText 对 CSS 属性选择器的支持总结

我们在排查过程中发现，虽然 iText 的 CSS 解析器完全支持复杂的属性选择器，但之前的渲染问题并非源于选择器失效，而是源于 Worker 和 Applier 的能力错配。以下是基于源码分析的详细总结。

-----

### 1\. 结论：完全支持

iText 7 (`html2pdf`) 的 CSS 解析器完全支持 CSS 2.1 标准的属性选择器，包括链式写法。

对于选择器：

```css
mjx-container[jax="SVG"][display="true"]
```

iText 能够精准识别并选中对应的 HTML 元素。

-----

### 2. 源码证据 (Source Code Evidence)

根据我们审查的 `CssSelectorParser.java` 和 `CssAttributeSelectorItem.java` 源码，iText 的支持机制如下：

* **正则捕获**：`SELECTOR_PATTERN_STR` 包含极其复杂的正则表达式，能够捕获 `[...]` 结构的属性选择器，包括 `=`, `~=`, `|=` 等操作符。
* **链式解析**：`parseSelectorItems` 方法使用 `while(match.find())` 循环，将 `mjx-container`、`[jax="SVG"]` 和 `[display="true"]` 解析为三个独立的 `ICssSelectorItem` 对象。
* **精确匹配**：`CssAttributeSelectorItem.matches()` 方法会读取 HTML 元素的真实属性值（`element.getAttribute`），并进行字符串精确比对（`value.equals(attributeValue)`）。

-----

### 3. 为什么之前样式失效？(The "Invisible" Problem)

既然选择器生效了，为什么之前屏幕上是空白，或者没有居中？这是典型的 **“指令传达成功，但执行者无能为力”** 的情况。

1.  **解析成功**：CSS 引擎成功解析出 `display: block`、`text-align: center` 和 `margin: 1em`。
2.  **投递错误**：由于之前的工厂逻辑返回了 **`SpanTagWorker`**（行内 Worker）。
3.  **执行失败**：
    * iText 将样式交给 **`SpanTagCssApplier`** 执行。
    * `SpanTagCssApplier` 的源码逻辑只针对行内元素（Text/Image）。
    * 它**不支持** `text-align`（居中），也**忽略**垂直方向的 `margin`。
    * 更严重的是，试图在行内上下文（Span）中强行应用 `display: block` 导致了布局引擎的上下文冲突，最终导致渲染丢弃（空白）。

-----

### 4. 现在的解决方案

我们目前的 **Factory 分流 + Java 对象操作** 方案是解决此问题的最优解：

* **Factory 分流**：在 Java 层直接读取 `display` 属性，将块级公式分流给 **`MjxBlockContainerTagWorker`** (Div/Paragraph)，从根源上解决了上下文冲突。
* **Java 样式控制**：虽然 CSS 选择器可用，但我们在 `MjxBlockContainerTagWorker` 中直接调用 `container.setMargin` 和 `paragraph.setTextAlignment`。
    * **优势**：绕过了 CSS 解析的中间环节，直接作用于布局对象，性能更高，且不受外部 CSS 文件加载失败的影响，保证了 100% 的样式确定性。