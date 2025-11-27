<script lang="ts">
    import landscapeIcon from '../assets/icons/landscape.svg';
    import StyledSlider from './controls/StyledSlider.svelte';

    interface Props {
        thumbnails?: string[];
        zoom?: number;
    }

    let { thumbnails = [], zoom = $bindable(1.0) }: Props = $props();
</script>

<div class="thumbnail-pane">
    <div class="controls">
            <!-- Debug: {zoom} -->
            <img src={landscapeIcon} class="icon landscape-small" alt="Zoom Out" />
            <StyledSlider 
                min={0.5} 
                max={3.0} 
                step={0.1} 
                bind:value={zoom} 
            />
            <img src={landscapeIcon} class="icon landscape-large" alt="Zoom In" />
        </div>
        <div class="scroll-area">
            <div class="grid">
                
                {#each thumbnails as src, i}
                    <div class="thumbnail-wrapper" >
                        <div class="image-container" style="background-image: url('{src}')"></div>
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
    }
    .image-container {
        width: 100%;
        padding-top: 133.33%;
        background-size: contain;
        background-repeat: no-repeat;
        background-position: center;
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
