import { renderPdfPageAsUrl } from '@/lib/api/pdf-render';
import { renderPageToDataUrl, loadPdfFromPath, getPageLabels } from '@/lib/pdfjs';

class PdfRenderService {
    // Engine Switch
    private engine: 'pdfium' | 'pdfjs' = 'pdfjs';
    
    // Cache for PDF.js document to avoid reloading for every thumbnail
    private cachedPath: string | null = null;
    private cachedDoc: any = null;

    /**
     * Get page labels for the current document
     */
    async getPageLabels(path: string): Promise<string[] | null> {
        const doc = await this.getPdfJsDoc(path);
        return getPageLabels(doc);
    }

    /**
     * Get the total page count for the PDF
     */
    async getPageCount(path: string): Promise<number> {
        const doc = await this.getPdfJsDoc(path);
        return doc.numPages;
    }
    
    /**
     * Render a PDF page to a URL (Blob URL)
     * @param path Absolute path to PDF file
     * @param pageIndex 0-based page index
     * @param scaleOrType
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
                const doc = await this.getPdfJsDoc(path);
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

    private async getPdfJsDoc(path: string) {
        if (this.cachedPath !== path || !this.cachedDoc) {
        // Cleanup old doc if exists
        if (this.cachedDoc) {
            this.cachedDoc.destroy();
        }
            console.log(`[PdfRenderService] Loading PDF via PDF.js for rendering: ${path}`);
            this.cachedDoc = await loadPdfFromPath(path);
            this.cachedPath = path;
        }
        return this.cachedDoc;
    }

    public clearCache() {
        if (this.cachedDoc) {
            this.cachedDoc.destroy();
            this.cachedDoc = null;
        }
        this.cachedPath = null;
    }
}

export const pdfRenderService = new PdfRenderService();
