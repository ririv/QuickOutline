<script lang="ts">
  interface Props {
    selected?: string;
    onselect?: (s: string) => void;
  }

  let { selected = 'None', onselect }: Props = $props();

  const styles = ['None', 'Decimal', 'Roman Lower', 'Roman Upper', 'Alpha Lower', 'Alpha Upper'];
</script>

<div class="style-list">
  {#each styles as s}
      <!-- svelte-ignore a11y_click_events_have_key_events -->
      <!-- svelte-ignore a11y_no_static_element_interactions -->
      <div class="style-option {selected === s ? 'selected' : ''}" onclick={() => onselect?.(s)}>
          {s}
          {#if selected === s}<span>✓</span>{/if}
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
