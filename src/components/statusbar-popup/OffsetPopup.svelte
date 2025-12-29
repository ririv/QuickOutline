<script lang="ts">
  import ArrowPopup from '../controls/ArrowPopup.svelte';
  import StyledInput from '../controls/StyledInput.svelte';
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
      // We don't await this blocking the UI, but we process in chunks to be nice
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
              // Update state
              const found = pages.find(p => p.index === item.index);
              if (found) found.url = url;
          } catch (e) {
              console.error(`Failed to load thumb for page ${item.index}`, e);
          }
      }
  }

  function handleScroll(e: UIEvent) {
      const target = e.target as HTMLDivElement;
      if (target.scrollHeight - target.scrollTop - target.clientHeight < 100) {
          loadMorePages();
      }
  }

  function setAsLogicOne(idx: number) {
      // Logic Page 1 = Index + 1 - Offset
      // If we want this page (Index) to be Logic 1:
      // 1 = Index + 1 - Offset  =>  Offset = Index
      offset = idx;
      onchange?.();
  }

  onMount(() => {
      // Initial load
      loadMorePages();
  });

  const hasLabels = $derived($docStore.originalPageLabels && $docStore.originalPageLabels.length > 0);
</script>

<ArrowPopup 
  placement="top" 
  minWidth="340px" 
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
                <span class="badge success">Page Labels Detected</span>
            {:else}
                <span class="badge warning">No Page Labels</span>
            {/if}
            <div style="margin-top: 4px;">Adjust so logic page 1 matches content.</div>
          </div>
      </div>

      <div class="divider"></div>

      <div class="calibration-header">
          <span>Calibration Helper</span>
          <span class="sub-text">Find the actual 1st page</span>
      </div>

      <div class="page-list" onscroll={handleScroll} bind:this={listContainer}>
          {#each pages as page (page.index)}
              <div class="page-row" class:active={page.index === offset}>
                  <div class="thumb-col">
                      {#if page.url}
                          <img src={page.url} alt="p{page.index}"/>
                      {:else}
                          <div class="thumb-placeholder">...</div>
                      {/if}
                      <div class="phys-idx">#{page.index + 1}</div>
                  </div>
                  
                  <div class="info-col">
                      {#if hasLabels}
                        <div class="label-info">Label: <strong>{page.label}</strong></div>
                      {/if}
                      <div class="logic-info">Logic: <strong>{page.index - offset + 1}</strong></div>
                  </div>

                  <div class="action-col">
                      {#if page.index === offset}
                          <span class="current-badge">Start Here</span>
                      {:else}
                          <button class="set-btn" onclick={() => setAsLogicOne(page.index)} title="Set this as Logic Page 1">
                              Set #1
                          </button>
                      {/if}
                  </div>
              </div>
          {/each}
          {#if isLoading}
              <div class="loading-row">Loading...</div>
          {/if}
      </div>
  </div>
</ArrowPopup>

<style>
  .popup-content {
      display: flex;
      flex-direction: column;
      max-height: 450px; /* Limit total height */
  }

  .header-section {
      padding: 12px 15px;
      flex-shrink: 0;
  }

  label {
      display: block;
      font-weight: 600;
      margin-bottom: 6px;
      color: #333;
      font-size: 13px;
  }
  
  .hint {
      margin-top: 8px;
      font-size: 11px;
      color: #666;
      line-height: 1.4;
  }

  .badge {
      display: inline-block;
      padding: 2px 6px;
      border-radius: 4px;
      font-size: 10px;
      font-weight: bold;
      text-transform: uppercase;
  }
  .badge.success { background: #e6fffa; color: #047857; border: 1px solid #a7f3d0; }
  .badge.warning { background: #fffbeb; color: #92400e; border: 1px solid #fde68a; }

  .divider {
      height: 1px;
      background: #eee;
      width: 100%;
      flex-shrink: 0;
  }

  .calibration-header {
      padding: 8px 15px;
      background: #f9fafb;
      font-size: 12px;
      font-weight: 600;
      color: #555;
      border-bottom: 1px solid #eee;
      display: flex;
      justify-content: space-between;
      align-items: center;
      flex-shrink: 0;
  }
  
  .sub-text { font-weight: normal; color: #999; font-size: 11px; }

  .page-list {
      flex: 1;
      overflow-y: auto;
      padding: 0;
      min-height: 200px;
  }

  .page-row {
      display: flex;
      align-items: center;
      padding: 8px 15px;
      border-bottom: 1px solid #f0f0f0;
      transition: background 0.1s;
  }
  
  .page-row:hover {
      background: #f8f9fa;
  }
  
  .page-row.active {
      background: #e6f7ff; /* Highlight the offset page */
      border-left: 3px solid #1890ff;
  }

  .thumb-col {
      width: 50px;
      display: flex;
      flex-direction: column;
      align-items: center;
      margin-right: 12px;
  }

  .thumb-col img {
      width: 40px;
      height: auto;
      border: 1px solid #ddd;
      border-radius: 2px;
      background: #fff;
      display: block;
  }
  
  .thumb-placeholder {
      width: 40px;
      height: 56px;
      background: #eee;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 10px;
      color: #999;
  }

  .phys-idx {
      font-size: 10px;
      color: #999;
      margin-top: 2px;
  }

  .info-col {
      flex: 1;
      font-size: 12px;
      color: #444;
      display: flex;
      flex-direction: column;
      justify-content: center;
      gap: 2px;
  }
  
  .label-info { color: #666; font-size: 11px; }
  .logic-info { color: #333; }
  strong { font-weight: 600; color: #000; }

  .action-col {
      margin-left: 10px;
  }

  .set-btn {
      padding: 4px 8px;
      background: #fff;
      border: 1px solid #d9d9d9;
      border-radius: 4px;
      font-size: 11px;
      cursor: pointer;
      color: #666;
      transition: all 0.2s;
  }
  
  .set-btn:hover {
      border-color: #40a9ff;
      color: #40a9ff;
  }

  .current-badge {
      font-size: 11px;
      color: #1890ff;
      font-weight: bold;
  }
  
  .loading-row {
      padding: 10px;
      text-align: center;
      font-size: 11px;
      color: #999;
  }
</style>