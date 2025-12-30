# 外部编辑器实时同步功能实现总结

## 1. 功能概述
外部编辑器同步功能允许用户将当前 PDF 大纲文本导出到系统安装的专业编辑器（如 VS Code 或 Zed）中进行编辑，并在保存时实时将更改同步回 QuickOutline 界面。该功能已从 Java Sidecar 完全迁移至 Rust (Tauri) 后端，并在稳定性、兼容性和资源管理上进行了深度优化。

## 2. 核心功能特性
- **多编辑器支持**：通过抽象 Trait 实现，原生支持 VS Code (稳定版 & Insiders) 以及 Zed 编辑器。
- **精准光标定位**：启动编辑器时自动跳转到用户在网页端最后选中的行和列。
- **原子级实时同步**：基于文件系统事件监听，用户按下 `Ctrl+S` (或编辑器自动保存) 时，更改会毫秒级推送到前端。
- **稳健的监听策略**：采用“监视父目录”策略，完美解决了现代编辑器在保存时因“原子写（Atomic Save/Inode Swap）”导致的文件句柄失效问题。
- **智能过滤**：引入内容 Hash 校验，避免因编辑器多次触发保存事件而产生的冗余同步和界面抖动。
- **生命周期管理**：支持自动取消机制。当开启新编辑器任务时，旧的监听任务会被自动销毁，确保系统资源占用最小。
- **用户首选项**：前端设置支持手动指定偏好编辑器（Auto / VS Code / Zed），系统会根据选择精确匹配或自动探活。
- **进程自动清理**：主程序退出时，通过 Tauri 生命周期钩子自动杀掉由其启动的编辑器子进程，确保不留下残留窗口。

## 3. 技术实现架构

### 后端 (Rust / Tauri)
- **Trait 抽象 (`EditorBackend`)**：
    定义了通用的编辑器行为接口，包括环境检测 (`is_available`) 和命令构建 (`build_command`)。这使得添加新编辑器支持（如 Cursor 或 Sublime）仅需实现一个结构体。
- **异步监听 (`notify` + `tokio`)**：
    使用 `notify` 库捕捉文件系统事件。监听任务在 `tokio::spawn` 的异步协程中运行，并通过 `oneshot` 通道接收取消信号。
- **状态管理 (`ExternalEditorState`)**：在 Tauri 的 `Managed State` 中维护 `NamedTempFile`（临时文件）、取消信号发送端以及当前活跃的子进程句柄 (`active_child`)。
- **生命周期集成**：利用 Tauri v2 的 `.build().run()` 模式，在 `RunEvent::Exit` 应用退出事件中强制清理残留子进程。
- **进程 management**：使用 `std::process::Command` 启动子进程。在 Windows 下通过 `cmd /C` 增强批处理脚本（.cmd）的启动兼容性。

### 前端 (Svelte 5 + Bridge 模式)
- **Bridge 架构 (`useExternalEditor`)**：采用了基于 Context API 的 Bridge 模式，将逻辑、状态与 UI 彻底解耦。
- **自动生命周期管理**：通过 `provideExternalEditor` 自动管理 `onMount` (初始化) 和 `onDestroy` (清理) 钩子，实现组件级的无感监听。
- **实例类型化**：通过 `ReturnType<typeof Component>` 对 CodeMirror 6 实例进行严格类型定义，确保光标位置获取的安全。
- **双向事件流**：
    - 调用 `open_external_editor` 指令发起同步。
    - 监听 `external-editor-sync` 事件实时更新文本。
    - 监听 `start/end` 事件自动切换 UI 遮罩状态（防止编辑冲突）。

## 4. 关键问题解决 (Optimization Highlights)

### 针对 VS Code "原子写" 的优化
VS Code 在保存时会删除旧文件并重命名新文件。
- **解决方案**：Rust 监听临时文件所在的**父目录**。在接收到 `Modify` 事件后，过滤出目标文件名，并增加 **50ms 的安全延迟** 后再读取，确保内容已完全刷入磁盘且文件锁已释放。

### 内存与所有权优化
- **克隆优化**：在满足异步闭包 `'static` 生命周期要求的前提下，通过调整代码执行顺序，将 `PathBuf` 的克隆次数降至最少（仅一次），最后一步操作直接 `move` 原始所有权进入线程。
- **错误处理**：彻底移除了不安全的 `unwrap()`，所有可能的 Panic 点（如锁竞争、IO 异常）均转化为友好的 Result 错误或前端通知。