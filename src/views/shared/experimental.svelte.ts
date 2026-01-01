import { generateMockBlocks, mockMeasure, VirtualPager, type Page } from '@/lib/experimental/virtual-pager.ts';

export function useExperimentalActions() {
    let demoPages = $state<Page[]>([]);
    let demoCurrentPageIndex = $state(0);
    let demoStats = $state({ totalTime: 0, totalPages: 0 });
    let isRunning = $state(false);

    function runVirtualPagerDemo() {
        isRunning = true;
        // Use setTimeout to allow UI to update (show loading state)
        setTimeout(() => {
            const blockCount = 10000;
            const blocks = generateMockBlocks(blockCount);
            
            const pager = new VirtualPager({
                pageHeight: 800, 
                marginTop: 40,
                marginBottom: 40,
                lineHeight: 20
            });

            const start = performance.now();
            const pages = pager.paginate(blocks, mockMeasure);
            const end = performance.now();

            demoPages = pages;
            demoCurrentPageIndex = 0;
            demoStats = {
                totalTime: end - start,
                totalPages: pages.length
            };
            isRunning = false;
        }, 50);
    }
    
    // Getters and setters wrappers are not needed if we return the state objects directly,
    // but in Svelte 5, returning $state variables works if they are used in the component.
    // However, primitives (numbers, booleans) returned as values lose reactivity if destructured?
    // No, in Svelte 5 `let x = $state(0)`: `x` is a value.
    // If I return `{ demoPages }`, the component gets the value. If the component uses it in template, it's fine?
    // Wait, $state creates a signal. Reading it is fine.
    // But if the component wants to *bind* to it or react to it, we need to be careful.
    // Actually, in Svelte 5, state is passed by value but the reactivity is fine if it's an object/array.
    // For primitives, if I return `demoCurrentPageIndex`, it is just a number.
    // I should return an object that contains the state properties, OR use getters/setters.
    // Or return a "store" object.
    
    // Let's wrap state in a simple object to preserve reference/reactivity for primitives if needed,
    // or just return functions to mutate them?
    // The view needs to read `demoCurrentPageIndex`. If I return it, it's the current value (0). It won't update.
    
    // Correct Svelte 5 pattern for external state:
    // Return a state object.
    
    const state = {
        get demoPages() { return demoPages },
        set demoPages(v) { demoPages = v },
        
        get demoCurrentPageIndex() { return demoCurrentPageIndex },
        set demoCurrentPageIndex(v) { demoCurrentPageIndex = v },
        
        get demoStats() { return demoStats },
        set demoStats(v) { demoStats = v },
        
        get isRunning() { return isRunning },
        set isRunning(v) { isRunning = v }
    };

    return {
        state,
        runVirtualPagerDemo
    };
}
