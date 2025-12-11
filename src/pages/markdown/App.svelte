<script lang="ts">
  import SplitPane from '../../components/SplitPane.svelte';
  import MdEditor from '../../components/MdEditor.svelte';
  import Preview from '../../components/Preview.svelte';
  import StatusBar from '../../components/StatusBar.svelte';
  import SectionEditor from '../../components/SectionEditor.svelte';
  import CollapseTrigger from '../../components/CollapseTrigger.svelte';
  import ConfirmDialog from '../../components/ConfirmDialog.svelte'; // Import ConfirmDialog
  import { confirm } from '@/stores/confirm.svelte'; // Import confirm helper
  import '../../assets/global.css';
  import { onMount, onDestroy } from 'svelte';
  import { slide } from 'svelte/transition';
  import { markdownStore } from '@/stores/markdownStore.svelte';
  import { getEditorPreviewCss } from '@/lib/editor/style-converter';
  // katexCss is now globally imported via widgets.ts, no need to import/inject here
  // import { katexCss } from '@/lib/editor/markdown-renderer';
  import markdownPreviewCss from '@/lib/editor/styles/markdown-preview.css?inline';
  import { getRenderedTocData } from '@/lib/preview-engine/paged-engine';

  let editorComponent: MdEditor;
  let previewComponent: Preview;
  
  let debounceTimer: ReturnType<typeof setTimeout>;
  let currentDebounceTime = 10; // Start with almost instant preview for small docs

  // Debounced wrapper for triggerPreview to prevent excessive rendering during typing
  function debouncedPreview() {
      clearTimeout(debounceTimer);
      debounceTimer = setTimeout(() => {
          triggerPreview();
      }, currentDebounceTime);
  }

  function handleRenderStats(stats: { duration: number }) {
      // Dynamic Debounce Strategy:
      // If render takes < 100ms, keep it snappy (10ms delay).
      // If render takes longer, increase debounce to avoid blocking typing.
      // We cap it at 1000ms.
      if (stats.duration < 100) {
          currentDebounceTime = 10;
      } else {
          // If slow, wait longer to let user finish typing
          currentDebounceTime = Math.min(1000, stats.duration + 300);
      }
      console.log(`[Preview] Render took ${Math.round(stats.duration)}ms. Next debounce: ${currentDebounceTime}ms`);
  }

  onMount(() => {
    // Restore content from store if available
    if (markdownStore.content) {
        // Use setTimeout to ensure editor is mounted and init called (MdEditor init is in onMount)
        setTimeout(() => {
            // Re-initialize editor with restored content and default config
            editorComponent?.init(markdownStore.content, 'live',  { tableStyle: 'grid' });
        }, 0);
    }
  });

  onDestroy(() => {
      // Save content to store on unmount
      if (editorComponent) {
          markdownStore.updateContent(editorComponent.getValue());
      }
  });

  async function triggerPreview() {
      if (!editorComponent) return;
      
      // Get raw HTML from markdown-it, passing config from store
      const htmlContent = await editorComponent.getContentHtml({
          enableIndentedCodeBlocks: markdownStore.enableIndentedCodeBlocks
      });
      
      // Generate CSS from our shared theme objects
      // 1. Base Styles (Fonts, Colors, etc.)
      // 2. Table Styles (Selected Grid or Academic)
      const { tableStyle } = editorComponent.getStylesConfig(); // Get styles config from editor
      const editorThemeCss = getEditorPreviewCss(tableStyle, ".markdown-body");
      
      const generatedCss = `
        ${markdownPreviewCss}
        ${editorThemeCss}
        
        /* Injected Math and Code Highlighting Styles */
        /* katexCss is now globally handled, no longer injected here */
      `;

      // Update the reactive state, which will trigger Preview -> PagedRenderer
      markdownStore.currentPagedPayload = {
          html: htmlContent,
          styles: generatedCss,
          header: markdownStore.headerConfig,
          footer: markdownStore.footerConfig
      };
  }

  async function handleGenerate() {
     // Example usage of the new Confirm Dialog
     const ok = await confirm('Are you sure you want to print this document?', 'Print Confirmation', { type: 'info' });
     if (!ok) return;

     // Extract TOC data from the current preview
     const tocData = getRenderedTocData();
     console.log('[App] Extracted TOC Data:', tocData);

     // Trigger browser print via Java Bridge
     // JavaFX will handle this via WebEngine.print()
     if (window.javaBridge && window.javaBridge.renderPdf) {
        // Prefer renderPdf if available, as it can handle TOC/Bookmarks
        const payload = {
            ...markdownStore.currentPagedPayload,
            toc: tocData
        };
        window.javaBridge.renderPdf(JSON.stringify(payload));
     } else if (window.javaBridge && window.javaBridge.print) {
        window.javaBridge.print();
     } else {
        window.print(); // Fallback
     }
  }
</script>

<!-- Mount the Global Confirm Dialog -->
<ConfirmDialog />

<main>
  <div class="content-area">
      <SplitPane initialSplit={50}>
        {#snippet left()}
        <div class="h-full flex-col left-panel">
          <!-- Header Trigger & Editor -->
          <CollapseTrigger 
            position="top" 
            label="Header" 
            expanded={markdownStore.showHeader} 
            content={markdownStore.headerConfig}
            ontoggle={() => markdownStore.showHeader = !markdownStore.showHeader} 
          />
          {#if markdownStore.showHeader}
            <div transition:slide={{ duration: 200 }}>
              <SectionEditor 
                type="header"
                bind:config={markdownStore.headerConfig} 
                onchange={triggerPreview}
              />
            </div>
          {/if}

          <div class="editor-wrapper">
            <MdEditor bind:this={editorComponent} onchange={debouncedPreview} />
          </div>

          <!-- Footer Trigger & Editor -->
          {#if markdownStore.showFooter}
            <div transition:slide={{ duration: 200 }}>
              <SectionEditor 
                type="footer"
                bind:config={markdownStore.footerConfig} 
                onchange={triggerPreview}
              />
            </div>
          {/if}
          <CollapseTrigger 
            position="bottom" 
            label="Footer" 
            expanded={markdownStore.showFooter} 
            content={markdownStore.footerConfig}
            ontoggle={() => markdownStore.showFooter = !markdownStore.showFooter} 
          />
        </div>
        {/snippet}
        
        {#snippet right()}
        <div class="h-full right-panel">
          <!-- Pass payload via prop -->
          <Preview 
            bind:this={previewComponent} 
            mode="paged" 
            pagedPayload={markdownStore.currentPagedPayload}
            onrefresh={triggerPreview} 
            onRenderStats={handleRenderStats}
          />
        </div>
        {/snippet}
      </SplitPane>
  </div>
  
  <StatusBar 
      bind:insertPos={markdownStore.insertPos} 
      bind:numberingStyle={markdownStore.numberingStyle}
      showOffset={false} 
      showNumberingStyle={false}
      onGenerate={handleGenerate} 
      onParamChange={() => { /* No specific action for param changes in Markdown tab */ }}
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
