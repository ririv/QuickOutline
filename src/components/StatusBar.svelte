<script lang="ts">
  import OffsetPopup from './statusbar-popup/OffsetPopup.svelte';
  import InsertPositionPopup from './statusbar-popup/InsertPositionPopup.svelte';
  import NumberingStylePopup from './statusbar-popup/NumberingStylePopup.svelte';
  import PaperSizePopup from './statusbar-popup/PaperSizePopup.svelte';
  import PageMarginsPopup from './statusbar-popup/PageMarginsPopup.svelte';
  import HeaderFooterPopup from './statusbar-popup/HeaderFooterPopup.svelte';
  import Icon from '@/components/Icon.svelte';
  import { clickOutside } from '@/lib/actions/clickOutside';
  import {PageLabelNumberingStyle, pageLabelStyleMap, generateRulePreview, type PageLabel} from "@/lib/types/page-label.ts";
  import { type PageLayout, defaultPageLayout, type HeaderFooterLayout, defaultHeaderFooterLayout } from "@/lib/types/page";
  import labelSimpleIcon from '@/assets/icons/label-simple.svg?raw';

  export interface InsertionSettings {
    pos: number;
    autoCorrect: boolean;
    showAutoCorrect: boolean;
  }

  interface Props {
    offset?: number;
    insertion?: InsertionSettings;
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

  let activePopup: 'pagenum-offset' | 'insert-pos' | 'numbering-style' | 'paper-size' | 'page-margins' | 'header-footer' | null = $state(null);
  
  // Group expansion states
  let g1Expanded = $state(true);
  let g2Expanded = $state(true);
  let g3Expanded = $state(true);

  let barElement: HTMLElement;
  
  // Trigger elements for popups
  let offsetBtnEl = $state<HTMLElement | undefined>();
  let posBtnEl = $state<HTMLElement | undefined>();
  let numberingStyleBtnEl = $state<HTMLElement | undefined>();
  let sizeBtnEl = $state<HTMLElement | undefined>();
  let marginBtnEl = $state<HTMLElement | undefined>();
  let hfBtnEl = $state<HTMLElement | undefined>();

  function togglePopup(type: 'pagenum-offset' | 'insert-pos' | 'numbering-style' | 'paper-size' | 'page-margins' | 'header-footer') {
      if (activePopup === type) activePopup = null;
      else activePopup = type;
  }

  function onPopupChange() {
      onParamChange?.();
  }

  let marginSummary = $derived.by(() => {
      const { marginTop, marginBottom, marginLeft, marginRight } = pageLayout;
      let marginText = '';
      
      if (marginTop === marginBottom && marginLeft === marginRight && marginTop === marginLeft) {
          marginText = `${marginTop}mm`;
      } else if (marginTop === marginBottom && marginLeft === marginRight) {
          marginText = `${marginTop}x${marginLeft}mm`;
      } else {
          marginText = '*';
      }
      
      return `${marginText}`;
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
  {#snippet toggleArrow(expanded: boolean)}
    <div class="toggle-arrow">
        <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3">
            <path d={expanded ? "M15 18l-6-6 6-6" : "M9 18l6-6-6-6"}></path>
        </svg>
    </div>
  {/snippet}

  {#if showOffset}
      <!-- Content -->
      <div class="status-group-wrapper" class:collapsed={!g1Expanded}>
          <div class="status-group-inner">
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
                      </span> Offset {offset}
                  </div>
                  {#if activePopup === 'pagenum-offset'}
                      <OffsetPopup bind:offset onchange={onParamChange} triggerEl={offsetBtnEl} />
                  {/if}
              </div>
          </div>
      </div>

      <!-- Toggle Handle -->
      <!-- svelte-ignore a11y_click_events_have_key_events -->
      <!-- svelte-ignore a11y_no_static_element_interactions -->
      <div class="divider toggle-divider" 
        class:collapsed={!g1Expanded}
        onclick={() => g1Expanded = !g1Expanded}
        title={g1Expanded ? "Collapse Offset" : "Expand Offset"}
      >
        {@render toggleArrow(g1Expanded)}
      </div>
  {/if}

  <!-- Group 2: Pos / Size / Margins -->
  <!-- Content -->
  <div class="status-group-wrapper" class:collapsed={!g2Expanded}>
      <div class="status-group-inner">
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
                  </span> Pos {insertion.pos}
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
                bind:this={sizeBtnEl}
                class="status-item {activePopup === 'paper-size' ? 'active' : ''}"
                onclick={() => togglePopup('paper-size')}
                title="Paper Size: {pageLayout.size}, {pageLayout.orientation}"
              >
                  <span class="icon" class:rotated={pageLayout.orientation === 'landscape'}>
                      <Icon name="page-setup" width="16" height="16" />
                  </span>
                  {pageLayout.size}
              </div>
              {#if activePopup === 'paper-size'}
                  <PaperSizePopup bind:layout={pageLayout} onchange={onPopupChange} triggerEl={sizeBtnEl} />
              {/if}
          </div>

          <div class="status-item-wrapper">
              <!-- svelte-ignore a11y_click_events_have_key_events -->
              <!-- svelte-ignore a11y_no_static_element_interactions -->
              <div 
                bind:this={marginBtnEl}
                class="status-item {activePopup === 'page-margins' ? 'active' : ''}"
                onclick={() => togglePopup('page-margins')}
                title="Margins: {marginSummary}"
              >
                  <span class="icon">
                      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                          <rect x="3" y="3" width="18" height="18" rx="1" stroke-opacity="0.2"></rect>
                          <path d="M7 3v18M17 3v18M3 7h18M3 17h18" stroke-width="1.5" stroke-dasharray="2 2"></path>
                      </svg>
                  </span>
                  {marginSummary}
              </div>
              {#if activePopup === 'page-margins'}
                  <PageMarginsPopup bind:layout={pageLayout} onchange={onPopupChange} triggerEl={marginBtnEl} />
              {/if}
          </div>
      </div>
  </div>

  <!-- Toggle Handle -->
  <!-- svelte-ignore a11y_click_events_have_key_events -->
  <!-- svelte-ignore a11y_no_static_element_interactions -->
  <div class="divider toggle-divider" 
    class:collapsed={!g2Expanded}
    onclick={() => g2Expanded = !g2Expanded}
    title={g2Expanded ? "Collapse Page Settings" : "Expand Page Settings"}
  >
    {@render toggleArrow(g2Expanded)}
  </div>

  <!-- Group 3: HF / Numbering -->
  <!-- Content -->
  <div class="status-group-wrapper" class:collapsed={!g3Expanded}>
      <div class="status-group-inner">
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
                  <Icon data={labelSimpleIcon} width="14" height="14" />
              </span>
              {#if pageLabel.numberingStyle === PageLabelNumberingStyle.NONE && !pageLabel.labelPrefix}
                  None
              {:else}
                  {removeSuffix(generateRulePreview(pageLabel, 1), "...")}
              {/if}
                </div>
                {#if activePopup === 'numbering-style'}
                    <NumberingStylePopup bind:pageLabel onchange={onPopupChange} triggerEl={numberingStyleBtnEl} />
                {/if}
            </div>
      </div>
  </div>

  <!-- Toggle Handle -->
  <!-- svelte-ignore a11y_click_events_have_key_events -->
  <!-- svelte-ignore a11y_no_static_element_interactions -->
  <div class="divider toggle-divider" 
    class:collapsed={!g3Expanded}
    onclick={() => g3Expanded = !g3Expanded}
    title={g3Expanded ? "Collapse Header/Footer" : "Expand Header/Footer"}
  >
    {@render toggleArrow(g3Expanded)}
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

  .status-group-wrapper {
      display: grid;
      grid-template-columns: 1fr;
      transition: grid-template-columns 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  }

  .status-group-wrapper.collapsed {
      grid-template-columns: 0fr;
  }

  .status-group-inner {
      display: flex;
      align-items: stretch;
      overflow: hidden;
      min-width: 0;
      /* Ensure content doesn't wrap or jump during transition */
      white-space: nowrap; 
  }

  .status-item {
      padding: 0 12px;
      display: flex;
      align-items: center;
      gap: 6px;
      cursor: pointer;
      transition: background-color 0.1s;
      border-radius: 3px;
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
      transition: transform 0.2s ease;
      transform-origin: center center;
      flex-shrink: 0;
      will-change: transform;
  }

  .icon.rotated {
      transform: rotate(90deg);
  }

  .spacer { flex: 1; }

  .icon-btn {
      background: transparent;
      border: none;
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
      gap: 2px;
  }

  .hf-summary-content :global(svg) {
      transform: translateY(-1px);
  }

  .divider {
      width: 1px;
      background-color: #bbb;
      margin: 6px 4px;
      flex-shrink: 0;
      cursor: pointer;
      transition: all 0.2s;
      position: relative;
  }

  .toggle-divider {
      width: 12px;
      margin: 0 2px;
      background-color: transparent;
      display: flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;
      position: relative;
  }

  .toggle-divider::before {
      content: '';
      position: absolute;
      left: 5.5px;
      top: 6px;
      bottom: 6px;
      width: 1px;
      background-color: #bbb;
      transition: all 0.2s;
  }

  .toggle-divider.collapsed::before {
      opacity: 0;
  }

  .toggle-divider:hover::before {
      background-color: #1677ff;
      height: 100%;
      top: 0;
      bottom: 0;
      opacity: 1;
  }

  .toggle-arrow {
      opacity: 0;
      color: #bbb;
      transition: opacity 0.2s, color 0.2s;
      z-index: 1;
      background: #f6f7f9;
      display: flex;
      padding: 2px 0;
  }

  .toggle-divider:hover .toggle-arrow {
      opacity: 1;
      color: #1677ff;
  }

  .toggle-divider.collapsed .toggle-arrow {
      opacity: 1;
      color: #888;
  }
  
  .toggle-divider.collapsed:hover .toggle-arrow {
      color: #1677ff;
  }
</style>