<script lang="ts">
  import SettingsPopup from './SettingsPopup.svelte';
  import { onMount } from 'svelte';

  interface Props {
    offset?: number;
    insertPos?: number;
    style?: string;
    showOffset?: boolean;
    onGenerate?: () => void;
    onParamChange?: () => void;
  }

  let { 
    offset = $bindable(0),
    insertPos = $bindable(1),
    style = $bindable('None'),
    showOffset = true,
    showStyle = true,
    onGenerate,
    onParamChange
  }: Props = $props();

  let activePopup: 'offset' | 'pos' | 'style' | null = $state(null);
  let barElement: HTMLElement;
  
  // Trigger elements for popups
  let offsetBtnEl: HTMLElement = $state();
  let posBtnEl: HTMLElement = $state();
  let styleBtnEl: HTMLElement = $state();

  function togglePopup(type: 'offset' | 'pos' | 'style') {
      if (activePopup === type) {
          activePopup = null;
      } else {
          activePopup = type;
      }
  }

  function onPopupChange() {
      if (activePopup === 'style') {
          activePopup = null;
      }
      onParamChange?.();
  }

  onMount(() => {
      const closePopup = (e: MouseEvent) => {
          const target = e.target as HTMLElement;
          if (activePopup && barElement && !barElement.contains(target)) {
              activePopup = null;
          }
      };
      window.addEventListener('click', closePopup);
      return () => window.removeEventListener('click', closePopup);
  });
</script>

<div class="status-bar" bind:this={barElement}>
  {#if showOffset}
      <div class="status-item-wrapper">
          <!-- svelte-ignore a11y_click_events_have_key_events -->
          <!-- svelte-ignore a11y_no_static_element_interactions -->
          <div 
            bind:this={offsetBtnEl}
            class="status-item {activePopup === 'offset' ? 'active' : ''}" 
            onclick={() => togglePopup('offset')} 
            title="Set Page Offset"
          >
              <span class="icon">
                  <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polygon points="12 2 2 7 12 12 22 7 12 2"></polygon><polyline points="2 17 12 22 22 17"></polyline><polyline points="2 12 12 17 22 12"></polyline></svg>
              </span> Offset: {offset}
          </div>
          {#if activePopup === 'offset'}
              <SettingsPopup type="offset" bind:offset bind:insertPos bind:style onchange={onParamChange} triggerEl={offsetBtnEl} />
          {/if}
      </div>
  {/if}

  <div class="status-item-wrapper">
      <!-- svelte-ignore a11y_click_events_have_key_events -->
      <!-- svelte-ignore a11y_no_static_element_interactions -->
      <div 
        bind:this={posBtnEl}
        class="status-item {activePopup === 'pos' ? 'active' : ''}" 
        onclick={() => togglePopup('pos')} 
        title="Set Insert Position"
      >
          <span class="icon">
              <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"></path><circle cx="12" cy="10" r="3"></circle></svg>
          </span> Pos: {insertPos}
      </div>
      {#if activePopup === 'pos'}
          <SettingsPopup type="pos" bind:offset bind:insertPos bind:style triggerEl={posBtnEl} />
      {/if}
  </div>

  {#if showStyle}
  <div class="status-item-wrapper">
      <!-- svelte-ignore a11y_click_events_have_key_events -->
      <!-- svelte-ignore a11y_no_static_element_interactions -->
      <div 
        bind:this={styleBtnEl}
        class="status-item {activePopup === 'style' ? 'active' : ''}" 
        onclick={() => togglePopup('style')} 
        title="Set Numbering Style"
      >
          <span class="icon">
              <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="4" y1="9" x2="20" y2="9"></line><line x1="4" y1="15" x2="20" y2="15"></line><line x1="10" y1="3" x2="8" y2="21"></line><line x1="16" y1="3" x2="14" y2="21"></line></svg>
          </span> Page Num: {style}
      </div>
      {#if activePopup === 'style'}
          <SettingsPopup type="style" bind:offset bind:insertPos bind:style onchange={onPopupChange} triggerEl={styleBtnEl} />
      {/if}
  </div>
  {/if}
  
  <div class="spacer"></div>
  
  <button class="icon-btn generate-btn" onclick={onGenerate} title="Generate PDF">
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" width="20" height="20">
        <path d="M8 5v14l11-7z"/>
      </svg>
  </button>
</div>

<style>
  .status-bar {
      height: 28px;
      background-color: #f6f7f9;
      border-top: 1px solid #e1e4e8;
      display: flex;
      align-items: stretch;
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
  }

  .status-item {
      padding: 0 12px;
      display: flex;
      align-items: center;
      gap: 6px;
      cursor: pointer;
      transition: background-color 0.1s;
      margin-left: 4px;
      border-radius: 3px;
  }
  
  .status-item:first-of-type {
      margin-left: 8px;
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
  
  .spacer { flex: 1; }
  
  .icon-btn {
      background: transparent;
      border: none;
      color: #666;
      cursor: pointer;
      padding: 0;
      height: 22px;
      width: 22px;
      border-radius: 4px;
      display: flex;
      align-items: center;
      justify-content: center;
      transition: all 0.2s;
      align-self: center;
      margin: 0 8px 0 auto;
  }
  
  .icon-btn:hover {
      background-color: #e1e4e8;
      color: #1677ff;
  }
  
  .generate-btn {
      color: #28a745;
      margin-left: 0;
      margin-right: 8px;
  }
  .generate-btn:hover {
      background-color: rgba(40, 167, 69, 0.1);
      color: #218838;
  }
</style>
