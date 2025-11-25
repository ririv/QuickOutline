<script lang="ts">
  import SplitPane from '../../components/SplitPane.svelte';
  import MdEditor from '../../components/MdEditor.svelte';
  import Preview from '../../components/Preview.svelte';
  import StatusBar from '../../components/StatusBar.svelte';
  import { initBridge } from '../../lib/bridge';
  import '../../assets/global.css';
  import { onMount } from 'svelte';

  let editorComponent: MdEditor;
  let previewComponent: Preview;

  // State for StatusBar
  let insertPos = 1;
  // style is not used in Markdown tab currently, but binding is required by StatusBar prop
  let style = 'None';
  
  let debounceTimer: number; // For live preview debounce

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
      const json = await editorComponent.getPayloads(); // {html, styles}
      // Assuming Java bridge has updatePreview method that accepts {html, styles}
      if (window.javaBridge && window.javaBridge.updatePreview) {
          window.javaBridge.updatePreview(json);
      } else {
          console.warn('Java Bridge updatePreview not available', json);
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
          style
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
        <div slot="left" class="h-full">
          <MdEditor bind:this={editorComponent} />
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
</style>
