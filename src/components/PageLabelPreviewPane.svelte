<script lang="ts">
    import { docStore } from '@/stores/docStore.svelte';
    import { pageLabelStore } from '@/stores/pageLabelStore.svelte';
    import { pdfRenderService } from '@/lib/services/PdfRenderService';
    import { onDestroy } from 'svelte';
    import PreviewPopup from './PreviewPopup.svelte';

    interface Props {
        pageCount?: number;
    }

    let { pageCount = 0 }: Props = $props();
    
    // Lazy Load State
    let loadedState = $state<boolean[]>(new Array(pageCount).fill(false));
    let thumbnailUrls = $state<Record<number, string>>({}); 
    
    // Hover Preview State
    let hoveredPage = $state<{ src: string, y: number, anchorX: number } | null>(null);
    let previewCache = new Map<number, string>(); // Cache for high-res previews
    
    onDestroy(() => {
        Object.values(thumbnailUrls).forEach(url => URL.revokeObjectURL(url));
        previewCache.clear();
    });

    // Derived Labels
    const displayedPageLabels = $derived(
        (pageLabelStore.simulatedLabels && pageLabelStore.simulatedLabels.length > 0)
            ? pageLabelStore.simulatedLabels
            : Array.from({ length: docStore.pageCount }, (_, i) => String(i + 1))
    );
    
    const originalLabels = $derived(docStore.originalPageLabels || []);

    function isLabelModified(index: number, currentLabel: string) {
        if (!originalLabels || originalLabels.length <= index) {
            return currentLabel !== String(index + 1);
        }
        return currentLabel !== originalLabels[index];
    }
    
    $effect(() => {
        if (loadedState.length !== docStore.pageCount) {
             loadedState = new Array(docStore.pageCount).fill(false);
             Object.values(thumbnailUrls).forEach(url => URL.revokeObjectURL(url));
             thumbnailUrls = {};
             previewCache.clear();
        }
    });

    function lazyLoad(node: HTMLElement, index: number) {
        const observer = new IntersectionObserver((entries) => {
            if (entries[0].isIntersecting) {
                loadedState[index] = true;
                observer.disconnect();
                
                if (docStore.currentFilePath && !thumbnailUrls[index]) {
                    pdfRenderService.renderPage(docStore.currentFilePath, index, 'thumbnail')
                        .then(url => {
                            thumbnailUrls[index] = url;
                        })
                        .catch(err => console.error(err));
                }
            }
        }, { rootMargin: "400px" });

        observer.observe(node);
        return { destroy() { observer.disconnect(); } };
    }

    async function handleMouseEnter(e: MouseEvent, index: number) {
        const target = e.currentTarget as HTMLElement;
        const rect = target.getBoundingClientRect();
        
        // Calculate anchor position
        const anchorX = rect.right; 
        const y = rect.top + rect.height / 2;

        let src = previewCache.get(index) || thumbnailUrls[index];
        if (!src) return;

        hoveredPage = { src, y, anchorX };

        if (!previewCache.has(index) && docStore.currentFilePath) {
            try {
                const highResUrl = await pdfRenderService.renderPage(docStore.currentFilePath, index, 'preview');
                previewCache.set(index, highResUrl);
                if (hoveredPage && Math.abs(hoveredPage.y - y) < 1) { 
                     hoveredPage = { ...hoveredPage, src: highResUrl };
                }
            } catch (e) {
                console.error("Failed to load high-res preview", e);
            }
        }
    }

    function handleMouseLeave() {
        hoveredPage = null;
    }
</script>

<div class="h-full bg-white border-l border-[#e5e7eb] font-sans flex flex-col overflow-hidden">
    <!-- Header (Fixed) -->
    <div class="px-5 py-3 border-b border-[#e5e7eb] bg-[#f9fafb] flex justify-between items-center flex-none z-10">
        <div class="text-xs font-semibold text-gray-500 uppercase tracking-wider">Page Label Preview</div>
        <div class="text-xs text-gray-400 font-mono">{pageCount} Pages</div>
    </div>

    <!-- List (Scrollable) -->
    <div class="flex-1 overflow-y-auto p-0">
        {#each loadedState as isLoaded, i}
            {@const label = displayedPageLabels[i] || String(i + 1)}
            {@const modified = isLabelModified(i, label)}
            {@const original = originalLabels[i] || String(i + 1)}

            <div class="page-row" use:lazyLoad={i} class:is-modified={modified}>
                <!-- Column 1: Thumb + Label -->
                <div class="thumb-section flex flex-col items-center gap-2">
                    <!-- svelte-ignore a11y_mouse_events_have_key_events -->
                    <div 
                        class="thumb-col relative group"
                        onmouseenter={(e) => handleMouseEnter(e, i)}
                        onmouseleave={handleMouseLeave}
                        role="img"
                    >
                        {#if isLoaded && thumbnailUrls[i]}
                            <img src={thumbnailUrls[i]} alt="p{i+1}" class="fade-in"/>
                        {:else}
                            <div class="thumb-skeleton"></div>
                        {/if}

                        <!-- 物理页码 (左上角角标) -->
                        <div class="absolute top-0 left-0 bg-black/60 text-white text-[9px] font-mono px-1 py-0.5 backdrop-blur-[1px] opacity-80 group-hover:opacity-100 transition-opacity">
                            #{i + 1}
                        </div>
                    </div>
                    
                    <!-- Label Under Page -->
                    <div class="label-value text-xs font-mono font-bold text-center w-full truncate px-1
                                {modified ? 'text-blue-600' : 'text-gray-700'}" 
                         title="Current Label: {label}">
                        {label}
                    </div>
                </div>

                <!-- Column 2: Meta Info (Original) -->
                <div class="info-col">
                    {#if modified}
                        <div class="meta-row">
                            <span class="label">Orig:</span>
                            <span class="value orig">{original}</span>
                        </div>
                    {/if}
                </div>

                <!-- Column 3: Status Indicator -->
                {#if modified}
                    <div class="action-col">
                        <span class="badge modified">Modified</span>
                    </div>
                {/if}
            </div>
        {:else}
            <div class="flex flex-col items-center justify-center py-20 text-gray-400 gap-2">
                <span class="text-sm">No pages loaded</span>
            </div>
        {/each}
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
      padding: 16px 20px;
      border-bottom: 1px solid #f3f4f6;
      transition: background 0.15s ease;
      align-items: center;
  }
  
  .page-row:hover {
      background: #f9fafb;
  }

  .page-row.is-modified {
      background: #f0f9ff;
  }
  .page-row.is-modified:hover {
      background: #e0f2fe;
  }

  .thumb-section {
      width: 90px;
  }

  .thumb-col {
      display: flex;
      flex-direction: column;
      align-items: center;
      cursor: zoom-in;
      width: 100%;
  }

  .thumb-col img {
      width: 90px;
      height: auto;
      border: 1px solid #e5e7eb;
      background: #fff;
      display: block;
      box-shadow: 0 1px 3px rgba(0,0,0,0.1);
      transition: all 0.2s;
  }
  
  .thumb-col:hover img {
      border-color: #3b82f6;
      box-shadow: 0 4px 12px rgba(0,0,0,0.15);
  }

  .thumb-skeleton {
      width: 90px;
      height: 120px;
      background: #f3f4f6;
  }
  
  .label-value {
      line-height: 1.2;
  }

  .fade-in {
      animation: fadeIn 0.3s ease-in;
  }
  
  @keyframes fadeIn {
      from { opacity: 0; }
      to { opacity: 1; }
  }

  .info-col {
      display: flex;
      flex-direction: column;
      justify-content: center;
      align-items: flex-start;
      gap: 4px;
  }
  
  .meta-row {
      display: flex;
      align-items: center;
      gap: 8px;
  }
  
  .label { 
      font-size: 10px; 
      color: #9ca3af; 
      text-transform: uppercase; 
      font-family: 'JetBrains Mono', 'Consolas', monospace;
      width: 40px; 
      flex-shrink: 0;
  }
  
  .value { 
      color: #374151;
      font-family: 'JetBrains Mono', 'Consolas', monospace;
  }
  
  .value.orig {
      color: #9ca3af;
      text-decoration: line-through;
      font-size: 11px;
  }

  .action-col {
      min-width: 80px;
      display: flex;
      justify-content: flex-end;
  }

  .badge {
      display: inline-flex;
      align-items: center;
      padding: 3px 8px;
      border-radius: 4px;
      font-size: 10px;
      font-weight: 600;
      text-transform: uppercase;
      letter-spacing: 0.05em;
  }
  
  .badge.modified {
      background: #eff6ff; 
      color: #2563eb;
      border: 1px solid #dbeafe;
  }
</style>
