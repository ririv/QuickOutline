<script lang="ts">
    import { docStore } from '@/stores/docStore.svelte';
    import { pageLabelStore, type PageLabelRule } from '@/stores/pageLabelStore.svelte';
    import { pdfRenderService } from '@/lib/services/PdfRenderService';
    import { onDestroy } from 'svelte';
    import PreviewPopup from './PreviewPopup.svelte';
    import VirtualList from './common/VirtualList.svelte';
    import LazyPdfImage from './common/LazyPdfImage.svelte';

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

    function getRuleForPage(index: number): PageLabelRule | undefined {
        return pageLabelStore.rules.find(r => r.fromPage === index + 1);
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
            {@const rule = getRuleForPage(i)}

            <div class="page-row" style="height: {ITEM_HEIGHT}px;" class:is-modified={modified}>
                <div class="thumb-section flex flex-col items-center gap-2">
                    <div 
                        class="thumb-col relative group"
                        onmouseenter={(e) => handleMouseEnter(e, i)}
                        onmouseleave={handleMouseLeave}
                        role="img"
                    >
                        <LazyPdfImage
                            index={i}
                            scaleOrType="thumbnail"
                            className="w-full h-full"
                            imgClass="shadow-sm border border-gray-200 bg-white"
                            cache={thumbnailCache}
                        />
                        
                        <div class="absolute top-0 left-0 bg-black/60 text-white text-[9px] font-mono px-1 py-0.5 backdrop-blur-[1px] opacity-80 group-hover:opacity-100 transition-opacity z-30">
                            #{i + 1}
                        </div>
                    </div>
                    
                    <div class="flex items-baseline justify-center w-full px-1">
                        {#if modified}
                            <!-- Phantom element to balance the layout and ensure centering -->
                            <div class="invisible text-[10px] font-mono pr-1 select-none whitespace-nowrap" aria-hidden="true">{original}</div>
                        {/if}
                        
                        <div class="z-10 text-xs font-mono font-bold text-center {modified ? 'text-blue-600' : 'text-gray-700'} whitespace-nowrap" title="Label: {label}">
                            {label}
                        </div>

                        {#if modified}
                            <div class="text-[10px] text-gray-400 line-through font-mono pl-1 whitespace-nowrap" title="Original: {original}">
                                {original}
                            </div>
                        {/if}
                    </div>
                </div>

                <div class="info-col">
                    {#if rule}
                        <div class="rule-inline-container">
                            <svg class="w-4 h-4 text-blue-600 flex-shrink-0" fill="currentColor" viewBox="0 0 20 20" xmlns="http://www.w3.org/2000/svg"><path fill-rule="evenodd" d="M3 6a3 3 0 013-3h10a1 1 0 01.8 1.6L14.25 8l2.55 3.4A1 1 0 0116 13H6a1 1 0 00-1 1v3a1 1 0 11-2 0V6z" clip-rule="evenodd"></path></svg>
                            <span class="rule-text">
                                {#if rule.prefix}
                                    <span class="text-[#606266] bg-[#f4f4f5] px-1.5 rounded-[3px] text-[13px] border border-[#e9e9eb] font-mono mr-1.5" title="Prefix">{rule.prefix}</span>
                                {/if}
                                <span class="font-semibold text-gray-700">{rule.numberingStyleDisplay}</span>
                                {#if rule.start !== 1}
                                    <span class="text-[#3b82f6] bg-[#eff6ff] px-1.5 rounded-[3px] text-[13px] border border-[#dbeafe] font-mono ml-1.5" title="Start Number">Start {rule.start}</span>
                                {/if}
                            </span>
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
  
  .info-col { display: flex; flex-direction: column; justify-content: center; align-items: flex-start; gap: 6px; }
  
  /* Minimalistic Inline Rule Style */
  .rule-inline-container {
      display: flex;
      align-items: center;
      gap: 6px;
      padding: 2px 0;
  }
  .rule-text {
      font-size: 15px;
      display: flex;
      align-items: center;
      white-space: nowrap;
  }

  .action-col { min-width: 80px; display: flex; justify-content: flex-end; }
  .badge { display: inline-flex; align-items: center; padding: 3px 8px; border-radius: 4px; font-size: 10px; font-weight: 600; text-transform: uppercase; letter-spacing: 0.05em; }
  .badge.modified { background: #eff6ff; color: #2563eb; border: 1px solid #dbeafe; }
</style>
