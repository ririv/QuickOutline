use tauri::{AppHandle, Manager, Runtime};
use std::path::PathBuf;
use std::process::Command;
use std::fs;
use std::io::Cursor;

#[cfg(target_os = "windows")]
pub async fn print_windows(html: String, output_path: PathBuf) -> Result<String, String> {
    let temp_dir = std::env::temp_dir();
    let temp_html = temp_dir.join("toc_print.html");
    fs::write(&temp_html, html).map_err(|e| e.to_string())?;

    let output_str = output_path.to_string_lossy().to_string();
    
    // Find Edge
    let edge_paths = vec![
        "C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe",
        "C:\\Program Files\\Microsoft\\Edge\\Application\\msedge.exe",
        "msedge" // Try PATH
    ];

    let mut browser_path = None;
    for path in edge_paths {
        if path_exists(path) {
            browser_path = Some(path.to_string());
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
                browser_path = Some(path.to_string());
                break;
            }
        }
    }

    if let Some(browser) = browser_path {
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
        cmd.arg("--print-to-file")
           .arg(&output_str)
           .arg(&temp_html);
    } else {
        cmd.arg("--disable-gpu")
           .arg(format!("--print-to-pdf={}", output_str))
           .arg("--no-pdf-header-footer")
           .arg(&temp_html);
    }

    let output = cmd.output().map_err(|e| e.to_string())?;

    if output.status.success() {
        println!("PDF generated successfully at: {}", output_str);
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
pub async fn print_mac<R: Runtime>(app: &AppHandle<R>, html: String, output_path: PathBuf, browser_path: Option<String>, force_download: bool) -> Result<String, String> {
    // 1. Use explicitly provided path
    if let Some(path) = browser_path {
        if path_exists(&path) {
             println!("Using custom browser: {}", path);
             return execute_headless_print(&path, html, &output_path).await;
        } else {
             return Err(format!("Custom browser path not found: {}", path));
        }
    }

    // 2. Use locally downloaded Chromium (if exists and we are not forcing a new download)
    if !force_download {
        if let Ok(local_browser) = get_local_chromium_path(app) {
            if local_browser.exists() {
                let exec_path = local_browser.join("Contents/MacOS/Chromium");
                if exec_path.exists() {
                     println!("Using local Chromium: {:?}", exec_path);
                     return execute_headless_print(exec_path.to_str().unwrap(), html, &output_path).await;
                }
            }
        }

        // 3. Use system browsers
        let browsers = vec![
            "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome",
            "/Applications/Microsoft Edge.app/Contents/MacOS/Microsoft Edge",
            "/Applications/Firefox.app/Contents/MacOS/firefox",
            "/Applications/Brave Browser.app/Contents/MacOS/Brave Browser",
            "/Applications/Chromium.app/Contents/MacOS/Chromium"
        ];

        for browser in browsers {
            if std::path::Path::new(browser).exists() {
                println!("Using system browser: {}", browser);
                return execute_headless_print(browser, html, &output_path).await;
            }
        }
    }
    
    // 4. Download Chromium if forced or nothing else found
    println!("No suitable browser found. Downloading Chromium...");
    match download_chromium(app).await {
        Ok(path) => {
             let exec_path = path.join("Contents/MacOS/Chromium");
             return execute_headless_print(exec_path.to_str().unwrap(), html, &output_path).await;
        }
        Err(e) => return Err(format!("Failed to download Chromium: {}", e))
    }
}

#[cfg(target_os = "linux")]
pub async fn print_linux(html: String, output_path: PathBuf) -> Result<String, String> {
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
    #[cfg(target_os = "windows")]
    {
        return Command::new("where").arg(path).output().map(|o| o.status.success()).unwrap_or(false);
    }
    #[cfg(not(target_os = "windows"))]
    {
        return false;
    }
}

fn get_local_chromium_path<R: Runtime>(app: &AppHandle<R>) -> Result<PathBuf, String> {
    let app_data = app.path().app_data_dir().map_err(|e| e.to_string())?;
    Ok(app_data.join("chromium").join("chrome-mac").join("Chromium.app"))
}

#[cfg(target_os = "macos")]
async fn download_chromium<R: Runtime>(app: &AppHandle<R>) -> Result<PathBuf, String> {
    let app_data = app.path().app_data_dir().map_err(|e| e.to_string())?;
    let install_dir = app_data.join("chromium");
    fs::create_dir_all(&install_dir).map_err(|e| e.to_string())?;

    let arch = std::env::consts::ARCH;
    let platform_key = if arch == "aarch64" { "Mac_Arm" } else { "Mac" };
    
    let last_change_url = format!("https://www.googleapis.com/download/storage/v1/b/chromium-browser-snapshots/o/{platform_key}%2FLAST_CHANGE?alt=media");
    
    let version = reqwest::get(&last_change_url).await.map_err(|e| e.to_string())?
        .text().await.map_err(|e| e.to_string())?;
        
    let download_url = format!("https://storage.googleapis.com/chromium-browser-snapshots/{platform_key}/{version}/chrome-mac.zip");
    
    println!("Downloading Chromium from: {}", download_url);
    
    let response = reqwest::get(&download_url).await.map_err(|e| e.to_string())?;
    let bytes = response.bytes().await.map_err(|e| e.to_string())?;
    
    let cursor = Cursor::new(bytes);
    let mut archive = zip::ZipArchive::new(cursor).map_err(|e| e.to_string())?;
    
    archive.extract(&install_dir).map_err(|e| e.to_string())?;
    
    let app_path = install_dir.join("chrome-mac").join("Chromium.app");
    
    println!("Removing quarantine attribute...");
    let _ = Command::new("xattr")
        .arg("-d")
        .arg("com.apple.quarantine")
        .arg(&app_path)
        .status();
        
    let exec_path = app_path.join("Contents/MacOS/Chromium");
    use std::os::unix::fs::PermissionsExt;
    if let Ok(mut perms) = fs::metadata(&exec_path).map(|m| m.permissions()) {
        perms.set_mode(0o755);
        let _ = fs::set_permissions(&exec_path, perms);
    }
    
    Ok(app_path)
}

#[cfg(not(target_os = "macos"))]
async fn download_chromium<R: Runtime>(_app: &AppHandle<R>) -> Result<PathBuf, String> {
    Err("Automatic download only implemented for macOS for now.".to_string())
}
