<script lang="ts">
    import landscapeIcon from '../assets/icons/landscape.svg';
    import StyledSlider from './controls/StyledSlider.svelte';
    import { appStore } from '@/stores/appStore';
    import { docStore } from '@/stores/docStore';
    import PreviewTooltip from './PreviewTooltip.svelte';
    import { pageLabelStore } from '@/stores/pageLabelStore';
    import Tooltip from './Tooltip.svelte';

    interface Props {
        pageCount?: number;
        zoom?: number;
    }

    let { pageCount = 0, zoom = $bindable(1.0) }: Props = $props();

    let loadedState = $state<boolean[]>(new Array(pageCount).fill(false));
    let hoveredImage = $state<{src: string, y: number, x: number} | null>(null);
    let closeTimer: number | undefined;

    // Derived state for displayed page labels
    // Use backend simulated labels if available, otherwise fallback to simple numbering
    const displayedPageLabels = $derived(
        ($pageLabelStore.simulatedLabels && $pageLabelStore.simulatedLabels.length > 0)
            ? $pageLabelStore.simulatedLabels
            : Array.from({ length: $docStore.pageCount }, (_, i) => String(i + 1))
    );
    
    const originalLabels = $derived($docStore.originalPageLabels || []);

    function isLabelModified(index: number, currentLabel: string) {
        if (!originalLabels || originalLabels.length <= index) return false;
        return currentLabel !== originalLabels[index];
    }

    // Listener for pageCount changes
    $effect(() => {
        if (loadedState.length !== $docStore.pageCount) {
            console.log('PageCount changed:', $docStore.pageCount);
            loadedState = new Array($docStore.pageCount).fill(false);
        }
    });

    // Action for lazy loading
    function lazyLoad(node: HTMLElement, index: number) {
        const observer = new IntersectionObserver((entries) => {
            if (entries[0].isIntersecting) {
                loadedState[index] = true;
                observer.disconnect();
            }
        }, {
            rootMargin: "200px" // Load 200px early
        });

        observer.observe(node);

        return {
            destroy() {
                observer.disconnect();
            }
        };
    }

    function getThumbnailUrl(index: number) {
        if ($appStore.serverPort && $appStore.serverPort > 0) {
            const url = `http://127.0.0.1:${$appStore.serverPort}/page_images/${index}.png`;
            return url;
        }
        console.warn('Server port not set when requesting thumbnail');
        return '';
    }

    function handleMouseEnter(e: MouseEvent, index: number) {
        clearTimeout(closeTimer);
        const target = e.currentTarget as HTMLElement;
        const rect = target.getBoundingClientRect();
        hoveredImage = {
            src: getThumbnailUrl(index),
            y: rect.top + rect.height / 2,
            x: rect.left
        };
    }

    function handleMouseLeave() {
        hoveredImage = null;
    }
</script>

<div class="flex flex-col h-full bg-[#f5f5f5] border-l border-[#ddd]">
    <div class="flex items-center p-2.5 gap-2.5 border-b border-[#eee] bg-white">
        <span class="text-xs text-gray-500 mr-2">Pages: {$docStore.pageCount}</span>
        <img src={landscapeIcon} class="block opacity-60 w-3 h-3" alt="Zoom Out" />
        <StyledSlider
            min={0.5}
            max={3.0}
            step={0.01}
            bind:value={zoom}
        />
        <img src={landscapeIcon} class="block opacity-60 w-5 h-5" alt="Zoom In" />
    </div>
    <div class="flex-1 overflow-y-auto p-2.5">
        {#if !$appStore.serverPort}
            <div class="bg-red-100 text-red-700 p-2 text-center text-xs mb-2 border border-red-200 rounded">
                Backend not connected (Port: {$appStore.serverPort})
            </div>
        {/if}
        <div class="flex flex-wrap gap-2.5 justify-center" style="--zoom: {zoom}">

            {#each loadedState as isLoaded, i}
                <div 
                    class="flex-none w-[calc(100px*var(--zoom,1))] min-w-0 box-border text-center transition-[flex-basis] duration-75 ease-out flex flex-col items-center gap-1.5" 
                    use:lazyLoad={i}
                    role="group"
                >
                    <div 
                        class="w-full shadow-[0_2px_5px_rgba(0,0,0,0.1)] bg-white p-1.5 box-border overflow-hidden"
                        onmouseenter={(e) => handleMouseEnter(e, i)}
                        onmouseleave={handleMouseLeave}
                        role="img"
                        aria-label="Page {i + 1} thumbnail"
                    >
                        {#if isLoaded}
                            <div class="w-full pt-[133.33%] bg-contain bg-no-repeat bg-center shrink-0" style="background-image: url('{getThumbnailUrl(i)}')"></div>
                        {:else}
                            <div class="w-full pt-[133.33%] bg-contain bg-no-repeat bg-center shrink-0 bg-[#eee]"></div>
                        {/if}
                    </div>
                    <div class="w-full flex justify-center mt-1.5">
                        <Tooltip content="{i + 1} / {$docStore.pageCount}" position="top">
                             <div 
                                class="text-xs whitespace-nowrap overflow-hidden text-ellipsis max-w-full {isLabelModified(i, displayedPageLabels[i] || '') ? 'text-[#666] font-bold' : 'text-[#666]'}"
                            >
                                {displayedPageLabels[i] || (i + 1)}
                            </div>
                        </Tooltip>
                    </div>
                </div>
            {:else}
                <div class="w-full text-center text-[#999] mt-5">No thumbnails available</div>
            {/each}
        </div>
    </div>
    
    {#if hoveredImage}
        <PreviewTooltip 
            src={hoveredImage.src} 
            y={hoveredImage.y} 
            anchorX={hoveredImage.x} 
        />
    {/if}
</div>