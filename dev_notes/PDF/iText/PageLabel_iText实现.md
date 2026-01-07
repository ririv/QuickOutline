## 使用最新 iText (iText 7/8) 为 PDF 添加页码标签 (Page Label)

在 PDF 文档中，页码标签 (Page Label) 是一种元数据，它允许您为文档的不同部分设置不同的页码样式。例如，前言部分可以使用罗马数字 (i, ii, iii)，而正文部分则使用阿拉伯数字 (1, 2, 3)。这与简单地在页面上添加文本页码不同，页码标签会影响 PDF 阅读器软件中显示的实际页码。

使用最新版本的 iText (iText 7 及更高版本，包括 iText 8)，操作过程非常直观。核心方法是 `setPageLabel`，它可以应用于单个 `PdfPage` 对象。

-----

### 核心概念和类

在使用代码之前，请了解以下几个关键的类和枚举：

* **`PdfDocument`**: 代表整个 PDF 文档。
* **`PdfPage`**: 代表 PDF 文档中的单个页面。
* **`setPageLabel()`**: `PdfPage` 对象的方法，用于设置该页面的页码标签。
* **`PageLabelNumberingStyle`**: 一个枚举类，定义了不同的页码样式，例如：
    * `DECIMAL_ARABIC_NUMERALS` (1, 2, 3)
    * `UPPERCASE_ROMAN_NUMERALS` (I, II, III)
    * `LOWERCASE_ROMAN_NUMERALS` (i, ii, iii)
    * `UPPERCASE_LETTERS` (A, B, C)
    * `LOWERCASE_LETTERS` (a, b, c)

-----

### Java 代码示例

以下是一个完整的 Java 代码示例，演示了如何创建一个新的 PDF 文档，并为其设置不同的页码标签。

**请确保您的项目中已经添加了 iText 7 (或 iText 8) 的依赖。**

**Maven 依赖:**

```xml
<dependency>
    <groupId>com.itextpdf</groupId>
    <artifactId>kernel</artifactId>
    <version>8.0.4</version> </dependency>
```

**示例代码:**

```java
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.PageLabelNumberingStyle;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.AreaBreak;

import java.io.FileNotFoundException;

public class AddPageLabels {

    public static void main(String[] args) throws FileNotFoundException {
        String dest = "output/page_labels_example.pdf";
        PdfWriter writer = new PdfWriter(dest);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc);

        // 添加5页内容
        document.add(new Paragraph("This is the first page (Preface i)"));
        document.add(new AreaBreak());
        document.add(new Paragraph("This is the second page (Preface ii)"));
        document.add(new AreaBreak());
        document.add(new Paragraph("This is the third page (Content 1)"));
        document.add(new AreaBreak());
        document.add(new Paragraph("This is the fourth page (Content 2)"));
        document.add(new AreaBreak());
        document.add(new Paragraph("This is the fifth page (Appendix A-1)"));

        // 设置页码标签
        // 从第一页开始，使用小写罗马数字
        pdfDoc.getPage(1).setPageLabel(PageLabelNumberingStyle.LOWERCASE_ROMAN_NUMERALS, null);

        // 从第三页开始，使用阿拉伯数字
        pdfDoc.getPage(3).setPageLabel(PageLabelNumberingStyle.DECIMAL_ARABIC_NUMERALS, null);

        // 从第五页开始，使用带前缀的阿拉伯数字，并从 1 重新开始计数
        // 这将显示为 "A-1", "A-2", ...
        pdfDoc.getPage(5).setPageLabel(PageLabelNumberingStyle.DECIMAL_ARABIC_NUMERALS, "A-", 1);


        document.close();

        System.out.println("PDF with Page Labels created successfully!");
    }
}
```

-----

### `setPageLabel` 方法详解

`setPageLabel` 方法有几个重载的版本，最常用的版本是：

1.  **`setPageLabel(PageLabelNumberingStyle numberingStyle, String labelPrefix)`**

    * 这个版本为从当前页开始的页码范围设置样式和前缀。页码会从上一范围的页码继续。
    * **`numberingStyle`**: 页码样式，从 `PageLabelNumberingStyle` 枚举中选择。
    * **`labelPrefix`**: 页码的前缀字符串。如果不需要前缀，可以设置为 `null` 或空字符串 `""`。

2.  **`setPageLabel(PageLabelNumberingStyle numberingStyle, String labelPrefix, int firstPage)`**

    * 这个版本除了设置样式和前缀外，还可以指定起始页码。
    * **`firstPage`**: 新的页码范围的起始数字。

### 操作步骤分解

1.  **创建 `PdfDocument` 对象**:

    ```java
    PdfWriter writer = new PdfWriter("output/your_pdf.pdf");
    PdfDocument pdfDoc = new PdfDocument(writer);
    ```

2.  **获取 `PdfPage` 对象**:
    你可以通过页码（从1开始计数）来获取任何一个页面的 `PdfPage` 对象。

    ```java
    PdfPage firstPage = pdfDoc.getPage(1);
    PdfPage thirdPage = pdfDoc.getPage(3);
    ```

3.  **调用 `setPageLabel`**:
    在获取到的 `PdfPage` 对象上调用 `setPageLabel` 方法来定义从该页开始的页码规则。

    * **前两页为罗马数字 (i, ii):**

      ```java
      // 第一个参数是样式，第二个是前缀（这里没有）
      pdfDoc.getPage(1).setPageLabel(PageLabelNumberingStyle.LOWERCASE_ROMAN_NUMERALS, null);
      ```

      这个规则会自动应用到后续的页面，直到遇到新的 `setPageLabel` 定义。因此，第二页也会显示为 "ii"。

    * **从第三页开始为阿拉伯数字 (1, 2):**

      ```java
      // 从第三页开始，页码会从 1 重新开始（这是默认行为）
      pdfDoc.getPage(3).setPageLabel(PageLabelNumberingStyle.DECIMAL_ARABIC_NUMERALS, null);
      ```

      这个设置会覆盖之前的罗马数字规则。第三页会显示为 "1"，第四页为 "2"。

    * **从第五页开始带前缀 (A-1):**

      ```java
      // 第三个参数指定了新的起始页码
      pdfDoc.getPage(5).setPageLabel(PageLabelNumberingStyle.DECIMAL_ARABIC_NUMERALS, "A-", 1);
      ```

      这个规则定义了页码的前缀是 "A-"，并且数字部分从 1 重新开始。

### 总结

为 PDF 添加页码标签是一个强大的功能，可以让你的文档结构更清晰、更专业。使用最新版的 iText，只需通过 `PdfDocument` 获取特定页面，然后调用 `setPageLabel` 方法即可轻松实现。关键在于理解 `PageLabelNumberingStyle` 和 `setPageLabel` 方法的参数如何影响页码的显示。

## PageLabel 必须从第1页开始设置（未验证原因）
实际操作中发现，`setPageLabel` 方法必须先从第1页开始设置。

如果直接从 >1 页开始设置，不生效，生成的PDF文件会是从1开始的连续阿拉伯数字。

因此可以加入默认行为来防御这种情况：

```java
pdfDoc.getPage(1).setPageLabel(PageLabelNumberingStyle.DECIMAL_ARABIC_NUMERALS, null);
```