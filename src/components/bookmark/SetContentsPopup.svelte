<script lang="ts">
    import ArrowPopup from "../controls/ArrowPopup.svelte";
    import fitToHeightIcon from '@/assets/icons/fit-to-height.svg';
    import fitToWidthIcon from '@/assets/icons/fit-to-width.svg';
    import actualSizeIcon from '@/assets/icons/actual-size.svg';
    import GraphButton from '../controls/GraphButton.svelte';

    export type ViewScaleType = 'NONE' | 'FIT_TO_HEIGHT' | 'FIT_TO_WIDTH' | 'ACTUAL_SIZE';

    interface Props {
        triggerEl: HTMLElement;
        onSelect: (type: ViewScaleType) => void;
    }
    let { triggerEl, onSelect }: Props = $props();

    let selected = $state<ViewScaleType>('NONE');
    let label = $derived(getLabel(selected));

    function getLabel(type: ViewScaleType) {
        switch(type) {
            case 'FIT_TO_HEIGHT': return 'Fit to Height';
            case 'FIT_TO_WIDTH': return 'Fit to Width';
            case 'ACTUAL_SIZE': return 'Actual Size';
            default: return 'Default';
        }
    }

    function handleSelect(type: ViewScaleType) {
        selected = selected === type ? 'NONE' : type; // Allow toggle off
        onSelect(selected);
    }

</script>

<ArrowPopup placement="top" {triggerEl} padding="15px">
    <div class="popup-content">
        <span class="title">Set Page View Mode</span>
        <div class="icon-toggle-group">
            <GraphButton class={selected === 'FIT_TO_HEIGHT' ? 'active' : ''} onclick={() => handleSelect('FIT_TO_HEIGHT')}>
                <img src={fitToHeightIcon} alt="Fit to Height"/>
            </GraphButton>
            <GraphButton class={selected === 'FIT_TO_WIDTH' ? 'active' : ''} onclick={() => handleSelect('FIT_TO_WIDTH')}>
                <img src={fitToWidthIcon} alt="Fit to Width"/>
            </GraphButton>
            <GraphButton class={selected === 'ACTUAL_SIZE' ? 'active' : ''} onclick={() => handleSelect('ACTUAL_SIZE')}>
                <img src={actualSizeIcon} alt="Actual Size"/>
            </GraphButton>
        </div>
        <div class="label-display">{label}</div>
    </div>
</ArrowPopup>

<style>
    .popup-content {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 10px;
    }
    .icon-toggle-group {
        display: flex;
        gap: 8px;
    }
    /* Removed redundant styles, as GraphButton handles them */
    .label-display {
        font-size: 12px;
        color: #606266;
    }
</style>
