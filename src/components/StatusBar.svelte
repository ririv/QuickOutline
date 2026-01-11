<script lang="ts">
  import OffsetPopup from './statusbar-popup/OffsetPopup.svelte';
  import InsertPositionPopup from './statusbar-popup/InsertPositionPopup.svelte';
  import NumberingStylePopup from './statusbar-popup/NumberingStylePopup.svelte';
  import PageSetupPopup from './statusbar-popup/PageSetupPopup.svelte';
  import HeaderFooterPopup from './statusbar-popup/HeaderFooterPopup.svelte';
  import Icon from '@/components/Icon.svelte';
  import { clickOutside } from '@/lib/actions/clickOutside';
  import {PageLabelNumberingStyle, pageLabelStyleMap, type PageLabel} from "@/lib/types/page-label.ts";
  import { type PageLayout, defaultPageLayout, type HeaderFooterLayout, defaultHeaderFooterLayout } from "@/lib/types/page";

  export interface InsertionSettings {
    pos: number;
    autoCorrect: boolean;
    showAutoCorrect: boolean;
  }

  interface Props {
    offset?: number;
    insertion?: InsertionSettings; // New aggregated prop
    pageLabel?: PageLabel; 
    pageLayout?: PageLayout;
    hfLayout?: HeaderFooterLayout;
    showOffset?: boolean;
    onGenerate?: () => void;
    onParamChange?: () => void;
  }

  let { 
    offset = $bindable(0),
    insertion = $bindable({ pos: 1, autoCorrect: true, showAutoCorrect: false }),
    pageLabel = $bindable({
        pageIndex: 1,
        numberingStyle: PageLabelNumberingStyle.NONE,
        labelPrefix: '',
        startValue: 1
    }),
    pageLayout = $bindable(defaultPageLayout),
    hfLayout = $bindable(defaultHeaderFooterLayout),
    showOffset = true,
    onGenerate,
    onParamChange
  }: Props = $props();

  let activePopup: 'pagenum-offset' | 'insert-pos' | 'numbering-style' | 'page-setup' | 'header-footer' | null = $state(null);
  let barElement: HTMLElement;
  
  // Trigger elements for popups
  let offsetBtnEl = $state<HTMLElement | undefined>();
  let posBtnEl = $state<HTMLElement | undefined>();
  let numberingStyleBtnEl = $state<HTMLElement | undefined>();
  let setupBtnEl = $state<HTMLElement | undefined>();
  let hfBtnEl = $state<HTMLElement | undefined>();

  function togglePopup(type: 'pagenum-offset' | 'insert-pos' | 'numbering-style' | 'page-setup' | 'header-footer') {
      if (activePopup === type) activePopup = null;
      else activePopup = type;
  }

  function onPopupChange() {
      // Setup popup doesn't auto-close on change usually, as multiple fields exist
      onParamChange?.();
  }

  let layoutSummary = $derived.by(() => {
      const { size, marginTop, marginBottom, marginLeft, marginRight } = pageLayout;
      let marginText = '';
      
      if (marginTop === marginBottom && marginLeft === marginRight && marginTop === marginLeft) {
          marginText = `${marginTop}mm`;
      } else if (marginTop === marginBottom && marginLeft === marginRight) {
          marginText = `${marginTop}x${marginLeft}mm`;
      } else {
          marginText = '*';
      }
      
      return `${size} ${marginText}`;
  });

  let hfLayoutSummary = $derived.by(() => {
      const { headerDist, footerDist } = hfLayout;
      if (headerDist !== undefined && footerDist !== undefined) {
           return {
               type: headerDist === footerDist ? 'both-equal' : 'both-diff',
               header: headerDist,
               footer: footerDist
           };
      } else if (headerDist !== undefined) {
          return { type: 'header-only', header: headerDist };
      } else if (footerDist !== undefined) {
          return { type: 'footer-only', footer: footerDist };
      }
      return null;
  });

  const removeSuffix = (str:string, suffix:string) => str.endsWith(suffix) ? str.slice(0, -suffix.length) : str;
  
</script>

<div class="status-bar" bind:this={barElement} use:clickOutside={() => activePopup = null}>
  {#if showOffset}
      <div class="status-item-wrapper">
          <!-- svelte-ignore a11y_click_events_have_key_events -->
          <!-- svelte-ignore a11y_no_static_element_interactions -->
          <div 
            bind:this={offsetBtnEl}
            class="status-item {activePopup === 'pagenum-offset' ? 'active' : ''}"
            onclick={() => togglePopup('pagenum-offset')}
            title="Set Page Offset"
          >
              <span class="icon">
                <Icon name="offset" width="14" height="14" />
              </span> Offset: {offset}
          </div>
          {#if activePopup === 'pagenum-offset'}
              <OffsetPopup bind:offset onchange={onParamChange} triggerEl={offsetBtnEl} />
          {/if}
      </div>
  {/if}

  <div class="status-item-wrapper">
      <!-- svelte-ignore a11y_click_events_have_key_events -->
      <!-- svelte-ignore a11y_no_static_element_interactions -->
      <div
        bind:this={posBtnEl}
        class="status-item {activePopup === 'insert-pos' ? 'active' : ''}"
        onclick={() => togglePopup('insert-pos')}
        title="Set Insert Position"
      >
          <span class="icon">
              <Icon name="insert-position" width="14" height="14" />
          </span> Pos: {insertion.pos}
      </div>
      {#if activePopup === 'insert-pos'}
          <InsertPositionPopup 
            bind:insertPos={insertion.pos} 
            bind:autoCorrect={insertion.autoCorrect} 
            showAutoCorrect={insertion.showAutoCorrect} 
            triggerEl={posBtnEl} 
            onchange={onParamChange}
          />
      {/if}
  </div>



  <div class="status-item-wrapper">
      <!-- svelte-ignore a11y_click_events_have_key_events -->
      <!-- svelte-ignore a11y_no_static_element_interactions -->
      <div 
        bind:this={setupBtnEl}
        class="status-item {activePopup === 'page-setup' ? 'active' : ''}"
        onclick={() => togglePopup('page-setup')}
        title="Page Setup: {pageLayout.size}, {pageLayout.orientation}, Margins..."
      >
          <span class="icon" class:rotated={pageLayout.orientation === 'landscape'}>
              <Icon name="page-setup" width="16" height="16" />
          </span>
          {layoutSummary}
      </div>
      {#if activePopup === 'page-setup'}
          <PageSetupPopup bind:layout={pageLayout} onchange={onPopupChange} triggerEl={setupBtnEl} />
      {/if}
  </div>

  <div class="status-item-wrapper">
      <!-- svelte-ignore a11y_click_events_have_key_events -->
      <!-- svelte-ignore a11y_no_static_element_interactions -->
      <div 
        bind:this={hfBtnEl}
        class="status-item {activePopup === 'header-footer' ? 'active' : ''}" 
        onclick={() => togglePopup('header-footer')} 
        title="Header & Footer Position"
      >
          <span class="icon">
              <Icon name="header-footer" width="14" height="14" />
          </span>
          <span class="hf-summary-content">
            {#if hfLayoutSummary}
                {#if hfLayoutSummary.type === 'both-equal'}
                    <Icon name="arrow-up-down" width="10" height="10" />{hfLayoutSummary.header}mm
                {:else if hfLayoutSummary.type === 'both-diff'}
                    <Icon name="arrow-up" width="10" height="10" />{hfLayoutSummary.header} <Icon name="arrow-down" width="10" height="10" />{hfLayoutSummary.footer}mm
                {:else if hfLayoutSummary.type === 'header-only'}
                    <Icon name="arrow-up" width="10" height="10" />{hfLayoutSummary.header}mm
                {:else if hfLayoutSummary.type === 'footer-only'}
                    <Icon name="arrow-down" width="10" height="10" />{hfLayoutSummary.footer}mm
                {/if}
            {/if}
          </span>
      </div>
      {#if activePopup === 'header-footer'}
          <HeaderFooterPopup bind:layout={hfLayout} onchange={onParamChange} triggerEl={hfBtnEl} />
      {/if}
  </div>

    <div class="status-item-wrapper">
        <!-- svelte-ignore a11y_click_events_have_key_events -->
        <!-- svelte-ignore a11y_no_static_element_interactions -->
        <div
                bind:this={numberingStyleBtnEl}
                class="status-item {activePopup === 'numbering-style' ? 'active' : ''}"
                onclick={() => togglePopup('numbering-style')}
                title="Set Numbering Style"
        >
      <span class="icon">
          <Icon name="number-sign" width="14" height="14" />
      </span>{removeSuffix(pageLabelStyleMap.getDisplayText(pageLabel.numberingStyle), ", ...")}
        </div>
        {#if activePopup === 'numbering-style'}
            <NumberingStylePopup bind:pageLabel onchange={onPopupChange} triggerEl={numberingStyleBtnEl} />
        {/if}
    </div>
  
  <div class="spacer"></div>
  
  <button class="icon-btn generate-btn" onclick={onGenerate} title="Generate PDF">
      <Icon name="play" width="20" height="20" />
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
      transition: transform 0.2s ease; /* Add transition for smooth rotation */
      transform-origin: center center;
      flex-shrink: 0; /* Prevent shrinking */
      will-change: transform; /* Optimize for animation */
  }

  .icon.rotated {
      transform: rotate(90deg);
  }

  .spacer { flex: 1; }

  .icon-btn {
      background: transparent;
      border: none;
      /*color: #666;*/
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
      color: #4096ff;
      margin-left: 0;
      margin-right: 8px;
  }
  .generate-btn:hover {
      background-color: rgba(64, 150, 255, 0.1);
      color: #1677ff;
  }

  .hf-summary-content {
      display: inline-flex;
      align-items: center;
      gap: 2px; /* Adjust this value for desired spacing */
  }

  .hf-summary-content :global(svg) {
      transform: translateY(-1px); /* 向上移动 1px */
  }
</style>
