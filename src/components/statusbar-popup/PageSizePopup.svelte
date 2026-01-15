<script lang="ts">
    import type { PageLayout } from '@/lib/types/page';
    import type { LayoutDetectionState } from '@/lib/pdf-processing/usePdfLayoutDetection.svelte';
    import ArrowPopup from '../controls/ArrowPopup.svelte';
    import StyledSelect from '../controls/StyledSelect.svelte';
    import StyledSwitch from '../controls/StyledSwitch.svelte';
    import Icon from '../Icon.svelte';

    interface Props {
        layout: PageLayout;
        triggerEl?: HTMLElement;
        onchange?: () => void;
        mode?: 'new' | 'edit';
        autoDetect?: boolean;
        detection?: LayoutDetectionState;
    }

    let { 
        layout = $bindable(), 
        triggerEl, 
        onchange,
        mode = 'edit',
        autoDetect = $bindable(true),
        detection
    }: Props = $props();

    const sizeOptions = [
        { display: 'A4', detail: '210×297mm', value: 'A4' },
        { display: 'A3', detail: '297×420mm', value: 'A3' },
        { display: 'Letter', detail: '8.5×11"', value: 'Letter' },
        { display: 'Legal', detail: '8.5×14"', value: 'Legal' }
    ];

    let currentDimensions = $derived.by(() => {
        // Priority 1: If autoDetect is ON, show exact measurements from detection
        if (autoDetect && detection?.actualDimensions) {
            const { width, height } = detection.actualDimensions;
            // Round to 1 decimal place for readability
            return `${width.toFixed(1)}×${height.toFixed(1)}mm`;
        }

        // Priority 2: Standard size lookup based on current layout
        const opt = sizeOptions.find(o => o.value === layout.size);
        if (!opt) return '';

        const [w, hPart] = opt.detail.split('×');
        if (!w || !hPart) return opt.detail;

        const hMatch = hPart.match(/^([\d.]+)(.*)$/);
        if (!hMatch) return opt.detail;

        const h = hMatch[1];
        const unit = hMatch[2];

        if (layout.orientation === 'landscape') {
            return `${h}×${w}${unit}`;
        }
        return `${w}×${h}${unit}`;
    });

    function handleChange() {
        onchange?.();
    }

    $effect(() => {
        if (autoDetect && detection?.suggestedLayout && mode === 'edit') {
            const suggested = detection.suggestedLayout;
            const isSame = 
                layout.size === suggested.size &&
                layout.orientation === suggested.orientation &&
                layout.marginTop === suggested.marginTop &&
                layout.marginBottom === suggested.marginBottom &&
                layout.marginLeft === suggested.marginLeft &&
                layout.marginRight === suggested.marginRight;

            if (!isSame) {
                layout = { ...suggested };
                onchange?.();
            }
        }
    });

    $effect(() => {
        if (!detection?.suggestedLayout && autoDetect) {
            autoDetect = false;
        }
    });
</script>

<ArrowPopup triggerEl={triggerEl} placement="top" className="paper-size-popup" trackTrigger={false}>
    <div class="popup-content">
        <div class="header-row">
            <span class="title">Page Size</span>
            {#if mode === 'edit'}
                <div class="auto-switch" class:disabled={!detection?.suggestedLayout}>
                    <span class="switch-label">Auto</span>
                    <StyledSwitch 
                        bind:checked={autoDetect} 
                        onchange={handleChange} 
                        size="small" 
                        disabled={!detection?.suggestedLayout}
                    />
                </div>
            {/if}
        </div>

        {#if autoDetect && detection?.suggestedLayout && mode === 'edit'}
            <!-- Auto Mode Area -->
            <div class="mode-content">
                <div class="row">
                    <div class="ref-selector">
                        <span class="ref-label">Ref: Page</span>
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
                
                <div class="row">
                    <div class="ref-toggle-group">
                        {#if detection.options.above}
                            <button 
                                class="toggle-btn" 
                                class:active={detection.referencePage === detection.options.above}
                                onclick={() => detection.onReferenceChange?.(detection.options.above!)}
                                title="Same as Preceding Page ({detection.options.above})"
                            >
                                <Icon name="arrow-up" width="12" height="12" />
                            </button>
                        {/if}
                        {#if detection.options.below}
                            <button 
                                class="toggle-btn" 
                                class:active={detection.referencePage === detection.options.below}
                                onclick={() => detection.onReferenceChange?.(detection.options.below!)}
                                title="Same as Following Page ({detection.options.below})"
                            >
                                <Icon name="arrow-down" width="12" height="12" />
                            </button>
                        {/if}
                    </div>
                    <span class="ref-hint">
                        {#if detection.referencePage === detection.options.above}
                            Preceding (Page {detection.options.above})
                        {:else if detection.referencePage === detection.options.below}
                            Following (Page {detection.options.below})
                        {:else}
                            Page {detection.referencePage}
                        {/if}
                    </span>
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
                            bind:value={layout.size} 
                            onchange={handleChange}
                            displayKey="display"
                            placement="top"
                        >
                            {#snippet item(opt)}
                                <div class="size-option">
                                    <span class="main">{opt.display}</span>
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
                            class:active={layout.orientation === 'portrait'} 
                            onclick={() => { layout.orientation = 'portrait'; handleChange(); }}
                            title="Portrait"
                        >
                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="6" y="3" width="12" height="18" rx="2" ry="2"></rect></svg>
                        </button>
                        <button 
                            class:active={layout.orientation === 'landscape'} 
                            onclick={() => { layout.orientation = 'landscape'; handleChange(); }}
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

    .row {
        display: flex;
        align-items: center;
        gap: 12px;
        min-height: 28px;
    }
    
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
    .ref-selector {
        display: flex;
        align-items: center;
        gap: 4px;
        flex: 1;
    }
    .ref-input {
        width: 44px;
        height: 28px;
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
    
    .ref-toggle-group {
        display: flex;
        gap: 2px;
        background: #f0f0f0;
        padding: 2px;
        border-radius: 4px;
        height: 28px;
        box-sizing: border-box;
    }
    .ref-hint {
        font-size: 11px;
        color: #888;
        white-space: nowrap;
    }
    .toggle-btn {
        background: transparent;
        border: none;
        padding: 0 8px; /* Adjusted horizontal padding */
        border-radius: 3px;
        cursor: pointer;
        color: #666;
        display: flex;
        align-items: center;
        justify-content: center;
        transition: all 0.2s;
        height: 100%; /* Fill parent */
    }
    .toggle-btn:hover {
        background: rgba(0,0,0,0.05);
    }
    .toggle-btn.active {
        background: white;
        color: #1677ff;
        box-shadow: 0 1px 2px rgba(0,0,0,0.1);
    }
</style>