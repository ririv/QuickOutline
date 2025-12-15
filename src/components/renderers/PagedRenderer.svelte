<script lang="ts">
    import { onMount, onDestroy } from 'svelte';
    import { PagedEngine } from '../../lib/preview-engine/paged-engine';
  
    export let payload: { html: string, styles: string, header: any, footer: any };
    // 父组件通知渲染完成（例如用于恢复滚动条）
    export let onRenderComplete: ((duration: number) => void) | undefined = undefined;
    export let isActive: boolean = true; // Control style visibility based on tab activation
  
    let container: HTMLDivElement;
    let engine: PagedEngine;

    onMount(() => {
        engine = new PagedEngine();
        // Initial visibility
        engine.setVisible(isActive);
    });
  
    // 监听 payload 变化并触发渲染
    // 使用 $effect 或 reactive statement
    $: if (container && payload && engine) {
        engine.update(payload, container, onRenderComplete);
    }

    // Reactively update visibility when isActive changes
    $: if (engine) {
        engine.setVisible(isActive);
    }
  
    onDestroy(() => {
        engine?.destroy();
    });
  </script>
  
  <div class="paged-renderer-root" bind:this={container}>
      <!-- Paged Engine will inject buffers here -->
  </div>
  
  <style>
      .paged-renderer-root {
          width: 100%;
          /* 允许 Paged.js 的页面自然堆叠 */
          display: flex;
          flex-direction: column;
          /* 配合父组件的对齐方式 */
          align-items: flex-start; 
          /* 这里的 gap 可能由父组件控制，或者在这里控制 */
          gap: 20px;
      }
  
      /* --- PRINT STYLES (Scoped to this component context) --- */
      /* 注意：由于 Paged.js 动态生成 DOM，我们需要 :global 来命中那些元素 */
      
      @media print {
          /* 隐藏父组件的所有 UI，只保留这个组件的内容 */
          :global(body > *:not(.preview-root)),
          :global(#toolbar-container),
          :global(.refresh-fab) {
              display: none !important;
          }
  
          /* 重置容器样式以适应打印 */
          :global(body), :global(html), :global(.preview-root), :global(#viewport), .paged-renderer-root {
              width: 100%;
              height: auto !important;
              margin: 0;
              padding: 0;
              background: white;
              overflow: visible !important;
              display: block !important; /* 取消 Flex */
          }
          
          :global(#viewport) {
               /* 打印时取消缩放 padding */
               padding: 0 !important;
               text-align: left !important;
          }
  
          /* 确保 Paged.js 生成的页面正确分页 */
          :global(.pagedjs_page) {
              margin: 0 !important;
              box-shadow: none !important;
              break-after: page;
              page-break-after: always;
              width: 100% !important; /* 确保占满纸张 */
          }
  
          /* 显示当前的缓冲区 */
          :global(.paged-buffer) {
              display: block !important;
              opacity: 1 !important;
              position: static !important;
          }
          
          /* 隐藏可能存在的非活动缓冲区（虽然 paged-engine 会隐藏它们，但加一层保险） */
          /* 这一步比较难，因为我们不知道哪个是活动的。依赖 paged-engine 的 display:none 即可 */
      }
  </style>
