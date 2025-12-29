<script lang="ts">
  import SplitPane from '../../components/SplitPane.svelte';
  import MdEditor from '../../components/editor/MdEditor.svelte';
  import Preview from '../../components/Preview.svelte';
  import StatusBar from '../../components/StatusBar.svelte';
  import PageFrame from '../../components/headerfooter/PageFrame.svelte';
  import ConfirmDialog from '../../components/ConfirmDialog.svelte'; // Import ConfirmDialog
  import { confirm } from '@/stores/confirm.svelte'; // Import confirm helper
  import '../../assets/global.css';
  import { onMount, onDestroy } from 'svelte';
  import { markdownStore } from '@/stores/markdownStore.svelte';
  import { messageStore } from '@/stores/messageStore.svelte.ts'; // Import messageStore
  import { printStore } from '@/stores/printStore.svelte'; // Import printStore
  import { appStore, FnTab } from '@/stores/appStore.svelte.ts';
  import { invoke } from '@tauri-apps/api/core'; // Import invoke
  import { appDataDir, join } from '@tauri-apps/api/path'; // Import path utils
  import { getEditorPreviewCss } from '@/lib/editor/style-converter';
  import markdownPreviewCss from '@/lib/editor/styles/markdown-preview.css?inline';
  import { PageSectionTemplate } from '@/lib/templates/PageSectionTemplate.tsx';
  import { generatePageCss } from '@/lib/preview-engine/css-generator';
  import { MarkdownPrintTemplate } from '@/lib/templates/MarkdownPrintTemplate.tsx'; // Import the new template
  
  let editorComponent: MdEditor;
  let previewComponent: Preview;
  
  let debounceTimer: ReturnType<typeof setTimeout>;
  let currentDebounceTime = 10; // Start with almost instant preview for small docs

  let activeTab = $derived(appStore.activeTab);

  // Debounced wrapper for triggerPreview to prevent excessive rendering during typing
  function debouncedPreview() {
      clearTimeout(debounceTimer);
      debounceTimer = setTimeout(() => {
          triggerPreview();
      }, currentDebounceTime);
  }

  // Refresh preview when tab becomes active to restore CSS
  $effect(() => {
      if (activeTab === FnTab.markdown) {
          // Use a small timeout to ensure DOM is visible if needed, though not strictly required for CSS injection
          setTimeout(() => triggerPreview(), 0);
      }
  });

  function handleRenderStats(stats: { duration: number }) {
      if (stats.duration < 100) {
          currentDebounceTime = 10;
      } else {
          currentDebounceTime = Math.min(1000, stats.duration + 300);
      }
      console.log(`[Preview] Render took ${Math.round(stats.duration)}ms. Next debounce: ${currentDebounceTime}ms`);
  }

  onMount(() => {
    // Always init, use empty string if no content
    setTimeout(() => {
        editorComponent?.init(markdownStore.content || '', 'live',  { tableStyle: 'grid' });
    }, 0);
  });

  onDestroy(() => {
      if (editorComponent) {
          markdownStore.updateContent(editorComponent.getValue());
      }
  });

  async function triggerPreview() {
      if (!editorComponent) return;
      
      const htmlContent = await editorComponent.getRenderedHtml({
          enableIndentedCodeBlocks: markdownStore.enableIndentedCodeBlocks
      });
      
      const { tableStyle } = editorComponent.getStylesConfig(); 
      const editorThemeCss = getEditorPreviewCss(tableStyle, ".markdown-body");
      
      const generatedCss = `
        ${markdownPreviewCss}
        ${editorThemeCss}
      `;

      markdownStore.currentPagedContent = {
          html: `<div class="markdown-body">${htmlContent}</div>`,
          styles: generatedCss,
          header: markdownStore.headerConfig,
          footer: markdownStore.footerConfig,
          pageLayout: markdownStore.pageLayout,
          hfLayout: markdownStore.hfLayout
      };
  }

  async function handleGenerate() {
     const pagedContent = markdownStore.currentPagedContent;
     if (!pagedContent || !pagedContent.html) {
         messageStore.add("No content to generate.", "WARNING");
         return;
     }

    messageStore.add("Preparing PDF resources...", "INFO");


    // Determine Base URL for resource loading (fonts, images)
    // Since the backend now serves this HTML via a local HTTP server rooted at 'print_workspace',
    // relative paths (e.g. "fonts/...") will resolve correctly against the server root.

    // We set base to '.' to ensure relative resolution works.
    const baseUrl = '.';

    // Generate Header/Footer HTML
    const headerHtml = PageSectionTemplate(pagedContent.header);
    const footerHtml = PageSectionTemplate(pagedContent.footer);

    // Generate Page CSS
    const pageCss = generatePageCss(pagedContent.header, pagedContent.footer, pagedContent.pageLayout, pagedContent.hfLayout);

    // Construct full HTML for printing

    const fullHtml = MarkdownPrintTemplate({
       styles: pagedContent.styles,
       pageCss: pageCss,
       headerHtml: headerHtml,
       footerHtml: footerHtml,
       contentHtml: pagedContent.html,
       baseUrl: baseUrl
    });
    messageStore.add("Generating PDF...", "INFO");
     const filename = `markdown_${Date.now()}.pdf`;
     
     try {
         let modeParam = printStore.mode.toLowerCase();
         if (printStore.mode === 'HeadlessChrome') {
             modeParam = 'headless_chrome';
         }

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
      bind:numberingStyle={markdownStore.numberingStyle}
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