<script lang="ts">
    import type { PageMargins } from '@/lib/types/page.ts';
    import ArrowPopup from '../../controls/ArrowPopup.svelte';

    interface Props {
        margins: PageMargins;
        triggerEl?: HTMLElement;
        onchange?: () => void;
    }

    let { margins = $bindable(), triggerEl, onchange }: Props = $props();

    let linkTB = $state(margins.top === margins.bottom);
    let linkLR = $state(margins.left === margins.right);

    function handleChange() {
        onchange?.();
    }

    function handleMarginChange(side: 'top' | 'bottom' | 'left' | 'right') {
        if (linkTB) {
            if (side === 'top') margins.bottom = margins.top;
            if (side === 'bottom') margins.top = margins.bottom;
        }
        if (linkLR) {
            if (side === 'left') margins.right = margins.left;
            if (side === 'right') margins.left = margins.right;
        }
        handleChange();
    }

    function toggleLinkTB() {
        linkTB = !linkTB;
        if (linkTB) {
            margins.bottom = margins.top;
            handleChange();
        }
    }

    function toggleLinkLR() {
        linkLR = !linkLR;
        if (linkLR) {
            margins.right = margins.left;
            handleChange();
        }
    }
</script>

<ArrowPopup triggerEl={triggerEl} placement="top" className="page-margins-popup" trackTrigger={false}>
    <div class="popup-content">
        <div class="row label-row" title="Page Margins">
             <span style="font-size: 12px; color: #666;">Margins (mm)</span>
        </div>
        
        <div class="margin-container">
            <div class="margin-row">
                <div class="field">
                    <span class="icon-label" title="Top Margin">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                            <rect x="4" y="4" width="16" height="16" rx="2" stroke-opacity="0.3"></rect>
                            <path d="M5 4h14" stroke-width="2.5"></path>
                        </svg>
                    </span>
                    <input type="number" bind:value={margins.top} min="0" oninput={() => handleMarginChange('top')} />
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
                    <span class="icon-label" title="Bottom Margin">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                            <rect x="4" y="4" width="16" height="16" rx="2" stroke-opacity="0.3"></rect>
                            <path d="M5 20h14" stroke-width="2.5"></path>
                        </svg>
                    </span>
                    <input type="number" bind:value={margins.bottom} min="0" oninput={() => handleMarginChange('bottom')} />
                </div>
            </div>
            
            <div class="margin-row">
                <div class="field">
                    <span class="icon-label" title="Left Margin">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                            <rect x="4" y="4" width="16" height="16" rx="2" stroke-opacity="0.3"></rect>
                            <path d="M4 5v14" stroke-width="2.5"></path>
                        </svg>
                    </span>
                    <input type="number" bind:value={margins.left} min="0" oninput={() => handleMarginChange('left')} />
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
                    <span class="icon-label" title="Right Margin">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                            <rect x="4" y="4" width="16" height="16" rx="2" stroke-opacity="0.3"></rect>
                            <path d="M20 5v14" stroke-width="2.5"></path>
                        </svg>
                    </span>
                    <input type="number" bind:value={margins.right} min="0" oninput={() => handleMarginChange('right')} />
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
        gap: 12px;
    }
    
    .label-row {
        margin-bottom: -5px;
        font-weight: 500;
        color: #666;
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
</style>
