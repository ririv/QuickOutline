use tauri::{AppHandle, Manager, Runtime};
use std::path::PathBuf;
use std::process::Command;
use std::fs;

#[tauri::command]
pub async fn print_to_pdf<R: Runtime>(
    app: AppHandle<R>,
    _window: tauri::Window<R>, // Rename window to _window to suppress unused warning if not used
    html: String,
    filename: String
) -> Result<String, String> {
    let app_data_dir = app.path().app_data_dir()
        .map_err(|e| e.to_string())?;
    
    let output_path = app_data_dir.join(&filename);
    
    // Ensure directory exists
    if let Some(parent) = output_path.parent() {
        fs::create_dir_all(parent).map_err(|e| e.to_string())?;
    }

    #[cfg(target_os = "windows")]
    {
        return print_windows(html, output_path).await;
    }

    #[cfg(target_os = "macos")]
    {
        return print_mac(html, output_path).await;
    }

    #[cfg(target_os = "linux")]
    {
        return print_linux(html, output_path).await;
    }
}

#[cfg(target_os = "windows")]
async fn print_windows(html: String, output_path: PathBuf) -> Result<String, String> {
    let temp_dir = std::env::temp_dir();
    let temp_html = temp_dir.join("toc_print.html");
    fs::write(&temp_html, html).map_err(|e| e.to_string())?;

    let output_str = output_path.to_string_lossy().to_string();
    
    // Find Edge
    let edge_paths = vec![
        "C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe",
        "C:\\Program Files\\Microsoft\\Edge\\Application\\msedge.exe",
        "msedge"
    ];

    let mut browser_path = None;
    for path in edge_paths {
        if path_exists(path) {
            browser_path = Some(path);
            break;
        }
    }

    // Fallback to Chrome if Edge not found
    if browser_path.is_none() {
        let chrome_paths = vec![
            "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe",
            "C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe",
            "chrome"
        ];
        for path in chrome_paths {
            if path_exists(path) {
                browser_path = Some(path);
                break;
            }
        }
    }

    if let Some(browser) = browser_path {
        // msedge --headless --disable-gpu --print-to-pdf="C:\path\to\out.pdf" --no-pdf-header-footer "C:\path\to\in.html"
        let output = Command::new(browser)
            .arg("--headless")
            .arg("--disable-gpu")
            .arg(format!("--print-to-pdf={}", output_str))
            .arg("--no-pdf-header-footer")
            .arg(&temp_html)
            .output()
            .map_err(|e| e.to_string())?;

        if output.status.success() {
            Ok(output_str)
        } else {
            let stderr = String::from_utf8_lossy(&output.stderr);
            // Check if file exists anyway (sometimes Chrome logs warnings to stderr but succeeds)
            if output_path.exists() {
                Ok(output_str)
            } else {
                Err(format!("Windows print failed: {}", stderr))
            }
        }
    } else {
        Err("Microsoft Edge or Google Chrome not found.".to_string())
    }
}

async fn execute_headless_print(browser: &str, html: String, output_path: &PathBuf) -> Result<String, String> {
    let temp_dir = std::env::temp_dir();
    let temp_html = temp_dir.join("toc_print.html");
    fs::write(&temp_html, html).map_err(|e| e.to_string())?;

    let output_str = output_path.to_string_lossy().to_string();
    
    let mut cmd = Command::new(browser);
    cmd.arg("--headless");
    
    if browser.to_lowercase().contains("firefox") {
        // Firefox experimental PDF printing
        cmd.arg("--print-to-file")
           .arg(&output_str)
           .arg(&temp_html);
    } else {
        // Chromium (Chrome/Edge/Chromium)
        cmd.arg("--disable-gpu")
           .arg(format!("--print-to-pdf={}", output_str))
           .arg("--no-pdf-header-footer")
           .arg(&temp_html);
    }

    let output = cmd.output().map_err(|e| e.to_string())?;

    if output.status.success() {
        println!("PDF generated successfully at: {}", output_str); // Added println!
        Ok(output_str)
    } else {
        let stderr = String::from_utf8_lossy(&output.stderr);
        if std::path::Path::new(&output_str).exists() {
             Ok(output_str)
        } else {
             Err(format!("Browser print failed (exit code {}): {}", output.status, stderr))
        }
    }
}

#[cfg(target_os = "macos")]
async fn print_mac(html: String, output_path: PathBuf) -> Result<String, String> {
    let browsers = vec![
        "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome",
        "/Applications/Microsoft Edge.app/Contents/MacOS/Microsoft Edge",
        "/Applications/Firefox.app/Contents/MacOS/firefox",
        "/Applications/Brave Browser.app/Contents/MacOS/Brave Browser",
        "/Applications/Chromium.app/Contents/MacOS/Chromium"
    ];

    for browser in browsers {
        if std::path::Path::new(browser).exists() {
            return execute_headless_print(browser, html, &output_path).await;
        }
    }
    
    Err("No supported browser (Chrome, Edge, Firefox) found in standard macOS locations.".to_string())
}

#[cfg(target_os = "linux")]
async fn print_linux(html: String, output_path: PathBuf) -> Result<String, String> {
    let browsers = vec![
        "google-chrome",
        "microsoft-edge",
        "firefox",
        "chromium",
        "chromium-browser"
    ];

    for browser in browsers {
        if is_command_available(browser) {
            return execute_headless_print(browser, html, &output_path).await;
        }
    }

    Err("No supported browser (Chrome, Edge, Firefox) found in PATH.".to_string())
}

fn is_command_available(cmd: &str) -> bool {
    Command::new("which")
        .arg(cmd)
        .output()
        .map(|o| o.status.success())
        .unwrap_or(false)
}

fn path_exists(path: &str) -> bool {
    if std::path::Path::new(path).exists() {
        return true;
    }
    // Check PATH on Windows?
    // "where" command on Windows is roughly equivalent to "which"
    #[cfg(target_os = "windows")]
    {
        return Command::new("where").arg(path).output().map(|o| o.status.success()).unwrap_or(false);
    }
    #[cfg(not(target_os = "windows"))]
    {
        return false;
    }
}
