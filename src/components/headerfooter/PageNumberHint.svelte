<script lang="ts">
    import ArrowPopup from '../controls/ArrowPopup.svelte';
    import Icon from '@/components/Icon.svelte';
    import StyledSelect from '@/components/controls/StyledSelect.svelte';
    import { PageLabelNumberingStyle, pageLabelStyleMap, type PageNumberStyle } from '@/lib/types/page-label.ts';
    import { clickOutside } from '@/lib/actions/clickOutside';

    interface Props {
        type?: 'header' | 'footer';
        onInsert?: (text: string) => void;
    }

    let { 
        type = 'header', 
        onInsert,
    }: Props = $props();

    let numberingStyle = $state<PageNumberStyle>(PageLabelNumberingStyle.DECIMAL_ARABIC_NUMERALS);
    let triggerEl = $state<HTMLElement | undefined>();
    let isOpen = $state(false);
    
    // Filter out 'None' style
    const styles = pageLabelStyleMap.getAllStyles().filter(s => s.enumName !== PageLabelNumberingStyle.NONE);

    function toggle() {
        isOpen = !isOpen;
    }

    function close() {
        isOpen = false;
    }

    function getInsertText(style: PageNumberStyle): string {
        switch (style) {
             case PageLabelNumberingStyle.UPPERCASE_ROMAN_NUMERALS: return '{p R}';
             case PageLabelNumberingStyle.LOWERCASE_ROMAN_NUMERALS: return '{p r}';
             case PageLabelNumberingStyle.UPPERCASE_LETTERS: return '{p A}';
             case PageLabelNumberingStyle.LOWERCASE_LETTERS: return '{p a}';
             case PageLabelNumberingStyle.DECIMAL_ARABIC_NUMERALS:
             default: return '{p}';
        }
    }

    let insertText = $derived(getInsertText(numberingStyle));
</script>

<div class="btn-wrapper" use:clickOutside={close}>
    <button class="trigger-btn" bind:this={triggerEl} onclick={toggle} title="Page Number Settings">
        <span class="icon-box">
            <Icon name="number-sign" width="13" height="13" />
        </span>
    </button>
    
    {#if isOpen}
        <ArrowPopup 
            usePortal={true} 
            className="hover-popup" 
            placement={type === 'header' ? 'bottom' : 'top'}
            triggerEl={triggerEl}
            minWidth="220px"
        >
            <div class="hint-content">
                <div class="popup-title">Page Number</div>
                
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

                <div class="hint-row">
                    <span class="hint-icon">?</span>
                    <div class="hint-text">
                        Use <code>{insertText}</code> for page number
                    </div>
                </div>
                
                <button 
                    class="insert-btn"
                    onclick={() => {
                        onInsert?.(insertText);
                        close();
                    }}
                >
                    Insert
                </button>
            </div>
        </ArrowPopup>
    {/if}
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
            /* Width/Height handled by content (icon-box 14px + padding 4px*2 = 22px) */
        }
    
        .trigger-btn:hover {
            background: #f0f0f0;
            color: #333;
        }
        
        .icon-box {
            width: 14px;
            height: 14px;
            display: flex;
            align-items: center;
            justify-content: center;
        }
    
        /* Popup Visibility Control */
    /* Content Styles */
    .hint-content {
        display: flex;
        flex-direction: column;
        gap: 12px;
    }

    .popup-title {
        font-size: 14px;
        font-weight: 600;
        color: #333;
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