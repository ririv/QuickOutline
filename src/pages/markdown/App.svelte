<script lang="ts">
  import SplitPane from '../../components/SplitPane.svelte';
  import MdEditor from '../../components/MdEditor.svelte';
  import Preview from '../../components/Preview.svelte';
  import StatusBar from '../../components/StatusBar.svelte';
  import SectionEditor from '../../components/SectionEditor.svelte';
  import CollapseTrigger from '../../components/CollapseTrigger.svelte';
  import ConfirmDialog from '../../components/ConfirmDialog.svelte'; // Import ConfirmDialog
  import { confirm } from '@/stores/confirm.svelte'; // Import confirm helper
  import { initBridge } from '@/lib/bridge';
  import '../../assets/global.css';
  import { onMount, onDestroy } from 'svelte';
  import { slide } from 'svelte/transition';
  import { markdownStore } from '@/stores/markdownStore.svelte';
  import { getEditorPreviewCss } from '@/lib/editor/style-converter';
  import { katexCss } from '@/lib/editor/markdown-renderer';
  import markdownPreviewCss from '@/lib/editor/styles/markdown-preview.css?inline';

  let editorComponent: MdEditor;
  let previewComponent: Preview;
  
  let debounceTimer: number; // For live preview debounce

  onMount(() => {
    // Initialize Bridge to route Java calls to components
    initBridge({
      // Preview actions - Kept for compatibility but routed to safe checks
      onUpdateSvg: (json) => (previewComponent as any)?.renderSvg && (previewComponent as any).renderSvg(json),
      onUpdateImage: (json) => (previewComponent as any)?.renderImage && (previewComponent as any).renderImage(json),
      onSetSvgDoubleBuffering: (enable) => (previewComponent as any)?.setDoubleBuffer && (previewComponent as any).setDoubleBuffer(enable),

      // Editor actions
      onInitVditor: (md) => {
          // Pass a default editorConfig, or load from markdownStore if available/desired
          editorComponent?.init(md, 'live', { tableStyle: 'grid' });
          // Also update store if init comes from outside
          markdownStore.updateContent(md);
      },
      onInsertContent: (text) => editorComponent?.insertValue(text),
      onGetContent: () => editorComponent?.getValue(),
      onSetContent: (md) => {
          editorComponent?.setValue(md);
          markdownStore.updateContent(md);
      },
      onInsertImageMarkdown: (path) => editorComponent?.insertImageMarkdown(path),
      onGetContentHtml: () => editorComponent?.getContentHtml(),
      onGetPayloads: () => editorComponent?.getPayloads(),
    });

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


  async function triggerPreview2() {
    if (!editorComponent) return;
    const payloadJson = await editorComponent.getPayloads();
    const payload = JSON.parse(payloadJson);

    const request = {
      ...payload,
      header: markdownStore.headerConfig,
      footer: markdownStore.footerConfig
    };

    // Assuming Java bridge has updatePreview method that accepts json string
    if (window.javaBridge && window.javaBridge.updatePreview) {
      window.javaBridge.updatePreview(JSON.stringify(request));
    } else {
      console.warn('Java Bridge updatePreview not available', request);
    }
  }

  async function handleGenerate2() {
    if (!editorComponent) return;

    // Get editor content
    const payloadJson = await editorComponent.getPayloads();
    const payload = JSON.parse(payloadJson);

    // Merge with status bar params
    // Note: style is not used in Markdown PDF generation currently, but we pass it anyway
    const request = {
      ...payload, // html, styles
      insertPos: markdownStore.insertPos,
      style: markdownStore.numberingStyle,
      header: markdownStore.headerConfig,
      footer: markdownStore.footerConfig
    };

    if (window.javaBridge && window.javaBridge.renderPdf) {
      window.javaBridge.renderPdf(JSON.stringify(request));
    } else {
      console.warn('Java Bridge renderPdf not available', request);
    }
  }

  async function triggerPreview() {
      if (!editorComponent) return;
      
      // Get raw HTML from markdown-it
      const htmlContent = await editorComponent.getContentHtml();
      
      // Generate CSS from our shared theme objects
      // 1. Base Styles (Fonts, Colors, etc.)
      // 2. Table Styles (Selected Grid or Academic)
      const { tableStyle } = editorComponent.getStylesConfig(); // Get styles config from editor
      const editorThemeCss = getEditorPreviewCss(tableStyle, ".markdown-body");
      
      const generatedCss = `
        ${markdownPreviewCss}
        ${editorThemeCss}
        
        /* Injected Math and Code Highlighting Styles */
        ${katexCss}
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

     // Trigger browser print via Java Bridge
     // JavaFX will handle this via WebEngine.print()
     if (window.javaBridge && window.javaBridge.print) {
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
              />
            </div>
          {/if}

          <div class="editor-wrapper">
            <MdEditor bind:this={editorComponent} onchange={triggerPreview} />
          </div>

          <!-- Footer Trigger & Editor -->
          {#if markdownStore.showFooter}
            <div transition:slide={{ duration: 200 }}>
              <SectionEditor 
                type="footer"
                bind:config={markdownStore.footerConfig} 
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
