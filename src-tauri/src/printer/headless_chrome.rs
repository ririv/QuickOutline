use headless_chrome::{Browser, LaunchOptions};
use std::path::PathBuf;
use std::fs;
use anyhow::Result;

pub async fn print_to_pdf_with_url(url: String, output_path: PathBuf) -> Result<String> {
     // 1. Configure Launch Options
    let launch_options = LaunchOptions {
        headless: true,
        sandbox: false, // Often necessary in certain environments
        enable_gpu: false,
        ..Default::default()
    };

    // 2. Launch Browser
    let browser = Browser::new(launch_options)?;

    // 3. Create a new tab
    let tab = browser.new_tab()?;

    // 4. Navigate to URL
    tab.navigate_to(&url)?;
    tab.wait_until_navigated()?;

    // // Wait for Paged.js to finish rendering (if present)
    // // We ignore the error (timeout) to support pages without Paged.js
    // let _ = tab.wait_for_element_with_custom_timeout(".pagedjs_ready", std::time::Duration::from_secs(10));

    // 5. Print to PDF
    let pdf_options = headless_chrome::types::PrintToPdfOptions {
        print_background: Some(true), 
        margin_top: Some(0.0),
        margin_bottom: Some(0.0),
        margin_left: Some(0.0),
        margin_right: Some(0.0),
        ..Default::default()
    };

    let pdf_data = tab.print_to_pdf(Some(pdf_options))?;

    // 6. Save to File
    fs::write(&output_path, pdf_data)?;

    Ok(output_path.to_string_lossy().to_string())
}

pub async fn print_to_pdf_with_html_string(html: String, output_path: PathBuf) -> Result<String> {
    // 1. Configure Launch Options
    let launch_options = LaunchOptions {
        headless: true,
        sandbox: false, // Often necessary in certain environments
        enable_gpu: false,
        ..Default::default()
    };

    // 2. Launch Browser
    let browser = Browser::new(launch_options)?;

    // 3. Create a new tab
    let tab = browser.new_tab()?;

    // 4. Set Content
    // headless_chrome supports set_content, but let's be robust and use a temp file
    // because direct string injection can sometimes have encoding/length issues with CDP.
    // However, the library has a helper for this. Let's try content directly first for speed.
    // tab.navigate_to("data:text/html;charset=utf-8," + url_encoded_html)? 
    // Actually, navigate_to is for URLs. content is better set via loading a file.
    
    let temp_dir = std::env::temp_dir();
    let temp_html_path = temp_dir.join(format!("print_job_{}.html", uuid::Uuid::new_v4()));
    fs::write(&temp_html_path, &html)?;
    
    // Convert path to file URL
    let file_url = format!("file://{}", temp_html_path.to_string_lossy());
    
    // Navigate and wait for load
    tab.navigate_to(&file_url)?;
    tab.wait_until_navigated()?;

    // 5. Print to PDF
    // options: landscape, display_header_footer, print_background, scale, etc.
    // We use defaults mostly, but ensure background is printed.
    let pdf_options = headless_chrome::types::PrintToPdfOptions {
        print_background: Some(true), 
        margin_top: Some(0.0),
        margin_bottom: Some(0.0),
        margin_left: Some(0.0),
        margin_right: Some(0.0),
        ..Default::default()
    };

    let pdf_data = tab.print_to_pdf(Some(pdf_options))?;

    // 6. Save to File
    fs::write(&output_path, pdf_data)?;

    // Cleanup temp file
    let _ = fs::remove_file(temp_html_path);

    Ok(output_path.to_string_lossy().to_string())
}
