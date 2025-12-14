<script lang="ts">
    import { onMount } from 'svelte';
    import type { PageLayout } from '@/lib/types/page';
    import ArrowPopup from './controls/ArrowPopup.svelte';
    import StyledSelect from './controls/StyledSelect.svelte';
    import Icon from './Icon.svelte';

    interface Props {
        layout: PageLayout;
        triggerEl?: HTMLElement;
        onchange?: () => void;
    }

    let { layout = $bindable(), triggerEl, onchange }: Props = $props();

    const sizeOptions = [
        { display: 'A4', detail: '210×297mm', value: 'A4' },
        { display: 'A3', detail: '297×420mm', value: 'A3' },
        { display: 'Letter', detail: '8.5×11"', value: 'Letter' },
        { display: 'Legal', detail: '8.5×14"', value: 'Legal' }
    ];

    let linkTB = $state(layout.marginTop === layout.marginBottom);
    let linkLR = $state(layout.marginLeft === layout.marginRight);

    function handleChange() {
        onchange?.();
    }

    function handleMarginChange(side: 'top' | 'bottom' | 'left' | 'right') {
        if (linkTB) {
            if (side === 'top') layout.marginBottom = layout.marginTop;
            if (side === 'bottom') layout.marginTop = layout.marginBottom;
        }
        if (linkLR) {
            if (side === 'left') layout.marginRight = layout.marginLeft;
            if (side === 'right') layout.marginLeft = layout.marginRight;
        }
        handleChange();
    }

    function toggleLinkTB() {
        linkTB = !linkTB;
        if (linkTB) {
            layout.marginBottom = layout.marginTop;
            handleChange();
        }
    }

    function toggleLinkLR() {
        linkLR = !linkLR;
        if (linkLR) {
            layout.marginRight = layout.marginLeft;
            handleChange();
        }
    }
</script>

<ArrowPopup triggerEl={triggerEl} placement="top" className="page-setup-popup">
    <div class="popup-content">
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

        <div class="divider"></div>
        <div class="row label-row" title="Page Margins">
             <span style="font-size: 12px; color: #666;">Margins (mm)</span>
        </div>
        
        <div class="margin-container">
            <div class="margin-row">
                <div class="field">
                    <span>Top</span>
                    <input type="number" bind:value={layout.marginTop} min="0" oninput={() => handleMarginChange('top')} />
                </div>
                <button class="link-btn" class:active={linkTB} onclick={toggleLinkTB} title={linkTB ? "Unlink" : "Link Top & Bottom"}>
                    <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        {#if linkTB}
                            <path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"></path><path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"></path>
                        {:else}
                            <path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"></path><path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"></path><line x1="2" y1="2" x2="22" y2="22"></line>
                        {/if}
                    </svg>
                </button>
                <div class="field">
                    <span>Bottom</span>
                    <input type="number" bind:value={layout.marginBottom} min="0" oninput={() => handleMarginChange('bottom')} />
                </div>
            </div>
            
            <div class="margin-row">
                <div class="field">
                    <span>Left</span>
                    <input type="number" bind:value={layout.marginLeft} min="0" oninput={() => handleMarginChange('left')} />
                </div>
                <button class="link-btn" class:active={linkLR} onclick={toggleLinkLR} title={linkLR ? "Unlink" : "Link Left & Right"}>
                    <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        {#if linkLR}
                            <path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"></path><path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"></path>
                        {:else}
                            <path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"></path><path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"></path><line x1="2" y1="2" x2="22" y2="22"></line>
                        {/if}
                    </svg>
                </button>
                <div class="field">
                    <span>Right</span>
                    <input type="number" bind:value={layout.marginRight} min="0" oninput={() => handleMarginChange('right')} />
                </div>
            </div>
        </div>
    </div>
</ArrowPopup>

<style>
    .popup-content {
        padding: 12px;
        width: 240px;
        display: flex;
        flex-direction: column;
        gap: 10px;
        font-size: 13px;
        color: #333;
    }

    .row {
        display: flex;
        align-items: center;
        /* justify-content: space-between;  Removed to allow left-aligned icons */
        gap: 12px; /* Increased gap for better spacing */
    }
    
    .row-icon {
        display: flex;
        align-items: center;
        justify-content: center;
        width: 20px;
        height: 20px;
        color: #666;
        /* cursor: help; Removed */
        flex-shrink: 0;
    }
    
    .label-row {
        margin-bottom: -5px;
        font-weight: 500;
        color: #666;
    }

    .radio-group {
        display: flex;
        gap: 2px;
        background: #f0f0f0;
        padding: 2px;
        border-radius: 4px;
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
        height: 28px;
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

    .divider {
        height: 1px;
        background: #eee;
        margin: 4px 0;
    }

    .margin-container {
        display: flex;
        flex-direction: column;
        gap: 8px;
    }

    .margin-row {
        display: flex;
        align-items: center;
        gap: 4px;
        justify-content: space-between;
    }

    .link-btn {
        background: transparent;
        border: none;
        padding: 4px;
        border-radius: 3px;
        cursor: pointer;
        color: #999;
        display: flex;
        align-items: center;
        justify-content: center;
        transition: all 0.2s;
        height: 24px;
        width: 24px;
    }

    .link-btn:hover {
        background: rgba(0,0,0,0.05);
        color: #666;
    }

    .link-btn.active {
        color: #1677ff;
        /* background: rgba(22, 119, 255, 0.1); */
    }

    .field {
        display: flex;
        align-items: center;
        gap: 6px;
    }

    .field span {
        width: 40px;
        color: #888;
        font-size: 12px;
    }

    .field input {
        width: 100%;
        padding: 4px 6px;
        border: 1px solid #d9d9d9;
        border-radius: 4px;
        font-size: 12px;
        text-align: right;
    }
    
    .field input:focus {
        border-color: #1677ff;
        outline: none;
    }
    
    /* Override StyledSelect height */
    :global(.page-setup-popup .select-trigger) {
        min-height: 28px;
        padding: 3px 11px;
    }
    :global(.page-setup-popup .select-trigger .value) {
        font-size: 12px;
    }
    :global(.page-setup-popup .select-option) {
        height: 28px;
        line-height: 28px;
    }

    .size-option {
        display: flex;
        justify-content: flex-start; /* Align main and sub to the left */
        align-items: center;
        width: 100%;
        /* Ensure it fits within the 28px line-height */
        line-height: normal; 
    }
    .main { font-weight: 500; color: #333; font-size: 12px; }
    .sub { font-size: 10px; color: #999; margin-left: 8px; }
</style>