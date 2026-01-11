<script lang="ts">
  import type {HeaderFooterConfig} from "@/lib/types/header-footer.ts";

  interface Props {
    position: 'top' | 'bottom';
    label?: string;
    expanded?: boolean;
    content?: HeaderFooterConfig;
    ontoggle?: () => void;
  }

  let { 
    position, 
    label = '', 
    expanded = false, 
    content = {} as HeaderFooterConfig,
    ontoggle 
  }: Props = $props();

  let isHovered = $state(false);

  function handleMouseEnter(e: MouseEvent) {
      // Ignore if mouse button is down (dragging)
      if (e.buttons !== 0) return;
      isHovered = true;
  }

  function handleMouseLeave() {
      isHovered = false;
  }

  function hasContent(config: HeaderFooterConfig) {
      const hasText = Object.entries(config).some(([k, v]) => {
          if (k === 'drawLine') return false;
          return typeof v === 'string' && v.trim().length > 0 && v !== '{p}';
      });
      return hasText || config.drawLine;
  }

</script>

<!-- svelte-ignore a11y_click_events_have_key_events -->
<!-- svelte-ignore a11y_no_static_element_interactions -->
<div 
  class="collapse-trigger {position} {expanded ? 'expanded' : ''} {isHovered ? 'hover' : ''} {content.drawLine ? 'has-content-line' : ''}"
  onclick={ontoggle}
  onmouseenter={handleMouseEnter}
  onmouseleave={handleMouseLeave}
  title={expanded ? `Hide ${label}` : `Show ${label}`}
>
  <div class="indicator-line"></div>
  <div class="static-divider"></div>
  
  <!-- Persistent hint dot (centered) when collapsed and not hovered -->
  <div class="center-dot" class:visible={hasContent(content) && !expanded}></div>

  <div class="content">
    <span class="icon-wrapper">
        <span class="icon {expanded ? 'rotated' : ''}">
            {#if position === 'top'}
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M6 9l6 6 6-6"/></svg>
            {:else}
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M18 15l-6-6-6 6"/></svg>
            {/if}
        </span>
        <!-- Inner dot (follows icon) for hover/expanded state -->
        <span 
          class="dot" 
          class:has-content={hasContent(content)}
        ></span>
    </span>
    <span class="hint-text">{label}</span>
  </div>
</div>

<style>
  .collapse-trigger {
    height: 12px; /* Collapsed height */
    width: 100%;
    cursor: pointer;
    display: flex;
    justify-content: center;
    align-items: center;
    transition: height 0.2s ease;
    will-change: height;  /* Hint for browser optimization */
    position: relative;
    background-color: #fdfdfd; /* Very subtle background */
    border-bottom: 1px solid transparent;
    border-top: 1px solid transparent;
    user-select: none;
    overflow: hidden;
    z-index: 10;
  }

  /* Hover State (Managed by JS) */
  .collapse-trigger.hover {
      height: 24px;
      background-color: #f0f0f0;
  }

  /* Expanded State */
  .collapse-trigger.expanded {
      height: 24px; /* Keep height visible when expanded */
      background-color: #f5f5f5;
  }
  
  .collapse-trigger.expanded.top {
      border-bottom-color: #e0e0e0;
  }
  
  .collapse-trigger.expanded.bottom {
      border-top-color: #e0e0e0;
  }

  .indicator-line {
      position: absolute;
      left: 0;
      right: 0;
      height: 2px;
      background-color: transparent;
      transition: background-color 0.2s;
  }
  
  .collapse-trigger.hover .indicator-line {
      background-color: rgba(55, 143, 255, 0.75);
      /* opacity removed to prevent flash */
  }
  .collapse-trigger.top .indicator-line { bottom: 0; }
  .collapse-trigger.bottom .indicator-line { top: 0; }

  /* Static Divider (Short, aligned line) */
  .static-divider {
      position: absolute;
      left: 32px;
      width: calc(100% - 64px);
      height: 1px;
      background-color: #eee;
      transition: opacity 0.2s;
      pointer-events: none;
  }
  .collapse-trigger.top .static-divider { bottom: 0; }
  .collapse-trigger.bottom .static-divider { top: 0; }

  .collapse-trigger.has-content-line .static-divider {
      background-color: #333;
  }

  /* Hide static divider on interaction to show indicator line */
  .collapse-trigger.hover .static-divider,
  .collapse-trigger.expanded .static-divider {
      opacity: 0;
  }

  .content {
      display: flex;
      align-items: center;
      gap: 6px;
      opacity: 0; /* Hidden by default */
      transform: scale(0.9);
      transition: all 0.2s;
  }

  .collapse-trigger.hover .content,
  .collapse-trigger.expanded .content {
      opacity: 1;
      transform: scale(1);
  }

  .icon-wrapper {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 16px;
      height: 16px;
      position: relative; /* For dot positioning */
  }

  .icon {
      font-size: 10px; /* Keep font-size to avoid affecting SVG directly */
      color: #666;
      transition: transform 0.2s ease;
      display: inline-block;
      will-change: transform; /* Hint for browser optimization */
      backface-visibility: hidden; /* Prevent flickering */
  }
  
  /* Rotate icon when expanded based on position */
  .collapse-trigger.top .icon.rotated {
      transform: rotate(180deg);
  }
  
  .collapse-trigger.bottom .icon.rotated {
      transform: rotate(180deg);
  }

  .hint-text {
      font-size: 11px;
      color: #666;
      font-weight: 500;
  }

  /* Center Dot (Default State) */
  .center-dot {
      position: absolute;
      top: 50%;
      left: 50%;
      transform: translate(-50%, -50%);
      width: 5px;
      height: 5px;
      background-color: #ccc; /* Subtle gray */
      border-radius: 50%;
      opacity: 0;
      transition: opacity 0.2s;
      pointer-events: none;
      z-index: 5;
  }
  
  .center-dot.visible {
      opacity: 1;
  }
  
  /* Hide center dot on hover or expand */
  .collapse-trigger.hover .center-dot,
  .collapse-trigger.expanded .center-dot {
      opacity: 0 !important;
  }

  /* Inner Dot (Hover/Expanded State) */
  .dot {
      position: absolute;
      top: 50%;
      left: -8px; 
      transform: translateY(-50%);
      width: 5px;
      height: 5px;
      background-color: #1677ff; /* Bright blue */
      border-radius: 50%;
      opacity: 0;
      transition: opacity 0.2s;
      pointer-events: none;
  }
  
  /* Only show inner dot if has-content is true AND (hovered OR expanded) */
  /* Note: .content itself handles the fade-in of the wrapper. 
     We just need to ensure .dot is visible within that. */
  
  .dot.has-content {
      /* Default hidden inside content */
      opacity: 1; 
  }
  
  /* If content is hidden (opacity 0), dot is hidden. 
     If content is visible (hover/expanded), dot is visible. 
     But we want to hide dot when expanded? 
     "expanded state: hide the dot if it's there, as editor is visible" - user said previously.
  */
  
  /* Hover state: make it visible and bright blue */
  .collapse-trigger.hover .dot.has-content {
      opacity: 1;
      background-color: #1677ff;
  }

  /* Expanded state: hide the dot because the full editor is open */
  .collapse-trigger.expanded .dot.has-content {
      opacity: 0;
  }
</style>
