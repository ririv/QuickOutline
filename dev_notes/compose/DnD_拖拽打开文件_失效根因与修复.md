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
