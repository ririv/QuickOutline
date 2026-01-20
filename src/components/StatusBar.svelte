<script lang="ts">
  import OffsetPopup from './statusbar-popup/OffsetPopup.svelte';
  import InsertPositionPopup from './statusbar-popup/InsertPositionPopup.svelte';
  import PageLabelPopup from './statusbar-popup/PageLabelPopup.svelte';
  import PageSizePopup from './statusbar-popup/PageSizePopup.svelte';
  import PageMarginsPopup from './statusbar-popup/PageMarginsPopup.svelte';
  import HeaderFooterPopup from './statusbar-popup/HeaderFooterPopup.svelte';
  import ColumnLayoutPopup from './statusbar-popup/ColumnLayoutPopup.svelte';
  import StatusBarGroup from './StatusBarGroup.svelte';
  import StatusBarItem from './StatusBarItem.svelte';
  import Icon from '@/components/Icon.svelte';
  import Tooltip from '@/components/Tooltip.svelte';
  import { clickOutside } from '@/lib/actions/clickOutside';
  import {PageLabelNumberingStyle, generateRulePreview, type PageLabel} from "@/lib/types/page-label.ts";
  import { type PageLayout, defaultPageLayout, type HeaderFooterLayout, defaultHeaderFooterLayout, PAGE_SIZES_MM, defaultColumnLayout, type ColumnLayoutConfig } from "@/lib/types/page";
  import labelSimpleIcon from '@/assets/icons/label-simple.svg?raw';
  import doubleColumnIcon from '@/assets/icons/double-column.svg?raw';
  import type { PageSizeDetectionState } from '@/lib/pdf-processing/usePdfPageSizeDetection.svelte';

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
    columnLayout?: ColumnLayoutConfig;
    hfLayout?: HeaderFooterLayout;
    showOffset?: boolean;
    layoutDetection?: PageSizeDetectionState;
    mode?: 'new' | 'edit';
    onGenerate?: () => void;
    onParamChange?: () => void;
    onGuide?: () => void;
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
    columnLayout = $bindable(defaultColumnLayout),
    hfLayout = $bindable(defaultHeaderFooterLayout),
    showOffset = true,
    layoutDetection,
    mode = 'edit',
    onGenerate,
    onParamChange,
    onGuide
  }: Props = $props();

  type PopupType = 'pagenum-offset' | 'insert-pos' | 'page-label' | 'page-size' | 'page-margins' | 'header-footer' | 'column-layout';
  let activePopup = $state<PopupType | null>(null);
  
  // Group expansion states
  let g1Expanded = $state(true);
  let g2Expanded = $state(true);
  let g3Expanded = $state(true);
  let g4Expanded = $state(true);

  let pageSizeAutoDetect = $state(true);

  let barElement: HTMLElement;

  function togglePopup(type: PopupType) {
      if (activePopup === type) activePopup = null;
      else activePopup = type;
  }

  function onPopupChange() {
      onParamChange?.();
  }

  let pageSizeSummary = $derived.by(() => {
      const format = (num: number) => {
          const rounded = Math.round(num * 10) / 10;
          return Number.isInteger(rounded) ? rounded.toString() : rounded.toFixed(1);
      };

      let w: number;
      let h: number;
      let matchedName: string = pageLayout.pageSize.size;

      // In Auto mode, the actual detection might differ from the selected size
      if (pageSizeAutoDetect && layoutDetection?.actualDimensions) {
          w = layoutDetection.actualDimensions.width;
          h = layoutDetection.actualDimensions.height;
          
          // Match standard size name for the auto-detected dimensions
          const rw = Math.round(w * 10) / 10;
          const rh = Math.round(h * 10) / 10;

          const matched = Object.entries(PAGE_SIZES_MM).find(([_, [sw, sh]]) => 
              (rw === sw && rh === sh) || (rw === sh && rh === sw)
          );
          if (matched) matchedName = matched[0] as any;
          else matchedName = '';
      } else {
          const std = PAGE_SIZES_MM[pageLayout.pageSize.size];
          if (pageLayout.pageSize.orientation === 'landscape') {
              w = std[1];
              h = std[0];
          } else {
              w = std[0];
              h = std[1];
          }
      }

      const orientation = w > h ? 'Landscape' : 'Portrait';
      const dimStr = `${format(w)}×${format(h)}mm`;
      
      return {
          display: matchedName || dimStr,
          full: matchedName ? `${dimStr} (${matchedName}, ${orientation})` : dimStr,
          orientation: orientation.toLowerCase() as 'portrait' | 'landscape'
      };
  });

  let marginSummary = $derived.by(() => {
      const { top, bottom, left, right } = pageLayout.margins;
      let marginText = '';
      
      if (top === bottom && left === right && top === left) {
          marginText = `${top}mm`;
      } else if (top === bottom && left === right) {
          marginText = `${top}x${left}mm`;
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

  {#if showOffset}
      <StatusBarGroup bind:expanded={g1Expanded} title="Offset">
          <StatusBarItem 
              active={activePopup === 'pagenum-offset'} 
              title="Set Page Offset"
              onclick={() => togglePopup('pagenum-offset')}
          >
              {#snippet icon()}
                  <Icon name="offset" width="14" height="14" />
              {/snippet}
              Offset {offset}
              {#snippet popup(triggerEl)}
                  <OffsetPopup bind:offset onchange={onParamChange} {triggerEl} />
              {/snippet}
          </StatusBarItem>
      </StatusBarGroup>
  {/if}

  <!-- Group 2: Pos / Size / Margins -->
  <StatusBarGroup bind:expanded={g2Expanded} title="Page Settings">
      <StatusBarItem
          active={activePopup === 'insert-pos'}
          title="Set Insert Position"
          onclick={() => togglePopup('insert-pos')}
      >
          {#snippet icon()}
              <Icon name="insert-position" width="14" height="14" />
          {/snippet}
          Pos {insertion.pos}
          {#snippet popup(triggerEl)}
              <InsertPositionPopup 
                bind:insertPos={insertion.pos} 
                bind:autoCorrect={insertion.autoCorrect} 
                showAutoCorrect={insertion.showAutoCorrect} 
                {triggerEl} 
                onchange={onParamChange}
              />
          {/snippet}
      </StatusBarItem>

      <StatusBarItem
          active={activePopup === 'page-size'}
          title="Page Size: {pageLayout.pageSize.size}, {pageLayout.pageSize.orientation}"
          onclick={() => togglePopup('page-size')}
      >
          {#snippet icon()}
              <!-- svelte-ignore css_unused_selector -->
              <div class="icon-rotator" class:rotated={pageLayout.pageSize.orientation === 'landscape'}>
                  <Icon name="page-setup" width="16" height="16" />
              </div>
          {/snippet}
          {pageSizeSummary.display}
          {#snippet popup(triggerEl)}
              <PageSizePopup 
                bind:pageSize={pageLayout.pageSize} 
                bind:autoDetect={pageSizeAutoDetect}
                onchange={onPopupChange} 
                {triggerEl} 
                {mode}
                detection={layoutDetection}
              />
          {/snippet}
      </StatusBarItem>

      <StatusBarItem
          active={activePopup === 'page-margins'}
          title="Margins: {marginSummary}"
          onclick={() => togglePopup('page-margins')}
      >
          {#snippet icon()}
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                  <rect x="3" y="3" width="18" height="18" rx="1" stroke-opacity="0.2"></rect>
                  <path d="M7 3v18M17 3v18M3 7h18M3 17h18" stroke-width="1.5" stroke-dasharray="2 2"></path>
              </svg>
          {/snippet}
          {marginSummary}
          {#snippet popup(triggerEl)}
              <PageMarginsPopup bind:margins={pageLayout.margins} onchange={onPopupChange} {triggerEl} />
          {/snippet}
      </StatusBarItem>

      <StatusBarItem
          active={activePopup === 'column-layout'}
          title="Column Layout"
          onclick={() => togglePopup('column-layout')}
      >
          {#snippet icon()}
              <Icon data={doubleColumnIcon} width="14" height="14" />
          {/snippet}
          {(columnLayout.count || 1) === 1 ? '1 Col' : `${columnLayout.count} Cols`}
          {#snippet popup(triggerEl)}
              <ColumnLayoutPopup bind:layout={columnLayout} onchange={onParamChange} {triggerEl} />
          {/snippet}
      </StatusBarItem>
  </StatusBarGroup>

  <!-- Group 3: HF -->
  <StatusBarGroup bind:expanded={g3Expanded} title="Header/Footer">
      <StatusBarItem
          active={activePopup === 'header-footer'}
          title="Header & Footer Position"
          onclick={() => togglePopup('header-footer')}
      >
          {#snippet icon()}
              <Icon name="header-footer" width="14" height="14" />
          {/snippet}
          
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
          
          {#snippet popup(triggerEl)}
              <HeaderFooterPopup bind:layout={hfLayout} onchange={onParamChange} {triggerEl} />
          {/snippet}
      </StatusBarItem>
  </StatusBarGroup>

  <!-- Group 4: Numbering -->
  <StatusBarGroup bind:expanded={g4Expanded} title="Numbering">
      <StatusBarItem
          active={activePopup === 'page-label'}
          title="Set Numbering Style"
          onclick={() => togglePopup('page-label')}
      >
          {#snippet icon()}
              <Icon data={labelSimpleIcon} width="14" height="14" />
          {/snippet}

          {#if pageLabel.numberingStyle === PageLabelNumberingStyle.NONE && !pageLabel.labelPrefix}
              None
          {:else}
              {removeSuffix(generateRulePreview(pageLabel, 1), "...")}
          {/if}
          
          {#snippet popup(triggerEl)}
              <PageLabelPopup bind:pageLabel onchange={onPopupChange} {triggerEl} />
          {/snippet}
      </StatusBarItem>
  </StatusBarGroup>

  <div class="spacer"></div>
  
  {#if onGuide}
      <Tooltip content="Syntax Guide" position="top">
          <button class="icon-btn guide-btn" onclick={onGuide}>
              <Icon name="help" width="16" height="16" />
          </button>
      </Tooltip>
  {/if}

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

  .guide-btn {
      color: #666;
      margin-right: 12px;
  }
  .guide-btn:hover {
      background-color: #e1e4e8;
      color: #333;
  }

  .hf-summary-content {
      display: inline-flex;
      align-items: center;
      gap: 2px;
  }

  .hf-summary-content :global(svg) {
      transform: translateY(-1px);
  }
  
  .icon-rotator {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 100%;
      height: 100%;
      transition: transform 0.2s ease;
      will-change: transform;
  }

  .icon-rotator.rotated {
      transform: rotate(90deg);
  }
</style>