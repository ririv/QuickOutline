<script lang="ts">
  interface SectionConfig {
    left: string;
    center: string;
    right: string;
  }

  interface Props {
    config?: SectionConfig;
    type?: 'header' | 'footer';
    onchange?: () => void;
  }

  let { 
    config = $bindable({ left: '', center: '', right: '' }), 
    type = 'header',
    onchange 
  }: Props = $props();

  let activePos: 'left' | 'center' | 'right' = $state('center');

  function setActive(pos: 'left' | 'center' | 'right') {
    activePos = pos;
  }

  function handleInput() {
    onchange?.();
  }

  // Helper to check if a position has content (for dot indicator)
  function hasContent(pos: 'left' | 'center' | 'right') {
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
  <div class="controls">
    <div class="pos-selector">
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
    
    <div class="input-wrapper">
      <input 
        type="text" 
        bind:value={config[activePos]} 
        oninput={handleInput}
        placeholder="{type === 'header' ? 'Header' : 'Footer'} ({activePos}) (e.g. &lbrace;p&rbrace;)..." 
      />
      {#if type === 'footer' && activePos !== 'left'}
        <div class="tooltip-container">
            <span class="hint-icon">?</span>
            <div class="tooltip">Use <code>&lbrace;p&rbrace;</code> for page number</div>
        </div>
      {/if}
    </div>
  </div>
</div>

<style>
  .section-editor {
    background: #f8f9fa;
    border-bottom: 1px solid #e1e4e8;
    padding: 6px 10px;
  }

  .section-editor.footer {
    border-bottom: none;
    border-top: 1px solid #e1e4e8;
  }

  .controls {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .pos-selector {
    display: flex;
    background: #e1e4e8;
    border-radius: 4px;
    padding: 2px;
  }

  .pos-btn {
    background: transparent;
    border: none;
    padding: 4px 6px;
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

  .input-wrapper {
    flex: 1;
    position: relative;
    display: flex;
    align-items: center;
  }

  input {
    width: 100%;
    padding: 4px 8px;
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
