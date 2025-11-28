<script lang="ts">
  import SplitPane from '../../components/SplitPane.svelte';
  import MdEditor from '../../components/MdEditor.svelte';
  import Preview from '../../components/Preview.svelte';
  import StatusBar from '../../components/StatusBar.svelte';
  import SectionEditor from '../../components/SectionEditor.svelte';
  import CollapseTrigger from '../../components/CollapseTrigger.svelte';
  import { initBridge } from '@/lib/bridge';
  import '../../assets/global.css';
  import { onMount } from 'svelte';
  import { slide } from 'svelte/transition';

  let editorComponent: MdEditor;
  let previewComponent: Preview;
  
  // Payload state for Preview component
  let currentPagedPayload: any = $state(null);

  // State for StatusBar
  let insertPos = $state(1);
  // style is not used in Markdown tab currently, but binding is required by StatusBar prop
  let style = $state('None');
  
  let headerConfig = $state({ left: '', center: '', right: '', inner: '', outer: '', drawLine: false });
  let footerConfig = $state({ left: '', center: '{p}', right: '', inner: '', outer: '', drawLine: false });
  
  let showHeader = $state(false);
  let showFooter = $state(false);
  
  let debounceTimer: number; // For live preview debounce

  function hasContent(config: typeof headerConfig) {
      const hasText = Object.entries(config).some(([k, v]) => {
          if (k === 'drawLine') return false;
          return typeof v === 'string' && v.trim().length > 0 && v !== '{p}';
      });
      return hasText || config.drawLine;
  }

  onMount(() => {
    // Initialize Bridge to route Java calls to components
    initBridge({
      // Preview actions - Kept for compatibility but routed to safe checks
      onUpdateSvg: (json) => (previewComponent as any)?.renderSvg && (previewComponent as any).renderSvg(json),
      onUpdateImage: (json) => (previewComponent as any)?.renderImage && (previewComponent as any).renderImage(json),
      onSetSvgDoubleBuffering: (enable) => (previewComponent as any)?.setDoubleBuffer && (previewComponent as any).setDoubleBuffer(enable),

      // Editor actions
      onInitVditor: (md) => editorComponent?.init(md),
      onInsertContent: (text) => editorComponent?.insertValue(text),
      onGetContent: () => editorComponent?.getValue(),
      onSetContent: (md) => editorComponent?.setValue(md),
      onInsertImageMarkdown: (path) => editorComponent?.insertImageMarkdown(path),
      onGetContentHtml: () => editorComponent?.getContentHtml(),
      onGetPayloads: () => editorComponent?.getPayloads(),
    });
  });


  async function triggerPreview2() {
    if (!editorComponent) return;
    const payloadJson = await editorComponent.getPayloads();
    const payload = JSON.parse(payloadJson);

    const request = {
      ...payload,
      header: headerConfig,
      footer: footerConfig
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
      insertPos,
      style,
      header: headerConfig,
      footer: footerConfig
    };

    if (window.javaBridge && window.javaBridge.renderPdf) {
      window.javaBridge.renderPdf(JSON.stringify(request));
    } else {
      console.warn('Java Bridge renderPdf not available', request);
    }
  }

  async function triggerPreview() {
      if (!editorComponent) return;
      const payloadJson = await editorComponent.getPayloads();
      const payload = JSON.parse(payloadJson);
      
      // Update the reactive state, which will trigger Preview -> PagedRenderer
      currentPagedPayload = {
          ...payload,
          header: headerConfig,
          footer: footerConfig
      };
  }

  async function handleGenerate() {
     // Trigger browser print via Java Bridge
     // JavaFX will handle this via WebEngine.print()
     if (window.javaBridge && window.javaBridge.print) {
        window.javaBridge.print();
     } else {
        window.print(); // Fallback
     }
  }
</script>

<main>
  <div class="content-area">
      <SplitPane initialSplit={50}>
        <div slot="left" class="h-full flex-col">
          <!-- Header Trigger & Editor -->
          <CollapseTrigger 
            position="top" 
            label="Header" 
            expanded={showHeader} 
            hasContent={hasContent(headerConfig)}
            ontoggle={() => showHeader = !showHeader} 
          />
          {#if showHeader}
            <div transition:slide={{ duration: 200 }}>
              <SectionEditor 
                type="header"
                bind:config={headerConfig} 
              />
            </div>
          {/if}

          <div class="editor-wrapper">
            <MdEditor bind:this={editorComponent} onchange={triggerPreview} />
          </div>

          <!-- Footer Trigger & Editor -->
          {#if showFooter}
            <div transition:slide={{ duration: 200 }}>
              <SectionEditor 
                type="footer"
                bind:config={footerConfig} 
              />
            </div>
          {/if}
          <CollapseTrigger 
            position="bottom" 
            label="Footer" 
            expanded={showFooter} 
            hasContent={hasContent(footerConfig)}
            ontoggle={() => showFooter = !showFooter} 
          />
        </div>
        <div slot="right" class="h-full">
          <!-- Pass payload via prop -->
          <Preview 
            bind:this={previewComponent} 
            mode="paged" 
            pagedPayload={currentPagedPayload}
            onrefresh={triggerPreview} 
          />
        </div>
      </SplitPane>
  </div>
  
  <StatusBar 
      bind:insertPos 
      bind:style 
      showOffset={false} 
      showStyle={false}
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
      flex: 1;
      overflow: hidden;
      position: relative;
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
    :global([slot="left"]),
    :global(.status-bar) {
        display: none !important;
    }
    
    :global([slot="right"]) {
        height: auto !important;
        overflow: visible !important;
    }
  }
</style>
