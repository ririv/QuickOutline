<script lang="ts">
  import { type Snippet } from 'svelte';
  import { autoPosition } from '@/lib/actions/autoPosition';
  import { portal } from '@/lib/actions/portal';

  interface Props {
    placement?: 'top' | 'bottom'; // top: popup is above trigger (arrow points down); bottom: popup is below trigger (arrow points up)
    minWidth?: string;
    padding?: string;
    className?: string;
    children?: Snippet;
    triggerEl?: HTMLElement; // The element that triggers the popup, for positioning
    usePortal?: boolean;
    offset?: number;
    popupElement?: HTMLElement; // Exposed DOM element for clickOutside exclusion
    onmouseenter?: (e: MouseEvent) => void;
    onmouseleave?: (e: MouseEvent) => void;
  }

  let { 
    placement = 'top', 
    minWidth = '140px',
    padding = '8px',
    className = '',
    children,
    triggerEl,
    usePortal = true,
    offset = 10,
    popupElement = $bindable(),
    onmouseenter,
    onmouseleave
  }: Props = $props();

  function portalAction(node: HTMLElement, enabled: boolean) {
      if (enabled) {
          return portal(node);
      }
  }
</script>

<!-- svelte-ignore a11y_click_events_have_key_events -->
<!-- svelte-ignore a11y_no_static_element_interactions -->
<div 
  bind:this={popupElement}
  class="arrow-popup {placement} {className}" 
  class:portal={usePortal}
  style:--min-width={minWidth}
  style:--padding={padding}
  style:--popup-offset="{offset}px"
  {onmouseenter}
  {onmouseleave}
  use:portalAction={usePortal}
  use:autoPosition={{ triggerEl, fixed: usePortal, offset }}
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
          z-index: 5000; /* Increased to stay above LeftPane/SplitPane */
          min-width: var(--min-width);
          padding: var(--padding);    /* Initial state: managed by autoPosition */
    left: 0; 
    /* No default transform/centering */
    
    /* Animation only for opacity/vertical slide, NOT horizontal */
    animation: popupFade 0.15s ease-out;
  }

  /* When using portal, JS handles the offset calculation via autoPosition. 
     We reset margins here to avoid double spacing. */
  .arrow-popup.portal {
      margin: 0 !important;
  }

  @keyframes popupFade {
      from { opacity: 0; transform: translateY(4px); }
      to { opacity: 1; transform: translateY(0); }
  }

  /* 
     Placement: TOP
     Arrow points DOWN at bottom.
  */
  .arrow-popup.top {
    bottom: 100%;
    margin-bottom: var(--popup-offset); 
  }
  .arrow-popup.top::after {
    content: "";
    position: absolute;
    top: 100%; 
    /* Dynamic Arrow Position */
    left: var(--arrow-x, 50%);
    margin-left: -5px;
    border-width: 5px;
    border-style: solid;
    border-color: #fff transparent transparent transparent;
  }
  .arrow-popup.top::before {
    content: "";
    position: absolute;
    top: 100%;
    left: var(--arrow-x, 50%);
    margin-left: -6px;
    border-width: 6px;
    border-style: solid;
    border-color: #e1e4e8 transparent transparent transparent;
  }

  /* 
     Placement: BOTTOM
     Arrow points UP at top.
  */
  .arrow-popup.bottom {
    top: 100%;
    margin-top: var(--popup-offset);
  }
  .arrow-popup.bottom::after {
    content: "";
    position: absolute;
    bottom: 100%;
    /* Dynamic Arrow Position */
    left: var(--arrow-x, 50%);
    margin-left: -5px;
    border-width: 5px;
    border-style: solid;
    border-color: transparent transparent #fff transparent;
  }
  .arrow-popup.bottom::before {
    content: "";
    position: absolute;
    bottom: 100%;
    left: var(--arrow-x, 50%);
    margin-left: -6px;
    border-width: 6px;
    border-style: solid;
    border-color: transparent transparent #e1e4e8 transparent;
  }
</style>