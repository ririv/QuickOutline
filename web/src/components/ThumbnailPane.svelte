<script lang="ts">
    import landscapeIcon from '../assets/icons/landscape.svg';
    import StyledSlider from './controls/StyledSlider.svelte';
    import { appStore } from '@/stores/appStore';

    interface Props {
        pageCount?: number;
        zoom?: number;
    }

    let { pageCount = 0, zoom = $bindable(1.0) }: Props = $props();
    
    // Track which indices are visible to trigger load
    let visibleIndices = $state(new Set<number>());
    
    // Action for lazy loading
    function lazyLoad(node: HTMLElement, index: number) {
        const observer = new IntersectionObserver((entries) => {
            if (entries[0].isIntersecting) {
                visibleIndices.add(index);
                // Svelte 5 Set reactivity might require re-assignment or specific methods? 
                // In Svelte 5 $state(Set), methods like add() are reactive.
                // But to be safe/sure, we can do:
                // visibleIndices = new Set(visibleIndices);
                // Or relying on fine-grained reactivity if it works.
                // Let's assume it works or force update if needed.
                observer.disconnect();
            }
        }, { rootMargin: "200px" });

        observer.observe(node);

        return {
            destroy() {
                observer.disconnect();
            }
        };
    }

    function getThumbnailUrl(index: number) {
        if ($appStore.serverPort > 0) {
            return `http://127.0.0.1:${$appStore.serverPort}/page_images/${index}.png`;
        }
        return '';
    }
</script>

<div class="thumbnail-pane">
    <div class="controls">
            <!-- Debug: {zoom} -->
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
            <div class="grid" style="--zoom: {zoom}">
                
                {#each Array(pageCount) as _, i}
                    <div class="thumbnail-wrapper" use:lazyLoad={i}>
                        {#if visibleIndices.has(i)}
                            <div class="image-container" style="background-image: url('{getThumbnailUrl(i)}')"></div>
                        {:else}
                            <div class="image-container placeholder"></div>
                        {/if}
                        <div class="page-number">{i + 1}</div>
                    </div>
                {:else}
                <div class="empty-state">No thumbnails available</div>
            {/each}
        </div>
    </div>
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
    .thumbnail-wrapper {
        /*不要使用width，而是使用flex，前者会有刚性宽度导致压缩其他元素（比如leftPane）*/
        /*width: calc(100px * var(--zoom, 1));*/
        flex: 0 1 calc(100px * var(--zoom, 1)); /* Use flex-basis for size, allow shrinking */
        min-width: 0;
        overflow: hidden;
        box-shadow: 0 2px 5px rgba(0,0,0,0.1);
        background: white;
        padding: 5px;
        box-sizing: border-box;
        text-align: center;
        transition: flex-basis 0.05s ease-out; /* Changed from 0.1s to 0.05s */
    }
    .image-container {
        width: 100%;
        padding-top: 133.33%; /* Aspect ratio */
        background-size: contain;
        background-repeat: no-repeat;
        background-position: center;
    }
    .image-container.placeholder {
        background-color: #eee;
    }
    .page-number {
        font-size: 12px;
        color: #666;
        padding-top: 4px;
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
