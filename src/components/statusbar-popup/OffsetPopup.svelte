<script lang="ts">
  import ArrowPopup from '../controls/ArrowPopup.svelte';
  import StyledInput from '../controls/StyledInput.svelte';
  import PreviewPopup from '../PreviewPopup.svelte'; // Import PreviewPopup
  import { docStore } from '@/stores/docStore';
  import { pdfRenderService } from '@/lib/services/PdfRenderService';
  import { onMount } from 'svelte';

  interface Props {
    offset?: number;
    onchange?: () => void;
    triggerEl: HTMLElement | undefined;
  }

  let { 
    offset = $bindable(0),
    onchange,
    triggerEl
  }: Props = $props();

  let pages = $state<{index: number, label: string, url: string | null}[]>([]);
  let isLoading = $state(false);
  let listContainer: HTMLDivElement;
  
  // Hover Preview State
  let hoveredPage = $state<{ src: string, y: number, anchorX: number } | null>(null);
  let previewCache = new Map<number, string>(); // Cache for high-res previews

  // How many pages to load initially. Usually offset is found within the first few pages (front matter).
  const INITIAL_LOAD_COUNT = 30; 
  const LOAD_INCREMENT = 20;
  let loadedCount = 0;

  async function loadMorePages() {
      if (!$docStore.currentFilePath || isLoading || loadedCount >= $docStore.pageCount) return;
      
      isLoading = true;
      const start = loadedCount;
      const end = Math.min(loadedCount + LOAD_INCREMENT, $docStore.pageCount);
      
      // Create placeholders
      const newItems = [];
      for (let i = start; i < end; i++) {
          newItems.push({
              index: i,
              label: $docStore.originalPageLabels?.[i] ?? '',
              url: null
          });
      }
      pages = [...pages, ...newItems]; // Append to list
      loadedCount = end;

      // Fetch thumbnails concurrently
      processThumbnails(newItems);
      
      isLoading = false;
  }

  async function processThumbnails(items: typeof pages) {
      const filePath = $docStore.currentFilePath;
      if (!filePath) return;

      for (const item of items) {
          try {
              // 'thumbnail' scale is usually 0.5 or small
              const url = await pdfRenderService.renderPage(filePath, item.index, 'thumbnail');
              const found = pages.find(p => p.index === item.index);
              if (found) found.url = url;
          } catch (e) {
              console.error(`Failed to load thumb for page ${item.index}`, e);
          }
      }
  }
  
  async function handleMouseEnter(e: MouseEvent, page: {index: number, url: string | null}) {
      const target = e.currentTarget as HTMLElement;
      const rect = target.getBoundingClientRect();
      
      // Calculate anchor position (center right of the thumbnail)
      const anchorX = rect.right; 
      const y = rect.top + rect.height / 2;

      // Determine initial source (cache > thumbnail > placeholder)
      let src = previewCache.get(page.index) || page.url || '';
      
      if (!src) return; // Should not happen if thumb loaded, but just in case

      hoveredPage = { src, y, anchorX };

      // If not cached, fetch high-res
      if (!previewCache.has(page.index) && $docStore.currentFilePath) {
          try {
              const highResUrl = await pdfRenderService.renderPage($docStore.currentFilePath, page.index, 'preview');
              previewCache.set(page.index, highResUrl);
              // Update if still hovering the same position (simple check)
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

  function handleScroll(e: UIEvent) {
      const target = e.target as HTMLDivElement;
      if (target.scrollHeight - target.scrollTop - target.clientHeight < 100) {
          loadMorePages();
      }
  }

  function setAsLogicOne(idx: number) {
      offset = idx;
      onchange?.();
  }

  onMount(() => {
      // Initial load
      loadMorePages().then(() => {
          // Auto-scroll to offset after initial load
          if (offset > 0 && listContainer) {
              const targetRow = listContainer.children[offset] as HTMLElement;
              if (targetRow) {
                  targetRow.scrollIntoView({ behavior: 'smooth', block: 'center' });
              }
          }
      });
  });

  const hasLabels = $derived($docStore.originalPageLabels && $docStore.originalPageLabels.length > 0);
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
>
  <div class="popup-content">
      <div class="header-section">
          <label>Page Offset</label>
          <div class="input-row">
            <StyledInput type="number" bind:value={offset} oninput={onchange} numericType="integer" />
          </div>
          <div class="hint">
            {#if hasLabels}
                <span class="badge success">Labels Detected</span>
            {:else}
                <span class="badge warning">No Labels</span>
            {/if}
            <span class="hint-text">Set the physical page that matches "Page 1".</span>
          </div>
      </div>

      <div class="divider"></div>

      <div class="calibration-header">
          <span>Visual Calibration</span>
          <span class="sub-text">Scroll to find start of content</span>
      </div>

      <div class="page-list" onscroll={handleScroll} bind:this={listContainer}>
          {#each pages as page (page.index)}
              <div class="page-row" class:active={page.index === offset}>
                  <!-- svelte-ignore a11y_mouse_events_have_key_events -->
                  <div 
                    class="thumb-col"
                    onmouseenter={(e) => handleMouseEnter(e, page)}
                    onmouseleave={handleMouseLeave}
                    role="img" 
                  >
                      {#if page.url}
                          <img src={page.url} alt="p{page.index}" class="fade-in"/>
                      {:else}
                          <div class="thumb-skeleton"></div>
                      {/if}
                      <div class="phys-idx">#{page.index + 1}</div>
                  </div>
                  
                  <div class="info-col">
                      <div class="logic-row">
                          <span class="label">Logic:</span>
                          <span class="value logic">{page.index - offset + 1}</span>
                      </div>
                      {#if hasLabels}
                        <div class="meta-row">
                            <span class="label">Label@</span>
                            <span class="value label-text">{page.label}</span>
                        </div>
                      {/if}
                  </div>

                  <div class="action-col">
                      {#if page.index === offset}
                          <div class="current-indicator">
                              <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"></polyline></svg>
                              <span>Logic P1</span>
                          </div>
                      {:else}
                          <button class="set-btn" onclick={() => setAsLogicOne(page.index)} title="Set this page as Logic Page 1">
                              Set as Logic P1
                          </button>
                      {/if}
                  </div>
              </div>
          {/each}
          {#if isLoading}
              <div class="loading-row">
                  <span class="spinner"></span> Loading pages...
              </div>
          {/if}
      </div>
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

  label {
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

  .page-list {
      flex: 1;
      overflow-y: auto;
      padding: 0;
      min-height: 250px;
      position: relative;
  }

  .page-row {
      display: grid;
      grid-template-columns: 70px 1fr auto;
      gap: 12px;
      padding: 12px 16px;
      border-bottom: 1px solid #f3f4f6;
      transition: background 0.15s ease;
      align-items: center;
  }
  
  .page-row:hover {
      background: #f9fafb;
  }
  
  .page-row.active {
      background: #eff6ff;
  }

  .thumb-col {
      display: flex;
      flex-direction: column;
      align-items: center;
      cursor: zoom-in; /* Indicate zoom interaction */
  }

  .thumb-col img {
      width: 60px; /* Increased size */
      height: auto;
      border: 1px solid #e5e7eb;
      border-radius: 4px;
      background: #fff;
      display: block;
      box-shadow: 0 1px 2px rgba(0,0,0,0.05);
  }
  
  .thumb-skeleton {
      width: 60px;
      height: 84px; /* Approx A4 aspect ratio */
      background: #f3f4f6;
      border-radius: 4px;
      animation: pulse 1.5s infinite;
  }
  
  @keyframes pulse {
      0% { opacity: 0.6; }
      50% { opacity: 1; }
      100% { opacity: 0.6; }
  }
  
  .fade-in {
      animation: fadeIn 0.3s ease-in;
  }
  
  @keyframes fadeIn {
      from { opacity: 0; }
      to { opacity: 1; }
  }

  .phys-idx {
      font-size: 10px;
      color: #9ca3af;
      margin-top: 4px;
      font-family: monospace;
  }

  .info-col {
      display: flex;
      flex-direction: column;
      justify-content: center;
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
      width: 45px; /* Fixed width for alignment */
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

  .set-btn {
      padding: 6px 12px;
      background: white;
      border: 1px solid #d1d5db;
      border-radius: 6px;
      font-size: 11px;
      font-weight: 500;
      cursor: pointer;
      color: #374151;
      transition: all 0.2s;
      box-shadow: 0 1px 1px rgba(0,0,0,0.05);
      opacity: 0; /* Hidden by default */
      transform: translateX(5px);
  }
  
  .page-row:hover .set-btn {
      opacity: 1;
      transform: translateX(0);
  }
  
  /* Always show button on touch devices or if preferred */
  @media (hover: none) {
      .set-btn { opacity: 1; transform: none; }
  }
  
  .set-btn:hover {
      border-color: #3b82f6;
      color: #2563eb;
      background: #eff6ff;
  }

  .current-indicator {
      display: flex;
      align-items: center;
      gap: 4px;
      font-size: 11px;
      color: #2563eb;
      font-weight: 600;
      background: #dbeafe;
      padding: 4px 8px;
      border-radius: 12px;
  }
  
  .loading-row {
      padding: 16px;
      text-align: center;
      font-size: 12px;
      color: #9ca3af;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
  }
  
  .spinner {
      width: 12px;
      height: 12px;
      border: 2px solid #e5e7eb;
      border-top-color: #6b7280;
      border-radius: 50%;
      animation: spin 1s linear infinite;
  }
  
  @keyframes spin {
      to { transform: rotate(360deg); }
  }
</style>