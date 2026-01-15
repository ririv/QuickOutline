import { type PageLayout, defaultPageLayout, PAGE_SIZES_MM } from '@/lib/types/page';
import type { PDFDocumentProxy } from 'pdfjs-dist';
import { getPageDimensions } from '@/lib/pdfjs/layout-extractor';

export interface DetectedLayoutResult {
    layout: PageLayout;
    actualWidth: number;
    actualHeight: number;
}

export async function detectPageLayout(doc: PDFDocumentProxy | null, pageNumber: number): Promise<DetectedLayoutResult | null> {
    if (!doc) return null;

    try {
        const { widthMm, heightMm } = await getPageDimensions(doc, pageNumber);
        
        let orientation: 'portrait' | 'landscape' = 'portrait';
        let matchedSize: PageLayout['size'] = 'A4'; 
        let bestDiff = Infinity;

        if (widthMm > heightMm) {
            orientation = 'landscape';
        }

        const checkW = Math.min(widthMm, heightMm);
        const checkH = Math.max(widthMm, heightMm);

        for (const [key, [stdW, stdH]] of Object.entries(PAGE_SIZES_MM)) {
            const diff = Math.abs(stdW - checkW) + Math.abs(stdH - checkH);
            if (diff < bestDiff) {
                bestDiff = diff;
                matchedSize = key as PageLayout['size'];
            }
        }
        
        return {
            layout: {
                ...defaultPageLayout,
                size: matchedSize,
                orientation: orientation
            },
            actualWidth: widthMm,
            actualHeight: heightMm
        };

    } catch (e) {
        console.error("Failed to detect page layout:", e);
        return null;
    }
}