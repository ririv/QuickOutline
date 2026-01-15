import type { PDFDocumentProxy } from 'pdfjs-dist';

export interface RawPageDimensions {
    widthMm: number;
    heightMm: number;
    rotation: number;
}

const PT_TO_MM = 25.4 / 72;

export async function getPageDimensions(doc: PDFDocumentProxy, pageNumber: number): Promise<RawPageDimensions> {
    // pdfjs pages are 1-indexed
    if (pageNumber < 1 || pageNumber > doc.numPages) {
        throw new Error(`Invalid page number: ${pageNumber}`);
    }

    const page = await doc.getPage(pageNumber);
    const viewport = page.getViewport({ scale: 1.0 }); // Scale 1.0 gives standard points (1/72 inch)
    
    // viewport.width and viewport.height already account for rotation
    return {
        widthMm: viewport.width * PT_TO_MM,
        heightMm: viewport.height * PT_TO_MM,
        rotation: page.rotate
    };
}
