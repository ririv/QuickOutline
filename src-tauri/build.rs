use std::env;
use std::fs;
use std::io::Cursor;
use std::path::Path;
use anyhow::{Context, Result};

fn main() -> Result<()> {
    // Determine the target architecture and OS
    let target_os = env::var("CARGO_CFG_TARGET_OS").unwrap();
    let target_arch = env::var("CARGO_CFG_TARGET_ARCH").unwrap();

    let (file_name, binary_name, inner_path) = match (target_os.as_str(), target_arch.as_str()) {
        ("macos", "x86_64") => (
            "pdfium-mac-x64.tgz",
            "libpdfium.dylib",
            "lib/libpdfium.dylib",
        ),
        ("macos", "aarch64") => (
            "pdfium-mac-arm64.tgz",
            "libpdfium.dylib",
            "lib/libpdfium.dylib",
        ),
        ("windows", "x86_64") => (
            "pdfium-win-x64.tgz",
            "pdfium.dll",
            "bin/pdfium.dll",
        ),
        ("windows", "aarch64") => (
            "pdfium-win-arm64.tgz",
            "pdfium.dll",
            "bin/pdfium.dll",
        ),
        ("linux", "x86_64") => (
            "pdfium-linux-x64.tgz",
            "libpdfium.so",
            "lib/libpdfium.so",
        ),
        _ => {
            println!("cargo:warning=Unsupported platform for pdfium download: {}-{}", target_os, target_arch);
            tauri_build::build();
            return Ok(());
        }
    };

    let libs_dir = Path::new("libs");
    if !libs_dir.exists() {
        fs::create_dir_all(libs_dir).context("Failed to create libs directory")?;
    }

    let dest_path = libs_dir.join(binary_name);

    if !dest_path.exists() {
        let url = format!("https://github.com/bblanchon/pdfium-binaries/releases/latest/download/{}", file_name);
        println!("cargo:warning=Downloading PDFium from {}", url);
        download_and_extract(&url, &dest_path, inner_path)
            .context("Failed to download and extract PDFium")?;
    } else {
        println!("cargo:warning=PDFium library already exists at {:?}", dest_path);
    }
    
    // clean_res_dir(); // Optional cleanup if needed from previous logic
    tauri_build::build();
    Ok(())
}

fn download_and_extract(url: &str, dest_path: &Path, inner_path: &str) -> Result<()> {
    let response = reqwest::blocking::get(url)?.bytes()?;
    let cursor = Cursor::new(response);

    if url.ends_with(".tgz") {
        let tar = flate2::read::GzDecoder::new(cursor);
        let mut archive = tar::Archive::new(tar);

        for entry in archive.entries()? {
            let mut entry = entry?;
            let path = entry.path()?;
            if path.to_str() == Some(inner_path) {
                let mut outfile = fs::File::create(dest_path)?;
                std::io::copy(&mut entry, &mut outfile)?;
                return Ok(());
            }
        }
    } else if url.ends_with(".zip") {
        let mut archive = zip::ZipArchive::new(cursor)?;
        
        // zip crate uses forward slashes in names usually, but let's be careful
        // The inner_path provided above uses forward slashes which is good for zip match
        if let Ok(mut file) = archive.by_name(inner_path) {
             let mut outfile = fs::File::create(dest_path)?;
             std::io::copy(&mut file, &mut outfile)?;
             return Ok(());
        }
    }

    Err(anyhow::anyhow!("Could not find {} in the downloaded archive", inner_path))
}