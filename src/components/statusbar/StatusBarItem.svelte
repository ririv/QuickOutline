<script lang="ts">
  import { getContext, type Snippet } from 'svelte';

  interface Props {
    id: string; // Unique ID for popup handling
    title: string;
    icon: Snippet;
    children?: Snippet;
    popup?: Snippet<[HTMLElement]>;
  }

  let { 
    id,
    title, 
    icon, 
    children, 
    popup 
  }: Props = $props();

  let triggerEl = $state<HTMLElement>();
  
  // Consume StatusBar Context
  const ctx = getContext('STATUS_BAR_CTX') as { activeId: string | null, toggle: (id: string) => void };
  let active = $derived(ctx.activeId === id);
</script>

<div class="status-item-wrapper">
    <!-- svelte-ignore a11y_click_events_have_key_events -->
    <!-- svelte-ignore a11y_no_static_element_interactions -->
    <div 
        bind:this={triggerEl}
        class="status-item" 
        class:active={active}
        onclick={() => ctx.toggle(id)}
        {title}
    >
        <span class="icon">
            {@render icon()}
        </span>
        {@render children?.()}
    </div>
    
    {#if active && triggerEl}
        {@render popup?.(triggerEl)}
    {/if}
</div>

<style>
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
        border-radius: 3px;
        white-space: nowrap;
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
    
    /* Global support for rotated class if used inside the icon snippet */
    .icon :global(.rotated) {
        transform: rotate(90deg);
    }
</style>