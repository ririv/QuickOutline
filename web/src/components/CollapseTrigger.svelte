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
    {#if hasContent && !expanded}
      <span class="dot"></span>
    {/if}
    <span class="icon">
        {#if position === 'top'}
            {expanded ? '▲' : '▼'}
        {:else}
            {expanded ? '▼' : '▲'}
        {/if}
    </span>
    {#if !expanded}
        <span class="hint-text">{label}</span>
    {/if}
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
      height: 16px; /* Slightly taller when expanded to serve as a close bar */
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

  .icon {
      font-size: 10px;
      color: #666;
  }

  .hint-text {
      font-size: 11px;
      color: #666;
      font-weight: 500;
  }

  .dot {
      width: 6px;
      height: 6px;
      background-color: #1677ff;
      border-radius: 50%;
      /* Make dot visible even when not hovered if we want, but user asked for clean interface. 
         Let's keep it inside content for now, or move it out if needed. */
  }
  
  /* If has content, show a subtle indicator even when collapsed and not hovered? 
     The requirement was "悬浮后会有提示". Let's stick to hover reveal mostly.
     But a small persistent dot might be useful. */
  
</style>
