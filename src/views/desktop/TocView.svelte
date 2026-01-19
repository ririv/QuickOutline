<script lang="ts">
  import SplitPane from '../../components/SplitPane.svelte';
  import Preview from '../../components/Preview.svelte';
  import TocEditor from '../../components/editor/TocEditor.svelte';
  import StatusBar from '../../components/StatusBar.svelte';
  import PageFrame from '../../components/headerfooter/PageFrame.svelte';
  import GuideWindow from '../../components/common/GuideWindow.svelte';
  import MarkdownViewer from '../../components/common/MarkdownViewer.svelte';
  import Icon from '../../components/Icon.svelte';
  import { onMount, onDestroy } from 'svelte';

  import { docStore } from '@/stores/docStore.svelte.ts';
  import { tocStore } from '@/stores/tocStore.svelte.js';
  import { appStore, FnTab } from '@/stores/appStore.svelte.ts';
  
  import { useTocActions } from '../shared/toc.svelte.ts';
  import { usePdfPageSizeDetection } from '@/lib/pdf-processing/usePdfPageSizeDetection.svelte';

  const { loadOutline, handleContentChange, triggerPreview, handleGenerate, handleRenderStats, clearDebounce } = useTocActions();

  let previewComponent: Preview;
  
  let showHeader = $state(false);
  let showFooter = $state(false);
  let showGuide = $state(false);
  let rawGuideContent = $state('');
  let copied = $state(false);
  
  let activeTab = $derived(appStore.activeTab);
  
  const layoutDetection = usePdfPageSizeDetection(() => tocStore.insertionConfig.pos);

  function handleGuide() {
      showGuide = true;
  }

  async function handleCopy() {
      if (!rawGuideContent) return;
      try {
          await navigator.clipboard.writeText(rawGuideContent);
          copied = true;
          setTimeout(() => copied = false, 2000);
      } catch (err) {
          console.error('Failed to copy:', err);
      }
  }

  // Auto-load TOC when file changes
  $effect(() => {
      const path = docStore.currentFilePath;
      
      // Only load if path has changed (new file opened)
      if (tocStore.filePath !== path) {
          if (path) {
              loadOutline();
          } else {
              tocStore.setFile(null);
          }
      }
  });
  
  // onMount: just trigger preview if we have content (e.g. switching back to tab)
  onMount(() => {
      // Check if store matches current file
      if (tocStore.filePath !== docStore.currentFilePath) {
          // Store is stale, do not render previewData
          return;
      }

      if (!tocStore.previewData && tocStore.content) {
          triggerPreview();
      }
  });

  onDestroy(() => {
      clearDebounce();
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
            bind:hfLayout={tocStore.hfLayout}
            bind:showHeader={showHeader}
            bind:showFooter={showFooter}
            defaultFooterContent={{ center: '{p r}' }}
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
            onRenderStats={handleRenderStats}
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
      onGuide={handleGuide}
      {layoutDetection}
  />

  <GuideWindow bind:visible={showGuide} onClose={() => showGuide = false} title="TOC Syntax Guide">
      {#snippet actions()}
          <button class="action-btn" onclick={handleCopy} title="Copy raw markdown">
              {#if copied}
                  <Icon name="check" width="14" height="14" style="color: #67c23a;" />
              {:else}
                  <Icon name="copy" width="14" height="14" />
              {/if}
          </button>
      {/snippet}
      <MarkdownViewer src="/docs/TOC_User_Guide.md" bind:rawContent={rawGuideContent} />
  </GuideWindow>
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
  .action-btn {
      background: transparent;
      border: none;
      cursor: pointer;
      color: #909399;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 4px;
      border-radius: 4px;
      transition: all 0.2s;
  }

  .action-btn:hover {
      background-color: #e4e7ed;
      color: #409eff;
  }
</style>