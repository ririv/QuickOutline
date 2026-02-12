import { type PageSize, PAGE_SIZES_MM, type PresetPageSize } from '@/lib/types/page';
import type { PDFDocumentProxy } from 'pdfjs-dist';
import { getPageDimensions } from '@/lib/pdfjs/layout-extractor';

export interface DetectedPageSizeResult {
    pageSize: PageSize;
    actualWidth: number;
    actualHeight: number;
}

export async function detectPageSize(doc: PDFDocumentProxy | null, pageNumber: number): Promise<DetectedPageSizeResult | null> {
    if (!doc) return null;

    try {
        const { widthMm, heightMm } = await getPageDimensions(doc, pageNumber);
        
        let orientation: 'portrait' | 'landscape' = 'portrait';
        let matchedSize: PresetPageSize['size'] = 'A4';
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
                matchedSize = key as PresetPageSize['size'];
            }
        }

        // If the best match is more than 2mm off total, use CustomPageSize
        if (bestDiff > 2) {
            return {
                pageSize: {
                    type: 'custom',
                    width: widthMm,
                    height: heightMm
                },
                actualWidth: widthMm,
                actualHeight: heightMm
            };
        }
        
        return {
            pageSize: {
                type: 'preset',
                size: matchedSize,
                orientation: orientation
            },
            actualWidth: widthMm,
            actualHeight: heightMm
        };

    } catch (e) {
        console.error("Failed to detect page size:", e);
        return null;
    }
}