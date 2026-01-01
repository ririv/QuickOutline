<script lang="ts">
  import { generateMockBlocks, mockMeasure, VirtualPager, type Page } from '@/lib/experimental/virtual-pager.ts';

  // Demo State
  let demoPages = $state<Page[]>([]);
  let demoCurrentPageIndex = $state(0);
  let demoStats = $state({ totalTime: 0, totalPages: 0 });
  let isRunning = $state(false);

  function runVirtualPagerDemo() {
      isRunning = true;
      // Use setTimeout to allow UI to update (show loading state)
      setTimeout(() => {
          const blockCount = 10000;
          const blocks = generateMockBlocks(blockCount);
          
          const pager = new VirtualPager({
              pageHeight: 800, 
              marginTop: 40,
              marginBottom: 40,
              lineHeight: 20
          });

          const start = performance.now();
          const pages = pager.paginate(blocks, mockMeasure);
          const end = performance.now();

          demoPages = pages;
          demoCurrentPageIndex = 0;
          demoStats = {
              totalTime: end - start,
              totalPages: pages.length
          };
          isRunning = false;
      }, 50);
  }
</script>

<div class="experimental-container">
    <div class="control-panel">
        <h2>Virtual Pager Demo</h2>
        <p>This demo generates 10,000 text blocks and paginates them purely in memory (no DOM reflow).</p>
        
        <button class="run-btn" onclick={runVirtualPagerDemo} disabled={isRunning}>
            {isRunning ? 'Processing...' : 'Run Pagination'}
        </button>

        {#if demoStats.totalPages > 0}
            <div class="stats">
                <div class="stat-item">
                    <span class="label">Time:</span>
                    <span class="value">{demoStats.totalTime.toFixed(2)}ms</span>
                </div>
                <div class="stat-item">
                    <span class="label">Pages:</span>
                    <span class="value">{demoStats.totalPages}</span>
                </div>
                <div class="stat-item">
                    <span class="label">Avg/Block:</span>
                    <span class="value">{(demoStats.totalTime / 10000).toFixed(4)}ms</span>
                </div>
            </div>
        {/if}
    </div>

    <div class="preview-panel">
        {#if demoPages.length > 0}
            <div class="page-container">
                <div class="virtual-page">
                    <div class="page-content">
                        {#each demoPages[demoCurrentPageIndex].blocks as block}
                           <div class="virtual-block {block.type}">
                               {block.content}
                           </div>
                        {/each}
                    </div>
                    <div class="page-footer">Page {demoPages[demoCurrentPageIndex].index + 1}</div>
                </div>
            </div>
            
            <div class="pagination-controls">
                <button onclick={() => demoCurrentPageIndex = Math.max(0, demoCurrentPageIndex - 1)} disabled={demoCurrentPageIndex === 0}>Previous</button>
                <span>Page {demoCurrentPageIndex + 1} of {demoStats.totalPages}</span>
                <button onclick={() => demoCurrentPageIndex = Math.min(demoPages.length - 1, demoCurrentPageIndex + 1)} disabled={demoCurrentPageIndex === demoPages.length - 1}>Next</button>
            </div>
        {:else}
            <div class="placeholder">
                Run the demo to see pagination results.
            </div>
        {/if}
    </div>
</div>

<style>
    .experimental-container {
        display: flex;
        height: 100%;
        background: #f5f7fa;
    }

    .control-panel {
        width: 300px;
        background: white;
        border-right: 1px solid #e0e0e0;
        padding: 20px;
        display: flex;
        flex-direction: column;
        gap: 20px;
    }

    .run-btn {
        padding: 10px 20px;
        background: #1677ff;
        color: white;
        border: none;
        border-radius: 6px;
        cursor: pointer;
        font-weight: 500;
        transition: background 0.2s;
    }

    .run-btn:hover { background: #4096ff; }
    .run-btn:disabled { background: #ccc; cursor: not-allowed; }

    .stats {
        background: #f8f9fa;
        padding: 15px;
        border-radius: 6px;
        border: 1px solid #eee;
    }

    .stat-item {
        display: flex;
        justify-content: space-between;
        margin-bottom: 8px;
    }
    .stat-item:last-child { margin-bottom: 0; }
    .label { color: #666; font-size: 13px; }
    .value { font-weight: 600; color: #333; }

    .preview-panel {
        flex: 1;
        display: flex;
        flex-direction: column;
        align-items: center;
        padding: 20px;
        overflow: hidden;
    }

    .page-container {
        flex: 1;
        overflow: auto;
        width: 100%;
        display: flex;
        justify-content: center;
        padding-bottom: 20px;
    }

    .virtual-page {
        width: 500px; /* Simulated A4 width scaled */
        height: 800px; /* Simulated A4 height scaled */
        background: white;
        box-shadow: 0 4px 15px rgba(0,0,0,0.1); /* 和app的主样式有一点区别，微调尝试效果 */
        padding: 40px; /* Margin */
        box-sizing: border-box;
        display: flex;
        flex-direction: column;
        position: relative;
    }

    .page-content {
        flex: 1;
        overflow: hidden; /* Content shouldn't overflow in theory if pager is correct */
    }

    .page-footer {
        height: 20px;
        border-top: 1px solid #eee;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 12px;
        color: #999;
        margin-top: 10px;
    }

    .virtual-block { margin-bottom: 10px; }
    .virtual-block.h1 { font-size: 20px; font-weight: bold; margin-bottom: 12px; color: #333; }
    .virtual-block.h2 { font-size: 16px; font-weight: bold; margin-bottom: 8px; color: #444; }
    .virtual-block.p { font-size: 12px; line-height: 20px; text-align: justify; color: #555; }

    .pagination-controls {
        height: 50px;
        display: flex;
        align-items: center;
        gap: 20px;
        background: white;
        padding: 0 20px;
        border-radius: 30px;
        box-shadow: 0 2px 10px rgba(0,0,0,0.1);
        margin-top: auto;
    }

    .pagination-controls button {
        background: transparent;
        border: none;
        color: #1677ff;
        cursor: pointer;
        font-weight: 500;
    }
    .pagination-controls button:disabled { color: #ccc; cursor: not-allowed; }

    .placeholder {
        color: #999;
        font-size: 16px;
        margin-top: 100px;
    }
</style>