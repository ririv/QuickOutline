import { invoke } from "@tauri-apps/api/core";

/**
 * Invokes the Rust backend to render a specific page of a PDF file.
 * Returns the raw PNG byte array.
 * 
 * @param path Absolute path to the PDF file.
 * @param pageIndex 0-based page index.
 * @param scale Scaling factor (e.g., 1.0 for original size, 0.2 for thumbnail).
 */
export async function renderPdfPage(path: string, pageIndex: number, scale: number): Promise<Uint8Array> {
    try {
        const result = await invoke("render_pdf_page", { 
            path, 
            pageIndex, 
            scale 
        });

        // Debugging Data Integrity
        // console.log(`[PDF Render DEBUG] Type: ${typeof result}, IsArray: ${Array.isArray(result)}`);
        // if (result && typeof result === 'object') {
        //      console.log(`[PDF Render DEBUG] Constructor: ${(result as any).constructor?.name}`);
        // }

        let bytes: Uint8Array;
        if (result instanceof ArrayBuffer) {
             // Zero-copy view on the ArrayBuffer returned by Tauri Response
             bytes = new Uint8Array(result);
        } else if (result instanceof Uint8Array) {
            bytes = result;
        } else if (Array.isArray(result)) {
            console.warn("[PDF Render] Received standard Array, converting to Uint8Array");
            bytes = new Uint8Array(result);
        } else {
            console.error("[PDF Render] Received unknown type!", result);
            throw new Error("Invalid data received from backend");
        }
        
        // Basic Length Check
        if (bytes.length < 8) {
             console.error("[PDF Render] Data too short to be a valid image.");
        }

        return bytes;
        } catch (error) {
            console.error(`[PDF Render] Failed to render page ${pageIndex} of ${path}:`, error);
            throw error;
        }
    }
    
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
     * Renders a page and returns a Blob URL directly usable in <img> tags.
 * Note: The caller is responsible for revoking the object URL when it's no longer needed
 * to avoid memory leaks.
 */
export async function renderPdfPageAsUrl(path: string, pageIndex: number, scale: number): Promise<string> {
    const bytes = await renderPdfPage(path, pageIndex, scale);
    console.log(`[PDF Render] Received ${bytes.length} bytes for page ${pageIndex}`);
    const blob = new Blob([bytes], { type: 'image/png' });
    const url = URL.createObjectURL(blob);
    console.log(`[PDF Render] Created Blob URL: ${url}`);
    return url;
}
