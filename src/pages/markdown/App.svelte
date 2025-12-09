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
        .markdown-body { 
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Helvetica, Arial, sans-serif; 
            line-height: 1.6; 
            color: #24292e; 
            box-sizing: border-box;
            min-width: 200px;
            max-width: 980px;
            margin: 0 auto;
            padding: 45px;
            background-color: white; /* Ensure background is white */
        }
        ${editorThemeCss}
        
        /* General Markdown-it output styling (can be adjusted to match CM) */
        .markdown-body h1, .markdown-body h2, .markdown-body h3, .markdown-body h4, .markdown-body h5, .markdown-body h6 { font-weight: 600; line-height: 1.25; margin-top: 24px; margin-bottom: 16px; }
        .markdown-body h1 { font-size: 2em; border-bottom: 1px solid #eaecef; padding-bottom: 0.3em; }
        .markdown-body h2 { font-size: 1.5em; border-bottom: 1px solid #eaecef; padding-bottom: 0.3em; }
        .markdown-body p { margin-top: 0; margin-bottom: 16px; }
        .markdown-body code { font-family: ui-monospace, SFMono-Regular, SF Mono, Menlo, Consolas, Liberation Mono, monospace; background-color: rgba(27,31,35,0.05); padding: 0.2em 0.4em; border-radius: 3px; font-size: 85%; }
        .markdown-body pre { background-color: #f6f8fa; padding: 16px; overflow: auto; border-radius: 3px; }
        .markdown-body pre code { background-color: transparent; padding: 0; }
        .markdown-body blockquote { padding: 0 1em; color: #6a737d; border-left: 0.25em solid #dfe2e5; margin: 0 0 16px 0; }
        .markdown-body img { max-width: 100%; box-sizing: content-box; background-color: #fff; }

        /* --- List Styles --- */
        .markdown-body ul, .markdown-body ol { margin-top: 0; margin-bottom: 16px; padding-left: 2em; }
        .markdown-body li { margin-bottom: 4px; }
        .markdown-body ul li { list-style-type: disc; }
        .markdown-body ol li { list-style-type: decimal; }
        .markdown-body ul ul, .markdown-body ol ul, .markdown-body ul ol, .markdown-body ol ol { margin-top: 0; margin-bottom: 0; }

        /* Task List Specifics - Custom Checkbox */
        .markdown-body .task-list-item { 
            list-style-type: none; 
            position: relative; 
        }
        
        /* Custom Checkbox Container */
        .markdown-body .custom-checkbox {
            position: absolute;
            left: -1.5em; 
            top: 0.25em;  
            width: 14px;
            height: 14px;
            border: 1.5px solid #d0d7de; 
            border-radius: 3px;
            background-color: #fff;
            display: inline-block;
            box-sizing: border-box;
            line-height: 1;
        }

        /* Checked State */
        .markdown-body .custom-checkbox.checked {
            background-color: #0969da; 
            border-color: #0969da;
        }

        /* Checkmark */
        .markdown-body .custom-checkbox.checked::after {
            content: '';
            position: absolute;
            left: 0;
            top: 0;
            width: 100%;
            height: 100%;
            background-image: url("data:image/svg+xml,%3Csvg width='12' height='10' viewBox='0 0 12 10' fill='none' xmlns='http://www.w3.org/2000/svg'%3E%3Cpath d='M1.5 5L4.5 8L10.5 1' stroke='white' stroke-width='1.5' stroke-linecap='round' stroke-linejoin='round'/%3E%3C/svg%3E");
            background-repeat: no-repeat;
            background-position: center;
            background-size: 10px;
        }
        
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
