<script lang="ts">
    import type { PageLayout } from '@/lib/types/page';
    import ArrowPopup from '../controls/ArrowPopup.svelte';
    import StyledSelect from '../controls/StyledSelect.svelte';
    import Icon from '../Icon.svelte';

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

    let currentDimensions = $derived.by(() => {
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
</script>

<ArrowPopup triggerEl={triggerEl} placement="top" className="paper-size-popup" trackTrigger={false}>
    <div class="popup-content">
        <div class="row label-row" title="Paper Size">
            <span style="font-size: 12px; color: #666;">Paper Size</span>
        </div>
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
        <div class="row">
            <span class="row-icon" title="Detailed Size">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <rect x="2" y="7" width="20" height="10" rx="2" />
                    <line x1="7" y1="7" x2="7" y2="11" />
                    <line x1="12" y1="7" x2="12" y2="13" />
                    <line x1="17" y1="7" x2="17" y2="11" />
                </svg>
            </span>
            <div class="dimension-text">
                {currentDimensions}
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
</style>