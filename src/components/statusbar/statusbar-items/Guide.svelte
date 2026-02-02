<script lang="ts">
  import Icon from '@/components/Icon.svelte';
  import Tooltip from '@/components/Tooltip.svelte';

  interface Props {
    onGuide?: () => void;
  }

  let { onGuide }: Props = $props();
</script>

{#if onGuide}
    <Tooltip content="Syntax Guide" position="top">
        <button class="icon-btn guide-btn" onclick={onGuide}>
            <Icon name="help" width="16" height="16" />
        </button>
    </Tooltip>
{/if}

<style>
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

  .guide-btn {
      color: #666;
      margin-right: 12px;
  }
  .guide-btn:hover {
      background-color: #e1e4e8;
      color: #333;
  }
</style>
