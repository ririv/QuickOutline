use serde_json::Value;
use std::sync::Mutex;
use tauri::{AppHandle, Emitter, Manager, State};
use tauri_plugin_shell::ShellExt;
use tauri_plugin_shell::process::CommandEvent;

// 1. 定义全局状态结构体
pub struct JavaState {
    pub port: Mutex<Option<u16>>,
}

// 2. 定义给前端调用的命令
#[tauri::command]
pub fn get_java_port(state: State<'_, JavaState>) -> Option<u16> {
    *state.port.lock().unwrap()
}

// 消息结构体
#[derive(Clone, serde::Serialize)]
struct SidecarMessage {
    message: String,
}

pub fn start(app: &AppHandle) {
    let app_handle = app.clone();

    tauri::async_runtime::spawn(async move {
        let sidecar_command = app_handle.shell().sidecar("app-sidecar").unwrap();
        let (mut rx, _child) = sidecar_command
            .spawn()
            .expect("Failed to spawn java sidecar");

        println!("Rust: Java Sidecar 启动指令已发送...");

        while let Some(event) = rx.recv().await {
            match event {
                CommandEvent::Stdout(data) => {
                    let line = String::from_utf8_lossy(&data).into_owned();
                    println!("[Java]: {}", line);

                    if line.contains("port") {
                        // 3. 解析并保存端口到全局状态
                        if let Ok(json) = serde_json::from_str::<Value>(&line) {
                            if let Some(port_val) = json["port"].as_u64() {
                                let port = port_val as u16;

                                // === 更新全局状态 ===
                                // 这样前端调用 get_java_port 就能拿到了
                                if let Some(state) = app_handle.try_state::<JavaState>() {
                                    *state.port.lock().unwrap() = Some(port);
                                    println!("Rust: 端口 {} 已保存到全局状态", port);
                                }

                                // 发送事件 (Push)
                                let _ = app_handle.emit(
                                    "java-ready",
                                    SidecarMessage {
                                        message: line.clone(),
                                    },
                                );
                            }
                        }
                    }
                }
                CommandEvent::Stderr(data) => {
                    let line = String::from_utf8_lossy(&data).into_owned();
                    print!("[Java Log]: {}", line);
                }
                _ => {}
            }
        }
    });
}
