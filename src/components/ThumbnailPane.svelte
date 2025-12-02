<script lang="ts">
    import landscapeIcon from '../assets/icons/landscape.svg';
    import StyledSlider from './controls/StyledSlider.svelte';
    import { appStore } from '@/stores/appStore';
    import PreviewTooltip from './PreviewTooltip.svelte';

    interface Props {
        pageCount?: number;
        zoom?: number;
    }

    let { pageCount = 0, zoom = $bindable(1.0) }: Props = $props();

    // 性能优化方案：使用布尔数组代替 Set
    // 初始化为空，依靠下方的 $effect 根据 pageCount 填充
    let loadedState = $state<boolean[]>([]);
    let hoveredImage = $state<{src: string, y: number, x: number} | null>(null);

    // 监听 pageCount 变化，如果页数变了（例如文档加载完成），重置加载状态数组
    $effect(() => {
        if (loadedState.length !== pageCount) {
            // 创建指定长度的数组，全部填充为 false
            loadedState = new Array(pageCount).fill(false);
        }
    });

    // Action for lazy loading
    function lazyLoad(node: HTMLElement, index: number) {
        const observer = new IntersectionObserver((entries) => {
            if (entries[0].isIntersecting) {
                // 【核心修改】
                // Svelte 5 代理数组：直接修改索引是响应式的，且只会触发当前图片的更新
                loadedState[index] = true;

                observer.disconnect();
            }
        }, {
            rootMargin: "200px" // 提前 200px 加载，体验更好
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
            return `http://127.0.0.1:${$appStore.serverPort}/page_images/${index}.png`;
        }
        return '';
    }

    function handleMouseEnter(e: MouseEvent, index: number) {
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
                <div 
                    class="thumbnail-wrapper" 
                    use:lazyLoad={i}
                    onmouseenter={(e) => handleMouseEnter(e, i)}
                    onmouseleave={handleMouseLeave}
                    role="group"
                >
                    {#if loadedState[i]}
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
    
    {#if hoveredImage}
        <PreviewTooltip src={hoveredImage.src} y={hoveredImage.y} anchorX={hoveredImage.x} />
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
    .thumbnail-wrapper {
        flex: 0 1 calc(100px * var(--zoom, 1));
        min-width: 0;
        overflow: hidden;
        box-shadow: 0 2px 5px rgba(0,0,0,0.1);
        background: white;
        padding: 5px;
        box-sizing: border-box;
        text-align: center;
        transition: flex-basis 0.05s ease-out;
    }
    .image-container {
        width: 100%;
        padding-top: 133.33%;
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