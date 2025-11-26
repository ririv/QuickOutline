<script lang="ts">
  import ArrowPopup from './ArrowPopup.svelte';
  import PositionDiagram from './PositionDiagram.svelte';

  interface SectionConfig {
    left: string;
    center: string;
    right: string;
    inner: string;
    outer: string;
    drawLine: boolean;
  }

  interface Props {
    config?: SectionConfig;
    type?: 'header' | 'footer';
  }

  let { 
    config = $bindable({ left: '', center: '', right: '', inner: '', outer: '', drawLine: false }), 
    type = 'header',
  }: Props = $props();

  let activePos: 'left' | 'center' | 'right' | 'inner' | 'outer' = $state('center');

  function setActive(pos: 'left' | 'center' | 'right' | 'inner' | 'outer') {
    activePos = pos;
  }

  function handleInput() {
  }
  
  function toggleDrawLine() {
      config.drawLine = !config.drawLine;
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
  // Trigger element references for autoPosition action
  let leftBtnEl: HTMLElement = $state();
  let centerBtnEl: HTMLElement = $state();
  let rightBtnEl: HTMLElement = $state();
  let innerBtnEl: HTMLElement = $state();
  let outerBtnEl: HTMLElement = $state();

</script>

<div class="section-editor {type}" class:show-line={config.drawLine}>
  {#if type === 'footer'}
    <!-- Footer Top Line Trigger -->
    <div 
      class="divider-line-trigger footer-line" 
      onclick={toggleDrawLine} 
      role="button" 
      tabindex="0"
      onkeydown={(e) => e.key === 'Enter' && toggleDrawLine()}
      title={config.drawLine ? "点击移除分割线" : "点击添加分割线"}
    ></div>
  {/if}

  <div class="toolbar">
    <!-- Absolute Positions -->
    <div class="pos-group">
      <div class="btn-wrapper">
        <button 
          bind:this={leftBtnEl}
          class="pos-btn" class:active={activePos === 'left'}
          onclick={() => setActive('left')} 
          title="Left Aligned"
        >
          <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="17" y1="10" x2="3" y2="10"></line><line x1="21" y1="6" x2="3" y2="6"></line><line x1="21" y1="14" x2="3" y2="14"></line><line x1="17" y1="18" x2="3" y2="18"></line></svg>
          {#if hasContent('left')}<span class="dot"></span>{/if}
        </button>
        <ArrowPopup placement={type === 'header' ? 'bottom' : 'top'} className="hover-popup" triggerEl={leftBtnEl}>
            <PositionDiagram type={type} pos="left" />
        </ArrowPopup>
      </div>
      
      <div class="btn-wrapper">
        <button 
          bind:this={centerBtnEl}
          class="pos-btn" class:active={activePos === 'center'}
          onclick={() => setActive('center')} 
          title="Center Aligned"
        >
          <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="10" x2="6" y2="10"></line><line x1="21" y1="6" x2="3" y2="6"></line><line x1="21" y1="14" x2="3" y2="14"></line><line x1="18" y1="18" x2="6" y2="18"></line></svg>
          {#if hasContent('center')}<span class="dot"></span>{/if}
        </button>
        <ArrowPopup placement={type === 'header' ? 'bottom' : 'top'} className="hover-popup" triggerEl={centerBtnEl}>
            <PositionDiagram type={type} pos="center" />
        </ArrowPopup>
      </div>

      <div class="btn-wrapper">
        <button 
          bind:this={rightBtnEl}
          class="pos-btn" class:active={activePos === 'right'}
          onclick={() => setActive('right')} 
          title="Right Aligned"
        >
          <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="21" y1="10" x2="7" y2="10"></line><line x1="21" y1="6" x2="3" y2="6"></line><line x1="21" y1="14" x2="3" y2="14"></line><line x1="21" y1="18" x2="7" y2="18"></line></svg>
          {#if hasContent('right')}<span class="dot"></span>{/if}
        </button>
        <ArrowPopup placement={type === 'header' ? 'bottom' : 'top'} className="hover-popup" triggerEl={rightBtnEl}>
            <PositionDiagram type={type} pos="right" />
        </ArrowPopup>
      </div>
    </div>

    <div class="divider"></div>

    <!-- Relative Positions -->
    <div class="pos-group relative-group">
      <div class="btn-wrapper">
          <button 
            bind:this={innerBtnEl}
            class="pos-btn" class:active={activePos === 'inner'}
            onclick={() => setActive('inner')} 
            title="Inner Side"
          >
            <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"></path><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"></path></svg>
            {#if hasContent('inner')}<span class="dot"></span>{/if}
          </button>
          <ArrowPopup placement={type === 'header' ? 'bottom' : 'top'} className="hover-popup" triggerEl={innerBtnEl}>
              <PositionDiagram type={type} pos="inner" />
          </ArrowPopup>
      </div>

      <div class="btn-wrapper">
          <button 
            bind:this={outerBtnEl}
            class="pos-btn" class:active={activePos === 'outer'}
            onclick={() => setActive('outer')} 
            title="Outer Side"
          >
            <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z"></path><path d="M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z"></path></svg>
            {#if hasContent('outer')}<span class="dot"></span>{/if}
          </button>
          <ArrowPopup placement={type === 'header' ? 'bottom' : 'top'} className="hover-popup" triggerEl={outerBtnEl}>
              <PositionDiagram type={type} pos="outer" />
          </ArrowPopup>
      </div>
    </div>

    <div class="right-tools">
            {#if type === 'footer'}
        <div class="tooltip-container">
            <span class="hint-icon">?</span>
            <div class="tooltip">Use <code>&lbrace;p&rbrace;</code> for page number</div>
        </div>
      {/if}
      <button 
        class="toggle-line-btn" class:active={config.drawLine}
        onclick={toggleDrawLine} 
        title="Show Divider Line"
      >
        <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          {#if type === 'header'}
              <rect x="2" y="4" width="18" height="12" rx="2" stroke-opacity="0.3" stroke-dasharray="2 2" stroke={config.drawLine ? '#1677ff' : '#999'}></rect>
              <line x1="2" y1="20" x2="20" y2="20" stroke={config.drawLine ? '#1677ff' : '#999'} stroke-dasharray={config.drawLine ? '0' : '2 2'}></line>
          {:else}
              <rect x="2" y="8" width="18" height="12" rx="2" stroke-opacity="0.3" stroke-dasharray="2 2" stroke={config.drawLine ? '#1677ff' : '#999'}></rect>
              <line x1="2" y1="4" x2="20" y2="4" stroke={config.drawLine ? '#1677ff' : '#999'} stroke-dasharray={config.drawLine ? '0' : '2 2'}></line>
          {/if}
        </svg>
      </button>


    </div>
  </div>
    
  <div class="input-wrapper">
    <input 
      type="text" 
      bind:value={config[activePos]} 
      oninput={handleInput}
      style:text-align={activePos === 'right' ? 'right' : activePos === 'center' ? 'center' : 'left'}
      style:padding-left={activePos === 'center' ? '40px' : '22px'}
      placeholder="{type === 'header' ? 'Header' : 'Footer'} ({activePos}) (e.g. &lbrace;p&rbrace;)..." 
    />
  </div>

  {#if type === 'header'}
    <!-- Header Bottom Line Trigger -->
    <div 
      class="divider-line-trigger header-line" 
      onclick={toggleDrawLine} 
      role="button" 
      tabindex="0"
      onkeydown={(e) => e.key === 'Enter' && toggleDrawLine()}
      title={config.drawLine ? "点击移除分割线" : "点击添加分割线"}
    ></div>
  {/if}
</div> <!-- /section-editor -->

<style>
    .section-editor {
      background: #fff;
      padding: 8px 10px;
      display: flex;
      flex-direction: column;
      gap: 8px;
      position: relative;
    }
  
    /* Trigger area for line clicking */
    .divider-line-trigger {
      position: absolute;
      left: 32px;
      width: calc(100% - 64px);
      height: 9px; /* Larger hit area */
      cursor: pointer;
      z-index: 1;
    }
    
      /* Visual Line inside trigger */
      .divider-line-trigger::before {
        content: '';
        position: absolute;
        left: 0;
        width: 100%;
        height: 1px;
                background: #eee;
                transition: background-color 0.2s;
              }
              
              .section-editor.footer::before:hover { /* Hover for non-active state */
                background: #ccc;
              }          
          .section-editor::after:hover { /* Hover for non-active state */
            background: #ccc;
          }    
      /* Hover State */
      .divider-line-trigger:hover::before {
        background: #bbb;
      }    
    /* Header line at bottom */
    .header-line {
      bottom: 0;
    }
    .header-line::before {
      bottom: 0;
    }
  
    /* Footer line at top */
    .footer-line {
      top: 0;
    }
    .footer-line::before {
      top: 0;
    }
  
    .section-editor.footer {
      flex-direction: column-reverse;
    }
  
      /* Active State */
      .section-editor.show-line .divider-line-trigger::before {
        background: #333;
      }
      
  .section-editor.footer.show-line::before {
    background: #333;
  }

  /* Hover for active state */
  .section-editor.show-line::after:hover,
  .section-editor.footer.show-line::before:hover {
    background: #ccc;
  }
    

    .toolbar {
      display: flex;
      align-items: center;
      padding-left: 20px;
      padding-right: 20px;
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

  

    /* Hide popup by default */

    .btn-wrapper :global(.hover-popup) {

        visibility: hidden;

        opacity: 0;

        transition: all 0.2s;

        pointer-events: none;

    }

  

    /* Show popup on hover */

    .btn-wrapper:hover :global(.hover-popup) {

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
      padding: 10px 22px 10px 22px;
      border: none;
      background: transparent;
      border-radius: 4px;
      font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
      font-size: 13px;
      box-sizing: border-box;
      transition: background-color 0.2s;
      user-select: text;
    }

  input:focus {
    outline: none;
    background-color: transparent;
  }
  
  input:hover {
    background-color: rgba(0, 0, 0, 0.02);
  }
  
  .toggle-line-btn {
      background: transparent;
      border: none;
      padding: 2px;
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;
      border-radius: 3px;
      color: #999;
      transition: all 0.2s;
      width: 24px; /* Fixed width to make clickable area consistent */
      height: 24px; /* Fixed height */
  }
  
  .toggle-line-btn:hover {
      background-color: #f0f0f0;
      color: #666;
  }
  
  .toggle-line-btn.active {
      background-color: #e6f7ff;
      color: #1677ff;
  }

  /* Specific styling for SVG inside toggle-line-btn */
  .toggle-line-btn svg {
      width: 100%;
      height: 100%;
      stroke: currentColor; /* Inherit color from parent button */
  }

  .right-tools {
    margin-left: auto;
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .hint-icon {
    font-size: 12px;
    color: #999;
    border: 1px solid #ccc;
    border-radius: 50%;
    width: 18px;
    height: 18px;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: help;
    user-select: none;
  }
  
  .tooltip-container {
      position: relative; 
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