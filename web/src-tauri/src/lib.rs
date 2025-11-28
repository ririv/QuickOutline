use std::process::{Command, Child, Stdio};
use std::io::{BufReader, BufRead};
use std::sync::{Mutex, Arc};
use std::thread;
use std::time::Duration;
use tauri::{AppHandle, Manager};

// Global state to hold the sidecar process and port
// Using std::sync::Mutex for thread safety
struct SidecarState {
    child: Option<Child>,
    port: u16,
}

// Using a simple global Mutex. In production, managing this via Tauri's State<T> is better,
// but for a quick implementation in lib.rs, this works.
static SIDECAR_STATE: Mutex<SidecarState> = Mutex::new(SidecarState {
    child: None,
    port: 0,
});

#[tauri::command]
fn greet(name: &str) -> String {
    format!("Hello, {}! You've been greeted from Rust!", name)
}

#[tauri::command]
async fn get_java_sidecar_port(app: AppHandle) -> Result<u16, String> {
    // 1. Check if already running
    {
        let state = SIDECAR_STATE.lock().map_err(|e| e.to_string())?;
        if state.port > 0 {
            return Ok(state.port);
        }
    }

    // 2. Resolve path to the sidecar jar
    // Note: You must configure tauri.conf.json to include the jar in resources
    // "resources": ["../build/libs/quickoutline-2.3.0-all.jar", "sidecar.jar"]
    // For now, let's assume a fixed name 'sidecar.jar' in the resource directory.
    let resource_path = app.path().resource_dir()
        .map_err(|e| format!("Failed to get resource dir: {}", e))?;
    
    // Adjust this filename to match your actual built jar name
    let jar_path = resource_path.join("sidecar.jar"); 

    if !jar_path.exists() {
        // Fallback for development: try to find it in the project build directory
        // This is a hack for dev mode if resources aren't copied yet
        println!("Sidecar jar not found at {:?}, checking build dir...", jar_path);
    }

    println!("Starting Java Sidecar from: {:?}", jar_path);

    // 3. Start the process
    let mut child = Command::new("java")
        .arg("-jar")
        .arg(&jar_path)
        .stdout(Stdio::piped())
        .stderr(Stdio::piped())
        .spawn()
        .map_err(|e| format!("Failed to spawn java: {}", e))?;

    let stdout = child.stdout.take().ok_or_else(|| "Failed to capture stdout".to_string())?;
    let reader = BufReader::new(stdout);

    // 4. Read stdout in a background thread to find the port
    let port_found = Arc::new(Mutex::new(0));
    let port_clone = port_found.clone();

    thread::spawn(move || {
        for line in reader.lines() {
            match line {
                Ok(l) => {
                    println!("[Java] {}", l);
                    if l.contains("\"port\":") {
                        // Simple parsing: {"port": 12345}
                        // remove non-digits to be safe-ish or parse json
                        if let Some(start) = l.find(':') {
                            if let Some(end) = l.find('}') {
                                let num_str = &l[start+1..end].trim();
                                if let Ok(p) = num_str.parse::<u16>() {
                                    let mut p_lock = port_clone.lock().unwrap();
                                    *p_lock = p;
                                    break;
                                }
                            }
                        }
                    }
                }
                Err(e) => eprintln!("Error reading java stdout: {}", e),
            }
        }
    });

    // 5. Wait for port (with timeout)
    let start = std::time::Instant::now();
    let mut resolved_port = 0;
    
    while start.elapsed().as_secs() < 10 {
        {
            let p = *port_found.lock().unwrap();
            if p > 0 {
                resolved_port = p;
                break;
            }
        }
        thread::sleep(Duration::from_millis(100));
    }

    if resolved_port == 0 {
        let _ = child.kill();
        return Err("Timed out waiting for Java Sidecar to report port".to_string());
    }

    // 6. Update global state
    {
        let mut state = SIDECAR_STATE.lock().map_err(|e| e.to_string())?;
        state.child = Some(child);
        state.port = resolved_port;
    }

    Ok(resolved_port)
}

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    tauri::Builder::default()
        .plugin(tauri_plugin_opener::init())
        .invoke_handler(tauri::generate_handler![greet, get_java_sidecar_port])
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}