<script lang="ts">
    import type { HeaderFooterLayout } from '@/lib/types/page';
    import ArrowPopup from '../controls/ArrowPopup.svelte';

    interface Props {
        layout: HeaderFooterLayout;
        triggerEl?: HTMLElement;
        onchange?: () => void;
    }

    let { layout = $bindable(), triggerEl, onchange }: Props = $props();

    function handleChange() {
        onchange?.();
    }
</script>

<ArrowPopup triggerEl={triggerEl} placement="top" className="hf-setup-popup" trackTrigger={false}>
    <div class="popup-content">
        <div class="grid-header">
            <span></span>
            <span>Dist (mm)</span>
        </div>
        
        <div class="grid-row">
            <span class="label">Header</span>
            <input type="number" bind:value={layout.headerDist} min="0" oninput={handleChange} />
        </div>
        
        <div class="grid-row">
            <span class="label">Footer</span>
            <input type="number" bind:value={layout.footerDist} min="0" oninput={handleChange} />
        </div>
    </div>
</ArrowPopup>

<style>
    .popup-content {
        padding: 12px;
        width: 140px;
        display: flex;
        flex-direction: column;
        gap: 4px;
        font-size: 13px;
        color: #333;
    }
    
    .grid-header {
        display: grid;
        grid-template-columns: 50px 1fr;
        gap: 8px;
        font-size: 11px;
        color: #888;
        margin-bottom: 2px;
        text-align: center;
        font-weight: 500;
    }

    .grid-row {
        display: grid;
        grid-template-columns: 50px 1fr;
        gap: 8px;
        align-items: center;
        margin-bottom: 4px;
    }

    .label {
        font-size: 12px;
        color: #333;
        font-weight: 500;
    }

    input {
        width: 100%;
        padding: 4px 2px;
        border: 1px solid #d9d9d9;
        border-radius: 4px;
        font-size: 12px;
        text-align: center;
        box-sizing: border-box;
    }
    
    input:focus {
        border-color: #1677ff;
        outline: none;
    }
</style>