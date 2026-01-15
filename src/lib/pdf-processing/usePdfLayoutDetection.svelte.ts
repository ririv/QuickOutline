import { detectPageLayout } from './page-layout';
import type { PageLayout } from '@/lib/types/page';
import { docStore } from '@/stores/docStore.svelte';

export interface LayoutDetectionState {
    suggestedLayout: PageLayout | undefined;
    actualDimensions: { width: number, height: number } | undefined;
    referencePage: number | undefined;
    onReferenceChange: (page: number) => void;
}

export function usePdfLayoutDetection(getPosition: () => number): LayoutDetectionState {
    let suggestedLayout = $state<PageLayout | undefined>(undefined);
    let actualDimensions = $state<{ width: number, height: number } | undefined>(undefined);
    let manualRefPage = $state<number | null>(null);

    let activeRefPage = $derived.by(() => {
        if (manualRefPage !== null) return manualRefPage;
        
        const pos = getPosition();
        const count = docStore.pageCount;
        
        if (count === 0) return 0;
        if (pos <= 1) return 1;
        return pos - 1;
    });

    $effect(() => {
        const pdfDoc = docStore.pdfDoc;
        const pageCount = docStore.pageCount;
        const refPage = activeRefPage;
        
        if (pdfDoc && refPage >= 1 && refPage <= pageCount) {
            let active = true;
            
            detectPageLayout(pdfDoc, refPage).then(result => {
                if (active && result) {
                    suggestedLayout = result.layout;
                    actualDimensions = { width: result.actualWidth, height: result.actualHeight };
                }
            });

            return () => {
                active = false;
            };
        } else {
            suggestedLayout = undefined;
            actualDimensions = undefined;
        }
    });

    return {
        get suggestedLayout() { return suggestedLayout },
        get actualDimensions() { return actualDimensions },
        get referencePage() { return activeRefPage },
        onReferenceChange: (p: number) => {
            const count = docStore.pageCount;
            if (p < 1) p = 1;
            if (p > count) p = count;
            manualRefPage = p;
        }
    };
}