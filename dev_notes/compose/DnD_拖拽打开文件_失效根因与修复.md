# Compose Desktop DnD 拖拽打开文件失效：根因与修复

日期：2025-09-05  平台：macOS  技术栈：Compose Desktop 1.8.x / Kotlin 2.x

## 问题
compose 从 1.6.x 升级到 1.8.x 后拖拽打开文件失效

## 现象
- 应用启动时仅打印一次“handler attached …”的启动日志。
- 拖入 Finder 的 PDF 后，没有任何 canImport/importData 日志，文件也没有被打开。
- 偶发时成功，重启或界面交互后再次失败，表现不稳定。

## 根因
- Compose Desktop 在创建和后续布局过程中会动态“替换” Swing 层级中的关键面板（如 contentPane/glassPane/内部 Compose 容器）。
- 我们最初只在窗口初始化当下给 rootPane/glassPane 等挂了 TransferHandler，一旦这些面板被替换，TransferHandler 就丢失，DnD 事件不再到达 canImport/importData。
- macOS 下 DnD 命中的是指针下的具体 Swing 组件；若最新可见的组件层上没有 handler，事件不会冒泡到之前挂载的根组件。

## 解决方案（稳健版）
在 `src/main/kotlin/com/ririv/quickoutline/view/App.kt` 中做了以下改造：

1) 基础挂载
- 继续为 `rootPane`、`glassPane`、`contentPane`、`layeredPane` 设置同一个 `TransferHandler`。

2) 递归挂载（覆盖当前可视树）
- 启动后递归遍历 `contentPane` 子树，凡是 `JComponent` 一律设置该 `TransferHandler`，确保当前 Compose 容器能接收 DnD。

3) 动态保持（覆盖未来变化）
- 在 `frame` 上监听 `glassPane`/`layeredPane`/`contentPane`/`rootPane` 的属性变更（被替换时），对新组件重新挂载，并再次对其子树递归挂载。
- 在 `contentPane` 上监听组件新增事件，对新增子树递归挂载。

4) 兜底保障
- 在 `JFrame` 上保留 AWT `DropTarget`，即使某一时刻 `TransferHandler` 被后续替换冲掉，也能以最低公分母继续接收 DnD。

5) 数据口味与解析
- 支持 `javaFileListFlavor`、`text/uri-list`、`application/x-java-url`、`stringFlavor`，解析出第一个 `.pdf` 文件：
  - `text/uri-list` 通过 `parseUriList` 转本地文件；
  - `stringFlavor` 先尝试作为 URI，再尝试本地路径。
- 在 `importData` 中通过 `SwingUtilities.invokeLater { mainViewModel.openPdf(path) }` 与按钮行为保持一致线程语义。
- `support.dropAction = COPY`，并打印 canImport/importData 的诊断日志。

## 验证
- 启动时日志：
  - `[DnD][TH] handler attached to: …`
  - `[DnD][TH] recursively attached to N components under contentPane`
- 拖入 PDF：
  - 多次 `[DnD][TH] canImport=true …`（移动中），一次 `[DnD][TH] importData isDrop=true …`（释放时），随后
  - `[DnD][TH] opening PDF: /path/to/xxx.pdf`，应用成功打开文件。
- 面板被替换时：
  - 日志出现 `reattached handler to …` 和 `reattached recursively to …`，DnD 持续可用。

## 影响与注意事项
- 日志较为详细，便于排障；稳定后可仅保留成功/失败关键日志。
- 递归与监听带来极小开销，通常可忽略；如需进一步降噪/降开销，可在确认稳定后移除 AWT `DropTarget` 兜底。
- 跨平台：
  - Windows/Linux 常见来源主要是 `javaFileListFlavor`；
  - macOS Finder 会提供 `text/uri-list`，保留对其的支持很关键。

## 代码位置
- `App.kt` 中的以下要点：
  - `TransferHandler`：`canImport` / `importData`
  - 解析方法：`parseUriList`、`extractPdf`
  - 动态挂载：属性变更监听 + `ContainerListener` + 递归挂载
  - 兜底：JFrame 的 `DropTarget`

## 后续建议
- 降低日志噪音，仅保留必要的 importData 成功/失败与路径。
- 将解析函数抽取至 util 并补充最小单元测试（针对 text/uri-list 与 stringFlavor）。
- 若长期观察无须兜底，考虑移除 `DropTarget`，减少重复路径与维护成本。

---

## 实现方案对比：Java（Swing）vs Compose（纯 Compose）

### Java（Swing）实现
- 技术点：`TransferHandler` + 动态递归挂载 + `PropertyChangeListener` + `ContainerListener` + AWT `DropTarget` 兜底。
- 优势：
  - 成熟稳定，行为在桌面平台上一致性较好。
  - 通过“动态重挂载”规避 Compose 替换面板导致的 handler 丢失问题。
- 劣势：
  - 与 Compose 有跨层耦合，需要理解 Swing 层级与 Compose 的交互。
  - 代码样板较多（递归/监听/兜底）。

### Compose 实现（Modifier.dragAndDropTarget）
- 技术点：`Modifier.dragAndDropTarget`（`@OptIn(ExperimentalComposeUiApi)`）、在 `onDrop` 使用 `event.awtTransferable` 解析 `text/uri-list`/`javaFileListFlavor`。
- 优势：
  - 纯 Compose 写法，更贴合当前 UI 层次，不依赖 Swing 组件层级。
  - 不会因 Compose 替换 Swing 面板而丢失 handler。
- 注意点：
  - API 仍为实验性质（截至 Compose 1.8.2），需要按平台实机验证。
  - Finder 的 `text/uri-list` 与 `application/x-java-file-list` 需兼容解析。

### 推荐与迁移建议
- 结论：推荐“优先使用 Compose 实现”，并在一段时间内保留 Java 方案作为兜底，待稳定后逐步移除。
- 迁移步骤：
  1. 在 `MainView.kt` 根容器（`Box(Modifier.fillMaxSize())`）上添加 `.dragAndDropTarget`；`shouldStartDragAndDrop = { true }`，在 `onDrop` 中解析。
  2. 解析逻辑可重用 Java 版的 `parseUriList`/`extractPdf`（参考 `App.kt`），优先取第一个 `.pdf` 后调用 `mainViewModel.openPdf(path)`。
  3. 保留 Java 方案（`App.kt` 的 TransferHandler/DropTarget）一段时间，监控日志；确认 Compose 路径稳定命中后再移除 Java 方案与相关日志。

## 实现要点（参考）

### Compose 版（位于 `MainView.kt` 根 Box）
- 关键导入：
  - `androidx.compose.ui.draganddrop.dragAndDropTarget`
  - `androidx.compose.ui.draganddrop.DragAndDropTarget`
  - `androidx.compose.ui.draganddrop.DragAndDropEvent`
  - 并 `@OptIn(ExperimentalComposeUiApi::class)`
- 处理流程：
  - `shouldStartDragAndDrop = { true }`（桌面端简单接收）。
  - `onDrop(event)` 中使用 `event.awtTransferable`，兼容 `javaFileListFlavor`、`text/uri-list`、`application/x-java-url`、`stringFlavor`，解析 PDF 后 `mainViewModel.openPdf(path)`。

### Java 版（位于 `App.kt`）
- 关键点：
  - `TransferHandler.canImport/importData` + 在 `rootPane/glassPane/contentPane/layeredPane` 挂载。
  - 递归给 `contentPane` 子树挂 `TransferHandler`；监听 `frame` 的 `glassPane/layeredPane/contentPane/rootPane` 属性变更，重挂并递归；监听 `contentPane` `componentAdded` 事件，对新增子树递归挂载。
  - 解析同 Compose 版；成功后使用 `SwingUtilities.invokeLater { openPdf }`。
  - 在 `JFrame` 上保留 `DropTarget` 兜底。

## 代码片段

### Compose 版（MainView.kt 摘要）

```kotlin
@OptIn(ExperimentalComposeUiApi::class)
val dndModifier = Modifier.dragAndDropTarget(
  shouldStartDragAndDrop = { true },
  target = object : DragAndDropTarget {
    override fun onDrop(event: DragAndDropEvent): Boolean {
      val t = event.awtTransferable ?: return false
      val pdf = extractPdf(t) ?: return false
      mainViewModel.openPdf(pdf.absolutePath)
      return true
    }
  }
)

Box(Modifier.fillMaxSize().then(dndModifier)) { /* UI ... */ }
```

关键解析（复用逻辑）：

```kotlin
fun parseUriList(text: String): List<File> = /* text/uri-list -> File */
fun extractPdf(t: Transferable): File? { /* 支持 javaFileListFlavor / text-uri-list / x-java-url / stringFlavor */ }
```

### Java 版（App.kt 摘要）

```kotlin
val handler = object : TransferHandler() {
  override fun canImport(support: TransferSupport) = /* 检查 flavor + set COPY */
  override fun importData(support: TransferSupport): Boolean {
    val pdf = extractPdf(support.transferable) ?: return false
    SwingUtilities.invokeLater { mainViewModel.openPdf(pdf.absolutePath) }
    return true
  }
}

// 初始挂载 + 递归挂载 + 属性变更重挂 + componentAdded 监听
rootPane.transferHandler = handler
glassPane.transferHandler = handler
contentPane.transferHandler = handler
layeredPane.transferHandler = handler

// 兜底 AWT DropTarget
frame.dropTarget = object : DropTarget() { /* dragEnter/dragOver/drop */ }
```

## 代码组织与文件位置说明（App.kt vs MainView.kt）

- 为什么 Java DnD 在 `App.kt`：
  - Java/Swing 的 DnD（TransferHandler/DropTarget）必须挂在 Swing 组件（如 JFrame/rootPane/glassPane 等）上，最方便的接入点就是窗口初始化处（`App.kt`）。
  - 这使它能在 Swing 层级处理 AWT 事件，但需要处理 Compose 后续替换面板导致 handler 丢失的问题（因此引入递归/监听/兜底）。

- 为什么 Compose DnD 在 `MainView.kt`：
  - Compose 的 DnD 是 UI 层 Modifier（`dragAndDropTarget`），自然应该附着在具体的 Composable（如根 `Box`）上，这样与界面结构一致，也不会因 Swing 面板替换而失效。

- 当前仓库状态：
  - 代码中已移除 Java 版 DnD（`App.kt` 仅负责启动与渲染），保留 Compose 版 DnD（`MainView.kt` 根容器）。
  - 文档中保留两种实现的说明与代码片段，便于回溯与参考。

## 现状与后续
- 当前两种实现均已在 macOS 上验证可用。
- 建议默认走 Compose 路径，Java 路径作为临时兜底；稳定后移除 Java 路径，减少跨层复杂度与日志噪音。
