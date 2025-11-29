<script lang="ts">
  import type { PageLabelNumberingStyle } from '@/lib/api/rpc';
  import { pageLabelStyleMap } from '@/lib/styleMaps';

  interface Props {
    selected?: PageLabelNumberingStyle;
    onselect?: (s: PageLabelNumberingStyle) => void;
  }

  let { selected = PageLabelNumberingStyle.NONE, onselect }: Props = $props();
  
  const styles = pageLabelStyleMap.getAllStyles(); // 获取 [{ displayText, enumName }, ...]

</script>

<div class="style-list">
  {#each styles as s}
      <!-- svelte-ignore a11y_click_events_have_key_events -->
      <!-- svelte-ignore a11y_no_static_element_interactions -->
      <div class="style-option {selected === s.enumName ? 'selected' : ''}" onclick={() => onselect?.(s.enumName)}>
          {s.displayText}
          {#if selected === s.enumName}<span>✓</span>{/if}
      </div>
  {/each}
</div>

<style>
  .style-list {
      padding: 0;
      min-width: 160px;
  }
  
  .style-option {
      padding: 6px 12px;
      cursor: pointer;
      display: flex;
      justify-content: space-between;
      align-items: center;
      font-size: 13px;
      color: #333;
      border-radius: 4px;
      margin-bottom: 2px;
  }
  
  .style-option:last-child {
      margin-bottom: 0;
  }
  
  .style-option:hover {
      background-color: #f0f7ff;
      color: #1677ff;
  }
  
  .style-option.selected {
      font-weight: 500;
      color: #1677ff;
      background-color: #e6f4ff;
  }
</style>
