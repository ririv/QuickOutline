<script lang="ts">
  import StyleList from './StyleList.svelte';
  import ArrowPopup from './controls/ArrowPopup.svelte';

  interface Props {
    type: 'offset' | 'pos' | 'style';
    offset?: number;
    insertPos?: number;
    style?: string;
    onchange?: () => void;
    triggerEl: HTMLElement; // Prop for positioning
  }

  let { 
    type, 
    offset = $bindable(0),
    insertPos = $bindable(1),
    style = $bindable('None'),
    onchange,
    triggerEl
  }: Props = $props();

  function handleStyleSelect(s: string) {
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
      <!-- svelte-ignore a11y_autofocus -->
      <input type="number" bind:value={offset} oninput={onchange} class="styled-input" autofocus />
      <div class="hint">Adjusts the starting page number.</div>
  {:else if type === 'pos'}
      <!-- svelte-ignore a11y_label_has_associated_control -->
      <label>Insert Position</label>
      <input type="number" bind:value={insertPos} class="styled-input" />
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

  .styled-input {
      width: 100%;
      padding: 6px 8px; /* Slightly less padding */
      border: 1px solid #ddd;
      border-radius: 4px;
      font-size: 13px;
      box-sizing: border-box;
      outline: none;
      transition: border 0.2s;
  }
  .styled-input:focus {
      border-color: #1677ff;
      box-shadow: 0 0 0 2px rgba(22, 119, 255, 0.1);
  }
</style>