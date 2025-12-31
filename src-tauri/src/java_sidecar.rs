use serde_json::Value;
use std::sync::Mutex;
use tauri::{App, AppHandle, Builder, Emitter, Manager, Runtime, State};
use tauri_plugin_cli::CliExt;
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

/// 初始化 Java Sidecar 相关的状态和命令注册
pub fn init<R: Runtime>(builder: Builder<R>) -> Builder<R> {
    builder
        .manage(JavaState {
            port: Mutex::new(None),
        })
        .invoke_handler(tauri::generate_handler![get_java_port])
}

/// 处理 Java Sidecar 的启动逻辑，包括 CLI 参数解析
pub fn setup(app: &mut App) -> Result<(), Box<dyn std::error::Error>> {
    let mut custom_port: Option<u16> = None;
    let mut use_external_sidecar = false;

    // 使用 CLI 插件解析参数
    match app.cli().matches() {
        Ok(matches) => {
            if let Some(arg_data) = matches.args.get("port") {
                if let Some(port_val) = arg_data.value.as_u64() {
                    custom_port = Some(port_val as u16);
                } else if let Some(port_str) = arg_data.value.as_str() {
                    if let Ok(p) = port_str.parse::<u16>() {
                        custom_port = Some(p);
                    }
                }
            }
            if let Some(arg_data) = matches.args.get("external-sidecar") {
                use_external_sidecar = arg_data.value.as_bool().unwrap_or(false);
            }
        }
        Err(e) => {
            eprintln!("Rust (Java Sidecar): Failed to match CLI args: {}", e);
        }
    }

    if use_external_sidecar {
        if let Some(port) = custom_port {
            connect_external(app.handle(), port);
        } else {
            eprintln!("Rust Error: --external-sidecar requires --port to be specified.");
        }
    } else {
        start(app.handle(), custom_port);
    }

    Ok(())
}

pub fn start(app: &AppHandle, custom_port: Option<u16>) {
    let app_handle = app.clone();

    tauri::async_runtime::spawn(async move {
        let mut sidecar_command = app_handle.shell().sidecar("app-sidecar").unwrap();

        // If a custom port is provided, add it as an argument
        if let Some(port) = custom_port {
            sidecar_command = sidecar_command.args(&["--port", &port.to_string()]);
            println!("Rust: Passing custom port {} to Java Sidecar.", port);
        }
        
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
                _ => {} // Ignore other events
            }
        }
    });
}

pub fn connect_external(app: &AppHandle, port: u16) {
    let app_handle = app.clone();

    // Update global state
    if let Some(state) = app_handle.try_state::<JavaState>() {
        *state.port.lock().unwrap() = Some(port);
        println!("Rust: Configured for external Java Sidecar on port {}", port);
    }

    // Use a standard thread to poll for the external service
    std::thread::spawn(move || {
        println!("Rust: Waiting for external Sidecar at 127.0.0.1:{}...", port);
        let address = format!("127.0.0.1:{}", port);
        
        loop {
            // Try to connect to the TCP port
            if std::net::TcpStream::connect(&address).is_ok() {
                println!("Rust: External Sidecar is reachable!");
                
                // Wait a brief moment to ensure the server is fully ready to accept requests
                std::thread::sleep(std::time::Duration::from_millis(500));

                // Emit java-ready event immediately
                let message = format!("{{\"port\": {}}}", port);
                let _ = app_handle.emit(
                    "java-ready",
                    SidecarMessage {
                        message,
                    },
                );
                break;
            }
            
            // Wait 1 second before retrying
            std::thread::sleep(std::time::Duration::from_secs(1));
        }
    });
}