<script lang="ts">
    import ArrowPopup from '../controls/ArrowPopup.svelte';
    import Icon from '@/components/Icon.svelte';
    import StyledSelect from '@/components/controls/StyledSelect.svelte';
    import { PageLabelNumberingStyle, pageLabelStyleMap, type PageNumberStyle } from '@/lib/types/page-label.ts';
    import { clickOutside } from '@/lib/actions/clickOutside';
    import codeIcon from '@/assets/icons/code.svg?raw';
    import previewIcon from '@/assets/icons/eye.svg?raw';

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
    let activeTab = $state<'page' | 'date'>('page');
    
    // Filter out 'None' style
    const styles = pageLabelStyleMap.getAllStyles().filter(s => s.enumName !== PageLabelNumberingStyle.NONE);

    const dateStyles = [
        { label: 'YYYY-MM-DD', value: 'YYYY-MM-DD' },
        { label: 'YYYY年M月D日', value: 'YYYY年M月D日' },
        { label: 'YYYY年M月D日 dddd', value: 'YYYY年M月D日 dddd' },
        { label: 'MM/DD/YYYY', value: 'MM/DD/YYYY' },
        { label: 'DD/MM/YYYY', value: 'DD/MM/YYYY' },
        { label: 'Custom', value: 'custom' },
    ];

    const localeStyles = [
        { label: 'Auto', value: 'auto' },
        { label: 'Chinese (zh-CN)', value: 'zh-CN' },
        { label: 'English (en-US)', value: 'en-US' },
    ];

    let customFormat = $state('YYYY-MM-DD HH:mm');
    let selectedLocale = $state('auto');
    let dateStyle = $state('YYYY-MM-DD'); // Default to explicit ISO format

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

    function getDateInsertText(style: string, custom: string, locale: string): string {
        const pattern = style === 'custom' ? custom : style;
        
        let result = '';
        if (pattern.includes(' ')) {
             result = `{d "${pattern}"`;
        } else {
             result = `{d ${pattern}`;
        }

        if (locale !== 'auto') {
            // Append locale
             result += ` ${locale}`;
        }

        // Close brace if opened with {d ...
        if (result.startsWith('{d ')) {
             result += '}';
        }
        
        return result;
    }

    // Preview Logic
    function formatDatePreview(date: Date, formatStyle: string, custom: string, locale: string): string {
        let pattern = formatStyle === 'custom' ? custom : formatStyle;
        const localeArg = locale === 'auto' ? undefined : locale;

        // Pattern logic mirrors PageSectionTemplate
        const replacements: Record<string, string | number> = {
            'YYYY': date.getFullYear(),
            'YY': String(date.getFullYear()).slice(-2),
            'MM': String(date.getMonth() + 1).padStart(2, '0'),
            'M': date.getMonth() + 1,
            'DD': String(date.getDate()).padStart(2, '0'),
            'D': date.getDate(),
            'HH': String(date.getHours()).padStart(2, '0'),
            'H': date.getHours(),
            'mm': String(date.getMinutes()).padStart(2, '0'),
            'm': date.getMinutes(),
            'ss': String(date.getSeconds()).padStart(2, '0'),
            's': date.getSeconds(),
            'dddd': date.toLocaleDateString(localeArg, { weekday: 'long' }),
            'ddd': date.toLocaleDateString(localeArg, { weekday: 'short' }),
        };

        return pattern.replace(/YYYY|YY|MM|M|DD|D|HH|H|mm|m|ss|s|dddd|ddd/g, (match) => {
            return String(replacements[match]);
        });
    }

    let insertText = $derived(getInsertText(numberingStyle));
    let insertDateText = $derived(getDateInsertText(dateStyle, customFormat, selectedLocale));
    let previewDateText = $derived(formatDatePreview(new Date(), dateStyle, customFormat, selectedLocale));
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
            minWidth="240px"
        >
            <div class="hint-content">
                <div class="tab-header">
                    <button 
                        class="tab-btn" 
                        class:active={activeTab === 'page'} 
                        onclick={() => activeTab = 'page'}
                    >
                        Page Number
                    </button>
                    <button 
                        class="tab-btn" 
                        class:active={activeTab === 'date'} 
                        onclick={() => activeTab = 'date'}
                    >
                        Date
                    </button>
                </div>
                
                {#if activeTab === 'page'}
                    <div class="tab-content">
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
                            <div class="hint-text">
                                <span class="icon-label" title="Code"><Icon data={codeIcon} width="15" height="15" /></span>
                                <code>{insertText}</code>
                            </div>
                        </div>
                        
                        <button 
                            class="insert-btn"
                            onclick={() => {
                                onInsert?.(insertText);
                                close();
                            }}
                        >
                            Insert Page Number
                        </button>
                    </div>
                {:else}
                    <div class="tab-content">
                        <div class="date-settings-group">
                            <div class="style-select-wrapper">
                                <div class="section-title">Date Format</div>
                                <StyledSelect 
                                    options={dateStyles}
                                    displayKey="label"
                                    optionKey="label"
                                    valueKey="value"
                                    bind:value={dateStyle}
                                />
                                {#if dateStyle === 'custom'}
                                    <input 
                                        type="text" 
                                        class="custom-format-input" 
                                        bind:value={customFormat} 
                                        placeholder="YYYY-MM-DD HH:mm"
                                    />
                                {/if}
                            </div>

                            <div class="style-select-wrapper">
                                <div class="section-title">Language</div>
                                <StyledSelect 
                                    options={localeStyles}
                                    displayKey="label"
                                    optionKey="label"
                                    valueKey="value"
                                    bind:value={selectedLocale}
                                />
                            </div>
                        </div>

                        <div class="hint-row">
                            <div class="hint-text">
                                <span class="icon-label" title="Code"><Icon data={codeIcon} width="15" height="15" /></span>
                                <code>{insertDateText}</code>
                            </div>
                        </div>
                        
                        <div class="preview-row">
                            <span class="icon-label" title="Preview"><Icon data={previewIcon} width="15" height="15" /></span>
                            <div class="preview-text">
                                {previewDateText}
                            </div>
                        </div>

                        <button 
                            class="insert-btn"
                            onclick={() => {
                                onInsert?.(insertDateText);
                                close();
                            }}
                        >
                            Insert Date
                        </button>
                    </div>
                {/if}
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

    .hint-content {
        display: flex;
        flex-direction: column;
        gap: 0;
    }

    .tab-header {
        display: flex;
        border-bottom: 1px solid #eee;
        margin-bottom: 12px;
        margin-top: -4px; /* Slight adjustment to align with popup padding */
    }
    
    .tab-btn {
        flex: 1;
        background: transparent;
        border: none;
        padding: 8px;
        font-size: 13px;
        color: #888;
        cursor: pointer;
        font-weight: 500;
        position: relative;
        transition: all 0.2s;
        text-align: center;
    }

    .tab-btn:hover {
        color: #555;
        background-color: #f9f9f9;
    }

    .tab-btn.active {
        color: #60a5fa;
    }

    .tab-btn.active::after {
        content: '';
        position: absolute;
        bottom: -1px;
        left: 0;
        width: 100%;
        height: 2px;
        background-color: #60a5fa;
    }

    .tab-content {
        display: flex;
        flex-direction: column;
        gap: 12px;
    }

    .hint-row {
        display: flex;
        align-items: flex-start;
        gap: 8px;
    }

    .preview-row {
        display: flex;
        align-items: center;
        gap: 4px;
        margin-top: -4px;
    }
    
    .icon-label {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        color: #999;
        width: 20px;
        flex-shrink: 0;
    }

    .preview-text {
        font-size: 12px;
        color: #666;
        background: #f9f9f9;
        padding: 3px 6px;
        border-radius: 4px;
        border: 1px dashed #ddd;
        flex-grow: 1;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
    }

    .hint-text {
        color: #666;
        font-size: 13px;
        line-height: 1.4;
        padding-left: 0;
        display: flex;
        align-items: center;
        gap: 4px;
    }

    code {
        font-family: monospace;
        background: rgba(0, 0, 0, 0.06);
        padding: 0 3px;
        border-radius: 3px;
    }

    .insert-btn {
        background-color: #fff9eb;
        color: #d97706;
        border: 1px solid #fde68a;
        border-radius: 4px;
        padding: 5px 8px;
        cursor: pointer;
        font-size: 12px;
        transition: all 0.2s;
        text-align: center;
        width: 100%;
        font-weight: 500;
    }

    .insert-btn:hover {
        background-color: #fef3c7;
        border-color: #d97706;
    }
    
    .section-title {
        font-size: 11px;
        color: #999;
        margin-bottom: 4px;
        padding-left: 1px;
        text-transform: uppercase;
        font-weight: 600;
    }
    
    .date-settings-group {
        display: flex;
        flex-direction: column;
        gap: 8px;
    }
    
    .style-select-wrapper {
        display: flex;
        flex-direction: column;
        gap: 2px;
    }

    .custom-format-input {
        width: 100%;
        padding: 4px 6px;
        font-size: 12px;
        border: 1px solid #ddd;
        border-radius: 4px;
        margin-top: 4px;
        box-sizing: border-box;
    }
    .custom-format-input:focus {
        outline: none;
        border-color: #aaa;
    }
</style>