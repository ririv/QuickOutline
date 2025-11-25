<script lang="ts">
  import { type Snippet } from 'svelte';

  interface Props {
    placement?: 'top' | 'bottom'; // top: popup is above trigger (arrow points down); bottom: popup is below trigger (arrow points up)
    minWidth?: string;
    padding?: string;
    className?: string;
    children?: Snippet;
  }

  let { 
    placement = 'top', 
    minWidth = '140px',
    padding = '8px',
    className = '',
    children
  }: Props = $props();
</script>

<!-- svelte-ignore a11y_click_events_have_key_events -->
<!-- svelte-ignore a11y_no_static_element_interactions -->
<div 
  class="arrow-popup {placement} {className}" 
  style:--min-width={minWidth}
  style:--padding={padding}
  onclick={(e) => e.stopPropagation()}
>
  {@render children?.()}
</div>

<style>
  .arrow-popup {
    position: absolute;
    background: #fff;
    border: 1px solid #e1e4e8;
    box-shadow: 0 4px 16px rgba(0,0,0,0.12);
    border-radius: 6px;
    z-index: 100;
    min-width: var(--min-width);
    padding: var(--padding);
    
    /* Center horizontally relative to parent wrapper */
    left: 50%;
    transform: translateX(-50%);
    
    animation: popupFade 0.15s ease-out;
  }

  @keyframes popupFade {
      from { opacity: 0; transform: translateX(-50%) translateY(4px); }
      to { opacity: 1; transform: translateX(-50%) translateY(0); }
  }

  /* 
     Placement: TOP
     Popup is ABOVE the trigger. 
     Arrow should be at the BOTTOM of the popup, pointing DOWN.
     Margin-bottom ensures space from trigger.
  */
  .arrow-popup.top {
    bottom: 100%;
    margin-bottom: 10px; 
  }
  .arrow-popup.top::after {
    content: "";
    position: absolute;
    top: 100%; /* Arrow at bottom of popup */
    left: 50%;
    margin-left: -5px;
    border-width: 5px;
    border-style: solid;
    border-color: #fff transparent transparent transparent;
  }
  /* Border outline for arrow */
  .arrow-popup.top::before {
    content: "";
    position: absolute;
    top: 100%;
    left: 50%;
    margin-left: -6px;
    border-width: 6px;
    border-style: solid;
    border-color: #e1e4e8 transparent transparent transparent;
  }

  /* 
     Placement: BOTTOM
     Popup is BELOW the trigger.
     Arrow should be at the TOP of the popup, pointing UP.
     Margin-top ensures space from trigger.
  */
  .arrow-popup.bottom {
    top: 100%;
    margin-top: 10px;
  }
  .arrow-popup.bottom::after {
    content: "";
    position: absolute;
    bottom: 100%; /* Arrow at top of popup */
    left: 50%;
    margin-left: -5px;
    border-width: 5px;
    border-style: solid;
    border-color: transparent transparent #fff transparent;
  }
  /* Border outline for arrow */
  .arrow-popup.bottom::before {
    content: "";
    position: absolute;
    bottom: 100%;
    left: 50%;
    margin-left: -6px;
    border-width: 6px;
    border-style: solid;
    border-color: transparent transparent #e1e4e8 transparent;
  }
</style>
