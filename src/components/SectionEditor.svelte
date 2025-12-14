<script lang="ts">
    import ArrowPopup from './controls/ArrowPopup.svelte';
    import PositionDiagram from './PositionDiagram.svelte';
    import Tooltip from './Tooltip.svelte';

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
        onchange?: () => void;
    }

    let {
        config = $bindable({left: '', center: '', right: '', inner: '', outer: '', drawLine: false}),
        type = 'header',
        onchange
    }: Props = $props();

    let activePos: 'left' | 'center' | 'right' | 'inner' | 'outer' = $state('center');
    let isButtonHovered = $state(false); // State for linked hover effect
    let justToggled = $state(false); // State to suppress hover effect immediately after click

    function setActive(pos: 'left' | 'center' | 'right' | 'inner' | 'outer') {
        activePos = pos;
    }

    function handleInput() {
        if (onchange) onchange();
    }

    function handleInputDoubleClick(e: MouseEvent) {
        const currentValue = config[activePos];
        if (currentValue && currentValue.length > 0) {
            return;
        }

        const target = e.currentTarget as HTMLElement;
        const rect = target.getBoundingClientRect();
        const clickX = e.clientX - rect.left;
        const width = rect.width;

        if (clickX < width / 3) {
            setActive('left');
        } else if (clickX > 2 * width / 3) {
            setActive('right');
        } else {
            setActive('center');
        }
    }

    function toggleDrawLine() {
        config.drawLine = !config.drawLine;
        justToggled = true; // Set to true after any toggle
        if (onchange) onchange();
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
    let leftBtnEl = $state<HTMLElement | undefined>();
    let centerBtnEl = $state<HTMLElement | undefined>();
    let rightBtnEl = $state<HTMLElement | undefined>();
    let innerBtnEl = $state<HTMLElement | undefined>();
    let outerBtnEl = $state<HTMLElement | undefined>();
</script>

<div class="section-editor {type}" class:show-line={config.drawLine}>
  {#if type === 'footer'}
    <!-- Footer Top Line Trigger -->
    <div
        class="divider-line-trigger footer-line"
        class:force-hover={isButtonHovered}
        class:just-toggled={justToggled}
        onclick={toggleDrawLine}
        onmouseleave={() => justToggled = false}
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
          <svg fill="none" height="14" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round"
               stroke-width="2" viewBox="0 0 24 24" width="14" xmlns="http://www.w3.org/2000/svg">
            <line x1="17" x2="3" y1="10" y2="10"></line>
            <line x1="21" x2="3" y1="6" y2="6"></line>
            <line x1="21" x2="3" y1="14" y2="14"></line>
            <line x1="17" x2="3" y1="18" y2="18"></line>
          </svg>
          {#if hasContent('left')}<span class="dot"></span>{/if}
        </button>
        <ArrowPopup className="hover-popup" placement={type === 'header' ? 'bottom' : 'top'}
                    triggerEl={leftBtnEl}>
          <PositionDiagram pos="left" type={type}/>
        </ArrowPopup>
      </div>

      <div class="btn-wrapper">
        <button
            bind:this={centerBtnEl}
            class="pos-btn" class:active={activePos === 'center'}
            onclick={() => setActive('center')}
            title="Center Aligned"
        >
          <svg fill="none" height="14" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round"
               stroke-width="2" viewBox="0 0 24 24" width="14" xmlns="http://www.w3.org/2000/svg">
            <line x1="18" x2="6" y1="10" y2="10"></line>
            <line x1="21" x2="3" y1="6" y2="6"></line>
            <line x1="21" x2="3" y1="14" y2="14"></line>
            <line x1="18" x2="6" y1="18" y2="18"></line>
          </svg>
          {#if hasContent('center')}<span class="dot"></span>{/if}
        </button>
        <ArrowPopup className="hover-popup" placement={type === 'header' ? 'bottom' : 'top'}
                    triggerEl={centerBtnEl}>
          <PositionDiagram pos="center" type={type}/>
        </ArrowPopup>
      </div>

      <div class="btn-wrapper">
        <button
            bind:this={rightBtnEl}
            class="pos-btn" class:active={activePos === 'right'}
            onclick={() => setActive('right')}
            title="Right Aligned"
        >
          <svg fill="none" height="14" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round"
               stroke-width="2" viewBox="0 0 24 24" width="14" xmlns="http://www.w3.org/2000/svg">
            <line x1="21" x2="7" y1="10" y2="10"></line>
            <line x1="21" x2="3" y1="6" y2="6"></line>
            <line x1="21" x2="3" y1="14" y2="14"></line>
            <line x1="21" x2="7" y1="18" y2="18"></line>
          </svg>
          {#if hasContent('right')}<span class="dot"></span>{/if}
        </button>
        <ArrowPopup className="hover-popup" placement={type === 'header' ? 'bottom' : 'top'}
                    triggerEl={rightBtnEl}>
          <PositionDiagram pos="right" type={type}/>
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
          <svg fill="none" height="14" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round"
               stroke-width="2" viewBox="0 0 24 24" width="14" xmlns="http://www.w3.org/2000/svg">
            <line stroke-opacity="0.6" x1="12" x2="12" y1="3" y2="21"></line>
            <line x1="5" x2="10" y1="7" y2="7"></line>
            <line x1="7" x2="10" y1="12" y2="12"></line>
            <line x1="5" x2="10" y1="17" y2="17"></line>
            <line x1="14" x2="19" y1="7" y2="7"></line>
            <line x1="14" x2="17" y1="12" y2="12"></line>
            <line x1="14" x2="19" y1="17" y2="17"></line>
          </svg>
          {#if hasContent('inner')}<span class="dot"></span>{/if}
        </button>
        <ArrowPopup className="hover-popup" placement={type === 'header' ? 'bottom' : 'top'}
                    triggerEl={innerBtnEl}>
          <PositionDiagram pos="inner" type={type}/>
        </ArrowPopup>
      </div>

      <div class="btn-wrapper">
        <button
            bind:this={outerBtnEl}
            class="pos-btn" class:active={activePos === 'outer'}
            onclick={() => setActive('outer')}
            title="Outer Side"
        >
          <svg fill="none" height="14" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round"
               stroke-width="2" viewBox="0 0 24 24" width="14" xmlns="http://www.w3.org/2000/svg">
            <line stroke-opacity="0.6" x1="12" x2="12" y1="3" y2="21"></line>
            <line x1="2" x2="7" y1="7" y2="7"></line>
            <line x1="2" x2="5" y1="12" y2="12"></line>
            <line x1="2" x2="7" y1="17" y2="17"></line>
            <line x1="17" x2="22" y1="7" y2="7"></line>
            <line x1="19" x2="22" y1="12" y2="12"></line>
            <line x1="17" x2="22" y1="17" y2="17"></line>
          </svg>
          {#if hasContent('outer')}<span class="dot"></span>{/if}
        </button>
        <ArrowPopup className="hover-popup" placement={type === 'header' ? 'bottom' : 'top'}
                    triggerEl={outerBtnEl}>
          <PositionDiagram pos="outer" type={type}/>
        </ArrowPopup>
      </div>
    </div>

    <div class="right-tools">
      {#if type === 'footer'}
        <Tooltip position="top" rightAligned={true} className="flex items-center">
          <span class="hint-icon">?</span>
          {#snippet popup()}
            Use <code>{'{p}'}</code> for page number
          {/snippet}
        </Tooltip>
      {/if}
      <button
          class="toggle-line-btn" class:active={config.drawLine}
          onclick={toggleDrawLine}
          onmouseenter={() => isButtonHovered = true}
          onmouseleave={() => { isButtonHovered = false; justToggled = false; }}
          title="Show Divider Line"
      >
        <svg fill="none" height="14" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round"
             stroke-width="2" viewBox="0 0 24 24" width="14" xmlns="http://www.w3.org/2000/svg">
          {#if type === 'header'}
            <rect x="2" y="4" width="18" height="12" rx="2" stroke-opacity="0.3" stroke-dasharray="2 2"
                  stroke={config.drawLine ? '#1677ff' : '#999'}></rect>
            <line x1="2" y1="20" x2="20" y2="20" stroke={config.drawLine ? '#1677ff' : '#999'}
                  stroke-dasharray={config.drawLine ? '0' : '2 2'}></line>
          {:else}
            <rect x="2" y="8" width="18" height="12" rx="2" stroke-opacity="0.3" stroke-dasharray="2 2"
                  stroke={config.drawLine ? '#1677ff' : '#999'}></rect>
            <line x1="2" y1="4" x2="20" y2="4" stroke={config.drawLine ? '#1677ff' : '#999'}
                  stroke-dasharray={config.drawLine ? '0' : '2 2'}></line>
          {/if}
        </svg>
      </button>


    </div>
  </div>

  <div class="input-wrapper">
    <input
        bind:value={config[activePos]}
        oninput={handleInput}
        ondblclick={handleInputDoubleClick}
        placeholder="{type === 'header' ? 'Header' : 'Footer'} ({activePos}) (e.g. &lbrace;p&rbrace;)..."
        style:text-align={activePos === 'right' ? 'right' : activePos === 'center' ? 'center' : 'left'}
        type="text"
    />
  </div>

  {#if type === 'header'}
    <!-- Header Bottom Line Trigger -->
    <div
        class="divider-line-trigger header-line"
        class:force-hover={isButtonHovered}
        class:just-toggled={justToggled}
        onclick={toggleDrawLine}
        onmouseleave={() => justToggled = false}
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

    /* Hover State */
    .divider-line-trigger:hover::before,
    .divider-line-trigger.force-hover::before {
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

    /* Hover for active state (Cancel Hint) */
    .section-editor.show-line .divider-line-trigger:hover::before,
    .section-editor.show-line .divider-line-trigger.force-hover::before {
        background: #bbb;
    }

    /* Just Deactivated (Default just-toggled state): Keep White */
    .divider-line-trigger.just-toggled:hover::before,
    .divider-line-trigger.just-toggled.force-hover::before {
        background: #eee;
    }

    /* Just Activated (Specific override for show-line): Keep Black */
    .section-editor.show-line .divider-line-trigger.just-toggled:hover::before,
    .section-editor.show-line .divider-line-trigger.just-toggled.force-hover::before {
        background: #333;
    }

    /* Hover for active state */
    .section-editor.show-line .divider-line-trigger.force-hover::before {
        background: #bbb;
    }


    .toolbar {
        display: flex;
        align-items: center;
        padding-left: 20px;
        padding-right: 20px;
    }


    .pos-group {
        display: flex;
        border-radius: 4px;
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
        background: #f0f0f0;
        color: #333;
    }


    .pos-btn.active {
        background: #e6f7ff;
        color: #1677ff;
        box-shadow: none; /* Remove shadow as it might look weird without container bg */
    }


    .dot {
        position: absolute;
        top: 2px;
        right: 2px;
        width: 4px;
        height: 4px;
        background-color: #ccc; /* Matches CollapseTrigger center-dot */
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

    code {
      font-family: monospace;
      background: rgba(255, 255, 255, 0.2);
      padding: 0 2px;
      border-radius: 2px;
    }
</style>