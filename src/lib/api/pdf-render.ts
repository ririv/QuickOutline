import { invoke } from "@tauri-apps/api/core";

/**
 * Gets the total page count of a PDF file via Rust backend.
 */
export async function getPageCount(path: string): Promise<number> {
    try {
        const count = await invoke("get_pdf_page_count", { path }) as number;
        return count;
    } catch (error) {
        console.error(`[PDF Render] Failed to get page count for ${path}:`, error);
        throw error;
    }
}

/**
 * Renders a page and returns a URL directly usable in <img> tags.
 * Uses custom protocol `pdfstream://` for zero-copy streaming from Rust.
 */
export async function renderPdfPageAsUrl(path: string, pageIndex: number, scale: number): Promise<string> {
    // URL Encode the path to ensure special characters are handled
    const encodedPath = encodeURIComponent(path);
    // Construct custom protocol URL
    // pdfstream://render?path=...&page=...&scale=...
    return `pdfstream://render?path=${encodedPath}&page=${pageIndex}&scale=${scale}`;
}
