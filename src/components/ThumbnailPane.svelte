<script lang="ts">
    import landscapeIcon from '../assets/icons/landscape.svg';
    import StyledSlider from './controls/StyledSlider.svelte';
    import { appStore } from '@/stores/appStore';
    import { docStore } from '@/stores/docStore';
    import PreviewTooltip from './PreviewTooltip.svelte';
    import { pageLabelStore } from '@/stores/pageLabelStore';
    import Tooltip from './Tooltip.svelte';
    import { pdfRenderService } from '@/lib/services/PdfRenderService';
    import { onDestroy } from 'svelte';

    interface Props {
        pageCount?: number;
        zoom?: number;
    }

    let { pageCount = 0, zoom = $bindable(1.0) }: Props = $props();

    let loadedState = $state<boolean[]>(new Array(pageCount).fill(false));
    let aspectRatios = $state<number[]>(new Array(pageCount).fill(1.3333)); // Default A4 ratio
    let thumbnailUrls = $state<Record<number, string>>({}); // Store blob URLs
    let hoveredImage = $state<{src: string, y: number, x: number} | null>(null);
    let closeTimer: number | undefined;

    // Clean up Blob URLs on destroy
    onDestroy(() => {
        Object.values(thumbnailUrls).forEach(url => URL.revokeObjectURL(url));
        pdfRenderService.clearCache();
    });
    onDestroy(() => {
        console.log('[ThumbnailPane] Destroying component, cleaning up URLs');
        // Object.values(thumbnailUrls).forEach(url => URL.revokeObjectURL(url));
    });

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
            aspectRatios = new Array($docStore.pageCount).fill(1.3333);
            // Revoke old URLs when page count (file) changes
            Object.values(thumbnailUrls).forEach(url => URL.revokeObjectURL(url));
            thumbnailUrls = {};
        }
    });

    function onImageLoad(e: Event, index: number) {
        const img = e.target as HTMLImageElement;
        if (img.naturalWidth && img.naturalHeight) {
            const ratio = img.naturalHeight / img.naturalWidth;
            if (Math.abs(aspectRatios[index] - ratio) > 0.01) {
                aspectRatios[index] = ratio;
            }
        }
    }

    // Action for lazy loading
    function lazyLoad(node: HTMLElement, index: number) {
        const observer = new IntersectionObserver((entries) => {
            if (entries[0].isIntersecting) {
                loadedState[index] = true;
                observer.disconnect();
                
                // Fetch thumbnail
                if ($docStore.currentFilePath && !thumbnailUrls[index]) {
                    const path = $docStore.currentFilePath;
                    const pageIndex = index; // 0-based
                    
                    pdfRenderService.renderPage(path, pageIndex, 'thumbnail')
                        .then(url => {
                            // console.log(`[ThumbnailPane] Thumbnail URL for page ${index}: ${url}`);
                            thumbnailUrls[index] = url;
                        })
                        .catch(err => console.error(`[ThumbnailPane] Failed to load thumbnail for page ${index}`, err));
                }
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

    // Removed getThumbnailUrl and getNormalImageUrl (replaced by async logic)

    function handleMouseEnter(e: MouseEvent, index: number) {
        clearTimeout(closeTimer);
        const target = e.currentTarget as HTMLElement;
        const rect = target.getBoundingClientRect();
        
        // For now, we'll try to load the full image (scale 1.0 or similar)
        if ($docStore.currentFilePath) {
            const path = $docStore.currentFilePath;
            
            pdfRenderService.renderPage(path, index, 'preview')
                .then(url => {
                    hoveredImage = {
                        src: url,
                        y: rect.top + rect.height / 2,
                        x: rect.left
                    };
                })
                .catch(e => console.error(`[ThumbnailPane] Failed to load preview for page ${index}`, e));
        }
    }

    function handleMouseLeave() {
        if (hoveredImage && hoveredImage.src.startsWith('blob:')) {
            URL.revokeObjectURL(hoveredImage.src); // Clean up tooltip image immediately
        }
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
           <!-- Keep the warning but it might be less relevant if we use Rust directly, 
                though serverPort implies Java backend is running for other features (outline etc.) -->
            <div class="bg-red-100 text-red-700 p-2 text-center text-xs mb-2 border border-red-200 rounded">
                Backend not connected (Port: {$appStore.serverPort})
            </div>
        {/if}
        {#key $docStore.version}
        <div class="flex flex-wrap gap-2.5 justify-center" style="--zoom: {zoom}">

            {#each loadedState as isLoaded, i}
                <div 
                    class="flex-none w-[calc(100px*var(--zoom,1))] min-w-0 box-border text-center transition-[flex-basis] duration-75 ease-out flex flex-col items-center gap-1.5" 
                    use:lazyLoad={i}
                    role="group"
                >
                    <div 
                        class="w-full shadow-[0_2px_5px_rgba(0,0,0,0.1)] bg-white p-1.5 box-border overflow-hidden relative"
                        onmouseenter={(e) => handleMouseEnter(e, i)}
                        onmouseleave={handleMouseLeave}
                        role="img"
                        aria-label="Page {i + 1} thumbnail"
                    >
                        <div class="w-full bg-[#eee] relative transition-[padding] duration-200" style="padding-top: {aspectRatios[i] * 100}%">
                            {#if isLoaded && thumbnailUrls[i]}
                                <img 
                                    src={thumbnailUrls[i]} 
                                    class="absolute top-0 left-0 w-full h-full object-contain" 
                                    alt="Page {i + 1}"
                                    onload={(e) => onImageLoad(e, i)}
                                />
                            {/if}
                        </div>
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
        {/key}
    </div>
    
    {#if hoveredImage}
        <PreviewTooltip 
            src={hoveredImage.src} 
            y={hoveredImage.y} 
            anchorX={hoveredImage.x} 
        />
    {/if}
</div>