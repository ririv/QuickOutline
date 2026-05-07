import { detectPageSize } from './page-size';
import type { PageSize } from '@/lib/types/page';
import { docStore } from '@/stores/docStore.svelte';

export interface PageSizeDetectionState {
    suggestedPageSize: PageSize | undefined;
    actualDimensions: { width: number, height: number } | undefined;
    autoDetect: boolean;
    referencePage: number;
    pageCount: number;
    options: {
        above: number | null;
        below: number | null;
    };
    setAutoDetect: (enabled: boolean) => void;
    onReferenceChange: (page: number) => void;
}

interface PageSizeDetectionOptions {
    getPageSize?: () => PageSize;
    setPageSize?: (pageSize: PageSize) => void;
    onPageSizeChange?: () => void;
}

function isSamePageSize(a: PageSize, b: PageSize) {
    if (a.type !== b.type) return false;
    if (a.type === 'preset' && b.type === 'preset') {
        return a.size === b.size && a.orientation === b.orientation;
    }
    if (a.type === 'custom' && b.type === 'custom') {
        return Math.abs(a.width - b.width) < 0.1 && Math.abs(a.height - b.height) < 0.1;
    }
    return false;
}

export function usePdfPageSizeDetection(
    getPosition: () => number,
    options: PageSizeDetectionOptions = {}
): PageSizeDetectionState {
    let suggestedPageSize = $state<PageSize | undefined>(undefined);
    let actualDimensions = $state<{ width: number, height: number } | undefined>(undefined);
    let manualRefPage = $state<number | null>(null);
    let autoDetect = $state(true);

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

    $effect(() => {
        if (!autoDetect || !suggestedPageSize || !options.getPageSize || !options.setPageSize) return;
        const currentPageSize = options.getPageSize();
        if (isSamePageSize(currentPageSize, suggestedPageSize)) return;
        options.setPageSize(suggestedPageSize);
        options.onPageSizeChange?.();
    });

    return {
        get suggestedPageSize() { return suggestedPageSize },
        get actualDimensions() { return actualDimensions },
        get autoDetect() { return autoDetect },
        get referencePage() { return info.activePage },
        get pageCount() { return docStore.pageCount },
        get options() { return { above: info.above, below: info.below } },
        setAutoDetect: (enabled: boolean) => {
            autoDetect = enabled;
        },
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
