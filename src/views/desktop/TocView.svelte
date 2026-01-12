<script lang="ts">
  import SplitPane from '../../components/SplitPane.svelte';
  import Preview from '../../components/Preview.svelte';
  import TocEditor from '../../components/editor/TocEditor.svelte';
  import StatusBar from '../../components/StatusBar.svelte';
  import PageFrame from '../../components/headerfooter/PageFrame.svelte';
  import '../../assets/global.css';
  import { onMount } from 'svelte';

  import { docStore } from '@/stores/docStore.svelte.ts';
  import { tocStore } from '@/stores/tocStore.svelte.js';
  import { appStore, FnTab } from '@/stores/appStore.svelte.ts';
  
  import { useTocActions } from '../shared/toc.svelte.ts';

  const { loadOutline, handleContentChange, triggerPreview, handleGenerate } = useTocActions();

  let previewComponent: Preview;
  
  let showHeader = $state(false);
  let showFooter = $state(false);
  
  let activeTab = $derived(appStore.activeTab);

  // Refresh preview when tab becomes active to restore CSS
  $effect(() => {
      if (activeTab === FnTab.tocGenerator) {
          setTimeout(() => triggerPreview(), 0);
      }
  });

  // Auto-load TOC when file changes
  $effect(() => {
      const path = docStore.currentFilePath;
      
      // Only load if path has changed (new file opened)
      // If path matches tocStore, we are just remounting (switching tabs), 
      // so we rely on onMount to restore state from store.
      if (tocStore.filePath !== path) {
          if (path) {
              loadOutline();
          } else {
              tocStore.setFile(null);
          }
      }
  });
  
  // React to config changes
  $effect(() => {
    // Create dependencies on store properties to trigger updates
    const _ = { 
        h: JSON.stringify(tocStore.headerConfig), 
        f: JSON.stringify(tocStore.footerConfig),
        pl: JSON.stringify(tocStore.pageLayout),
        hfl: JSON.stringify(tocStore.hfLayout),
        t: tocStore.title,
        o: tocStore.offset,
        i: JSON.stringify(tocStore.insertionConfig), // Watch insertion object
        s: JSON.stringify(tocStore.pageLabel)
    };

    // Use a small timeout to let the store update settle before triggering preview
    // Note: The actual debouncing is handled inside triggerPreview in logic.ts
    triggerPreview();
  });
  
  // onMount: just trigger preview if we have content (e.g. switching back to tab)
  onMount(() => {
      // Check if store matches current file
      if (tocStore.filePath !== docStore.currentFilePath) {
          // Store is stale, do not render previewData
          return;
      }

      if (tocStore.previewData) {
          // Restore scroll position after render (timeout to ensure DOM updated)
          setTimeout(() => {
              previewComponent?.restoreScroll(tocStore.scrollTop);
          }, 0);
      } else if (tocStore.content) {
          triggerPreview();
      }
  });

</script>

<main>
  <div class="content-area">
      <SplitPane initialSplit={40}>
        {#snippet left()}
        <div class="left-panel">
          <PageFrame
            bind:headerConfig={tocStore.headerConfig}
            bind:footerConfig={tocStore.footerConfig}
            bind:showHeader={showHeader}
            bind:showFooter={showFooter}
            onHeaderChange={triggerPreview}
            onFooterChange={triggerPreview}
          >
            <div class="header">
              <input type="text" bind:value={tocStore.title} oninput={triggerPreview} placeholder="Title" class="title-input"/>
            </div>
            
            <div class="editor-wrapper">
              <TocEditor 
                  bind:value={tocStore.content} 
                  onchange={handleContentChange} 
                  placeholder="Enter TOC here..."
                  offset={tocStore.offset}
                  totalPage={docStore.pageCount}
                  pageLabels={docStore.originalPageLabels}
                  insertPos={parseInt(String(tocStore.insertionConfig.pos), 10) || 0}
              />
            </div>
          </PageFrame>
        </div>
        {/snippet}
        
        {#snippet right()}
        <div class="h-full">
          <Preview 
            bind:this={previewComponent} 
            mode="paged"
            pagedPayload={tocStore.previewData}
            isActive={activeTab === FnTab.tocGenerator}
            onrefresh={triggerPreview} 
            onScroll={(top) => tocStore.scrollTop = top}
          />
        </div>
        {/snippet}
      </SplitPane>
  </div>

  <StatusBar 
      bind:offset={tocStore.offset} 
      bind:insertion={tocStore.insertionConfig}
      bind:pageLabel={tocStore.pageLabel}
      bind:pageLayout={tocStore.pageLayout}
      bind:hfLayout={tocStore.hfLayout}
      onGenerate={handleGenerate} 
      onParamChange={triggerPreview} 
  />
</main>

<style>
  main {
    height: 100%;
    width: 100%;
    overflow: hidden;
    display: flex;
    flex-direction: column;
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
  }
  
  .content-area {
      flex: 1;
      overflow: hidden;
      position: relative;
  }
  
  .h-full { height: 100%; }
  
  .left-panel {
    height: 100%;
    display: flex;
    flex-direction: column;
    border-right: 1px solid #ddd;
    background: #fff;
  }

  .header {
    padding: 10px;
    position: relative;
  }
  
  .header::after {
    content: '';
    position: absolute;
    bottom: 0;
    left: 32px;
    width: calc(100% - 64px);
    height: 1px;
    background: #eee;
  }
/* ... middle content ... */
  .title-input {
    width: 100%;
    padding: 16px 22px;
    font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
    font-size: 20px;
    font-weight: bold;
    border: none;
    background: transparent;
    box-sizing: border-box;
    text-align: center;
    color: #333;
    transition: background-color 0.2s;
  }
  
  .title-input:focus {
    outline: none;
  }

  .title-input:hover {
    background-color: rgba(0, 0, 0, 0.02);
  }

  .editor-wrapper {
    flex: 1;
    overflow: hidden;
    padding: 10px;
  }
</style>