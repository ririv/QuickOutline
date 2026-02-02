<script lang="ts">
  interface Props {
    expanded?: boolean;
    title: string;
    children?: import('svelte').Snippet;
  }

  let { 
    expanded = $bindable(true), 
    title, 
    children 
  }: Props = $props();
</script>

<div class="status-group-wrapper" class:collapsed={!expanded}>
    <div class="status-group-inner">
        {@render children?.()}
    </div>
</div>

<!-- svelte-ignore a11y_click_events_have_key_events -->
<!-- svelte-ignore a11y_no_static_element_interactions -->
<div class="divider toggle-divider" 
  class:collapsed={!expanded}
  onclick={() => expanded = !expanded}
  title={expanded ? "Collapse " + title : "Expand " + title}
>
    <div class="toggle-arrow">
        <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3">
            <path d={expanded ? "M15 18l-6-6 6-6" : "M9 18l6-6-6-6"}></path>
        </svg>
    </div>
</div>

<style>
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