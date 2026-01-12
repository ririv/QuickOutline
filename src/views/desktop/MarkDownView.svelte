<script lang="ts">
  import SplitPane from '../../components/SplitPane.svelte';
  import MdEditor from '../../components/editor/MdEditor.svelte';
  import Preview from '../../components/Preview.svelte';
  import StatusBar from '../../components/StatusBar.svelte';
  import PageFrame from '../../components/headerfooter/PageFrame.svelte';
  import ConfirmDialog from '../../components/ConfirmDialog.svelte';
  import '../../assets/global.css';
  import { onMount, onDestroy } from 'svelte';
  import { markdownStore } from '@/stores/markdownStore.svelte.js';
  import { appStore, FnTab } from '@/stores/appStore.svelte.ts';
  
  import { useMarkdownActions } from '../shared/markdown.svelte.ts';
  
  const { handleRenderStats, debouncedTrigger, clearDebounce, updatePreview, handleGenerate, initEditor, saveContent } = useMarkdownActions();
  
  let editorComponent: MdEditor;
  let previewComponent: Preview;
  
  let activeTab = $derived(appStore.activeTab);

  // Wrapper for triggerPreview to be used in UI callbacks
  async function triggerPreview() {
      if (!editorComponent) return;
      
      let htmlContent = '';
      try {
        htmlContent = await editorComponent.getRenderedMdx();
      } catch (e) {
        console.warn('MDX Render failed, falling back to standard Markdown:', e);
      }

      if (!htmlContent) {
          htmlContent = await editorComponent.getRenderedHtml({
              enableIndentedCodeBlocks: markdownStore.enableIndentedCodeBlocks
          });
      }
      
      const { tableStyle } = editorComponent.getStylesConfig(); 
      await updatePreview(htmlContent, tableStyle);
  }

  function debouncedPreview() {
      debouncedTrigger(triggerPreview);
  }

  onMount(() => {
    initEditor(editorComponent);
  });

  onDestroy(() => {
      clearDebounce();
      saveContent(editorComponent);
  });

</script>

<!-- Mount the Global Confirm Dialog -->
<ConfirmDialog />

<main>
  <div class="content-area">
      <SplitPane initialSplit={50}>
        {#snippet left()}
        <div class="h-full flex-col left-panel">
          <PageFrame
            bind:headerConfig={markdownStore.headerConfig}
            bind:footerConfig={markdownStore.footerConfig}
            bind:showHeader={markdownStore.showHeader}
            bind:showFooter={markdownStore.showFooter}
            onHeaderChange={triggerPreview}
            onFooterChange={triggerPreview}
          >
            <div class="editor-wrapper">
              <MdEditor bind:this={editorComponent} onchange={debouncedPreview} />
            </div>
          </PageFrame>
        </div>
        {/snippet}
        
        {#snippet right()}
        <div class="h-full right-panel">
          <!-- Pass payload via prop -->
                      <Preview 
                      bind:this={previewComponent} 
                      mode="paged" 
                      pagedPayload={markdownStore.currentPagedContent}
                      isActive={activeTab === FnTab.markdown}
                      onrefresh={triggerPreview} 
                      onRenderStats={handleRenderStats}
                    />        </div>
        {/snippet}
      </SplitPane>
  </div>
  
  <StatusBar 
      bind:insertion={markdownStore.insertionConfig}
      bind:pageLabel={markdownStore.pageLabel}
      bind:pageLayout={markdownStore.pageLayout}
      bind:hfLayout={markdownStore.hfLayout}
      showOffset={false}
      onGenerate={handleGenerate} 
      onParamChange={debouncedPreview}
  />
</main>

<style>
  main {
    height: 100%;
    width: 100%;
    overflow: hidden;
    display: flex;
    flex-direction: column;
  }
  
  .content-area {
      flex: 1;
      overflow: hidden;
      position: relative;
  }
  
  .h-full {
      height: 100%;
  }
  
  .flex-col {
      display: flex;
      flex-direction: column;
  }
  
  .editor-wrapper {
      flex: 1; /* Make editor fill remaining space */
      overflow: hidden;
      position: relative;
      padding: 10px; /* Add padding to match TOC/SimpleEditor layout */
      background-color: white; /* Ensure wrapper background is white */
  }

  @media print {
    main, .content-area, .h-full {
        height: auto !important;
        width: auto !important;
        overflow: visible !important;
        display: block !important;
    }
    
    /* Hide the editor pane explicitly if global CSS doesn't catch it */
    .editor-wrapper, 
    .left-panel,
    :global(.status-bar) {
        display: none !important;
    }
    
    .right-panel {
        height: auto !important;
        overflow: visible !important;
    }
  }
</style>