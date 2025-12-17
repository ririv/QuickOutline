import * as pdfjsLib from 'pdfjs-dist';
import { renderPageToUrl, renderPageToDataUrl } from './renderer';
import { getBookmarks } from './outline';
// No need to import BookmarkUI here if we re-export

// Assuming we have copied the worker to public/libs
pdfjsLib.GlobalWorkerOptions.workerSrc = '/libs/pdf.worker.min.mjs';

// Re-export core functions for external usage
export { renderPageToUrl, renderPageToDataUrl, getBookmarks };

// Main PDF document loading function
export async function loadPdfDocument(url: string): Promise<pdfjsLib.PDFDocumentProxy> {
    const loadingTask = pdfjsLib.getDocument(url);
    return loadingTask.promise;
}
