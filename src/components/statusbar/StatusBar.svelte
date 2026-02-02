<script lang="ts">
  import { setContext } from 'svelte';
  import { clickOutside } from '@/lib/actions/clickOutside.ts';

  interface Props {
    children?: import('svelte').Snippet;
    class?: string;
  }

  let { children, class: className }: Props = $props();

  let activePopupId = $state<string | null>(null);

  setContext('STATUS_BAR_CTX', {
    get activeId() { return activePopupId; },
    toggle: (id: string) => {
        activePopupId = activePopupId === id ? null : id;
    }
  });
</script>

<div class="status-bar {className || ''}" use:clickOutside={() => activePopupId = null}>
    {@render children?.()}
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
</style>