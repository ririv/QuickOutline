<script lang="ts">
  interface Props {
    position: 'top' | 'bottom';
    label?: string;
    expanded?: boolean;
    hasContent?: boolean;
    ontoggle?: () => void;
  }

  let { 
    position, 
    label = '', 
    expanded = false, 
    hasContent = false,
    ontoggle 
  }: Props = $props();
</script>

<!-- svelte-ignore a11y_click_events_have_key_events -->
<!-- svelte-ignore a11y_no_static_element_interactions -->
<div 
  class="collapse-trigger {position} {expanded ? 'expanded' : ''}" 
  onclick={ontoggle}
  title={expanded ? `Hide ${label}` : `Show ${label}`}
>
  <div class="indicator-line"></div>
  <div class="content">
    <span class="icon-wrapper">
        <span class="icon {expanded ? 'rotated' : ''}">
            {#if position === 'top'}
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M6 9l6 6 6-6"/></svg>
            {:else}
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M18 15l-6-6-6 6"/></svg>
            {/if}
        </span>
        <span class="dot {hasContent && !expanded ? 'visible' : ''}"></span>
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
    transition: all 0.2s ease;
    position: relative;
    background-color: #fdfdfd; /* Very subtle background */
    border-bottom: 1px solid transparent;
    border-top: 1px solid transparent;
    user-select: none;
    overflow: hidden;
    z-index: 10;
  }

  /* Top Trigger Styles */
  .collapse-trigger.top {
      border-bottom-color: #eee;
  }
  
  /* Bottom Trigger Styles */
  .collapse-trigger.bottom {
      border-top-color: #eee;
  }

  /* Hover State */
  .collapse-trigger:hover {
      height: 24px;
      background-color: #f0f0f0;
  }

  /* Expanded State */
  .collapse-trigger.expanded {
      height: 24px; /* Keep height visible when expanded */
      background-color: #f5f5f5;
      border-color: #e0e0e0;
  }

  .indicator-line {
      position: absolute;
      left: 0;
      right: 0;
      height: 2px;
      background-color: transparent;
      transition: background-color 0.2s;
  }
  
  .collapse-trigger:hover .indicator-line {
      background-color: #1677ff;
      opacity: 0.3;
  }
  .collapse-trigger.top .indicator-line { bottom: 0; }
  .collapse-trigger.bottom .indicator-line { top: 0; }

  .content {
      display: flex;
      align-items: center;
      gap: 6px;
      opacity: 0; /* Hidden by default */
      transform: scale(0.9);
      transition: all 0.2s;
  }

  .collapse-trigger:hover .content,
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

  .dot {
      position: absolute;
      top: 50%;
      left: -8px; /* Adjust as needed for spacing */
      transform: translateY(-50%);
      width: 5px;
      height: 5px;
      background-color: #1677ff;
      border-radius: 50%;
      opacity: 0;
      transition: opacity 0.2s;
      pointer-events: none;
  }
  
  .dot.visible {
      opacity: 1;
  }
</style>
