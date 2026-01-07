<script lang="ts">
    import { messageStore } from '@/stores/messageStore.svelte';
    import { docStore } from '@/stores/docStore.svelte';
    import { pageLabelStore } from '@/stores/pageLabelStore.svelte';
    import { pageLabelStyleMap, type PageLabel } from '@/lib/types/page-label.ts';
    import { pdfRenderService } from '@/lib/services/PdfRenderService';
    import { onDestroy } from 'svelte';
    import PreviewPopup from './PreviewPopup.svelte';
    import VirtualList from './common/VirtualList.svelte';
    import LazyPdfImage from './common/LazyPdfImage.svelte';
    import { usePageLabelActions } from '../views/shared/pagelabel.svelte';
    import Icon from "@/components/Icon.svelte";
    import deleteIcon from '@/assets/icons/delete-item.svg?raw';
    import addIcon from '@/assets/icons/plus.svg?raw';

    interface Props {
        pageCount?: number;
    }

    let { pageCount = 0 }: Props = $props();

    const ITEM_HEIGHT = 180; 
    const { deleteRule, editRule } = usePageLabelActions();

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

    function getRuleForPage(index: number): PageLabel | undefined {
        return pageLabelStore.getRuleByPage(index + 1);
    }

    function handleAdd(pageIndex: number) {
        pageLabelStore.resetForm();
        pageLabelStore.startPage = String(pageIndex + 1);
        pageLabelStore.isFormOpen = true;
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

            <div class="page-row group/row" style="height: {ITEM_HEIGHT}px;" class:is-modified={modified}>
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

                <div class="info-col relative h-full flex flex-col justify-center">
                    {#if rule}
                        {@const originalRule = docStore.originalRules?.find(r => r.pageIndex === i + 1)}
                        {@const hasDiff = originalRule && (
                            originalRule.numberingStyle !== rule.numberingStyle ||
                            (originalRule.labelPrefix || '') !== (rule.labelPrefix || '') ||
                            (originalRule.startValue ?? 1) !== (rule.startValue ?? 1)
                        )}

                        <div class="rule-inline-container relative">
                            {#if hasDiff}
                                <div class="absolute bottom-full left-0 mb-1 pl-5 flex items-center whitespace-nowrap opacity-60">
                                    {#if originalRule.labelPrefix}
                                        <span class="text-[#606266] bg-[#f4f4f5] px-1.5 rounded-[3px] text-[11px] border border-[#e9e9eb] font-mono mr-1.5 line-through">{originalRule.labelPrefix}</span>
                                    {/if}
                                    <span class="font-semibold text-gray-700 text-[12px] line-through">{pageLabelStyleMap.getDisplayText(originalRule.numberingStyle)}</span>
                                    {#if (originalRule.startValue ?? 1) !== 1}
                                        <span class="text-gray-600 bg-gray-50 px-1.5 rounded-[3px] text-[11px] border border-gray-200 font-mono ml-1.5 line-through">Start {originalRule.startValue ?? 1}</span>
                                    {/if}
                                </div>
                            {/if}

                            <svg class="w-4 h-4 text-blue-600 flex-shrink-0" fill="currentColor" viewBox="0 0 20 20" xmlns="http://www.w3.org/2000/svg"><path fill-rule="evenodd" d="M3 6a3 3 0 013-3h10a1 1 0 01.8 1.6L14.25 8l2.55 3.4A1 1 0 0116 13H6a1 1 0 00-1 1v3a1 1 0 11-2 0V6z" clip-rule="evenodd"></path></svg>
                            <span class="rule-text">
                                {#if rule.labelPrefix}
                                    <span class="text-[#606266] bg-[#f4f4f5] px-1.5 rounded-[3px] text-[13px] border border-[#e9e9eb] font-mono mr-1.5" title="Prefix">{rule.labelPrefix}</span>
                                {/if}
                                <span class="font-semibold text-gray-700">{pageLabelStyleMap.getDisplayText(rule.numberingStyle)}</span>
                                {#if (rule.startValue ?? 1) !== 1}
                                    <span class="text-[#3b82f6] bg-[#eff6ff] px-1.5 rounded-[3px] text-[13px] border border-[#dbeafe] font-mono ml-1.5" title="Start Number">Start {rule.startValue}</span>
                                {/if}
                            </span>
                        </div>
                    {/if}

                    <!-- Actions Overlay -->
                    <div class="absolute right-2 top-1/2 -translate-y-1/2 flex flex-col gap-2 opacity-0 group-hover/row:opacity-100 transition-opacity z-20">
                        {#if rule}
                            <button class="p-[2px] rounded action-btn text-amber-600 hover:bg-amber-100" onclick={() => editRule(rule)} title="Edit Rule">
                                <svg class="w-[17px] h-[17px]" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"></path></svg>
                            </button>
                            <button class="p-[2px] rounded action-btn text-red-500 hover:bg-el-plain-important-bg-hover" onclick={() => deleteRule(rule.pageIndex)} title="Delete Rule">
                                <Icon data={deleteIcon} class="w-[1rem] h-[1rem] text-red-500" />
                            </button>
                        {:else}
                            <button class="p-[2px] rounded action-btn text-blue-500 hover:bg-blue-100" onclick={() => handleAdd(i)} title="Add Rule Here">
                                <Icon data={addIcon} class="w-[1rem] h-[1rem]" />
                            </button>
                        {/if}
                    </div>
                </div>
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
      grid-template-columns: 100px 1fr;
      gap: 24px;
      padding: 0 20px; 
      border-bottom: 1px solid #f3f4f6;
      align-items: center;
      box-sizing: border-box;
      contain: content;
      transition: background-color 0.15s;
  }
  
  .page-row:hover { background: #f9fafb; }
  
  .page-row.is-modified { 
      background: #f0f9ff; /* Blue tint */
  }
  
  .page-row.is-modified:hover { 
      background: #e0f2fe; 
  }

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
</style>
