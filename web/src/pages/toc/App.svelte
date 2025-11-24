<script lang="ts">
  import SplitPane from '../../components/SplitPane.svelte';
  import Preview from '../../components/Preview.svelte';
  import SimpleEditor from '../../components/SimpleEditor.svelte';
  import SettingsPopup from '../../components/SettingsPopup.svelte';
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
  let activePopup: 'offset' | 'pos' | 'style' | null = null;

  onMount(() => {
    initBridge({
      onUpdateSvg: (json) => previewComponent?.renderSvg(json),
      onUpdateImage: (json) => previewComponent?.renderImage(json),
      onSetSvgDoubleBuffering: (enable) => previewComponent?.setDoubleBuffer(enable),
    });
    
    // Click outside to close popup
    window.addEventListener('click', (e) => {
        const target = e.target as HTMLElement;
        if (activePopup && !target.closest('.status-item') && !target.closest('.popup-card')) {
            activePopup = null;
        }
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
        style
      });
      if (window.javaBridge && window.javaBridge.generateToc) {
        window.javaBridge.generateToc(payload);
      }
  }
  
  function togglePopup(type: 'offset' | 'pos' | 'style') {
      if (activePopup === type) {
          activePopup = null;
      } else {
          activePopup = type;
      }
  }
  
  function onPopupChange() {
      // For style selection, close popup immediately
      if (activePopup === 'style') {
          activePopup = null;
      }
      triggerPreview();
  }

</script>

<main>
  <div class="content-area">
      <SplitPane initialSplit={40}>
        <div slot="left" class="left-panel">
          <div class="header">
            <input type="text" bind:value={title} oninput={triggerPreview} placeholder="Title" class="title-input"/>
          </div>
          
          <div class="editor-wrapper">
            <SimpleEditor bind:value={tocContent} onchange={triggerPreview} placeholder="Enter TOC here..." />
          </div>
        </div>
        
        <div slot="right" class="h-full">
          <Preview bind:this={previewComponent} mode="preview-only" />
        </div>
      </SplitPane>
  </div>

  <!-- Status Bar -->
  <div class="status-bar">
      <!-- Offset Trigger -->
      <div class="status-item-wrapper">
          <!-- svelte-ignore a11y_click_events_have_key_events -->
          <!-- svelte-ignore a11y_no_static_element_interactions -->
          <div class="status-item {activePopup === 'offset' ? 'active' : ''}" onclick={() => togglePopup('offset')} title="Set Page Offset">
              <span class="icon">
                  <svg xmlns="http://www.w3.org/2000/svg" class="icon" viewBox="0 0 1024 1024"
                       width="200" height="200">
                     <path d="M356.992 203.9552a51.2 51.2 0 0 0-99.584-23.8976l-153.6 640a51.2 51.2 0 0 0 99.584 23.8976l153.6-640zM358.4 486.4a38.4 38.4 0 1 0 0 76.8h180.3776l-31.168 31.168a38.4 38.4 0 1 0 54.2976 54.2976l126.72-126.7072-126.72-126.72a38.4 38.4 0 1 0-54.2976 54.3104L544.4608 486.4H358.4z m523.9424-344.1792a51.2 51.2 0 0 1 37.8368 61.7344l-19.2 80a51.2 51.2 0 0 1-99.5712-23.8976l19.2-80a51.2 51.2 0 0 1 61.7344-37.8368z m-57.6 240a51.2 51.2 0 0 1 37.8368 61.7344l-38.4 160a51.2 51.2 0 0 1-99.5712-23.8976l38.4-160a51.2 51.2 0 0 1 61.7344-37.8368z m-76.8 320a51.2 51.2 0 0 1 37.8368 61.7344l-19.2 80a51.2 51.2 0 0 1-99.5712-23.8976l19.2-80a51.2 51.2 0 0 1 61.7344-37.8368z"></path>
                  </svg>
              </span> Offset: {offset}
          </div>
          {#if activePopup === 'offset'}
              <SettingsPopup type="offset" bind:offset bind:insertPos bind:style onchange={triggerPreview} />
          {/if}
      </div>

      <!-- Pos Trigger -->
      <div class="status-item-wrapper">
          <!-- svelte-ignore a11y_click_events_have_key_events -->
          <!-- svelte-ignore a11y_no_static_element_interactions -->
          <div class="status-item {activePopup === 'pos' ? 'active' : ''}" onclick={() => togglePopup('pos')} title="Set Insert Position">
              <span class="icon">
                  <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"></path><circle cx="12" cy="10" r="3"></circle></svg>
              </span> Pos: {insertPos}
          </div>
          {#if activePopup === 'pos'}
              <SettingsPopup type="pos" bind:offset bind:insertPos bind:style />
          {/if}
      </div>

      <!-- Style Trigger -->
      <div class="status-item-wrapper">
          <!-- svelte-ignore a11y_click_events_have_key_events -->
          <!-- svelte-ignore a11y_no_static_element_interactions -->
          <div class="status-item {activePopup === 'style' ? 'active' : ''}" onclick={() => togglePopup('style')} title="Set Numbering Style">
              <span class="icon">
                  <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="4" y1="9" x2="20" y2="9"></line><line x1="4" y1="15" x2="20" y2="15"></line><line x1="10" y1="3" x2="8" y2="21"></line><line x1="16" y1="3" x2="14" y2="21"></line></svg>
              </span> Page Num: {style}
          </div>
          {#if activePopup === 'style'}
              <SettingsPopup type="style" bind:offset bind:insertPos bind:style onchange={onPopupChange} />
          {/if}
      </div>
      
      <div class="spacer"></div>
      
      <button class="icon-btn generate-btn" onclick={handleGenerate} title="Generate PDF">
          <!-- Play Icon -->
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" width="20" height="20">
            <path d="M8 5v14l11-7z"/>
          </svg>
      </button>
  </div>
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

  /* Status Bar - VS Code style */
  .status-bar {
      height: 28px; /* Taller height */
      background-color: #f6f7f9;
      border-top: 1px solid #e1e4e8;
      display: flex;
      align-items: stretch; /* Items stretch */
      padding: 0; 
      font-size: 12px;
      user-select: none;
      color: #555;
      z-index: 20;
  }
  
  .status-item-wrapper {
      position: relative;
      height: 100%;
      display: flex;
      align-items: stretch;
      /* Adjust popup positioning based on this wrapper */
  }

  .status-item {
      padding: 0 10px; /* Horizontal padding, will be height: 100% from parent */
      display: flex;
      align-items: center;
      gap: 6px;
      cursor: pointer;
      transition: background-color 0.1s;
      margin-left: 4px; /* Small gap between items */
      border-radius: 3px;
  }
  
  .status-item:first-of-type {
      margin-left: 8px; /* First item has left margin */
  }
  
  .status-item:hover, .status-item.active {
      background-color: #e1e4e8;
      color: #333;
  }
  
  .icon { 
      display: flex;
      align-items: center;
      justify-content: center;
      width: 16px; 
      height: 16px;
      opacity: 0.8; 
  }
  .icon svg {
      width: 14px;
      height: 14px;
  }

  .spacer { flex: 1; }
  
  /* Right side button - Distinct style */
  .icon-btn {
      background: transparent;
      border: none;
      color: #666;
      cursor: pointer;
      padding: 0;
      height: 22px; /* Fixed small size */
      width: 22px;
      border-radius: 4px; /* Rounded */
      display: flex;
      align-items: center;
      justify-content: center;
      transition: all 0.2s;
      align-self: center; /* Vertically centered */
      margin: 0 8px 0 auto; /* Auto margin left to push to right, and right margin */
  }
  
  .icon-btn:hover {
      background-color: #e1e4e8;
      color: #1677ff;
  }
  
  .generate-btn {
      color: #28a745;
      margin-left: 0; /* Remove auto-margin, controlled by its own wrapper */
      margin-right: 8px; /* Padding from right edge */
  }
  .generate-btn:hover {
      background-color: rgba(40, 167, 69, 0.1); /* Greenish hover */
      color: #218838;
  }
</style>
