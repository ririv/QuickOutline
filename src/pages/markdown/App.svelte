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
  import { messageStore } from '@/stores/messageStore'; // Import messageStore
  import { printStore } from '@/stores/printStore.svelte'; // Import printStore
  import { invoke } from '@tauri-apps/api/core'; // Import invoke
  import { getEditorPreviewCss } from '@/lib/editor/style-converter';
  import markdownPreviewCss from '@/lib/editor/styles/markdown-preview.css?inline';
  // Import KaTeX CSS explicitly for PDF generation ensure it's included in the payload
  import katexCss from 'katex/dist/katex.min.css?inline';
  
  // getRenderedTocData was used for JavaBridge, currently unused in simple print_to_pdf
  // import { getRenderedTocData } from '@/lib/preview-engine/paged-engine';

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
      if (stats.duration < 100) {
          currentDebounceTime = 10;
      } else {
          currentDebounceTime = Math.min(1000, stats.duration + 300);
      }
      console.log(`[Preview] Render took ${Math.round(stats.duration)}ms. Next debounce: ${currentDebounceTime}ms`);
  }

  onMount(() => {
    if (markdownStore.content) {
        setTimeout(() => {
            editorComponent?.init(markdownStore.content, 'live',  { tableStyle: 'grid' });
        }, 0);
    }
  });

  onDestroy(() => {
      if (editorComponent) {
          markdownStore.updateContent(editorComponent.getValue());
      }
  });

  async function triggerPreview() {
      if (!editorComponent) return;
      
      const htmlContent = await editorComponent.getContentHtml({
          enableIndentedCodeBlocks: markdownStore.enableIndentedCodeBlocks
      });
      
      const { tableStyle } = editorComponent.getStylesConfig(); 
      const editorThemeCss = getEditorPreviewCss(tableStyle, ".markdown-body");
      
      const generatedCss = `
        ${markdownPreviewCss}
        ${editorThemeCss}
      `;

      markdownStore.currentPagedPayload = {
          html: htmlContent,
          styles: generatedCss,
          header: markdownStore.headerConfig,
          footer: markdownStore.footerConfig
      };
  }

  async function handleGenerate() {
     const payload = markdownStore.currentPagedPayload;
     if (!payload || !payload.html) {
         messageStore.add("No content to generate.", "WARNING");
         return;
     }

     // Fetch UnoCSS Runtime to inject into the HTML
     // This ensures utility classes in the content are styled correctly in the PDF
     let runtimeScript = '';
     try {
         const res = await fetch('/libs/unocss-runtime.bundle.js');
         if (res.ok) {
             runtimeScript = await res.text();
         } else {
             console.warn("Failed to fetch UnoCSS runtime for PDF generation");
         }
     } catch (e) {
         console.warn("Error fetching UnoCSS runtime:", e);
     }

     // Construct full HTML for printing
     const fullHtml = `<!DOCTYPE html>
        <html>
        <head>
            <base href="${window.location.origin}/">
            <meta charset="UTF-8">
            <style>${payload.styles}</style>
            <style>${katexCss}</style>
            <script>
                ${runtimeScript}
            <\/script>
        </head>
        <body class="markdown-body">
            ${payload.html}
        </body>
        </html>`;

     messageStore.add("Generating PDF...", "INFO");
     const filename = `markdown_${Date.now()}.pdf`;
     
     try {
         // Determine mode from global store
         let modeParam = printStore.mode.toLowerCase();
         if (printStore.mode === 'HeadlessChrome') {
             modeParam = 'headless_chrome';
         }

         // Invoke Tauri command
         const pdfPath = await invoke('print_to_pdf', { 
             html: fullHtml, 
             filename: filename,
             mode: modeParam
         });
         
         console.log("PDF Generated at:", pdfPath);
         messageStore.add(`PDF Generated successfully!`, "SUCCESS");

     } catch (e: any) {
         console.error("Generate failed", e);
         messageStore.add("Failed: " + (e.message || e), "ERROR");
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