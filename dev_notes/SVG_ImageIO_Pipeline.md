# 基于 JavaFX 24 ImageIO 的 SVG 加载管线设计与实现

> 本文档围绕项目中的 `SvgImageReader` / `SvgImageReaderSpi` / `SvgImageReadParam` / `SvgImageIOUtils` 这一整套 **SVG ImageIO 管线** 展开，记录设计背景、关键实现和在 UI 中的使用方式。

---

## 1. 背景与目标

### 1.1 JavaFX 24 的新能力

JavaFX 24 引入了一个重要特性：

- **Pluggable Image Loading via `javax.imageio`**

这意味着：

- JavaFX 的 `Image` / `ImageView` 在解析图片时，不再只依赖内置格式（PNG、JPG 等）；
- 可以通过标准的 ImageIO 插件机制（`ImageReader` + `ImageReaderSpi`）扩展新的图片格式；
- 只要我们提供合规的 `ImageReader` 插件，JavaFX 就可以像加载 PNG 一样加载自定义格式——包括 SVG。

参考 
- [JavaFX 24 ImageIO](https://openjfx.io/javadoc/24/javafx.graphics/javafx/scene/image/Image.html) 文档。
- [Pluggable Image Loading via javax.imageio](https://github.com/openjdk/jfx/blob/jfx25/doc-files/release-notes-24.md#pluggable-image-loading-via-javaximageio)

### 1.2 项目原有 SVG 方案的局限

之前项目中的 SVG 图标方案大致是：

- 使用 Apache Batik 解析 SVG DOM，提取 `<path>` 的 `d` 属性；
- 在 JavaFX 控件上使用 `-fx-shape`（例如封装在 `SvgIcon` 之类的类里）来绘制矢量路径；

优点：
- 保持了矢量特性，缩放不失真；

局限：
- 绘制逻辑偏底层，和普通 `ImageView` 使用方式不一致；
- 对复杂 SVG（多 path、多填充、多渐变）的支持不如完整渲染；
- 不利于与 JavaFX 24 的 ImageIO 体系统一。

### 1.3 新方案目标

围绕新的 **SVG ImageIO 管线**，我们希望达成：

1. **统一入口**：让 SVG 图标的加载方式尽量接近普通位图，简化 UI 层调用。
2. **尺寸控制**：支持通过 `ImageReadParam.sourceRenderSize` 控制渲染尺寸。
3. **复用 Batik**：利用 Apache Batik 的 SVG 渲染能力，处理复杂 SVG。
4. **可渐进替换**：先从某些图标开始替换，逐步迁移原来的 `SvgIcon` 方案。

为此，我们实现了一条完整的 ImageIO 管线：

- `SvgImageReader` — 真正的 SVG → `BufferedImage` 渲染器；
- `SvgImageReaderSpi` — 把 Reader 注册到 ImageIO；
- `SvgImageReadParam` — 支持 `setSourceRenderSize` 的参数对象；
- `SvgImageIOUtils` — JavaFX 友好的工具入口（包含 SPI 注册和 JavaFX `Image` 转换）。

---

## 2. 总体结构概览

### 2.1 关键类一览

- `com.ririv.quickoutline.view.svgimageio.SvgImageReader`
  - 继承 `javax.imageio.ImageReader`
  - 内部使用 Batik `ImageTranscoder` 将 SVG 渲染为 `BufferedImage`
  - 支持从 `ImageReadParam.getSourceRenderSize()` 读取目标尺寸

- `com.ririv.quickoutline.view.svgimageio.SvgImageReaderSpi`
  - 继承 `javax.imageio.spi.ImageReaderSpi`
  - 声明支持的格式：
    - 名称：`svg` / `SVG`
    - 后缀：`.svg`
    - MIME type：`image/svg+xml`
  - 将 `SvgImageReader` 接入到 ImageIO 体系

- `com.ririv.quickoutline.view.svgimageio.SvgImageReadParam`
  - 继承 `javax.imageio.ImageReadParam`
  - 安全实现 `setSourceRenderSize` / `getSourceRenderSize`
  - 避免默认实现抛 `UnsupportedOperationException`

- `com.ririv.quickoutline.view.svgimageio.SvgImageIOUtils`
  - 提供静态方法：
    - `registerIfNeeded()` — 向默认 `IIORegistry` 注册 `SvgImageReaderSpi`
    - `loadSvgAsFxImage(String resourcePath, double targetWidth, double targetHeight)`
  - 内部完成：
    - SPI 注册
    - 使用 ImageIO 找到 SVG Reader
    - 设置 `ImageReadParam.sourceRenderSize`
    - 调用 `reader.read(0, param)` 获得 `BufferedImage`
    - 用 `SwingFXUtils.toFXImage` 转为 JavaFX `Image`

### 2.2 调用链路线图

从 UI 控件发起调用的大致路线：

1. UI 层（例如 `LeftPaneController`）：
   - 调用 `SvgImageIOUtils.loadSvgAsFxImage("/drawable/markdown.svg", 24, 24)`
2. `SvgImageIOUtils`：
   - 保证 SVG Reader SPI 已注册（`registerIfNeeded()`）
   - 用 `ImageIO.getImageReadersByMIMEType("image/svg+xml")` 找到 `SvgImageReader`
   - 创建并配置 `ImageReadParam`（含 `sourceRenderSize`）
   - 调用 `reader.read(0, param)` 拿到 `BufferedImage`
3. `SvgImageReader`：
   - 从 `ImageInputStream` 读入完整 SVG 源字节
   - 根据 `param.getSourceRenderSize()` 计算目标像素宽高
   - 使用 Batik `ImageTranscoder` 渲染出 `BufferedImage`
4. `SvgImageIOUtils` 再用 `SwingFXUtils.toFXImage`：
   - 将 `BufferedImage` 转换为 JavaFX `Image`
5. UI 层：
   - 将 `Image` 绑定到 `ImageView` 或其他控件。

---

## 3. SvgImageReader：SVG → BufferedImage 渲染核心

`SvgImageReader` 是 ImageIO 管线的核心实现，负责从 `ImageInputStream` 中读取 SVG 并调用 Batik 进行渲染。

### 3.1 类型与基本约定

- 继承自 `javax.imageio.ImageReader`
- 内部维护：
  - `private BufferedImage image;`
  - `private byte[] svgBytes;` — 缓存 SVG 源字节
- 单图像约定：
  - `getNumImages(...)` 永远返回 `1`
  - 只支持 `imageIndex == 0`

### 3.2 默认参数：SvgImageReadParam

`SvgImageReader` 重写了：

```java
@Override
public ImageReadParam getDefaultReadParam() {
    return new SvgImageReadParam();
}
```

这样保证：

- 上层通过 `reader.getDefaultReadParam()` 得到的是 **支持 `setSourceRenderSize`** 的实现；
- 在 `SvgImageIOUtils` 中调用 `param.setSourceRenderSize(...)` 不会抛出 `UnsupportedOperationException`；
- 尺寸信息可以顺利传递到后续渲染逻辑。

### 3.3 读取 SVG 源字节

在 `ensureImageLoaded(int imageIndex, ImageReadParam param)` 中：

- 首先检查 `imageIndex` 是否为 `0`；
- 再检查 `input` 是否是 `ImageInputStream`；
- 将整个 `ImageInputStream` 读入到 `svgBytes`：
  - 使用 `ByteArrayOutputStream` 缓冲
  - 调用 `stream.mark()` / `stream.reset()` 保证后续读取不受影响

目的：

- 将 SVG 原始字节缓存下来，未来需要不同尺寸渲染时可以重用；
- 避免对同一个 `ImageInputStream` 进行复杂反复操作。

### 3.4 读取目标尺寸（sourceRenderSize）

在同一个方法中：

```java
float pixelWidth = -1;
float pixelHeight = -1;
if (param != null && param.getSourceRenderSize() != null) {
    pixelWidth = param.getSourceRenderSize().width;
    pixelHeight = param.getSourceRenderSize().height;
}
```

- 如果 `param` 是 `SvgImageReadParam`，则 `getSourceRenderSize()` 返回我们之前设置的尺寸；
- 如果上层没有设置，保持 `pixelWidth/pixelHeight` 为 `-1`，交给 SVG 自己决定尺寸。

### 3.5 使用 Batik ImageTranscoder 渲染

内部定义了一个本地类：

```java
class BufferedImageTranscoder extends ImageTranscoder {
    private BufferedImage bufferedImage;

    @Override
    public BufferedImage createImage(int w, int h) {
        return new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    }

    @Override
    public void writeImage(BufferedImage img, TranscoderOutput out) {
        this.bufferedImage = img;
    }

    public BufferedImage getBufferedImage() {
        return bufferedImage;
    }
}
```

然后按照如下步骤渲染：

1. 创建 `BufferedImageTranscoder transcoder = new BufferedImageTranscoder();`
2. 如果 `pixelWidth > 0`，调用：
   - `transcoder.addTranscodingHint(ImageTranscoder.KEY_WIDTH, pixelWidth);`
3. 如果 `pixelHeight > 0`，调用：
   - `transcoder.addTranscodingHint(ImageTranscoder.KEY_HEIGHT, pixelHeight);`
4. 使用我们缓存的 `svgBytes` 构造 `InputStream`：
   - `InputStream is = new ByteArrayInputStream(svgBytes)`
5. 构造 `TranscoderInput tIn = new TranscoderInput(is);`
6. 调用 `transcoder.transcode(tIn, null);`
7. 最终将 `this.image = transcoder.getBufferedImage();`

异常处理：

- `TranscoderException` 被包装成 `IOException("Failed to transcode SVG", e)` 抛出，便于上层统一处理。

### 3.6 关于多次渲染 / 缓存策略

当前实现中：

- 一旦 `image` 被成功渲染并缓存，再次调用 `read(...)` 会直接返回同一张 `BufferedImage`；
- 欲实现“同一 SVG 多尺寸渲染”的需求，可以在未来：
  - 将 `image` 缓存设计为 `Map<Dimension, BufferedImage>`；
  - 或者增加 API 以便按需重新渲染。

目前项目需求主要集中在“按指定尺寸渲染一次并显示”，因此单缓存的实现足够。

---

## 4. SvgImageReadParam：尺寸控制的关键

Java 标准库中的 `ImageReadParam` 默认实现对 `setSourceRenderSize` 的支持并不完整，直接调用可能抛出 `UnsupportedOperationException`。

为了解决这个问题，我们提供了自定义实现 `SvgImageReadParam`：

```java
public class SvgImageReadParam extends ImageReadParam {

    private Dimension sourceRenderSize;

    @Override
    public void setSourceRenderSize(Dimension size) {
        this.sourceRenderSize = (size == null) ? null : new Dimension(size);
    }

    @Override
    public Dimension getSourceRenderSize() {
        return (sourceRenderSize == null) ? null : new Dimension(sourceRenderSize);
    }
}
```

特点：

- 通过内部字段保存尺寸信息；
- `setSourceRenderSize(null)` 可以清除尺寸限制；
- `getSourceRenderSize()` 返回一个拷贝，避免外部代码修改内部状态；
- 与 `SvgImageReader.getDefaultReadParam()` 配合使用，形成一条完整的“尺寸控制链路”。

---

## 5. SvgImageReaderSpi：把 SvgImageReader 接入 ImageIO

`SvgImageReaderSpi` 的角色是：

- 声明 `SvgImageReader` 支持什么格式（名称、后缀、MIME type）；
- 告诉 ImageIO “当遇到 SVG 时，应该使用哪一个 `ImageReader`”；
- 提供实例创建方法 `createReaderInstance`。

核心实现结构如下：

```java
public class SvgImageReaderSpi extends ImageReaderSpi {

    private static final String VENDOR_NAME = "QuickOutline";
    private static final String VERSION = "1.0";
    private static final String READER_CLASS_NAME = SvgImageReader.class.getName();
    private static final String[] NAMES = {"svg", "SVG"};
    private static final String[] SUFFIXES = {"svg"};
    private static final String[] MIME_TYPES = {"image/svg+xml"};

    public SvgImageReaderSpi() {
        super(
                VENDOR_NAME,
                VERSION,
                NAMES,
                SUFFIXES,
                MIME_TYPES,
                READER_CLASS_NAME,
                STANDARD_INPUT_TYPE,
                null,
                false,
                null, null, null, null,
                false,
                null, null, null, null
        );
    }

    @Override
    public boolean canDecodeInput(Object source) throws IOException {
        return source instanceof ImageInputStream;
    }

    @Override
    public ImageReader createReaderInstance(Object extension) {
        return new SvgImageReader(this);
    }

    @Override
    public String getDescription(Locale locale) {
        return "SVG ImageReader using Apache Batik";
    }
}
```

注意：

- 这里只做了最基础的能力声明；
- 识别输入的策略很简单：`source instanceof ImageInputStream` 即认为可以解码；
- 更复杂的实现可以在 `canDecodeInput` 中读取部分数据检查 `<svg` 标签等，但当前场景没有强需求。

---

## 6. SvgImageIOUtils：JavaFX 友好的入口工具

UI 层不需要直接接触 `ImageReader` / `ImageReaderSpi` / `ImageInputStream` 这些底层概念，因此我们通过 `SvgImageIOUtils` 提供了一个清晰的封装：

```java
public final class SvgImageIOUtils {

    private static volatile boolean registered = false;

    private SvgImageIOUtils() {}

    public static void registerIfNeeded() {
        if (!registered) {
            synchronized (SvgImageIOUtils.class) {
                if (!registered) {
                    IIORegistry registry = IIORegistry.getDefaultInstance();
                    registry.registerServiceProvider(new SvgImageReaderSpi());
                    registered = true;
                }
            }
        }
    }

    public static Image loadSvgAsFxImage(String resourcePath, double targetWidth, double targetHeight) {
        registerIfNeeded();

        try (InputStream is = SvgImageIOUtils.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IllegalArgumentException("SVG resource not found: " + resourcePath);
            }

            try (ImageInputStream iis = ImageIO.createImageInputStream(is)) {
                if (iis == null) {
                    throw new IOException("Failed to create ImageInputStream for: " + resourcePath);
                }

                Iterator<ImageReader> readers = ImageIO.getImageReadersByMIMEType("image/svg+xml");
                if (!readers.hasNext()) {
                    readers = ImageIO.getImageReadersBySuffix("svg");
                }
                if (!readers.hasNext()) {
                    throw new IOException("No SVG ImageReader available. Did registration fail?");
                }

                ImageReader reader = readers.next();
                try {
                    reader.setInput(iis, true, true);
                    ImageReadParam param = reader.getDefaultReadParam();
                    if (targetWidth > 0 && targetHeight > 0) {
                        param.setSourceRenderSize(new Dimension(
                                (int) Math.round(targetWidth),
                                (int) Math.round(targetHeight))
                        );
                    }
                    BufferedImage buffered = reader.read(0, param);
                    return SwingFXUtils.toFXImage(buffered, null);
                } finally {
                    reader.dispose();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load SVG as Image: " + resourcePath, e);
        }
    }
}
```

要点说明：

1. **懒注册（Lazy Registration）**：
   - 使用静态标记 `registered` 保证 SPI 只注册一次；
   - 在 `loadSvgAsFxImage` 被调用时自动触发注册，调用方无需关心。

2. **Reader 选择策略**：
   - 优先通过 `MIME type` 查找：`image/svg+xml`；
   - 若未找到，则通过后缀查找：`"svg"`；
   - 任一方式找到 Reader 后，即可使用。

3. **尺寸控制**：
   - 当 `targetWidth > 0 && targetHeight > 0` 时，设置 `param.setSourceRenderSize(...)`；
   - 否则不设置，让 SVG 自己决定最终尺寸。

4. **错误处理**：
   - 资源不存在时抛出 `IllegalArgumentException`，帮助尽早发现配置错误；
   - IO 问题统一包装为 `RuntimeException`，避免在 UI 层到处捕获 `IOException`。

---

## 7. UI 集成示例：LeftPane 中的 Markdown 图标

下面以 `LeftPane` 中的 Markdown 按钮为例，说明如何在真实 UI 中使用这条 SVG ImageIO 管线。

### 7.1 FXML：使用 ImageView 作为图标

在 `LeftPane.fxml` 中，Markdown 按钮的 `graphic` 部分使用 `ImageView`：

- 示例布局（简化）：
  - `fx:id="markdownSvgImage"`
  - 由控制器在运行时设置 `image` 属性

这样可以最大限度地保持与 PNG 等其它图标的使用方式一致：

- 仍然是标准的 JavaFX `ImageView`；
- 只是在背后使用了自定义的 SVG ImageIO 管线加载图像。

### 7.2 控制器：在 initialize 中加载 SVG

在 `LeftPaneController` 中：

```java
@FXML
private ImageView markdownSvgImage;

@FXML
public void initialize() {
    Image svgImage = SvgImageIOUtils.loadSvgAsFxImage("/drawable/markdown.svg", 24, 24);
    markdownSvgImage.setImage(svgImage);
}
```

说明：

- `resourcePath` 使用的是类路径资源路径，`/drawable/markdown.svg` 需要确保在 `resources` 中存在；
- `24, 24` 对应目标渲染尺寸，实测可明显感受到尺寸变化；
- 这段代码也验证了：
  - SPI 注册成功；
  - `SvgImageReader` 能被 ImageIO 正确选中；
  - `SvgImageReadParam` 的 `sourceRenderSize` 正常生效；
  - 渲染结果可以在 UI 中稳定显示。

---

## 8. 关于不规范 SVG（如 fill=""）的容错

在实际使用中，我们遇到了一些来自网络的 SVG 文件，其 CSS/属性并不完全规范，例如：

- 属性 `fill=""`（空字符串）等非法值；

Batik 在解析这些 SVG 时：

- 会在内部记录并可能打印 ERROR 级别日志（`DOMException` 等）；
- 但多数情况下仍然能够生成可用的渲染结果；
- 我们当前的 `SvgImageReader` 对 `TranscoderException` 做了包装处理，只在真正渲染失败时抛出 `IOException`。

当前策略：

- 对于非致命问题（比如 `fill=""` 但整体仍可渲染），允许 Batik 打印错误日志，但不影响 UI 逻辑；
- 只有在 `transcoder.transcode(...)` 抛出异常时才视为真正失败。

如果未来希望进一步降低日志噪音，可以考虑：

- 尝试通过自定义 Batik `UserAgent` 或相关日志配置，降低错误日志的级别；
- 或在加载前对 SVG 内容进行简单清洗（例如将 `fill=""` 替换为 `fill="none"` 等）。

---

## 9. 与旧 SvgIcon 方案的关系

本 SVG ImageIO 管线并不会立即移除原有的 `SvgIcon` / `-fx-shape` 方案，而是：

- 在需要更高保真渲染（或希望与 ImageView/位图统一使用方式）的场景下优先使用 **ImageIO + Batik** 管线；
- 逐步将旧方案中的高频图标迁移过来；
- 遇到兼容问题时，可以保留旧方案作为 fallback。

长期目标：

- 在 JavaFX 24+ 环境下，尽可能将 SVG 图标的加载统一到 ImageIO 管线；
- 简化 UI 层代码，让“加载 SVG 图标”这件事和“加载 PNG 图标”尽量接近。

---

## 10. 小结

- 我们围绕 JavaFX 24 的 **Pluggable Image Loading via ImageIO** 特性，实现了一套完整的 **SVG ImageIO 管线**：`SvgImageReader` / `SvgImageReaderSpi` / `SvgImageReadParam` / `SvgImageIOUtils`；
- 这条管线基于 Apache Batik 进行 SVG → `BufferedImage` 渲染，并通过 `SwingFXUtils.toFXImage` 接入 JavaFX；
- 支持通过 `ImageReadParam.sourceRenderSize` 控制渲染尺寸；
- 已在 `LeftPane` 中的 Markdown 图标上完成真实 UI 验证；
- 对不规范 SVG 保持一定容错能力，UI 行为稳定。

后续如果扩展到更多图标或更复杂的 SVG，可以以本文档为基础继续演进实现和记录。