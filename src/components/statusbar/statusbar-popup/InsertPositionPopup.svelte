<script lang="ts">
  import ArrowPopup from '../../controls/ArrowPopup.svelte';
  import StyledInput from '../../controls/StyledInput.svelte';
  import StyledSwitch from '../../controls/StyledSwitch.svelte';

  interface Props {
    insertPos?: number;
    autoCorrect?: boolean;
    showAutoCorrect?: boolean; // Keep this toggle
    triggerEl: HTMLElement | undefined;
    onchange?: () => void;
  }

  let { 
    insertPos = $bindable(1),
    autoCorrect = $bindable(true),
    showAutoCorrect = false,
    triggerEl,
    onchange
  }: Props = $props();
</script>

<!--没有加 trackTrigger={false}，因为它在最左边，触发位置本就因为防止移除隐藏被偏移了-->
<ArrowPopup 
  placement="top" 
  minWidth="180px" 
  padding="12px 15px"
  {triggerEl}
>
  <!-- svelte-ignore a11y_label_has_associated_control -->
  <label>Insert Position</label>
  <StyledInput type="number" bind:value={insertPos} numericType="unsigned-integer" {onchange} />
  <div class="hint">Insert before this page.</div>
  
  {#if showAutoCorrect}
    <div class="divider"></div>
    
    <div class="switch-row">
        <span>Auto-correct Page Numbers</span>
        <StyledSwitch bind:checked={autoCorrect} {onchange} size="small" />
    </div>
    <div class="hint">Adjust references for insertion.</div>
  {/if}
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
  
  .divider {
      height: 1px;
      background-color: #eee;
      margin: 12px 0;
  }
  
  .switch-row {
      display: flex;
      justify-content: space-between;
      align-items: center;
      font-size: 13px;
      font-weight: 600;
      color: #333;
  }
</style>
