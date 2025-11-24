<script lang="ts">
  import SplitPane from '../../components/SplitPane.svelte';
  import Preview from '../../components/Preview.svelte';
  import SimpleEditor from '../../components/SimpleEditor.svelte';
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
  
  let debounceTimer: number;

  // Styles options (matches PageLabel.STYLE_MAP)
  const styles = ['None', 'Decimal', 'Roman Lower', 'Roman Upper', 'Alpha Lower', 'Alpha Upper'];

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
        style
      });
      // Call Java to generate preview
      if (window.javaBridge && window.javaBridge.previewToc) {
        window.javaBridge.previewToc(payload);
      } else {
        console.warn('Java Bridge previewToc not available', payload);
      }
    }, 500);
  }

  function handleGenerate() {
      const payload = JSON.stringify({
        tocContent,
        title,
        offset,
        insertPos,
        style
      });
      if (window.javaBridge && window.javaBridge.generateToc) {
        window.javaBridge.generateToc(payload);
      } else {
        console.warn('Java Bridge generateToc not available', payload);
      }
  }

</script>

<main>
  <SplitPane initialSplit={40}>
    <div slot="left" class="left-panel">
      <div class="header">
        <input type="text" bind:value={title} on:input={triggerPreview} placeholder="Title" class="title-input"/>
      </div>
      
      <div class="editor-wrapper">
        <SimpleEditor bind:value={tocContent} onchange={triggerPreview} placeholder="Enter TOC here..." />
      </div>

      <div class="footer">
        <div class="input-group">
            <label>Offset:</label>
            <input type="number" bind:value={offset} on:input={triggerPreview} style="width: 60px"/>
        </div>
        <div class="input-group">
            <label>Pos:</label>
            <input type="number" bind:value={insertPos} style="width: 60px"/>
        </div>
        <div class="input-group">
            <select bind:value={style} on:change={triggerPreview}>
                {#each styles as s}
                    <option value={s}>{s}</option>
                {/each}
            </select>
        </div>
        <button class="primary-btn" on:click={handleGenerate}>Generate</button>
      </div>
    </div>
    
    <div slot="right" class="h-full">
      <Preview bind:this={previewComponent} mode="preview-only" />
    </div>
  </SplitPane>
</main>

<style>
  main {
    height: 100vh;
    width: 100vw;
    overflow: hidden;
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
  }

  .editor-wrapper {
    flex: 1;
    overflow: hidden;
  }

  .footer {
    padding: 10px;
    border-top: 1px solid #eee;
    display: flex;
    gap: 10px;
    align-items: center;
    background: #f9f9f9;
    flex-wrap: wrap;
  }

  .input-group {
    display: flex;
    align-items: center;
    gap: 5px;
    font-size: 12px;
  }
  
  input[type=number], select {
      padding: 4px;
      border: 1px solid #ccc;
      border-radius: 3px;
  }

  .primary-btn {
      margin-left: auto;
      background: #1677ff;
      color: white;
      border: none;
      padding: 6px 12px;
      border-radius: 4px;
      cursor: pointer;
  }
  .primary-btn:hover { background: #4096ff; }
</style>