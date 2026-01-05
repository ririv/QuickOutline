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
        rootMargin = "600px 0px", 
        debounce = 200,
        onLoad
    }: Props = $props();

    let src = $state("");
    let loading = $state(true);
    let fromCache = $state(false); // 标记是否命中缓存
    
    let container: HTMLElement | undefined = $state();
    let loadTimeout: number | undefined;
    
    let observer: IntersectionObserver | undefined;
    let active = true;
    let isIntersecting = $state(false);

    // 核心加载逻辑
    async function load() {
        if (!active) return;
        const path = docStore.currentFilePath;
        if (!path) return;

        if (cache?.has(index)) {
            applySource(cache.get(index)!, true);
            return;
        }

        try {
            const url = await pdfRenderService.renderPage(path, index, scaleOrType);
            if (active) {
                cache?.set(index, url);
                applySource(url, false);
            }
        } catch (e) {
            console.error(`[LazyPdfImage] Failed to load page ${index}:`, e);
            loading = false;
        }
    }

    function applySource(url: string, cached: boolean) {
        src = url;
        fromCache = cached;
        // 如果是缓存命中，我们甚至可以乐观地认为 loading = false，
        // 但为了稳妥（等待 onload），我们只用 fromCache 来控制 Spinner 不显示
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

    // 1. Intersection Observer
    $effect(() => {
        if (!container) return;

        observer = new IntersectionObserver((entries) => {
            isIntersecting = entries[0].isIntersecting;
            
            if (isIntersecting && !src) {
                if (loadTimeout) clearTimeout(loadTimeout);
                loadTimeout = window.setTimeout(load, debounce);
            } else if (!isIntersecting) {
                if (loadTimeout) clearTimeout(loadTimeout);
                loadTimeout = undefined;
            }
        }, { rootMargin });

        observer.observe(container);

        return () => {
            observer?.disconnect();
            if (loadTimeout) clearTimeout(loadTimeout);
        };
    });

    // 2. Index 变化监听 (Fast Path)
    $effect(() => {
        const _i = index;
        
        if (loadTimeout) clearTimeout(loadTimeout);

        // Fast Path: 缓存命中
        if (cache?.has(index)) {
            applySource(cache.get(index)!, true);
        } else {
            // Slow Path: 缓存未命中
            src = ""; 
            loading = true; 
            fromCache = false;
            
            if (isIntersecting) {
                loadTimeout = window.setTimeout(load, debounce);
            }
        }
    });

    // 3. 全局重置
    $effect(() => {
        const _v = docStore.version;
        src = "";
        loading = true;
        fromCache = false;
    });

    onDestroy(() => {
        active = false;
    });
</script>

<div bind:this={container} class="relative overflow-hidden {className}">
    <!-- 背景：白色 -->
    <div class="absolute inset-0 bg-white z-0"></div>

    <!-- Spinner：仅在未命中缓存且正在加载时显示 -->
    {#if loading && !fromCache}
        <div class="absolute inset-0 flex items-center justify-center z-10">
            <div class="w-5 h-5 border-2 border-gray-200 border-t-blue-500 rounded-full animate-spin"></div>
        </div>
    {/if}

    <!-- 图片 -->
    {#if src}
        <img 
            {src} 
            {alt} 
            class="relative z-20 w-full h-full object-contain transition-opacity duration-300 {loading ? 'opacity-0' : 'opacity-100'} {imgClass}"
            onload={handleImgLoad}
        />
    {/if}
</div>

<style>
    @keyframes spin { to { transform: rotate(360deg); } }
    .animate-spin { animation: spin 0.8s linear infinite; }
</style>