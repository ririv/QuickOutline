<script lang="ts">
    import { docStore } from '@/stores/docStore.svelte';
    import { pageLabelStore } from '@/stores/pageLabelStore.svelte';
    import { pdfRenderService } from '@/lib/services/PdfRenderService';
    import { onDestroy } from 'svelte';
    import PreviewPopup from './PreviewPopup.svelte';
    import VirtualList from './common/VirtualList.svelte';

    interface Props {
        pageCount?: number;
    }

    let { pageCount = 0 }: Props = $props();

    const ITEM_HEIGHT = 180; 

    // Non-reactive Cache
    const thumbnailCache = new Map<number, string>();
    const previewCache = new Map<number, string>();
    
    let hoveredPage = $state<{ src: string, y: number, anchorX: number } | null>(null);

    // Derived Data
    const originalLabels = $derived(docStore.originalPageLabels || []);

    // --- Action: Loader (Smart with IntersectionObserver) ---
    function lazyImage(node: HTMLImageElement, index: number) {
        let active = true;
        let currentIdx = index;
        let observer: IntersectionObserver;
        let loadTimeout: number | undefined;
        
        // Find sibling skeleton
        const skeleton = node.nextElementSibling as HTMLElement;

        function showSkeleton() {
            node.style.display = 'none'; 
            if (skeleton) skeleton.style.display = 'flex'; 
        }

        function hideSkeleton() {
            node.style.display = 'block'; 
            if (skeleton) skeleton.style.display = 'none'; 
        }

        function load(idx: number) {
            if (!active) return;
            
            showSkeleton();

            if (thumbnailCache.has(idx)) {
                node.src = thumbnailCache.get(idx)!;
                hideSkeleton();
                return;
            }

            const path = docStore.currentFilePath;
            if (path) {
                pdfRenderService.renderPage(path, idx, 'thumbnail')
                    .then(url => {
                        if (active && currentIdx === idx) {
                            thumbnailCache.set(idx, url);
                            node.src = url;
                            hideSkeleton();
                        }
                    })
                    .catch(e => console.error(e));
            }
        }

        // Initialize Observer
        observer = new IntersectionObserver((entries) => {
            const entry = entries[0];
            if (entry.isIntersecting) {
                // Debounce load
                loadTimeout = window.setTimeout(() => {
                    load(currentIdx);
                    loadTimeout = undefined;
                }, 200);
            } else {
                if (loadTimeout) {
                    clearTimeout(loadTimeout);
                    loadTimeout = undefined;
                }
            }
        }, {
             rootMargin: "200px 0px"
        });

        observer.observe(node.parentElement || node);

        return {
            update(newIndex: number) {
                if (newIndex !== currentIdx) {
                    currentIdx = newIndex;
                    // Reset state
                    if (loadTimeout) clearTimeout(loadTimeout);
                    // Re-trigger observer check naturally or force a check?
                    // Usually observer will re-fire if element moves, but here only index changes.
                    // Let's force a reload check if already intersecting? 
                    // Simpler: Just rely on observer staying active. 
                    // However, if we reuse the node for a new index, we should probably clear the old image.
                    node.src = ""; // Clear old
                    showSkeleton(); 
                }
            },
            destroy() { 
                active = false; 
                observer.disconnect();
                if (loadTimeout) clearTimeout(loadTimeout);
            }
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

    // Reset Cache on file change
    $effect(() => {
        const _v = docStore.version; 
        thumbnailCache.forEach(url => URL.revokeObjectURL(url));
        thumbnailCache.clear();
        previewCache.forEach(url => URL.revokeObjectURL(url));
        previewCache.clear();
    });

    onDestroy(() => {
        thumbnailCache.forEach(url => URL.revokeObjectURL(url));
        previewCache.forEach(url => URL.revokeObjectURL(url));
    });

    async function handleMouseEnter(e: MouseEvent, index: number) {
        const target = e.currentTarget as HTMLElement;
        const rect = target.getBoundingClientRect();
        
        // Calculate anchor position (left side)
        const anchorX = rect.left; 
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

    <!-- Usage of Generic VirtualList -->
    <VirtualList 
        totalCount={pageCount} 
        itemHeight={ITEM_HEIGHT}
        className="flex-1"
    >
        {#snippet children(i)}
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
                        <img alt="p{i+1}" use:lazyImage={i} />
                        
                        <div class="thumb-skeleton absolute inset-0 -z-10"></div>
                        
                        <div class="absolute top-0 left-0 bg-black/60 text-white text-[9px] font-mono px-1 py-0.5 backdrop-blur-[1px] opacity-80 group-hover:opacity-100 transition-opacity z-10">
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
        {/snippet}
    </VirtualList>

    {#if hoveredPage}
        <PreviewPopup 
            src={hoveredPage.src} 
            y={hoveredPage.y} 
            anchorX={hoveredPage.anchorX} 
            placement="left"
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
  .thumb-col { 
      display: flex; 
      flex-direction: column; 
      align-items: center; 
      justify-content: center; 
      cursor: zoom-in; 
      width: 90px; 
      height: 127px; 
      position: relative; 
  }
  
  .thumb-col img {
      width: auto;
      height: auto;
      max-width: 100%;
      max-height: 100%;
      object-fit: contain;
      
      border: 1px solid #e5e7eb;
      background: #fff;
      display: none; /* Initially hidden */
      box-shadow: 0 1px 3px rgba(0,0,0,0.1);
      transition: border-color 0.2s;
  }
  
  .thumb-col:hover img { border-color: #3b82f6; box-shadow: 0 4px 12px rgba(0,0,0,0.15); }
  
  .thumb-skeleton { 
      width: 100%; 
      height: 100%; 
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
</style>
