/*!
    Performance Benchmark for lopdf loading

    # Important Performance Note (2026-01-05)
    We observed a massive performance difference between Debug and Release builds when loading large PDFs with `lopdf`.

    ## Test Details
    - **Test File**: "普林斯顿微积分读本(修订版) (阿德里安．班纳 (Adrian Banner)) (Z-Library).pdf"
    - **Size**: ~16MB
    - **Pages**: ~673

    ## Results
    - **Debug Build**: ~13.3 - 14.5 seconds
    - **Release Build**: ~0.24 seconds

    ## Conclusion
    `lopdf` relies heavily on compiler optimizations (inlining, zero-cost abstractions, vectorization) and removing overflow checks.
    The >50x slowdown in Debug mode is expected and does not reflect production performance.
    **Always benchmark with `--release`.**

    ## Usage
    Run this test with:
    ```sh
    cargo test --test pdf_bench_test --release -- --nocapture
    ```
*/

use std::time::Instant;
use lopdf::Document;
use std::path::Path;

#[test]
fn benchmark_lopdf_load() {
    let test_dir = std::env::var("TEST_DIR").expect("TEST_DIR environment variable not set");
    let file_path = std::path::Path::new(&test_dir).join("普林斯顿微积分读本(修订版) (阿德里安．班纳 (Adrian Banner)) (Z-Library).pdf");
    
    if !file_path.exists() {
        println!("Skipping benchmark: Test file not found at {:?}", file_path);
        return;
    }

    println!("Starting to load PDF from: {:?}", file_path);
    
    let start = Instant::now();
    let doc = Document::load(&file_path);
    let duration = start.elapsed();

    match doc {
        Ok(d) => {
            println!("Successfully loaded PDF!");
            println!("Number of pages: {}", d.get_pages().len());
            println!("Time taken to load: {:?}", duration);
            
            if cfg!(debug_assertions) {
                 println!("\n[WARNING] You are running in DEBUG mode. Expect slow performance (e.g. >10s).");
                 println!("Run with --release to see real performance (e.g. <300ms).\n");
            }
        },
        Err(e) => {
            panic!("Failed to load PDF: {:?}", e);
        }
    }
}
