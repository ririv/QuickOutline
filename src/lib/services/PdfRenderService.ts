import { renderPdfPageAsUrl } from '@/lib/api/pdf-render';
import { renderPageToDataUrl, getPageLabels } from '@/lib/pdfjs';
import { docStore } from '@/stores/docStore.svelte';

class PdfRenderService {
    // Engine Switch
    private engine: 'pdfium' | 'pdfjs' = 'pdfjs';

    /**
     * Get page labels for the current document
     * Uses the shared document instance from docStore.
     */
    async getPageLabels(path: string): Promise<string[] | null> {
        const doc = this.getDoc(path);
        if (!doc) return null;
        return getPageLabels(doc);
    }

    /**
     * Get the total page count for the PDF
     */
    async getPageCount(path: string): Promise<number> {
        const doc = this.getDoc(path);
        return doc ? doc.numPages : 0;
    }
    
    /**
     * Render a PDF page to a URL (Blob URL)
     */
    async renderPage(path: string, pageIndex: number, scaleOrType: number | 'thumbnail' | 'preview'): Promise<string> {
        if (!path) throw new Error("Path is required for rendering");

        let scale: number;
        const dpr = window.devicePixelRatio || 1;

        if (typeof scaleOrType === 'number') {
            scale = scaleOrType;
        } else if (scaleOrType === 'thumbnail') {
            scale = 0.5 * dpr;
        } else if (scaleOrType === 'preview') {
            scale = 1.0 * dpr;
        } else {
            scale = 1.0;
        }

        try {
            if (this.engine === 'pdfjs') {
                const doc = this.getDoc(path);
                if (!doc) throw new Error("Document not loaded in docStore");
                // PDF.js uses 1-based page numbers
                return await renderPageToDataUrl(doc, pageIndex + 1, scale);
            } else {
                // Rust PDFium uses 0-based page numbers
                return await renderPdfPageAsUrl(path, pageIndex, scale);
            }
        } catch (e) {
            console.error(`[PdfRenderService] Failed to render page ${pageIndex} via ${this.engine}`, e);
            throw e;
        }
    }

    /**
     * Private helper to get the managed document from docStore.
     * Ensures we are accessing the correct file.
     */
    private getDoc(path: string) {
        if (docStore.currentFilePath === path && docStore.pdfDoc) {
            return docStore.pdfDoc;
        }
        return null;
    }

    /**
     * Legacy Cleanup - DocStore now handles lifecycle
     */
    public clearCache() {
        // No-op: docStore handles destruction
    }
}

export const pdfRenderService = new PdfRenderService();