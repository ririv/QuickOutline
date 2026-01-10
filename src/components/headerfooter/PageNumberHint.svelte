<script lang="ts">
    import ArrowPopup from '../controls/ArrowPopup.svelte';
    import Icon from '@/components/Icon.svelte';
    import StyledSelect from '@/components/controls/StyledSelect.svelte';
    import { PageLabelNumberingStyle, pageLabelStyleMap } from '@/lib/types/page-label.ts';
    import { clickOutside } from '@/lib/actions/clickOutside';

    interface Props {
        type?: 'header' | 'footer';
        onInsert?: (text: string) => void;
    }

    let { 
        type = 'header', 
        onInsert,
    }: Props = $props();

    let numberingStyle = $state(PageLabelNumberingStyle.DECIMAL);
    let triggerEl = $state<HTMLElement | undefined>();
    let isOpen = $state(false);
    
    const styles = pageLabelStyleMap.getAllStyles();

    function toggle() {
        isOpen = !isOpen;
    }

    function close() {
        isOpen = false;
    }
</script>

<div class="btn-wrapper" use:clickOutside={close}>
    <button class="trigger-btn" bind:this={triggerEl} onclick={toggle} title="Page Number Settings">
        <Icon name="number-sign" width="13" height="13" />
    </button>
    
    <ArrowPopup 
        usePortal={false} 
        className="hover-popup {isOpen ? 'visible' : ''}" 
        placement={type === 'header' ? 'bottom' : 'top'}
        triggerEl={triggerEl}
        minWidth="220px"
    >
        <div class="hint-content">
            <div class="hint-row">
                <span class="hint-icon">?</span>
                <div class="hint-text">
                    Use <code>{'{p}'}</code> for page number
                </div>
            </div>
            
            <div class="style-select-wrapper">
                <div class="section-title">Numbering Style</div>
                <StyledSelect 
                    options={styles}
                    displayKey="displayText"
                    optionKey="displayText"
                    valueKey="enumName"
                    bind:value={numberingStyle}
                />
            </div>
            
            <button 
                class="insert-btn"
                onclick={() => {
                    onInsert?.('{p}');
                    close();
                }}
            >
                Insert
            </button>
        </div>
    </ArrowPopup>
</div>

<style>
    .btn-wrapper {
        position: relative;
        display: inline-flex;
    }

    .trigger-btn {
        background: transparent;
        border: none;
        padding: 4px;
        cursor: pointer;
        border-radius: 3px;
        display: flex;
        align-items: center;
        justify-content: center;
        color: #666;
        transition: all 0.2s;
        width: 24px;
        height: 24px;
    }

    .trigger-btn:hover {
        background: #f0f0f0;
        color: #333;
    }

    /* Popup Visibility Control */
    .btn-wrapper :global(.hover-popup) {
        visibility: hidden;
        opacity: 0;
        transition: all 0.2s;
        pointer-events: none;
    }

    .btn-wrapper :global(.hover-popup.visible) {
        visibility: visible;
        opacity: 1;
        pointer-events: auto;
    }

    /* Content Styles */
    .hint-content {
        display: flex;
        flex-direction: column;
        gap: 12px; /* Increased gap to maintain breathing room without dividers */
    }

    .hint-row {
        display: flex;
        align-items: flex-start;
        gap: 8px;
    }

    .hint-icon {
        font-size: 12px;
        color: #999;
        border: 1px solid #ccc;
        border-radius: 50%;
        width: 18px;
        height: 18px;
        display: flex;
        align-items: center;
        justify-content: center;
        flex-shrink: 0;
        user-select: none;
        background: #fff;
        margin-top: 1px; /* Align with text */
    }

    .hint-text {
        color: #666;
        font-size: 13px;
        line-height: 1.4;
    }

    code {
        font-family: monospace;
        background: rgba(0, 0, 0, 0.06);
        padding: 0 3px;
        border-radius: 3px;
    }

    .insert-btn {
        background-color: #f0f7ff;
        color: #1677ff;
        border: 1px solid #91caff;
        border-radius: 4px;
        padding: 5px 8px;
        cursor: pointer;
        font-size: 12px;
        transition: all 0.2s;
        text-align: center;
        width: 100%;
    }

    .insert-btn:hover {
        background-color: #e6f7ff;
        border-color: #1677ff;
    }
    
    .section-title {
        font-size: 11px;
        color: #999;
        margin-bottom: 4px;
        padding-left: 1px;
        text-transform: uppercase;
        font-weight: 600;
    }
    
    .style-select-wrapper {
        display: flex;
        flex-direction: column;
        gap: 2px;
    }
</style>