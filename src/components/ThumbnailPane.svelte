<script lang="ts">
    import landscapeIcon from '../assets/icons/landscape.svg';
    import StyledSlider from './controls/StyledSlider.svelte';
    import { appStore } from '@/stores/appStore';
    import { docStore } from '@/stores/docStore';
    import PreviewTooltip from './PreviewTooltip.svelte';
    import { pageLabelStore } from '@/stores/pageLabelStore';

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

<div class="thumbnail-pane">
    <div class="controls">
        <span class="text-xs text-gray-500 mr-2">Pages: {$docStore.pageCount}</span>
        <img src={landscapeIcon} class="icon landscape-small" alt="Zoom Out" />
        <StyledSlider
            min={0.5}
            max={3.0}
            step={0.01}
            bind:value={zoom}
        />
        <img src={landscapeIcon} class="icon landscape-large" alt="Zoom In" />
    </div>
    <div class="scroll-area">
        {#if !$appStore.serverPort}
            <div class="bg-red-100 text-red-700 p-2 text-center text-xs mb-2 border border-red-200 rounded">
                Backend not connected (Port: {$appStore.serverPort})
            </div>
        {/if}
        <div class="grid" style="--zoom: {zoom}">

            {#each loadedState as isLoaded, i}
                <div 
                    class="outer-thumbnail-wrapper" 
                    use:lazyLoad={i}
                    role="group"
                >
                    <div 
                        class="thumbnail-card"
                        onmouseenter={(e) => handleMouseEnter(e, i)}
                        onmouseleave={handleMouseLeave}
                        role="img"
                        aria-label="Page {i + 1} thumbnail"
                    >
                        {#if isLoaded}
                            <div class="image-container" style="background-image: url('{getThumbnailUrl(i)}')"></div>
                        {:else}
                            <div class="image-container placeholder"></div>
                        {/if}
                    </div>
                    <div class="page-label-display text-xs text-gray-600 mt-1">
                        {displayedPageLabels[i] || (i + 1)}
                    </div>
                </div>
            {:else}
                <div class="empty-state">No thumbnails available</div>
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

<style>
    .thumbnail-pane {
        display: flex;
        flex-direction: column;
        height: 100%;
        background: #f5f5f5;
        border-left: 1px solid #ddd;
    }
    .controls {
        display: flex;
        align-items: center;
        padding: 10px;
        gap: 10px;
        border-bottom: 1px solid #eee;
        background: #fff;
    }

    .scroll-area {
        flex: 1;
        overflow-y: auto;
        padding: 10px;
    }
    .grid {
        display: flex;
        flex-wrap: wrap;
        gap: 10px;
        justify-content: center;
    }
    .outer-thumbnail-wrapper {
        flex: 0 1 calc(100px * var(--zoom, 1));
        min-width: 0;
        box-sizing: border-box;
        text-align: center;
        transition: flex-basis 0.05s ease-out;
        display: flex; 
        flex-direction: column; 
        align-items: center; 
        gap: 5px; /* Space between card and number */
    }
    .thumbnail-card { /* The actual "paper" */
        width: 100%;
        box-shadow: 0 2px 5px rgba(0,0,0,0.1);
        background: white;
        padding: 5px;
        box-sizing: border-box;
        overflow: hidden; /* For image-container to not overflow */
    }
    .image-container {
        width: 100%;
        padding-top: 133.33%; /* Maintain aspect ratio */
        background-size: contain;
        background-repeat: no-repeat;
        background-position: center;
        flex-shrink: 0; 
    }
    .image-container.placeholder {
        background-color: #eee;
    }
    .page-label-display {
        font-size: 12px;
        color: #666;
        margin-top: 5px; /* Adjust as per gap */
        white-space: nowrap; 
        overflow: hidden;
        text-overflow: ellipsis; 
        width: 100%; 
    }
    .empty-state {
        width: 100%;
        text-align: center;
        color: #999;
        margin-top: 20px;
    }
    .icon {
        display: block;
        opacity: 0.6;
    }
    .landscape-small {
        width: 12px;
        height: 12px;
    }
    .landscape-large {
        width: 20px;
        height: 20px;
    }
</style>