import * as pdfjsLib from 'pdfjs-dist';
import { renderPageToUrl, renderPageToDataUrl } from './renderer';
import { getBookmarks } from './outline';
import { getPageLabels } from './labels';
import { invoke } from '@tauri-apps/api/core';
import type { DocumentInitParameters } from 'pdfjs-dist/types/src/display/api';

// Assuming we have copied the worker to public/libs
pdfjsLib.GlobalWorkerOptions.workerSrc = '/libs/pdf.worker.min.mjs';

// Re-export core functions for external usage
export { renderPageToUrl, renderPageToDataUrl, getBookmarks, getPageLabels };

/**
 * Main PDF document loading function.
 * @param src URL string, ArrayBuffer, Uint8Array, or DocumentInitParameters
 */
export async function loadPdfDocument(src: string | ArrayBuffer | Uint8Array | DocumentInitParameters): Promise<pdfjsLib.PDFDocumentProxy> {
    let config: DocumentInitParameters = {};
    if (typeof src === 'string') {
        config = { url: src };
    } else if (src instanceof ArrayBuffer || ArrayBuffer.isView(src)) {
        config = { data: src as Uint8Array };
    } else {
        config = { ...src };
    }

    // Configure CMaps for CJK support
    config.cMapUrl = '/libs/bcmaps/';
    config.cMapPacked = true;

    const loadingTask = pdfjsLib.getDocument(config);
    return loadingTask.promise;
}

/**
 * Loads a PDF document using the stateless static server.
 * Supports multi-doc by passing the path as a query parameter.
 */
export async function loadPdfFromPath(path: string): Promise<pdfjsLib.PDFDocumentProxy> {
    // 1. Get static server port
    const port = await invoke<number>('get_static_server_port');
    
    // 2. Encode path for URL (handles spaces, slashes, etc.)
    const encodedPath = encodeURIComponent(path);
    
    // 3. Construct the stateless URL
    // New endpoint: /pdf/view?path=...
    const url = `http://127.0.0.1:${port}/pdf/view?path=${encodedPath}&t=${Date.now()}`;
    
    // 4. Load with PDF.js
    return loadPdfDocument(url);
}
