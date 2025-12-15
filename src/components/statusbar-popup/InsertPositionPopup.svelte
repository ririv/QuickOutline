<script lang="ts">
  import ArrowPopup from '../controls/ArrowPopup.svelte';
  import StyledInput from '../controls/StyledInput.svelte';

  interface Props {
    insertPos?: number;
    triggerEl: HTMLElement | undefined;
  }

  let { 
    insertPos = $bindable(1),
    triggerEl
  }: Props = $props();
</script>

<ArrowPopup 
  placement="top" 
  minWidth="180px" 
  padding="12px 15px"
  {triggerEl}
>
  <!-- svelte-ignore a11y_label_has_associated_control -->
  <label>Insert Position</label>
  <StyledInput type="number" bind:value={insertPos} numericType="unsigned-integer" />
  <div class="hint">Page number to insert TOC at.</div>
</ArrowPopup>

<style>
  label {
      display: block;
      font-weight: 600;
      margin-bottom: 6px;
      color: #333;
      font-size: 13px;
  }
  
  .hint {
      margin-top: 6px;
      font-size: 11px;
      color: #888;
      line-height: 1.4;
  }
</style>
