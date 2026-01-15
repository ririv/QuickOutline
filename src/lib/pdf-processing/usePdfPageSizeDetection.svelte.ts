import { detectPageSize } from './page-size';
import type { PageSize } from '@/lib/types/page';
import { docStore } from '@/stores/docStore.svelte';

export interface PageSizeDetectionState {
    suggestedPageSize: PageSize | undefined;
    actualDimensions: { width: number, height: number } | undefined;
    referencePage: number;
    pageCount: number;
    options: {
        above: number | null;
        below: number | null;
    };
    onReferenceChange: (page: number) => void;
}

export function usePdfPageSizeDetection(getPosition: () => number): PageSizeDetectionState {
    let suggestedPageSize = $state<PageSize | undefined>(undefined);
    let actualDimensions = $state<{ width: number, height: number } | undefined>(undefined);
    let manualRefPage = $state<number | null>(null);

    // Watch position change to reset manual override
    $effect(() => {
        getPosition(); // Track position
        manualRefPage = null; // Reset when pos changes
    });

    // Derived default reference page based on insertion position
    const info = $derived.by(() => {
        const pos = getPosition();
        const count = docStore.pageCount;
        
        // above is pos-1, below is pos (since inserting AT pos shifts existing pos to pos+1)
        const above = (pos > 1 && pos <= count + 1) ? pos - 1 : null;
        const below = (pos >= 1 && pos <= count) ? pos : null;

        let activePage: number;

        if (manualRefPage !== null) {
            activePage = manualRefPage;
        } else {
            // Smart default logic
            if (count === 0) activePage = 0;
            else if (pos <= 1) activePage = 1; // Insert at start -> look forward
            else activePage = pos - 1;       // Insert elsewhere -> look backward
        }

        return {
            above,
            below,
            activePage
        };
    });

    $effect(() => {
        const pdfDoc = docStore.pdfDoc;
        const refPage = info.activePage;
        
        if (pdfDoc && refPage >= 1 && refPage <= docStore.pageCount) {
            let active = true;
            
            detectPageSize(pdfDoc, refPage).then(result => {
                if (active && result) {
                    suggestedPageSize = result.pageSize;
                    actualDimensions = { width: result.actualWidth, height: result.actualHeight };
                }
            });

            return () => {
                active = false;
            };
        } else {
            suggestedPageSize = undefined;
            actualDimensions = undefined;
        }
    });

    return {
        get suggestedPageSize() { return suggestedPageSize },
        get actualDimensions() { return actualDimensions },
        get referencePage() { return info.activePage },
        get pageCount() { return docStore.pageCount },
        get options() { return { above: info.above, below: info.below } },
        onReferenceChange: (p: number) => {
            const count = docStore.pageCount;
            // Allow loose input but clamp before setting
            if (isNaN(p)) return;
            if (p < 1) p = 1;
            if (p > count) p = count;
            manualRefPage = p;
        }
    };
}