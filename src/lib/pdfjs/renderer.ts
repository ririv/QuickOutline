import type * as pdfjsLib from 'pdfjs-dist';

export async function renderPageToUrl(
    pdfDocument: pdfjsLib.PDFDocumentProxy, 
    pageNumber: number, 
    scale: number
): Promise<string> {
    const page = await pdfDocument.getPage(pageNumber); // PDF.js page numbers are 1-based by default for getPage

    const viewport = page.getViewport({ scale });
    
    // Create a canvas element to render the page onto
    const canvas = document.createElement('canvas');
    const context = canvas.getContext('2d');
    
    if (!context) {
        throw new Error("Could not get 2D rendering context for canvas.");
    }

    canvas.height = viewport.height;
    canvas.width = viewport.width;

    // Render the PDF page into the canvas
    const renderContext = {
        canvasContext: context,
        viewport: viewport,
        canvas: canvas 
    };
    
    await page.render(renderContext).promise;

    // Convert the canvas content to a Blob URL (more efficient than Data URL)
    return new Promise<string>((resolve, reject) => {
        canvas.toBlob((blob) => {
            if (blob) {
                resolve(URL.createObjectURL(blob));
            } else {
                reject(new Error("Canvas to Blob failed"));
            }
        }, 'image/jpeg', 0.8);
    });
}

// Alternative for getting Blob URL directly for better memory management
export async function renderPageToDataUrl(
    pdfDocument: pdfjsLib.PDFDocumentProxy, 
    pageNumber: number, 
    scale: number
): Promise<string> {
    // Re-use renderPageToUrl logic for simplicity, this is just a data URL
    return renderPageToUrl(pdfDocument, pageNumber, scale);
}
