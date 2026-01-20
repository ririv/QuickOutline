<script lang="ts">
  import ArrowPopup from '../controls/ArrowPopup.svelte';
  import Icon from '@/components/Icon.svelte';
  import { defaultColumnLayout, type ColumnLayoutConfig } from '@/lib/types/page';
  
  const singleColumnIcon = `
    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
        <rect x="3" y="4" width="18" height="16" rx="2" stroke="currentColor" stroke-width="1.5"/>
        <path d="M7 8H17" stroke="currentColor" stroke-width="1.2" stroke-linecap="round" stroke-opacity="0.3"/>
        <path d="M7 12H17" stroke="currentColor" stroke-width="1.2" stroke-linecap="round" stroke-opacity="0.3"/>
        <path d="M7 16H13" stroke="currentColor" stroke-width="1.2" stroke-linecap="round" stroke-opacity="0.3"/>
    </svg>
  `;

  const doubleColumnIcon = `
    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
        <rect x="3" y="4" width="18" height="16" rx="2" stroke="currentColor" stroke-width="1.5"/>
        <path d="M12 4V20" stroke="currentColor" stroke-width="1" stroke-dasharray="2 2" stroke-opacity="0.5"/>
        <path d="M6 8H9" stroke="currentColor" stroke-width="1.2" stroke-linecap="round" stroke-opacity="0.3"/>
        <path d="M6 12H9" stroke="currentColor" stroke-width="1.2" stroke-linecap="round" stroke-opacity="0.3"/>
        <path d="M6 16H8" stroke="currentColor" stroke-width="1.2" stroke-linecap="round" stroke-opacity="0.3"/>
        <path d="M15 8H18" stroke="currentColor" stroke-width="1.2" stroke-linecap="round" stroke-opacity="0.3"/>
        <path d="M15 12H18" stroke="currentColor" stroke-width="1.2" stroke-linecap="round" stroke-opacity="0.3"/>
        <path d="M15 16H17" stroke="currentColor" stroke-width="1.2" stroke-linecap="round" stroke-opacity="0.3"/>
    </svg>
  `;

  interface Props {
    layout?: ColumnLayoutConfig;
    onchange?: () => void;
    triggerEl: HTMLElement | undefined;
    [key: string]: any; 
  }

  let { 
    layout = $bindable(defaultColumnLayout),
    onchange,
    triggerEl,
    ...rest 
  }: Props = $props();

  const countOptions = [
    { value: 1, label: 'Single Column', desc: 'Standard layout', icon: singleColumnIcon },
    { value: 2, label: 'Two Columns', desc: 'Dense layout', icon: doubleColumnIcon }
  ];

  function selectCount(val: number) {
      if (layout.count !== val) {
          layout.count = val;
          onchange?.();
      }
  }

  function toggleDirection(dir: 'vertical' | 'horizontal') {
      if (layout.direction !== dir) {
          layout.direction = dir;
          onchange?.();
      }
  }
</script>

<ArrowPopup 
  placement="top" 
  minWidth="220px" 
  padding="12px"
  {triggerEl}
  {...rest}
>
  <div class="popup-content">
      {#if layout.count > 1}
          {#if layout.direction === 'vertical'}
            <div class="switch-row">
                <span class="switch-label">Separator Line</span>
                <button 
                    class="switch-btn" 
                    class:active={layout.rule}
                    onclick={() => { layout.rule = !layout.rule; onchange?.(); }}
                    role="switch"
                    aria-checked={layout.rule}
                    aria-label="Toggle separator line"
                >
                    <div class="switch-thumb"></div>
                </button>
            </div>
            <div class="divider"></div>
          {/if}

          <div class="popup-label">Flow Direction</div>
          <div class="direction-tabs">
              <button 
                  class="tab-btn" 
                  class:active={layout.direction === 'vertical'}
                  onclick={() => toggleDirection('vertical')}
                  title="Vertical Flow (N): Items flow down then across"
              >
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M12 5v14M19 12l-7 7-7-7"/></svg>
                  <span class="tab-label">N</span>
              </button>
              <button 
                  class="tab-btn" 
                  class:active={layout.direction === 'horizontal'}
                  onclick={() => toggleDirection('horizontal')}
                  title="Horizontal Flow (Z): Items flow across then down"
              >
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M5 12h14M12 5l7 7-7 7"/></svg>
                  <span class="tab-label">Z</span>
              </button>
          </div>
          
          <div class="divider"></div>
      {/if}

      <div class="popup-label">Columns</div>
      
      <div class="options-list">
        {#each countOptions as opt}
            <button 
                class="option-item" 
                class:selected={layout.count === opt.value}
                onclick={() => selectCount(opt.value)}
            >
                <div class="visual-icon">
                    <Icon data={opt.icon} width="24" height="24" />
                </div>
                <div class="info">
                    <div class="label">{opt.label}</div>
                    <div class="desc">{opt.desc}</div>
                </div>
                <div class="check-icon">
                    {#if layout.count === opt.value}
                         <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"></polyline></svg>
                    {/if}
                </div>
            </button>
        {/each}
      </div>
  </div>
</ArrowPopup>

<style>
  .popup-content {
      background: #fff;
      display: flex;
      flex-direction: column;
  }

  .popup-label {
      font-weight: 600;
      font-size: 12px;
      color: #6b7280;
      margin-bottom: 8px;
      text-transform: uppercase;
      letter-spacing: 0.05em;
  }

  .options-list {
      display: flex;
      flex-direction: column;
      gap: 6px;
  }

  .option-item {
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 8px 10px;
      background: transparent;
      border: 1px solid transparent;
      border-radius: 6px;
      cursor: pointer;
      text-align: left;
      transition: all 0.2s;
  }

  .option-item:hover {
      background: #f3f4f6;
  }

  .option-item.selected {
      background: #eff6ff;
      border-color: #dbeafe;
  }

  .visual-icon {
      width: 36px;
      height: 36px;
      display: flex;
      align-items: center;
      justify-content: center;
      background: #f9fafb;
      border-radius: 6px;
      color: #9ca3af;
      border: 1px solid #f3f4f6;
      transition: all 0.2s;
      flex-shrink: 0;
  }

  .option-item:hover .visual-icon {
      background: #fff;
      color: #6b7280;
      border-color: #e5e7eb;
  }

  .option-item.selected .visual-icon {
      background: #fff;
      color: #2563eb;
      border-color: #bfdbfe;
      box-shadow: 0 1px 2px rgba(37, 99, 235, 0.05);
  }

  .check-icon {
      width: 16px;
      height: 16px;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #2563eb;
      flex-shrink: 0;
      margin-left: auto;
  }

  .info {
      display: flex;
      flex-direction: column;
      flex: 1;
  }

  .label {
      font-size: 13px;
      font-weight: 600;
      color: #374151;
  }
  
  .option-item.selected .label {
      color: #1d4ed8;
  }

  .desc {
      font-size: 11px;
      color: #9ca3af;
      margin-top: 1px;
  }
  
  .option-item.selected .desc {
      color: #60a5fa;
  }

  .divider {
      height: 1px;
      background: #e5e7eb;
      margin: 12px 0;
  }

  .direction-tabs {
      display: flex;
      background: #f3f4f6;
      padding: 3px;
      border-radius: 6px;
      gap: 2px;
  }

  .tab-btn {
      flex: 1;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 6px;
      padding: 6px;
      border: none;
      background: transparent;
      border-radius: 4px;
      font-size: 12px;
      color: #6b7280;
      cursor: pointer;
      transition: all 0.2s;
      font-weight: 500;
  }

  .tab-btn:hover {
      color: #374151;
      background: rgba(0,0,0,0.03);
  }

  .tab-btn.active {
      background: #fff;
      color: #2563eb;
      box-shadow: 0 1px 2px rgba(0,0,0,0.1);
      font-weight: 600;
  }

  .tab-label {
      font-size: 11px;
      font-weight: 700;
      margin-left: 4px;
  }

  .switch-row {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 0 4px;
  }

  .switch-label {
      font-size: 13px;
      color: #374151;
  }

  .switch-btn {
      width: 36px;
      height: 20px;
      border-radius: 12px;
      background: #e5e7eb;
      border: none;
      position: relative;
      cursor: pointer;
      transition: background 0.2s;
      padding: 0;
  }

  .switch-btn.active {
      background: #2563eb;
  }

  .switch-thumb {
      width: 16px;
      height: 16px;
      background: white;
      border-radius: 50%;
      position: absolute;
      top: 2px;
      left: 2px;
      transition: transform 0.2s;
      box-shadow: 0 1px 2px rgba(0,0,0,0.2);
  }

  .switch-btn.active .switch-thumb {
      transform: translateX(16px);
  }
</style>
