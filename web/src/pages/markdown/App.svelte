<script lang="ts">
  import SplitPane from '../../components/SplitPane.svelte';
  import MdEditor from '../../components/MdEditor.svelte';
  import Preview from '../../components/Preview.svelte';
  import StatusBar from '../../components/StatusBar.svelte';
  import SectionEditor from '../../components/SectionEditor.svelte';
  import CollapseTrigger from '../../components/CollapseTrigger.svelte';
  import { initBridge } from '../../lib/bridge';
  import '../../assets/global.css';
  import { onMount } from 'svelte';
  import { slide } from 'svelte/transition';

  let editorComponent: MdEditor;
  let previewComponent: Preview;

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

  // Use $effect to react to changes in headerConfig or footerConfig (deeply reactive $state)

  onMount(() => {
    // Initialize Bridge to route Java calls to components
    initBridge({
      // Preview actions
      onUpdateSvg: (json) => previewComponent?.renderSvg(json),
      onUpdateImage: (json) => previewComponent?.renderImage(json),
      onSetSvgDoubleBuffering: (enable) => previewComponent?.setDoubleBuffer(enable),

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

  async function triggerPreview() {
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

  async function handleGenerate() {
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
          <Preview bind:this={previewComponent} mode="combined" onrefresh={triggerPreview} />
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
    height: 100vh;
    width: 100vw;
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
</style>
