<script lang="ts">
    import { onMount, onDestroy } from 'svelte';
    import type { Snippet } from 'svelte';

    interface Props {
        totalCount: number;
        itemHeight: number;
        overscan?: number;
        children: Snippet<[number]>;
        className?: string;
    }

    let { 
        totalCount, 
        itemHeight, 
        overscan = 4, 
        children,
        className = ""
    }: Props = $props();

    let container: HTMLDivElement | undefined = $state();
    
    // Core State
    let startIndex = $state(0);
    let endIndex = $state(15); 
    let viewportHeight = 800;

    // Derived Spacers
    const paddingTop = $derived(startIndex * itemHeight);
    const paddingBottom = $derived(Math.max(0, (totalCount - endIndex) * itemHeight));

    let rafId: number | null = null;

    function onScroll(e: UIEvent) {
        if (rafId) return;
        const target = e.currentTarget as HTMLDivElement;
        
        rafId = requestAnimationFrame(() => {
            const scrollTop = target.scrollTop;
            const height = target.clientHeight || viewportHeight;

            const start = Math.floor(scrollTop / itemHeight);
            const end = Math.ceil((scrollTop + height) / itemHeight);

            const newStart = Math.max(0, start - overscan);
            const newEnd = Math.min(totalCount, end + overscan);

            if (newStart !== startIndex || newEnd !== endIndex) {
                startIndex = newStart;
                endIndex = newEnd;
            }
            rafId = null;
        });
    }

    function calculateWindow() {
        if (!container) return;
        viewportHeight = container.clientHeight;
        const scrollTop = container.scrollTop;
        const start = Math.floor(scrollTop / itemHeight);
        const end = Math.ceil(viewportHeight / itemHeight);
        startIndex = Math.max(0, start - overscan);
        endIndex = Math.min(totalCount, start + end + overscan);
    }

    // Export methods for parent
    export function scrollTo(index: number, behavior: ScrollBehavior = 'auto') {
        if (container) {
            container.scrollTo({
                top: index * itemHeight,
                behavior
            });
            // Immediate re-calc
            calculateWindow();
        }
    }

    // Watch for total count changes
    $effect(() => {
        const _c = totalCount;
        if (totalCount === 0) {
            startIndex = 0;
            endIndex = 0;
        } else {
            // Re-calc window if count changes (don't force scroll to top unless index out of bounds)
            if (startIndex >= totalCount) {
                startIndex = 0;
                if (container) container.scrollTop = 0;
            }
            calculateWindow();
        }
    });

    onMount(() => {
        calculateWindow();
    });

    onDestroy(() => {
        if (rafId) cancelAnimationFrame(rafId);
    });
</script>

<div 
    bind:this={container} 
    class="flex-1 overflow-y-auto relative scrollbar-thin {className}"
    onscroll={onScroll}
>
    <div style="padding-top: {paddingTop}px; padding-bottom: {paddingBottom}px;">
        {#each { length: Math.max(0, Math.min(totalCount, endIndex) - startIndex) } as _, idx (startIndex + idx)}
            {@const index = startIndex + idx}
            {@render children(index)}
        {/each}
    </div>
</div>

<style>
    .scrollbar-thin::-webkit-scrollbar { width: 6px; height: 6px; }
    .scrollbar-thin::-webkit-scrollbar-track { background: transparent; }
    .scrollbar-thin::-webkit-scrollbar-thumb { background: #d1d5db; border-radius: 3px; }
    .scrollbar-thin::-webkit-scrollbar-thumb:hover { background: #9ca3af; }
</style>