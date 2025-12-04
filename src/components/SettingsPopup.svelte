<script lang="ts">
  import StyleList from './StyleList.svelte';
  import ArrowPopup from './controls/ArrowPopup.svelte';
  import StyledInput from './controls/StyledInput.svelte';

  import {PageLabelNumberingStyle} from "@/lib/styleMaps";

  interface Props {
    type: 'offset' | 'pos' | 'style';
    offset?: number;
    insertPos?: number;
    style?: PageLabelNumberingStyle; // 改为枚举名
    onchange?: () => void;
    triggerEl: HTMLElement; // Prop for positioning
  }

  let { 
    type, 
    offset = $bindable(0),
    insertPos = $bindable(1),
    style = $bindable(PageLabelNumberingStyle.NONE),
    onchange,
    triggerEl
  }: Props = $props();

  function handleStyleSelect(s: PageLabelNumberingStyle) {
      style = s;
      onchange?.();
  }
</script>

<ArrowPopup 
  placement="top" 
  minWidth={type === 'style' ? 'auto' : '180px'} 
  padding={type === 'style' ? '4px' : '12px 15px'}
  triggerEl={triggerEl}
>
  {#if type === 'offset'}
      <!-- svelte-ignore a11y_label_has_associated_control -->
      <label>Page Offset</label>
      <StyledInput type="number" bind:value={offset} oninput={onchange} autofocus />
      <div class="hint">Adjusts the starting page number.</div>
  {:else if type === 'pos'}
      <!-- svelte-ignore a11y_label_has_associated_control -->
      <label>Insert Position</label>
      <StyledInput type="number" bind:value={insertPos} />
      <div class="hint">Page number to insert TOC at.</div>
  {:else if type === 'style'}
      <StyleList selected={style} onselect={handleStyleSelect} />
  {/if}
</ArrowPopup>

<style>
  label {
      display: block;
      font-weight: 600;
      margin-bottom: 6px; /* Slightly less margin */
      color: #333;
      font-size: 13px;
  }
  
  .hint {
      margin-top: 6px; /* Slightly less margin */
      font-size: 11px;
      color: #888;
      line-height: 1.4;
  }
</style>