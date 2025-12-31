use std::env;
use std::fs;
use std::path::Path;

#[allow(dead_code)]
fn clean_res_dir(){
    // Cargo automatically sets PROFILE to "debug" or "release"
    if let Ok(profile) = env::var("PROFILE") {
        let target_resources_path = Path::new("..").join("target").join(&profile).join("resources");

        // 这个是防止生成的sidecar程序有权限，导致在IDE中生成的文件无法删除
        // sidecar目前已经不使用
        if target_resources_path.exists() {
            // Attempt to remove the directory and print a warning if it fails (e.g. file locked)
            if let Err(e) = fs::remove_dir_all(&target_resources_path) {
                println!("cargo:warning=Failed to clean target resources directory: {}", e);
            }
        }
    } else {
        println!("cargo:warning=PROFILE environment variable not found, skipping cleanup.");
    }

}

fn main() {
    // clean_res_dir();
    tauri_build::build()
}
