<script lang="ts">
  import ArrowPopup from '../../controls/ArrowPopup.svelte';
  import StyledInput from '../../controls/StyledInput.svelte';
  import PreviewPopup from '../../PreviewPopup.svelte';
  import VirtualList from '../../common/VirtualList.svelte';
  import LazyPdfImage from '../../common/LazyPdfImage.svelte';
  import { docStore } from '@/stores/docStore.svelte.js';
  import { pdfRenderService } from '@/lib/services/PdfRenderService.ts';
  import { onMount, untrack, onDestroy } from 'svelte';

  interface Props {
    offset?: number;
    onchange?: () => void;
    triggerEl: HTMLElement | undefined;
    [key: string]: any; 
  }

  let { 
    offset = $bindable(0),
    onchange,
    triggerEl,
    ...rest 
  }: Props = $props();

  const ITEM_HEIGHT = 110; 
  let virtualList: any; 

  let localOffset = $state('');

  const thumbnailCache = new Map<number, string>();
  const previewCache = new Map<number, string>();

  $effect(() => {
      const current = offset; 
      untrack(() => {
          if (localOffset === '-') return;
          const strVal = current === 0 ? '' : String(current);
          if (parseInt(localOffset || '0') !== current) {
              localOffset = strVal;
          }
      });
  });

  function handleInput(e: Event) {
      const target = e.target as HTMLInputElement;
      localOffset = target.value;
      const val = parseInt(localOffset, 10);
      if (!isNaN(val)) {
          offset = val;
          onchange?.();
      } else if (localOffset === '') {
          offset = 0;
          onchange?.();
      }
  }
  
  let hoveredPage = $state<{ src: string, y: number, anchorX: number } | null>(null);

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

  function setAsLogicOne(idx: number) {
      offset = idx;
      onchange?.();
  }

  onMount(() => {
      if (offset > 0 && virtualList) {
          setTimeout(() => virtualList.scrollTo(offset, 'smooth'), 100);
      }
  });

  onDestroy(() => {
      thumbnailCache.forEach(url => URL.revokeObjectURL(url));
      previewCache.forEach(url => URL.revokeObjectURL(url));
  });

  const hasLabels = $derived(docStore.originalPageLabels && docStore.originalPageLabels.length > 0);
</script>

{#if hoveredPage}
    <PreviewPopup 
        src={hoveredPage.src} 
        y={hoveredPage.y} 
        anchorX={hoveredPage.anchorX} 
        placement="right"
    />
{/if}

<ArrowPopup 
  placement="top" 
  minWidth="380px" 
  padding="0"
  {triggerEl}
  {...rest}
>
  <div class="popup-content">
      <div class="header-section">
          <div class="popup-label">Page Offset</div>
          <div class="input-row">
            <StyledInput 
                type="text" 
                bind:value={localOffset} 
                oninput={handleInput} 
                numericType="integer" 
            />
          </div>
          <div class="hint">
            {#if hasLabels}
                <span class="badge success">Labels Detected</span>
            {:else}
                <span class="badge warning">No Labels</span>
            {/if}
            <span class="hint-text">Set the logical page that matches "Page 1".</span>
          </div>
      </div>

      <div class="divider"></div>

      <div class="calibration-header">
          <span>Visual Calibration</span>
          <span class="sub-text">Scroll to find start of content</span>
      </div>

      <VirtualList 
        totalCount={docStore.pageCount} 
        itemHeight={ITEM_HEIGHT}
        bind:this={virtualList}
        className="page-list-container"
      >
          {#snippet children(i)}
              {@const label = docStore.originalPageLabels?.[i] ?? ''}
              <div class="page-row" class:active={i === offset} style="height: {ITEM_HEIGHT}px;">
                  <!-- svelte-ignore a11y_mouse_events_have_key_events -->
                  <div 
                    class="thumb-col"
                    onmouseenter={(e) => handleMouseEnter(e, i)}
                    onmouseleave={handleMouseLeave}
                    role="img" 
                  >
                      <LazyPdfImage
                        index={i}
                        scaleOrType="thumbnail"
                        className="w-full h-full"
                        imgClass="fade-in rounded-[4px] border border-gray-200"
                        cache={thumbnailCache}
                      />
                  </div>
                  
                  <div class="info-col">
                      <div class="phys-idx">#{i + 1}</div>
                      <div class="logic-row">
                          <span class="label">Logic:</span>
                          <span class="value logic">{i - offset + 1}</span>
                      </div>
                      {#if hasLabels}
                        <div class="meta-row">
                            <span class="label">Label@</span>
                            <span class="value label-text">{label}</span>
                        </div>
                      {/if}
                  </div>

                  <div class="action-col">
                      <button 
                          class="action-btn" 
                          class:is-active={i === offset}
                          onclick={() => i !== offset && setAsLogicOne(i)}
                          title={i === offset ? "Current Logic Page 1" : "Set this page as Logic Page 1"}
                      >
                          {#if i === offset}
                              <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"></polyline></svg>
                              <span>Logic P1</span>
                          {:else}
                              <span>Set as Logic P1</span>
                          {/if}
                      </button>
                  </div>
              </div>
          {/snippet}
      </VirtualList>
  </div>
</ArrowPopup>

<style>
  .popup-content {
      display: flex;
      flex-direction: column;
      max-height: 500px;
      background: #fff;
  }

  .header-section {
      padding: 16px;
      flex-shrink: 0;
      background: #fff;
      z-index: 2;
  }

  .popup-label {
      display: block;
      font-weight: 600;
      margin-bottom: 8px;
      color: #1f2937;
      font-size: 13px;
  }
  
  .hint {
      margin-top: 10px;
      font-size: 12px;
      display: flex;
      align-items: center;
      gap: 8px;
      flex-wrap: wrap;
  }
  
  .hint-text { color: #6b7280; font-size: 11px; }

  .badge {
      display: inline-flex;
      align-items: center;
      padding: 2px 8px;
      border-radius: 12px;
      font-size: 10px;
      font-weight: 600;
      text-transform: uppercase;
      letter-spacing: 0.02em;
  }
  .badge.success { background: #d1fae5; color: #065f46; }
  .badge.warning { background: #fef3c7; color: #92400e; }

  .divider {
      height: 1px;
      background: #e5e7eb;
      width: 100%;
      flex-shrink: 0;
  }

  .calibration-header {
      padding: 10px 16px;
      background: #f9fafb;
      font-size: 12px;
      font-weight: 600;
      color: #374151;
      border-bottom: 1px solid #e5e7eb;
      display: flex;
      justify-content: space-between;
      align-items: center;
      flex-shrink: 0;
  }
  
  .sub-text { font-weight: normal; color: #9ca3af; font-size: 11px; }

  :global(.page-list-container) {
      height: 350px !important; 
      min-height: 250px;
  }

  .page-row {
      display: grid;
      grid-template-columns: 70px 1fr auto;
      gap: 12px;
      padding: 12px 16px;
      border-bottom: 1px solid #f3f4f6;
      transition: background 0.15s ease;
      align-items: center;
      box-sizing: border-box;
  }
  
  .page-row:hover { background: #f9fafb; }
  .page-row.active { background: #eff6ff; }

  .thumb-col {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center; /* Center content */
      cursor: zoom-in;
      position: relative;
      width: 60px;
      height: 85px; /* Container Size */
  }

  .phys-idx {
      font-family: 'JetBrains Mono', 'Consolas', monospace;
      font-size: 10px;
      font-weight: 700;
      color: #9ca3af;
      background: #f3f4f6;
      padding: 1px 6px;
      border-radius: 4px;
      display: inline-block;
      margin-bottom: 6px;
      line-height: 1.2;
  }

  .info-col {
      display: flex;
      flex-direction: column;
      justify-content: center;
      align-items: flex-start;
      gap: 4px;
  }
  
  .logic-row, .meta-row {
      display: flex;
      align-items: center;
      gap: 8px;
  }
  
  .label { 
      font-size: 10px; 
      color: #9ca3af; 
      text-transform: uppercase; 
      font-family: 'Consolas', monospace;
      width: 45px; 
      display: inline-block;
      flex-shrink: 0;
  }
  
  .value { 
      font-size: 12px; 
      font-weight: 500; 
      color: #374151;
  }
  
  .page-row.active .value.logic {
      color: #2563eb;
      font-weight: 600;
  }

  .action-col {
      min-width: 80px;
      display: flex;
      justify-content: flex-end;
  }

  .action-btn {
      display: flex;
      align-items: center;
      gap: 4px;
      padding: 6px 10px;
      border-radius: 6px;
      font-size: 11px;
      font-weight: 500;
      cursor: pointer;
      transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
      
      background: white;
      border: 1px solid #d1d5db;
      color: #374151;
      opacity: 0;
      transform: translateX(5px);
  }
  
  .page-row:hover .action-btn {
      opacity: 1;
      transform: translateX(0);
  }

  .action-btn:hover:not(.is-active) {
      border-color: #3b82f6;
      color: #2563eb;
      background: #eff6ff;
  }

  .action-btn.is-active {
      opacity: 1; 
      transform: none;
      background: #dbeafe;
      color: #2563eb;
      border-color: transparent;
      font-weight: 600;
      cursor: default;
  }
</style>