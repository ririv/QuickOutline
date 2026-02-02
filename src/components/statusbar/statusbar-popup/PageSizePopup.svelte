<script lang="ts">
    import type { PageSize } from '@/lib/types/page.ts';
    import { PAPER_FORMAT_OPTIONS } from '@/lib/types/page.ts';
    import type { PageSizeDetectionState } from '@/lib/pdf-processing/usePdfPageSizeDetection.svelte.js';
    import ArrowPopup from '../../controls/ArrowPopup.svelte';
    import StyledSelect from '../../controls/StyledSelect.svelte';
    import StyledSwitch from '../../controls/StyledSwitch.svelte';
    import Icon from '../../Icon.svelte';

    interface Props {
        pageSize: PageSize;
        triggerEl?: HTMLElement;
        onchange?: () => void;
        mode?: 'new' | 'edit';
        autoDetect?: boolean;
        detection?: PageSizeDetectionState;
    }

    let { 
        pageSize = $bindable(), 
        triggerEl, 
        onchange,
        mode = 'edit',
        autoDetect = $bindable(true),
        detection
    }: Props = $props();

    const sizeOptions = [...PAPER_FORMAT_OPTIONS];

    let currentDimensions = $derived.by(() => {
        const format = (num: number) => {
            const rounded = Math.round(num * 10) / 10;
            return Number.isInteger(rounded) ? rounded.toString() : rounded.toFixed(1);
        };

        let w: number;
        let h: number;

        if (autoDetect && detection?.actualDimensions) {
            w = detection.actualDimensions.width;
            h = detection.actualDimensions.height;
        } else {
            const opt = sizeOptions.find(o => o.value === pageSize.size);
            if (!opt) return '';
            if (pageSize.orientation === 'landscape') {
                w = opt.h;
                h = opt.w;
            } else {
                w = opt.w;
                h = opt.h;
            }
        }

        const dimStr = `${format(w)}×${format(h)}mm`;
        
        // Match standard size name (allow for both orientations)
        const rw = Math.round(w * 10) / 10;
        const rh = Math.round(h * 10) / 10;
        const matched = sizeOptions.find(o => 
            (rw === o.w && rh === o.h) || (rw === o.h && rh === o.w)
        );

        if (!matched) return dimStr;

        const orientation = w > h ? 'Landscape' : 'Portrait';
        return `${dimStr} (${matched.label}, ${orientation})`;
    });

    function handleChange() {
        onchange?.();
    }

    $effect(() => {
        if (autoDetect && detection?.suggestedPageSize && mode === 'edit') {
            const suggested = detection.suggestedPageSize;
            const isSame = 
                pageSize.size === suggested.size &&
                pageSize.orientation === suggested.orientation;

            if (!isSame) {
                pageSize = { ...suggested };
                onchange?.();
            }
        }
    });

    $effect(() => {
        if (!detection?.suggestedPageSize && autoDetect) {
            autoDetect = false;
        }
    });
</script>

<ArrowPopup triggerEl={triggerEl} placement="top" className="paper-size-popup" trackTrigger={false}>
    <div class="popup-content">
        <div class="header-row">
            <span class="title">Page Size</span>
            {#if mode === 'edit'}
                <div class="auto-switch" class:disabled={!detection?.suggestedPageSize}>
                    <span class="switch-label">Auto</span>
                    <StyledSwitch 
                        bind:checked={autoDetect} 
                        onchange={handleChange} 
                        size="small" 
                        disabled={!detection?.suggestedPageSize}
                    />
                </div>
            {/if}
        </div>

        {#if autoDetect && detection?.suggestedPageSize && mode === 'edit'}
            <!-- Auto Mode Area -->
            <div class="mode-content">
                <div class="neighbor-grid">
                    {#if detection.options.above}
                        <button 
                            class="neighbor-btn" 
                            class:active={detection.referencePage === detection.options.above}
                            onclick={() => detection.onReferenceChange?.(detection.options.above!)}
                        >
                            <span class="n-label">Previous</span>
                            <span class="n-pg">P{detection.options.above}</span>
                        </button>
                    {:else}
                         <div class="neighbor-placeholder">Start</div>
                    {/if}

                    {#if detection.options.below}
                        <button 
                            class="neighbor-btn" 
                            class:active={detection.referencePage === detection.options.below}
                            onclick={() => detection.onReferenceChange?.(detection.options.below!)}
                        >
                            <span class="n-label">Following</span>
                            <span class="n-pg">P{detection.options.below}</span>
                        </button>
                    {:else}
                         <div class="neighbor-placeholder">End</div>
                    {/if}
                </div>

                <div class="manual-ref-row">
                    <span class="ref-label">Or specific page:</span>
                    <input 
                        type="number" 
                        class="ref-input"
                        value={detection.referencePage}
                        onchange={(e) => detection.onReferenceChange?.(parseInt(e.currentTarget.value))}
                        min="1"
                        max={detection.pageCount}
                    />
                </div>
            </div>
        {:else}
            <!-- Manual Mode Area -->
            <div class="mode-content">
                <div class="row">
                    <span class="row-icon" title="Paper Size">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path><polyline points="14 2 14 8 20 8"></polyline><line x1="16" y1="13" x2="8" y2="13"></line><line x1="16" y1="17" x2="8" y2="17"></line><polyline points="10 9 9 9 8 9"></polyline></svg>
                    </span>
                    <div style="flex: 1;">
                        <StyledSelect 
                            options={sizeOptions} 
                            bind:value={pageSize.size} 
                            onchange={handleChange}
                            displayKey="label"
                            placement="top"
                        >
                            {#snippet item(opt: typeof PAPER_FORMAT_OPTIONS[number])}
                                <div class="size-option">
                                    <span class="main">{opt.label}</span>
                                    <span class="sub">{opt.detail}</span>
                                </div>
                            {/snippet}
                        </StyledSelect>
                    </div>
                </div>
                
                <div class="row">
                    <span class="row-icon" title="Orientation">
                        <Icon name="page-orientation" width="16" height="16" />
                    </span>
                    <div class="radio-group icon-group">
                        <button 
                            class:active={pageSize.orientation === 'portrait'} 
                            onclick={() => { pageSize.orientation = 'portrait'; handleChange(); }}
                            title="Portrait"
                        >
                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="6" y="3" width="12" height="18" rx="2" ry="2"></rect></svg>
                        </button>
                        <button 
                            class:active={pageSize.orientation === 'landscape'} 
                            onclick={() => { pageSize.orientation = 'landscape'; handleChange(); }}
                            title="Landscape"
                        >
                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="6" width="18" height="12" rx="2" ry="2"></rect></svg>
                        </button>
                    </div>
                </div>
            </div>
        {/if}

        <!-- Common Area (Row 3) -->
        <div class="row">
            <span class="row-icon" title="Detailed Size">
                <Icon name="ruler" width="16" height="16" />
            </span>
            <div class="dimension-text" class:highlight={autoDetect}>
                {currentDimensions}
            </div>
        </div>
    </div>
</ArrowPopup>

<style>
    .header-row {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 8px;
    }
    .title {
        font-size: 13px;
        font-weight: 600;
        color: #333;
    }
    .auto-switch {
        display: flex;
        align-items: center;
        gap: 6px;
    }
    .auto-switch.disabled {
        opacity: 0.5;
    }
    .switch-label {
        font-size: 12px;
        color: #666;
        font-weight: normal;
    }

    .popup-content {
        padding: 12px;
        width: 240px;
        display: flex;
        flex-direction: column;
        gap: 10px;
        font-size: 13px;
        color: #333;
    }

    .mode-content {
        display: flex;
        flex-direction: column;
        gap: 10px;
        min-height: 66px;
        justify-content: flex-start;
    }

    /* Manual Mode & Common Styles */
    .row {
        display: flex;
        align-items: center;
        gap: 12px;
        min-height: 28px;
    }
    
    /* New Auto Mode Styles */
    .neighbor-grid {
        display: grid;
        grid-template-columns: 1fr 1fr;
        gap: 8px;
        margin-bottom: 0; /* Removed margin */
    }
    
    .neighbor-btn, .neighbor-placeholder {
        display: flex;
        flex-direction: row;
        align-items: center;
        justify-content: center;
        gap: 6px;
        padding: 0 8px;
        border-radius: 4px;
        border: 1px solid #e1e4e8;
        background: #fafafa;
        cursor: pointer;
        transition: all 0.2s;
        height: 32px;
    }
    .neighbor-placeholder {
        background: #f5f5f5;
        border: 1px dashed #ddd;
        color: #bbb;
        font-size: 11px;
        cursor: default;
    }

    .neighbor-btn:hover {
        background: white;
        border-color: #4096ff;
        color: #4096ff;
    }

    .neighbor-btn.active {
        background: #e6f4ff;
        border-color: #1677ff;
        color: #1677ff;
    }

    .n-label {
        font-size: 11px;
        opacity: 0.7;
        letter-spacing: 0.2px;
    }
    .n-pg {
        font-size: 12px;
        font-weight: 600;
    }

    .manual-ref-row {
        display: flex;
        align-items: center;
        justify-content: space-between;
        margin-top: 0; /* Removed margin */
        padding: 0 2px;
        height: 24px; /* Fixed height */
    }
    /* End New Styles */

    .row-icon {
        display: flex;
        align-items: center;
        justify-content: center;
        width: 20px;
        height: 20px;
        color: #666;
        flex-shrink: 0;
    }

    .radio-group {
        display: flex;
        gap: 2px;
        background: #f0f0f0;
        padding: 2px;
        border-radius: 4px;
        height: 28px;
        box-sizing: border-box;
    }

    .icon-group button {
        background: transparent;
        border: none;
        padding: 4px 8px;
        border-radius: 3px;
        cursor: pointer;
        color: #666;
        transition: all 0.2s;
        display: flex;
        align-items: center;
        justify-content: center;
        height: 100%;
        width: 36px;
    }

    .icon-group button:hover {
        background: rgba(0,0,0,0.05);
    }

    .icon-group button.active {
        background: white;
        color: #1677ff;
        box-shadow: 0 1px 2px rgba(0,0,0,0.1);
    }
    
    .icon-group button:disabled {
        cursor: not-allowed;
    }

    /* Override StyledSelect height */
    :global(.paper-size-popup .select-trigger) {
        min-height: 28px;
        padding: 3px 11px;
    }
    :global(.paper-size-popup .select-trigger .value) {
        font-size: 12px;
    }
    :global(.paper-size-popup .select-option) {
        height: 28px;
        line-height: 28px;
    }

    .dimension-text {
        flex: 1;
        display: flex;
        align-items: center;
        height: 28px;
        font-size: 12px;
        color: #666;
    }

    .size-option {
        display: flex;
        justify-content: flex-start;
        align-items: center;
        width: 100%;
        line-height: normal; 
    }
    .main { font-weight: 500; color: #333; font-size: 12px; }
    .sub { font-size: 10px; color: #999; margin-left: 8px; }

    .ref-label {
        font-size: 11px;
        color: #888;
    }
    .ref-input {
        width: 44px;
        height: 24px;
        padding: 0 4px;
        border: 1px solid #d9d9d9;
        border-radius: 3px;
        font-size: 12px;
        text-align: center;
        color: #333;
        background: white;
        box-sizing: border-box;
    }
    .ref-input:focus {
        border-color: #409eff;
        outline: none;
        box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.1);
    }
    /* Hide spin buttons */
    .ref-input::-webkit-outer-spin-button,
    .ref-input::-webkit-inner-spin-button {
      -webkit-appearance: none;
      margin: 0;
    }
    .ref-input[type=number] {
      -moz-appearance: textfield;
      appearance: textfield;
    }
</style>