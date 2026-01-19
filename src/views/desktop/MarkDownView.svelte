<script lang="ts">
  import SplitPane from '../../components/SplitPane.svelte';
  import MdEditor from '../../components/editor/MdEditor.svelte';
  import Preview from '../../components/Preview.svelte';
  import StatusBar from '../../components/StatusBar.svelte';
  import PageFrame from '../../components/headerfooter/PageFrame.svelte';
  import { onMount, onDestroy } from 'svelte';
  import { markdownStore, MARKDOWN_DEFAULT_FOOTER_CONTENT } from '@/stores/markdownStore.svelte.js';
  import { appStore, FnTab } from '@/stores/appStore.svelte.ts';
  
  import { useMarkdownActions } from '../shared/markdown.svelte.ts';
  import { usePdfPageSizeDetection } from '@/lib/pdf-processing/usePdfPageSizeDetection.svelte';
  
  const { handleRenderStats, debouncedTrigger, triggerPreview, clearDebounce, updatePreview, handleGenerate, saveContent } = useMarkdownActions();
  
  const layoutDetection = usePdfPageSizeDetection(() => markdownStore.insertionConfig.pos);
  
  let editorComponent: MdEditor;
  let previewComponent: Preview;
  
  let activeTab = $derived(appStore.activeTab);

  function debouncedPreview() {
      debouncedTrigger(triggerPreview);
  }

  // No onMount init needed - MdEditor handles it declaratively
  
  onDestroy(() => {
      clearDebounce();
  });

</script>

<main>
  <div class="content-area">
      <SplitPane initialSplit={50}>
        {#snippet left()}
        <div class="h-full flex-col left-panel">
          <PageFrame
            bind:headerConfig={markdownStore.headerConfig}
            bind:footerConfig={markdownStore.footerConfig}
            bind:hfLayout={markdownStore.hfLayout}
            bind:showHeader={markdownStore.showHeader}
            bind:showFooter={markdownStore.showFooter}
            defaultFooterContent={MARKDOWN_DEFAULT_FOOTER_CONTENT}
            onHeaderChange={triggerPreview}
            onFooterChange={triggerPreview}
          >
            <div class="editor-wrapper">
              <MdEditor 
                  bind:this={editorComponent} 
                  bind:value={markdownStore.content}
                  stylesConfig={{ tableStyle: markdownStore.tableStyle }}
                  onchange={debouncedPreview} 
              />
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
      {layoutDetection}
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