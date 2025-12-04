# QuickOutline 连接与 Sidecar 管理机制

本文档详细说明了 QuickOutline 前后端（Rust/Java/Svelte）之间的启动、发现、连接及容错机制。

## 1. 核心架构概述

QuickOutline 采用经典的三层架构：
*   **UI 层 (Frontend)**: Svelte/Vite，负责用户界面。
*   **系统层 (Rust Tauri)**: 负责应用生命周期、窗口管理、以及 **Sidecar (Java 后端) 的管理与发现**。
*   **服务层 (Java Sidecar)**: 负责 PDF 处理等核心业务逻辑，提供 WebSocket 和 HTTP 服务。

**核心原则**：
1.  **Rust 负责“牵线搭桥”**：Rust 确保 Java 服务可用后，将端口号告知前端。
2.  **前端直接消费**：前端获取端口后，直接通过 WebSocket 连接 Java 服务，后续通信不经过 Rust。
3.  **前端负责“生老病死”**：连接建立后的断线重连由前端独立负责。

---

## 2. 启动模式与场景

系统支持四种启动模式，涵盖生产发布与开发调试需求。

| 模式 | 启动命令示例 | Java 进程来源 | 端口策略 | Rust 行为 | 适用场景 |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **默认模式** | `./app` | Rust 启动子进程 | 随机端口 (OS分配) | 监听子进程 `stdout` 获取端口 | 最终用户 (生产环境) |
| **指定端口** | `./app --port 8080` | Rust 启动子进程 | 固定端口 (8080) | 监听子进程 `stdout` 获取端口 | 生产调试 / 特殊部署 |
| **外部 Sidecar** | `./app -e --port 8080` | **外部** (如 IDEA) | 外部指定 (8080) | **TCP 轮询** 直到端口通畅 | 开发者 (前后端联调) |
| **纯浏览器** | `vite dev` | **外部** (如 IDEA) | 外部指定 | **无** (无 Rust 环境) | 纯前端开发 |

---

## 3. 详细连接流程

### 3.1 服务发现阶段 (Rust)

Rust (`src-tauri/src/java_sidecar.rs`) 根据启动参数决定发现策略：

*   **子进程模式**：启动 `app-sidecar`，捕获其标准输出中的 `{"port": 12345}`。
*   **外部模式 (`-e`)**：
    *   启动一个后台线程。
    *   每秒尝试 `TcpStream::connect("127.0.0.1:port")`。
    *   一旦连接成功，且稳定 500ms，视为服务就绪。

**关键动作**：无论哪种模式，一旦 Rust 确认 Java 服务可用，都会向前端发射全局事件：
```json
// Event: "java-ready"
{
  "message": "{\"port\": 8080}"
}
```

### 3.2 握手与连接阶段 (Frontend)

前端 (`src/components/RpcProvider.svelte`) 的初始化逻辑：

1.  **监听 `java-ready`**：`onMount` 时注册监听器。
2.  **等待信号**：
    *   如果收到 `java-ready`，提取端口，调用 `performConnect(port)`。
    *   如果 4秒内未收到（或 Rust 尚未就绪），显示 **Waiting/Error** 界面。
    *   **外部模式下的体验**：界面显示黄色呼吸灯 "Listening for Sidecar..."，提示用户启动后端。一旦用户在 IDEA 启动后端，Rust 检测到后发送事件，前端自动连接。

### 3.3 容错与自动重连 (Auto-Reconnect)

这是保证系统健壮性的关键机制。

**场景：连接断开**
1.  `rpc.ts` 检测到 WebSocket `onclose`。
2.  触发 `rpc-disconnected` 事件。
3.  `RpcProvider` 捕获事件，更新 UI 为 "Connection lost"，并显示错误信息。
4.  **自动重连启动**：
    *   前端启动一个 `setInterval` (每 3 秒)。
    *   **静默重试**：后台尝试 `new WebSocket(port)`，**不刷新 UI**（避免 Loading/Error 界面闪烁）。
    *   一旦成功，UI 瞬间恢复为 `Connected` 状态。

**场景：手动修改端口**
1.  用户在错误界面手动输入新端口（如 `8081`）并点击连接。
2.  代码逻辑 (`performConnect`)：
    *   **立即终止**之前针对旧端口（`8080`）的自动重连定时器。
    *   尝试连接 `8081`。
    *   如果 `8081` 连接失败，**立即启动**针对 `8081` 的自动重连任务。
    *   **结果**：用户的最后一次手动操作拥有最高优先级，重连目标随之改变。

---

## 4. 关键代码索引

*   **Rust 启动与参数解析**: `src-tauri/src/lib.rs`
*   **Sidecar 管理与 TCP 轮询**: `src-tauri/src/java_sidecar.rs`
*   **命令行参数配置**: `src-tauri/tauri.conf.json`
*   **RPC 通信核心**: `src/lib/api/rpc.ts`
*   **连接状态管理 UI**: `src/components/RpcProvider.svelte`
*   **Java 入口与参数解析**: `server/src/main/java/com/ririv/quickoutline/server/SidecarApp.java`

## 5. 开发调试指南

**最佳实践：全栈调试**
1.  **Java**: 在 IDEA 中配置启动参数 `Program arguments: --port 8080`，以 Debug 模式启动。
2.  **Rust/Frontend**: 在 RustRover 或终端中运行：
    ```bash
    npm run tauri dev -- -- -- --port 8080 -e
    ```
    *(注：`-e` 启用外部模式，四组 `--` 用于穿透 npm/tauri/cargo 传递参数)*
3.  **效果**: Rust 会等待 IDEA 中的服务启动。一旦启动，前端自动连接。你可以在 Java 代码和前端代码中同时断点调试。
