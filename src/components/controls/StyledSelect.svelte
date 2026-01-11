<script lang="ts" generics="T">
  import { clickOutside } from '@/lib/actions/clickOutside';

  interface Props<T> {
    options: T[];
    value?: any;
    placeholder?: string;
    disabled?: boolean;
    onchange?: (val: any) => void;
    valueKey?: string;
    displayKey?: string; // Key for the label shown in the closed state
    optionKey?: string;  // Key for the label shown in the dropdown options
    item?: import('svelte').Snippet<[T]>; // Custom item renderer
    placement?: 'top' | 'bottom';
    maxHeight?: string;
  }

  let { 
    options = [], 
    value = $bindable(), 
    placeholder = 'Select...',
    disabled = false,
    onchange,
    valueKey = 'value',
    displayKey = 'display', // New default, assumes options have a 'display' prop
    optionKey = 'display',   // New default
    item,
    placement = 'bottom',
    maxHeight = '200px'
  }: Props<T> = $props();

  let isOpen = $state(false);

  function getLabel(opt: T, key: string = 'label'): string { // key is now explicitly passed
    if (typeof opt === 'object' && opt !== null && key in opt) {
      return String((opt as any)[key]);
    }
    return String(opt);
  }

  function getValue(opt: T): any {
    if (typeof opt === 'object' && opt !== null && valueKey in opt) {
      return (opt as any)[valueKey];
    }
    return opt;
  }

  // Find the label for the current value (uses displayKey)
  let currentLabel = $derived.by(() => {
    if (value === undefined || value === null) return '';
    const selectedOpt = options.find(opt => getValue(opt) === value);
    return selectedOpt ? getLabel(selectedOpt, displayKey) : ''; // Use displayKey here
  });

  function toggle() {
    if (!disabled) isOpen = !isOpen;
  }

  function select(opt: T) {
    const val = getValue(opt);
    value = val;
    isOpen = false;
    onchange?.(val);
  }

  function close() {
    isOpen = false;
  }
</script>

<div class="select-container" use:clickOutside={close}>
  <div 
      class="select-trigger {isOpen ? 'open' : ''} {disabled ? 'disabled' : ''}" 
      onclick={toggle}
      role="button"
      tabindex="0"
      onkeydown={(e) => { if(e.key === 'Enter') toggle(); }}
  >
      <span class="value {value ? '' : 'placeholder'}">{currentLabel || value || placeholder}</span>
      <span class="arrow">
          <svg viewBox="0 0 1024 1024" width="12" height="12">
              <path d="M831.872 340.864 512 652.672 192.128 340.864a30.592 30.592 0 0 0-42.752 0 29.12 29.12 0 0 0 0 41.6L489.664 714.24a32 32 0 0 0 44.672 0l340.288-331.712a29.12 29.12 0 0 0 0-41.728 30.592 30.592 0 0 0-42.752 0z" fill="#999"></path>
          </svg>
      </span>
  </div>
  
  {#if isOpen}
      <div class="select-dropdown {placement}" style:max-height={maxHeight}>
          {#each options as opt}
              {@const optVal = getValue(opt)}
              <div 
                  class="select-option {value === optVal ? 'selected' : ''}" 
                  onclick={() => select(opt)}
                  role="option"
                  tabindex="0"
                  aria-selected={value === optVal}
                  onkeydown={(e) => { if(e.key === 'Enter') select(opt); }}
              >
                  {#if item}
                      {@render item(opt)}
                  {:else}
                      {getLabel(opt, optionKey)}
                  {/if}
                  {#if value === optVal}
                      <span class="check">
                          <svg viewBox="0 0 1024 1024" width="12" height="12"><path d="M912 190h-69.9c-9.8 0-19.1 4.5-25.1 12.2L404.7 724.5 207 474a32 32 0 0 0-25.1-12.2H112c-6.7 0-10.4 7.7-6.3 12.9l273.9 347c12.8 16.2 37.4 16.2 50.3 0l488.4-618.9c4.1-5.1 0.4-12.8-6.3-12.8z" fill="#1677ff"></path></svg>
                      </span>
                  {/if}
              </div>
          {/each}
      </div>
  {/if}
</div>

<style>
  .select-container {
      position: relative;
      width: 100%;
      font-size: 14px;
  }

  .select-trigger {
      background-color: white;
      border: 1px solid #d9d9d9;
      border-radius: 4px;
      padding: 5px 11px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      cursor: pointer;
      transition: all 0.2s;
      min-height: 32px;
      box-sizing: border-box;
      user-select: none;
  }

  .select-trigger:hover {
      border-color: #409eff;
  }

  .select-trigger.open {
      border-color: #409eff;
      box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
  }
  
  .select-trigger.disabled {
      background-color: #f5f7fa;
      color: #c0c4cc;
      cursor: not-allowed;
      border-color: #e4e7ed;
  }

  .value {
      color: #333;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
  }
  
  .value.placeholder {
      color: #ccc;
  }

  .arrow {
      display: flex;
      align-items: center;
      transition: transform 0.3s;
  }
  
  .select-trigger.open .arrow {
      transform: rotate(180deg);
  }

  .select-dropdown {
      position: absolute;
      left: 0;
      width: 100%;
      background: white;
      border-radius: 4px;
      box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
      border: 1px solid #e4e7ed;
      z-index: 1000;
      max-height: 200px;
      overflow-y: auto;
      padding: 4px 0;
      box-sizing: border-box;
  }

  .select-dropdown.bottom {
      top: 100%;
      margin-top: 4px;
  }

  .select-dropdown.top {
      bottom: 100%;
      margin-bottom: 4px;
      box-shadow: 0 -2px 12px 0 rgba(0, 0, 0, 0.1); /* Optional: invert shadow for top popover */
  }

  .select-option {
      padding: 0 12px; /* Reduced horizontal padding to accommodate check icon inside */
      height: 34px;
      line-height: 34px;
      cursor: pointer;
      color: #606266;
      display: flex;
      justify-content: space-between;
      align-items: center;
  }

  .select-option:hover {
      background-color: #f5f7fa;
  }

  .select-option.selected {
      color: #409eff;
      font-weight: bold;
      background-color: #f5f7fa;
  }
  
  .check {
      display: flex;
      align-items: center;
  }
</style>
