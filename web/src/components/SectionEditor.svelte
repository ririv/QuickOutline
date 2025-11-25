<script lang="ts">
  import PositionTooltip from './PositionTooltip.svelte';

  interface SectionConfig {
    left: string;
    center: string;
    right: string;
    inner: string;
    outer: string;
  }

  interface Props {
    config?: SectionConfig;
    type?: 'header' | 'footer';
    onchange?: () => void;
  }

  let { 
    config = $bindable({ left: '', center: '', right: '', inner: '', outer: '' }), 
    type = 'header',
    onchange 
  }: Props = $props();

  let activePos: 'left' | 'center' | 'right' | 'inner' | 'outer' = $state('center');

  function setActive(pos: 'left' | 'center' | 'right' | 'inner' | 'outer') {
    activePos = pos;
  }

  function handleInput() {
    onchange?.();
  }

  // Helper to check if a position has content (for dot indicator)
  function hasContent(pos: 'left' | 'center' | 'right' | 'inner' | 'outer') {
    const value = config[pos];
    if (!value || value.trim().length === 0) {
      return false;
    }
    // For footer center, if the value is '{p}', it's considered default and shouldn't show a dot
    if (type === 'footer' && pos === 'center' && value.trim() === '{p}') {
      return false;
    }
    return true;
  }
</script>

<div class="section-editor {type}">
  <div class="toolbar">
    <!-- Absolute Positions -->
    <div class="pos-group">
      <button 
        class="pos-btn {activePos === 'left' ? 'active' : ''}" 
        onclick={() => setActive('left')} 
        title="Left Aligned"
      >
        <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="17" y1="10" x2="3" y2="10"></line><line x1="21" y1="6" x2="3" y2="6"></line><line x1="21" y1="14" x2="3" y2="14"></line><line x1="17" y1="18" x2="3" y2="18"></line></svg>
        {#if hasContent('left')}<span class="dot"></span>{/if}
      </button>
      <button 
        class="pos-btn {activePos === 'center' ? 'active' : ''}" 
        onclick={() => setActive('center')} 
        title="Center Aligned"
      >
        <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="10" x2="6" y2="10"></line><line x1="21" y1="6" x2="3" y2="6"></line><line x1="21" y1="14" x2="3" y2="14"></line><line x1="18" y1="18" x2="6" y2="18"></line></svg>
        {#if hasContent('center')}<span class="dot"></span>{/if}
      </button>
      <button 
        class="pos-btn {activePos === 'right' ? 'active' : ''}" 
        onclick={() => setActive('right')} 
        title="Right Aligned"
      >
        <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="21" y1="10" x2="7" y2="10"></line><line x1="21" y1="6" x2="3" y2="6"></line><line x1="21" y1="14" x2="3" y2="14"></line><line x1="21" y1="18" x2="7" y2="18"></line></svg>
        {#if hasContent('right')}<span class="dot"></span>{/if}
      </button>
    </div>

    <div class="divider"></div>

    <!-- Relative Positions -->
    <div class="pos-group relative-group">
      <div class="btn-wrapper">
          <button 
            class="pos-btn {activePos === 'inner' ? 'active' : ''}" 
            onclick={() => setActive('inner')} 
            title="Inner Side"
          >
            <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"></path><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"></path></svg>
            {#if hasContent('inner')}<span class="dot"></span>{/if}
          </button>
          <PositionTooltip type={type} pos="inner" />
      </div>

      <div class="btn-wrapper">
          <button 
            class="pos-btn {activePos === 'outer' ? 'active' : ''}" 
            onclick={() => setActive('outer')} 
            title="Outer Side"
          >
            <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z"></path><path d="M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z"></path></svg>
            {#if hasContent('outer')}<span class="dot"></span>{/if}
          </button>
          <PositionTooltip type={type} pos="outer" />
      </div>
    </div>
  </div>
    
  <div class="input-wrapper">
    <input 
      type="text" 
      bind:value={config[activePos]} 
      oninput={handleInput}
      placeholder="{type === 'header' ? 'Header' : 'Footer'} ({activePos}) (e.g. &lbrace;p&rbrace;)..." 
    />
    {#if type === 'footer' && activePos !== 'left' && activePos !== 'inner'}
      <div class="tooltip-container">
          <span class="hint-icon">?</span>
          <div class="tooltip">Use <code>&lbrace;p&rbrace;</code> for page number</div>
      </div>
    {/if}
  </div>
</div>

<style>
  .section-editor {
    background: #f8f9fa;
    border-bottom: 1px solid #e1e4e8;
    padding: 8px 10px;
  }

  .section-editor.footer {
    border-bottom: none;
    border-top: 1px solid #e1e4e8;
  }
  
  .toolbar {
    display: flex;
    align-items: center;
    margin-bottom: 8px;
  }

  .pos-group {
    display: flex;
    background: #e1e4e8;
    border-radius: 4px;
    padding: 2px;
  }

  .divider {
    width: 1px;
    height: 20px;
    background-color: #d0d4d9;
    margin: 0 10px;
  }

  .pos-btn {
    background: transparent;
    border: none;
    padding: 4px 8px;
    cursor: pointer;
    border-radius: 3px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #666;
    position: relative;
    transition: all 0.2s;
  }

  .pos-btn:hover {
    background: rgba(255,255,255,0.5);
    color: #333;
  }

  .pos-btn.active {
    background: #fff;
    color: #1677ff;
    box-shadow: 0 1px 2px rgba(0,0,0,0.1);
  }

  .dot {
    position: absolute;
    top: 2px;
    right: 2px;
    width: 4px;
    height: 4px;
    background-color: #ff4d4f;
    border-radius: 50%;
  }
  
  .relative-group {
    position: relative;
    display: flex;
    align-items: center;
  }
  
  .btn-wrapper {
      position: relative;
  }

  .btn-wrapper:hover :global(.info-tooltip) {
      visibility: visible;
      opacity: 1;
  }

  .input-wrapper {
    position: relative;
    display: flex;
    align-items: center;
  }

  input {
    width: 100%;
    padding: 6px 8px;
    padding-right: 24px; /* Space for hint icon */
    border: 1px solid #ddd;
    border-radius: 4px;
    font-size: 13px;
    box-sizing: border-box;
    transition: border 0.2s;
  }

  input:focus {
    outline: none;
    border-color: #1677ff;
  }

  .hint-icon {
    font-size: 11px;
    color: #999;
    border: 1px solid #ccc;
    border-radius: 50%;
    width: 14px;
    height: 14px;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: help;
    user-select: none;
  }
  
  .tooltip-container {
      position: absolute;
      right: 6px;
      display: flex;
      align-items: center;
  }

  .tooltip {
      visibility: hidden;
      background-color: #333;
      color: #fff;
      text-align: center;
      border-radius: 4px;
      padding: 4px 8px;
      position: absolute;
      z-index: 1;
      bottom: 125%; /* Position above */
      right: 0;
      font-size: 11px;
      white-space: nowrap;
      opacity: 0;
      transition: opacity 0.2s;
      pointer-events: none;
  }

  .tooltip::after {
      content: "";
      position: absolute;
      top: 100%; /* At the bottom of the tooltip */
      right: 4px;
      margin-left: -5px;
      border-width: 5px;
      border-style: solid;
      border-color: #333 transparent transparent transparent;
  }

  .tooltip-container:hover .tooltip {
      visibility: visible;
      opacity: 1;
  }
  
  code {
      font-family: monospace;
      background: rgba(255,255,255,0.2);
      padding: 0 2px;
      border-radius: 2px;
  }
</style>