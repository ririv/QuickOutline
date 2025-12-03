mod java_sidecar;

use std::sync::Mutex;
use tauri::Manager;
use std::env; // Add this import

#[tauri::command]
fn greet(name: &str) -> String {
    format!("Hello, {}! You've been greeted from Rust!", name)
}

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    let mut custom_port: Option<u16> = None;

    let args: Vec<String> = env::args().collect();
    // Start from the second argument to skip the executable name
    let mut args_iter = args.into_iter().skip(1).peekable();

    while let Some(arg) = args_iter.next() {
        if arg == "--port" {
            if let Some(port_str) = args_iter.next() {
                if let Ok(port) = port_str.parse::<u16>() {
                    custom_port = Some(port);
                    println!("Rust: Custom port {} parsed from CLI arguments.", port);
                } else {
                    eprintln!("Rust: Invalid port number provided: {}", port_str);
                }
            } else {
                eprintln!("Rust: --port argument requires a port number.");
            }
        }
    }

    tauri::Builder::default()
        .plugin(tauri_plugin_dialog::init())
        .plugin(tauri_plugin_shell::init())
        .plugin(tauri_plugin_opener::init())
        .manage(java_sidecar::JavaState {
            port: Mutex::new(None),
        })
        .setup(move |app| {
            // 2. 调用解耦后的启动逻辑，并传入 custom_port
            java_sidecar::start(app.handle(), custom_port);

            // (可选) 调试时自动打开控制台
            #[cfg(debug_assertions)]
            {
                if let Some(window) = app.get_webview_window("main") {
                    window.open_devtools();
                }
            }

            Ok(())
        })
        .invoke_handler(tauri::generate_handler![greet, java_sidecar::get_java_port])
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}
