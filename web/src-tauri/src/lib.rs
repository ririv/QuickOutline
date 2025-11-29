mod java_sidecar;

use std::sync::{Mutex};
use tauri::{Manager};

#[tauri::command]
fn greet(name: &str) -> String {
    format!("Hello, {}! You've been greeted from Rust!", name)
}


#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    tauri::Builder::default()
        .plugin(tauri_plugin_shell::init())
        .plugin(tauri_plugin_opener::init())
        .manage(java_sidecar::JavaState {
            port: Mutex::new(None)
        })
        .setup(|app| {
            // 2. 调用解耦后的启动逻辑
            java_sidecar::start(app.handle());

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
