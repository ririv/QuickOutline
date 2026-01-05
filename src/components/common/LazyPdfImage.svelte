<script lang="ts">
    import { pdfRenderService } from '@/lib/services/PdfRenderService';
    import { docStore } from '@/stores/docStore.svelte';
    import { onDestroy } from 'svelte';

    interface Props {
        index: number;
        scaleOrType?: number | 'thumbnail' | 'preview';
        className?: string;
        imgClass?: string;
        alt?: string;
        cache?: Map<number, string>;
        rootMargin?: string;
        debounce?: number;
        onLoad?: (dimensions: { width: number, height: number, ratio: number }) => void;
    }

    let { 
        index, 
        scaleOrType = 'thumbnail', 
        className = "", 
        imgClass = "",
        alt = "",
        cache,
        rootMargin = "600px 0px", // 进一步增大视口预加载范围
        debounce = 200,
        onLoad
    }: Props = $props();

    let src = $state("");
    let loading = $state(true);
    let container: HTMLElement | undefined = $state();
    let loadTimeout: number | undefined;
    let observer: IntersectionObserver | undefined;
    let active = true;
    let isIntersecting = $state(false);

    async function load() {
        if (!active) return;
        const path = docStore.currentFilePath;
        if (!path) return;

        // 检查外部缓存
        if (cache?.has(index)) {
            applySource(cache.get(index)!);
            return;
        }

        try {
            const url = await pdfRenderService.renderPage(path, index, scaleOrType);
            if (active) {
                cache?.set(index, url);
                applySource(url);
            }
        } catch (e) {
            console.error(`[LazyPdfImage] Failed to load page ${index}:`, e);
        }
    }

    function applySource(url: string) {
        src = url;
    }

    function handleImgLoad(e: Event) {
        const img = e.currentTarget as HTMLImageElement;
        loading = false;
        if (onLoad && img.naturalWidth > 0) {
            onLoad({
                width: img.naturalWidth,
                height: img.naturalHeight,
                ratio: img.naturalHeight / img.naturalWidth
            });
        }
    }

    // 1. 监听可视区域
    $effect(() => {
        if (!container) return;

        observer = new IntersectionObserver((entries) => {
            isIntersecting = entries[0].isIntersecting;
            if (isIntersecting) {
                if (loadTimeout) clearTimeout(loadTimeout);
                loadTimeout = window.setTimeout(load, debounce);
            } else {
                if (loadTimeout) {
                    clearTimeout(loadTimeout);
                    loadTimeout = undefined;
                }
            }
        }, { rootMargin });

        observer.observe(container);

        return () => {
            observer?.disconnect();
            if (loadTimeout) clearTimeout(loadTimeout);
        };
    });

    // 2. 当 index 变化且已经在视口内时，触发重新加载 (用于虚拟列表复用)
    $effect(() => {
        if (isIntersecting) {
            src = "";
            loading = true;
            if (loadTimeout) clearTimeout(loadTimeout);
            loadTimeout = window.setTimeout(load, debounce);
        }
    });

    // 3. 当文档版本变化时，完全重置
    $effect(() => {
        const _v = docStore.version;
        src = "";
        loading = true;
    });

    onDestroy(() => {
        active = false;
    });
</script>

<!-- 移除强制 relative，允许 className 决定布局 -->
<div bind:this={container} class="overflow-hidden {className}">
    {#if src}
        <img 
            {src} 
            {alt} 
            class="w-full h-full object-contain transition-opacity duration-300 {loading ? 'opacity-0' : 'opacity-100'} {imgClass}"
            onload={handleImgLoad}
        />
    {/if}
    
    {#if loading}
        <!-- 仅在 loading 时使用 absolute 覆盖 -->
        <div class="absolute inset-0 flex items-center justify-center bg-gray-50 transition-opacity duration-300">
            <div class="w-5 h-5 border-2 border-gray-200 border-t-blue-500 rounded-full animate-spin"></div>
        </div>
    {/if}
</div>

<style>
    @keyframes spin { to { transform: rotate(360deg); } }
    .animate-spin { animation: spin 0.8s linear infinite; }
</style>