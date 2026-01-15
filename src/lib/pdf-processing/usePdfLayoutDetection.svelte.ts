import { detectPageLayout } from './page-layout';
import type { PageLayout } from '@/lib/types/page';
import { docStore } from '@/stores/docStore.svelte';

export interface LayoutDetectionState {
    suggestedLayout: PageLayout | undefined;
    actualDimensions: { width: number, height: number } | undefined;
    referencePage: number;
    options: {
        above: number | null;
        below: number | null;
    };
    currentRefType: 'above' | 'below';
    setRefType: (type: 'above' | 'below') => void;
}

export function usePdfLayoutDetection(getPosition: () => number): LayoutDetectionState {
    let suggestedLayout = $state<PageLayout | undefined>(undefined);
    let actualDimensions = $state<{ width: number, height: number } | undefined>(undefined);
    let preferredType = $state<'above' | 'below' | null>(null);

    // Calculate available neighbors and active reference
    const info = $derived.by(() => {
        const pos = getPosition();
        const count = docStore.pageCount;
        
        // above is pos-1, below is pos (since inserting AT pos shifts existing pos to pos+1)
        const above = (pos > 1 && pos <= count + 1) ? pos - 1 : null;
        const below = (pos >= 1 && pos <= count) ? pos : null;

        let activeType: 'above' | 'below' = 'above';
        
        if (preferredType && (preferredType === 'above' ? above : below)) {
            activeType = preferredType;
        } else if (!above && below) {
            activeType = 'below';
        } else {
            activeType = 'above';
        }

        const activePage = activeType === 'above' ? above : below;

        return {
            above,
            below,
            activeType,
            activePage: activePage || 0
        };
    });

    $effect(() => {
        const pdfDoc = docStore.pdfDoc;
        const refPage = info.activePage;
        
        if (pdfDoc && refPage >= 1) {
            let active = true;
            detectPageLayout(pdfDoc, refPage).then(result => {
                if (active && result) {
                    suggestedLayout = result.layout;
                    actualDimensions = { width: result.actualWidth, height: result.actualHeight };
                }
            });
            return () => { active = false; };
        } else {
            suggestedLayout = undefined;
            actualDimensions = undefined;
        }
    });

    return {
        get suggestedLayout() { return suggestedLayout },
        get actualDimensions() { return actualDimensions },
        get referencePage() { return info.activePage },
        get options() { return { above: info.above, below: info.below } },
        get currentRefType() { return info.activeType },
        setRefType: (type: 'above' | 'below') => {
            preferredType = type;
        }
    };
}
