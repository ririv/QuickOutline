import * as pdfjsLib from 'pdfjs-dist';
import { renderPageToUrl, renderPageToDataUrl } from './renderer';
import { getBookmarks } from './outline';
import { invoke } from '@tauri-apps/api/core';

// Assuming we have copied the worker to public/libs
pdfjsLib.GlobalWorkerOptions.workerSrc = '/libs/pdf.worker.min.mjs';

// Re-export core functions for external usage
export { renderPageToUrl, renderPageToDataUrl, getBookmarks };

// Main PDF document loading function
// src can be URL string, ArrayBuffer, or PDFDataRangeTransport
export async function loadPdfDocument(src: any): Promise<pdfjsLib.PDFDocumentProxy> {
    const loadingTask = pdfjsLib.getDocument(src);
    return loadingTask.promise;
}

export async function loadPdfFromPath(path: string): Promise<pdfjsLib.PDFDocumentProxy> {
    // 1. Set current PDF path in Rust backend
    await invoke('set_current_pdf', { path });
    
    // 2. Get static server port
    const port = await invoke('get_static_server_port');
    
    // 3. Construct local URL
    // Append timestamp to prevent caching if file changes
    const url = `http://127.0.0.1:${port}/pdf/current.pdf?t=${Date.now()}`;
    
    // 4. Load with PDF.js (Standard HTTP Range Requests supported by backend)
    return loadPdfDocument(url);
}
