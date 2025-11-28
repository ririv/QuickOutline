<script lang="ts">
  import SplitPane from '../../components/SplitPane.svelte';
  import Preview from '../../components/Preview.svelte';
  import SimpleEditor from '../../components/SimpleEditor.svelte';
  import StatusBar from '../../components/StatusBar.svelte';
  import SectionEditor from '../../components/SectionEditor.svelte';
  import CollapseTrigger from '../../components/CollapseTrigger.svelte';
  import { initBridge } from '@/lib/bridge';
  import '../../assets/global.css';
  import { onMount } from 'svelte';
  import { slide } from 'svelte/transition';

  let previewComponent: Preview;
  
  // State
  let tocContent = $state('');
  let title = $state('Table of Contents');
  let offset = $state(0);
  let insertPos = $state(1);
  let style = $state('None');
  
  let headerConfig = $state({ left: '', center: '', right: '', inner: '', outer: '', drawLine: false });
  let footerConfig = $state({ left: '', center: '{p}', right: '', inner: '', outer: '', drawLine: false });
  
  let showHeader = $state(false);
  let showFooter = $state(false);
  
  let debounceTimer: number;
  
  function hasContent(config: typeof headerConfig) {
      const hasText = Object.entries(config).some(([k, v]) => {
          if (k === 'drawLine') return false;
          return typeof v === 'string' && v.trim().length > 0 && v !== '{p}';
      });
      return hasText || config.drawLine;
  }
  
  // Use $effect to react to changes in headerConfig or footerConfig (deeply reactive $state)
  $effect(() => {
    // Read headerConfig and footerConfig to trigger reactivity.
    // Accessing headerConfig.left, footerConfig.center etc. ensures deep reactivity
    // for string changes. For drawLine, config.drawLine is also watched.
    // stringify the whole object to ensure deep watching for all properties.
    const headerJson = JSON.stringify(headerConfig);
    const footerJson = JSON.stringify(footerConfig);

    // Call triggerPreview with debounce
    clearTimeout(debounceTimer);
    debounceTimer = setTimeout(() => {
        // Only trigger preview if config has actually changed from default or previous state.
        // This is a simple check, a more robust solution might track previous serialized state.
        // For now, rely on Svelte's reactivity to only run $effect when dependencies change.
        if (headerJson !== '{"left":"","center":"","right":"","inner":"","outer":"","drawLine":false}' ||
            footerJson !== '{"left":"","center":"{p}","right":"","inner":"","outer":"","drawLine":false}') {
            triggerPreview();
        }
    }, 500);
  });
  
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

          <div class="header">
            <input type="text" bind:value={title} oninput={triggerPreview} placeholder="Title" class="title-input"/>
          </div>
          
          <div class="editor-wrapper">
            <SimpleEditor bind:value={tocContent} onchange={triggerPreview} placeholder="Enter TOC here..." />
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
    height: 100%;
    width: 100%;
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
    position: relative;
  }
  
  .header::after {
    content: '';
    position: absolute;
    bottom: 0;
    left: 32px;
    width: calc(100% - 64px);
    height: 1px;
    background: #eee;
  }
/* ... middle content ... */
  .title-input {
    width: 100%;
    padding: 16px 22px;
    font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
    font-size: 20px;
    font-weight: bold;
    border: none;
    background: transparent;
    box-sizing: border-box;
    text-align: center;
    color: #333;
    transition: background-color 0.2s;
  }
  
  .title-input:focus {
    outline: none;
  }

  .title-input:hover {
    background-color: rgba(0, 0, 0, 0.02);
  }

  .editor-wrapper {
    flex: 1;
    overflow: hidden;
    padding: 10px;
  }
</style>