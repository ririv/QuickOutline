<script lang="ts">
    import { docStore } from '@/stores/docStore.svelte';
    import { pageLabelStore } from '@/stores/pageLabelStore.svelte';
    import { pdfRenderService } from '@/lib/services/PdfRenderService';
    import { onDestroy, onMount } from 'svelte';
    import PreviewPopup from './PreviewPopup.svelte';

    interface Props {
        pageCount?: number;
    }

    let { pageCount = 0 }: Props = $props();

    const ITEM_HEIGHT = 180; 
    const OVERSCAN = 4;

    let container: HTMLDivElement;
    
    // Core State
    let startIndex = $state(0);
    let endIndex = $state(15); 
    let viewportHeight = 800; 

    // Non-reactive Cache
    const thumbnailCache = new Map<number, string>();
    const previewCache = new Map<number, string>();
    
    let hoveredPage = $state<{ src: string, y: number, anchorX: number } | null>(null);

    // Derived Layout
    const paddingTop = $derived(startIndex * ITEM_HEIGHT);
    const paddingBottom = $derived(Math.max(0, (pageCount - endIndex) * ITEM_HEIGHT));

    const originalLabels = $derived(docStore.originalPageLabels || []);

    // --- Action: Robust Loader ---
    function lazyImage(node: HTMLImageElement, index: number) {
        let active = true;
        let currentIdx = index;

        function load(idx: number) {
            // Reset appearance
            node.classList.remove('loaded');
            // Don't clear src immediately to avoid flickering if we have cache, 
            // but here we want to show skeleton for new items
            node.style.opacity = '0'; 

            if (thumbnailCache.has(idx)) {
                node.src = thumbnailCache.get(idx)!;
                node.classList.add('loaded');
                node.style.opacity = '1';
                return;
            }

            const path = docStore.currentFilePath;
            if (path) {
                pdfRenderService.renderPage(path, idx, 'thumbnail')
                    .then(url => {
                        if (active && currentIdx === idx) {
                            thumbnailCache.set(idx, url);
                            node.src = url;
                            node.classList.add('loaded');
                            node.style.opacity = '1';
                        }
                    })
                    .catch(e => console.error(`Load failed for ${idx}`, e));
            }
        }

        // Initial load
        load(index);

        return {
            update(newIndex: number) {
                if (newIndex !== currentIdx) {
                    currentIdx = newIndex;
                    load(newIndex);
                }
            },
            destroy() { active = false; }
        };
    }

    // --- Helpers ---
    function getPageLabel(index: number): string {
        const sims = pageLabelStore.simulatedLabels;
        if (sims && sims.length > index) return sims[index];
        return String(index + 1);
    }

    function isLabelModified(index: number, currentLabel: string) {
        const orig = originalLabels[index];
        if (orig === undefined) return currentLabel !== String(index + 1);
        return currentLabel !== orig;
    }

    // --- Scroll Handler ---
    let rafId: number | null = null;

    function onScroll(e: UIEvent) {
        if (rafId) return;
        const target = e.currentTarget as HTMLDivElement;
        
        rafId = requestAnimationFrame(() => {
            const scrollTop = target.scrollTop;
            const height = target.clientHeight || viewportHeight;

            const start = Math.floor(scrollTop / ITEM_HEIGHT);
            const end = Math.ceil((scrollTop + height) / ITEM_HEIGHT);

            const newStart = Math.max(0, start - OVERSCAN);
            const newEnd = Math.min(pageCount, end + OVERSCAN);

            if (newStart !== startIndex || newEnd !== endIndex) {
                startIndex = newStart;
                endIndex = newEnd;
            }
            rafId = null;
        });
    }

    // --- Lifecycle ---
    $effect(() => {
        const _v = docStore.version; 
        
        thumbnailCache.forEach(url => URL.revokeObjectURL(url));
        thumbnailCache.clear();
        previewCache.forEach(url => URL.revokeObjectURL(url));
        previewCache.clear();
        
        startIndex = 0;
        endIndex = 15;
        if (container) container.scrollTop = 0;
    });

    onMount(() => {
        if (container) viewportHeight = container.clientHeight;
    });

    onDestroy(() => {
        if (rafId) cancelAnimationFrame(rafId);
        thumbnailCache.forEach(url => URL.revokeObjectURL(url));
        previewCache.forEach(url => URL.revokeObjectURL(url));
    });

    async function handleMouseEnter(e: MouseEvent, index: number) {
        const target = e.currentTarget as HTMLElement;
        const rect = target.getBoundingClientRect();
        const anchorX = rect.right; 
        const y = rect.top + rect.height / 2;

        let src = previewCache.get(index) || thumbnailCache.get(index);
        if (!src) return;

        hoveredPage = { src, y, anchorX };

        if (!previewCache.has(index) && docStore.currentFilePath) {
            try {
                const highResUrl = await pdfRenderService.renderPage(docStore.currentFilePath, index, 'preview');
                previewCache.set(index, highResUrl);
                if (hoveredPage && Math.abs(hoveredPage.y - y) < 1) hoveredPage = { ...hoveredPage, src: highResUrl };
            } catch (e) { console.error(e); }
        }
    }
    
    function handleMouseLeave() { hoveredPage = null; }
</script>

<div class="h-full bg-white border-l border-[#e5e7eb] font-sans flex flex-col overflow-hidden">
    <div class="px-5 py-3 border-b border-[#e5e7eb] bg-[#f9fafb] flex justify-between items-center flex-none z-10 shadow-sm h-[50px] box-border">
        <div class="text-xs font-semibold text-gray-500 uppercase tracking-wider">Page Label Preview</div>
        <div class="text-xs text-gray-400 font-mono">{pageCount} Pages</div>
    </div>

    <div 
        class="flex-1 overflow-y-auto relative scrollbar-thin" 
        bind:this={container} 
        onscroll={onScroll}
    >
        <div style="padding-top: {paddingTop}px; padding-bottom: {paddingBottom}px;">
            {#each { length: Math.max(0, Math.min(pageCount, endIndex) - startIndex) } as _, idx (startIndex + idx)}
                {@const i = startIndex + idx}
                {@const label = getPageLabel(i)}
                {@const modified = isLabelModified(i, label)}
                {@const original = originalLabels[i] || String(i + 1)}

                <div class="page-row" style="height: {ITEM_HEIGHT}px;" class:is-modified={modified}>
                    <div class="thumb-section flex flex-col items-center gap-2">
                        <div 
                            class="thumb-col relative group"
                            onmouseenter={(e) => handleMouseEnter(e, i)}
                            onmouseleave={handleMouseLeave}
                            role="img"
                        >
                            <!-- Use action and ensure key is passed -->
                            <img alt="p{i+1}" use:lazyImage={i} />
                            
                            <div class="thumb-skeleton absolute inset-0 -z-10"></div>

                            <div class="absolute top-0 left-0 bg-black/60 text-white text-[9px] font-mono px-1 py-0.5 backdrop-blur-[1px] opacity-80 group-hover:opacity-100 transition-opacity">
                                #{i + 1}
                            </div>
                        </div>
                        
                        <div class="label-value text-xs font-mono font-bold text-center w-full truncate px-1 {modified ? 'text-blue-600' : 'text-gray-700'}" title="Label: {label}">
                            {label}
                        </div>
                    </div>

                    <div class="info-col">
                        {#if modified}
                            <div class="meta-row">
                                <span class="label">Orig:</span>
                                <span class="value orig">{original}</span>
                            </div>
                        {/if}
                    </div>

                    {#if modified}
                        <div class="action-col">
                            <span class="badge modified">Modified</span>
                        </div>
                    {/if}
                </div>
            {/each}
        </div>
        
        {#if pageCount === 0}
             <div class="flex flex-col items-center justify-center h-[200px] text-gray-400 gap-2 absolute top-0 left-0 w-full">
                <span class="text-sm">No pages loaded</span>
            </div>
        {/if}
    </div>

    {#if hoveredPage}
        <PreviewPopup 
            src={hoveredPage.src} 
            y={hoveredPage.y} 
            anchorX={hoveredPage.anchorX} 
            placement="right"
        />
    {/if}
</div>

<style>
  .page-row {
      display: grid;
      grid-template-columns: 100px 1fr auto;
      gap: 24px;
      padding: 0 20px; 
      border-bottom: 1px solid #f3f4f6;
      align-items: center;
      box-sizing: border-box;
      contain: content;
  }
  
  .page-row:hover { background: #f9fafb; }
  .page-row.is-modified { background: #f0f9ff; }
  .page-row.is-modified:hover { background: #e0f2fe; }

  .thumb-section { width: 90px; }
  .thumb-col { display: flex; flex-direction: column; align-items: center; cursor: zoom-in; width: 100%; position: relative; }
  
  .thumb-col img {
      width: 90px;
      height: 127px;
      object-fit: contain;
      border: 1px solid #e5e7eb;
      background: #fff;
      display: block;
      box-shadow: 0 1px 3px rgba(0,0,0,0.1);
      transition: all 0.2s;
      opacity: 0; /* Hidden by default */
  }
  
  /* CSS rule to show image when loaded */
  .thumb-col img.loaded {
      opacity: 1;
  }

  .thumb-col:hover img { border-color: #3b82f6; box-shadow: 0 4px 12px rgba(0,0,0,0.15); }
  
  .thumb-skeleton { 
      width: 90px; 
      height: 127px; 
      background: #f3f4f6; 
      display: flex;
      align-items: center;
      justify-content: center;
  }
  .thumb-skeleton::after {
      content: "";
      width: 16px;
      height: 16px;
      border: 2px solid #e5e7eb;
      border-top-color: #9ca3af;
      border-radius: 50%;
      animation: spin 1s linear infinite;
  }

  @keyframes spin { to { transform: rotate(360deg); } }
  
  .label-value { line-height: 1.2; margin-top: 4px;}

  .info-col { display: flex; flex-direction: column; justify-content: center; align-items: flex-start; gap: 4px; }
  .meta-row { display: flex; align-items: center; gap: 8px; }
  .label { font-size: 10px; color: #9ca3af; text-transform: uppercase; font-family: 'JetBrains Mono', 'Consolas', monospace; width: 40px; flex-shrink: 0; }
  .value { color: #374151; font-family: 'JetBrains Mono', 'Consolas', monospace; }
  .value.orig { color: #9ca3af; text-decoration: line-through; font-size: 11px; }

  .action-col { min-width: 80px; display: flex; justify-content: flex-end; }
  .badge { display: inline-flex; align-items: center; padding: 3px 8px; border-radius: 4px; font-size: 10px; font-weight: 600; text-transform: uppercase; letter-spacing: 0.05em; }
  .badge.modified { background: #eff6ff; color: #2563eb; border: 1px solid #dbeafe; }
  
  .scrollbar-thin::-webkit-scrollbar { width: 6px; height: 6px; }
  .scrollbar-thin::-webkit-scrollbar-track { background: transparent; }
  .scrollbar-thin::-webkit-scrollbar-thumb { background: #d1d5db; border-radius: 3px; }
  .scrollbar-thin::-webkit-scrollbar-thumb:hover { background: #9ca3af; }
</style>