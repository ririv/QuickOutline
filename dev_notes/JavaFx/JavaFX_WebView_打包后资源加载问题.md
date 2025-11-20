# JavaFX WebView 打包后资源加载问题汇总与解决方案

## 1. 问题描述

在 IntelliJ IDE 中直接运行项目时，Markdown 编辑器一切正常。但在使用 `jpackage` 打包并安装运行后，出现以下严重问题：

1.  **图标丢失**：Vditor 编辑器的工具栏图标不显示，或者必须点击按钮触发重绘后才偶尔显示。
2.  **公式渲染失败**：MathJax 数学公式无法渲染。
3.  **控制台报错**：日志显示资源加载失败，或者 WebView 无法解析相对路径。

## 2. 根本原因分析

### 2.1. 协议兼容性问题 (`file:` vs `jrt:`/`jar:`)

* **IDE 环境**：资源文件位于磁盘上，WebView 使用 `file:///` 协议加载。浏览器引擎对本地文件系统的相对路径支持良好。
* **打包环境**：资源被封装在 JAR 包或 JLink 镜像（Modules）中。WebView 使用 `jar:file:/...` 或 `jrt:/...` 协议加载。

### 2.2. WebKit 的安全与路径限制

JavaFX 内置的 WebKit 引擎对非标准协议（如 `jar:` 和 `jrt:`）支持有限：

* **相对路径失效**：在 `jar:` 协议页面中引用 `./vditor/dist/index.css` 经常解析失败。
* **动态加载受阻**：Vditor 和 MathJax 内部会动态创建 `<script>` 或请求字体文件（Font Fetching）。WebKit 往往会出于安全策略或路径解析错误，阻止从 JAR 包内部加载这些动态资源。

-----

## 3. 最终解决方案：内嵌本地 HTTP 服务器 (LocalWebServer)

我们摒弃了“解压资源到临时文件”的方案，采用了更架构更优雅、性能更好的 **内存直读 HTTP 服务器** 方案。

### 3.1. 方案原理

1.  在 Java 内部启动一个轻量级 HTTP 服务器（使用 JDK 自带的 `com.sun.net.httpserver`），绑定 `localhost` 和随机空闲端口。
2.  拦截 HTTP 请求，通过 `Class.getResourceAsStream` 直接读取 Classpath（即 JAR 包内部）的资源流。
3.  将流数据以标准 HTTP 响应返回给 WebView。
4.  WebView 加载 `http://127.0.0.1:xxxxx/editor.html`。

### 3.2. 核心代码实现

#### A. `LocalWebServer` 工具类

负责启动服务并映射路径。

```java
// 关键逻辑：ClasspathHandler
private static class ClasspathHandler implements HttpHandler {
    // ...
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String requestPath = exchange.getRequestURI().getPath();
        if (requestPath.equals("/")) requestPath = "/editor.html";
        
        // 直接从 JAR 包读取流，无需解压
        InputStream is = getClass().getResourceAsStream(basePath + requestPath);
        
        // 必须设置正确的 MIME Type，否则浏览器不认
        String mimeType = guessMimeType(requestPath);
        exchange.getResponseHeaders().set("Content-Type", mimeType);
        // ... 发送响应 ...
    }
}
```

#### B. `MarkdownTabController` 调用

在界面初始化时启动服务。

```java
private void loadEditor() {
    try {
        if (webServer == null) {
            webServer = new LocalWebServer();
        }
        // 映射 Classpath 下的 /web 目录
        webServer.start("/web"); 
        
        // WebView 加载标准 HTTP 链接
        String url = webServer.getBaseUrl() + "editor.html";
        webEngine.load(url);
    } catch (Exception e) {
        // 异常处理...
    }
}
```

#### C. 模块化配置 (`module-info.java`)

必须显式引入 JDK 的 HTTP 模块。

```java
requires jdk.httpserver;
```

### 3.3. 资源释放 (Dispose)

为了防止端口占用，必须建立完整的销毁调用链：

1.  `MarkdownTabController.dispose()`: 调用 `webServer.stop()`。
2.  `MainController.dispose()`: 调用子 Controller 的 dispose。
3.  `App.stop()`: 在程序退出时调用主 Controller 的 dispose。

-----

## 4. 方案对比总结

| 方案 | 兼容性 | 性能 | 磁盘占用 | 推荐度 |
| :--- | :--- | :--- | :--- | :--- |
| **直接加载 (jrt/jar)** | ❌ 差 (相对路径失效) | ⭐ 高 | 无 | 不可用 |
| **解压到临时目录** | ✅ 好 (File 协议) | ⭐⭐ 中 (首次IO慢) | 有残留 | 备选 (兜底) |
| **本地 HTTP 服务器** | 🚀 **完美** | ⭐⭐⭐ **极高** (零IO) | **无** | 🏆 **首选** |

通过采用本地服务器方案，我们成功实现了：

1.  **零文件解压**：保持用户磁盘干净。
2.  **秒级启动**：无大文件 IO 操作。
3.  **完美兼容**：Vditor 和 MathJax 在打包环境下表现与 IDE 中完全一致。