import type * as pdfjsLib from 'pdfjs-dist';

/**
 * Extracts page labels from a PDF document.
 * Returns an array of strings where index i is the label for physical page i+1.
 * Returns null if no page labels are defined.
 */
export async function getPageLabels(pdfDocument: pdfjsLib.PDFDocumentProxy): Promise<string[] | null> {
    try {
        const labels = await pdfDocument.getPageLabels();
        if (!labels || labels.length === 0) return null;
        return labels;
    } catch (e) {
        console.error("[PdfJS] Failed to get page labels", e);
        return null;
    }
}
