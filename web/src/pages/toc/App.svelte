<script lang="ts">
  import SplitPane from '../../components/SplitPane.svelte';
  import Preview from '../../components/Preview.svelte';
  import SimpleEditor from '../../components/SimpleEditor.svelte';
  import StatusBar from '../../components/StatusBar.svelte';
  import SectionEditor from '../../components/SectionEditor.svelte';
  import { initBridge } from '../../lib/bridge';
  import '../../assets/global.css';
  import { onMount } from 'svelte';

  let previewComponent: Preview;
  
  // State
  let tocContent = '';
  let title = 'Table of Contents';
  let offset = 0;
  let insertPos = 1;
  let style = 'None';
  
  let headerConfig = { left: '', center: '', right: '', inner: '', outer: '' };
  let footerConfig = { left: '', center: '{p}', right: '', inner: '', outer: '' };
  
  let debounceTimer: number;

  onMount(() => {
    initBridge({
      onUpdateSvg: (json) => previewComponent?.renderSvg(json),
      onUpdateImage: (json) => previewComponent?.renderImage(json),
      onSetSvgDoubleBuffering: (enable) => previewComponent?.setDoubleBuffer(enable),
    });
  });

  function triggerPreview() {
    clearTimeout(debounceTimer);
    debounceTimer = setTimeout(() => {
      const payload = JSON.stringify({
        tocContent,
        title,
        offset,
        style,
        header: headerConfig,
        footer: footerConfig
      });
      if (window.javaBridge && window.javaBridge.previewToc) {
        window.javaBridge.previewToc(payload);
      }
    }, 500);
  }

  function handleGenerate() {
      const payload = JSON.stringify({
        tocContent,
        title,
        offset,
        insertPos,
        style,
        header: headerConfig,
        footer: footerConfig
      });
      if (window.javaBridge && window.javaBridge.generateToc) {
        window.javaBridge.generateToc(payload);
      }
  }

</script>

<main>
  <div class="content-area">
      <SplitPane initialSplit={40}>
        <div slot="left" class="left-panel">
          <!-- Header Settings (Above Title) -->
          <SectionEditor 
            type="header"
            bind:config={headerConfig} 
            onchange={triggerPreview} 
          />

          <div class="header">
            <input type="text" bind:value={title} oninput={triggerPreview} placeholder="Title" class="title-input"/>
          </div>
          
          <div class="editor-wrapper">
            <SimpleEditor bind:value={tocContent} onchange={triggerPreview} placeholder="Enter TOC here..." />
          </div>

          <!-- Footer Settings (Below Content) -->
          <SectionEditor 
            type="footer"
            bind:config={footerConfig} 
            onchange={triggerPreview} 
          />
        </div>
        
        <div slot="right" class="h-full">
          <Preview bind:this={previewComponent} mode="preview-only" onrefresh={triggerPreview} />
        </div>
      </SplitPane>
  </div>

  <StatusBar 
      bind:offset 
      bind:insertPos 
      bind:style 
      onGenerate={handleGenerate} 
      onParamChange={triggerPreview} 
  />
</main>

<style>
  main {
    height: 100vh;
    width: 100vw;
    overflow: hidden;
    display: flex;
    flex-direction: column;
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
  }
  
  .content-area {
      flex: 1;
      overflow: hidden;
      position: relative;
  }
  
  .h-full { height: 100%; }
  
  .left-panel {
    height: 100%;
    display: flex;
    flex-direction: column;
    border-right: 1px solid #ddd;
    background: #fff;
  }

  .header {
    padding: 10px;
    border-bottom: 1px solid #eee;
  }
  .title-input {
    width: 100%;
    padding: 8px;
    font-size: 16px;
    border: 1px solid #ddd;
    border-radius: 4px;
    box-sizing: border-box;
    text-align: center;
    transition: border-color 0.2s, box-shadow 0.2s;
  }
  
  .title-input:focus {
    border-color: #1677ff;
    outline: none;
    box-shadow: 0 0 0 2px rgba(22, 119, 255, 0.2);
  }

  .editor-wrapper {
    flex: 1;
    overflow: hidden;
  }
</style>