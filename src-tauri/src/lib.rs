mod java_sidecar;
mod printer_native;
mod printer_headless;
mod printer_headless_chrome;
mod printer;

use std::sync::Mutex;
use tauri::Manager;
use tauri_plugin_cli::CliExt; // Import CliExt trait

#[tauri::command]
fn greet(name: &str) -> String {
    format!("Hello, {}! You've been greeted from Rust!", name)
}

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    tauri::Builder::default()
        .plugin(tauri_plugin_dialog::init())
        .plugin(tauri_plugin_shell::init())
        .plugin(tauri_plugin_opener::init())
        .plugin(tauri_plugin_cli::init()) // Initialize CLI plugin
        .manage(java_sidecar::JavaState {
            port: Mutex::new(None),
        })
        .setup(move |app| {
            let mut custom_port: Option<u16> = None;
            let mut use_external_sidecar = false;

            // Use the CLI plugin to parse arguments
            match app.cli().matches() {
                Ok(matches) => {
                    if let Some(arg_data) = matches.args.get("port") {
                        // Check if value is number or string and parse
                        if let Some(port_val) = arg_data.value.as_u64() {
                            custom_port = Some(port_val as u16);
                            println!("Rust (CLI Plugin): Custom port {} parsed.", port_val);
                        } else if let Some(port_str) = arg_data.value.as_str() {
                            if let Ok(p) = port_str.parse::<u16>() {
                                custom_port = Some(p);
                                println!("Rust (CLI Plugin): Custom port {} parsed from string.", p);
                            }
                        }
                    }
                    if let Some(arg_data) = matches.args.get("external-sidecar") {
                         use_external_sidecar = arg_data.value.as_bool().unwrap_or(false);
                         if use_external_sidecar {
                             println!("Rust (CLI Plugin): External sidecar mode enabled.");
                         }
                    }
                }
                Err(e) => {
                    eprintln!("Rust: Failed to match CLI args: {}", e);
                }
            }

            if use_external_sidecar {
                if let Some(port) = custom_port {
                    java_sidecar::connect_external(app.handle(), port);
                } else {
                    eprintln!("Rust Error: --external-sidecar requires --port to be specified.");
                }
            } else {
                // 2. 调用解耦后的启动逻辑，并传入 custom_port
                java_sidecar::start(app.handle(), custom_port);
            }

            // (可选) 调试时自动打开控制台
            #[cfg(debug_assertions)]
            {
                if let Some(window) = app.get_webview_window("main") {
                    window.open_devtools();
                }
            }

            Ok(())
        })
        .invoke_handler(tauri::generate_handler![greet, java_sidecar::get_java_port, printer::print_to_pdf_with_html_string])
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}
