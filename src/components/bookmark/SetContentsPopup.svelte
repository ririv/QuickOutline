<script lang="ts">
    import ArrowPopup from "../controls/ArrowPopup.svelte";
    import StyledRadioButtonGroup from '../controls/StyledRadioButtonGroup.svelte';
    import fitToHeightIcon from '@/assets/icons/fit-to-height.svg';
    import fitToWidthIcon from '@/assets/icons/fit-to-width.svg';
    import fitToPageIcon from '@/assets/icons/fit-to-page.svg';
    import fitToBoxIcon from '@/assets/icons/fit-to-box.svg';
    import actualSizeIcon from '@/assets/icons/actual-size.svg';

    export type ViewScaleType = 'NONE' | 'FIT_TO_HEIGHT' | 'FIT_TO_WIDTH' | 'FIT_TO_PAGE' | 'FIT_TO_BOX' | 'ACTUAL_SIZE';

    interface Props {
        triggerEl: HTMLElement;
        selected: ViewScaleType;
        onSelect: (type: ViewScaleType) => void;
    }
    let { triggerEl, selected, onSelect }: Props = $props();

    let label = $derived(getLabel(selected));

    function getLabel(type: ViewScaleType) {
        switch(type) {
            case 'FIT_TO_HEIGHT': return 'Fit to Height';
            case 'FIT_TO_WIDTH': return 'Fit to Width';
            case 'FIT_TO_PAGE': return 'Fit to Page';
            case 'FIT_TO_BOX': return 'Fit Visible';
            case 'ACTUAL_SIZE': return 'Actual Size';
            default: return 'Inherit Zoom';
        }
    }

    const options = [
        { value: 'FIT_TO_PAGE', label: 'Fit to Page', icon: fitToPageIcon },
        { value: 'FIT_TO_WIDTH', label: 'Fit to Width', icon: fitToWidthIcon },
        { value: 'FIT_TO_HEIGHT', label: 'Fit to Height', icon: fitToHeightIcon },
        { value: 'FIT_TO_BOX', label: 'Fit Visible', icon: fitToBoxIcon },
        { value: 'ACTUAL_SIZE', label: 'Actual Size', icon: actualSizeIcon }
    ];

    // Local state to bridge 'NONE' (domain) and null (UI deselect)
    let groupValue = $state<string | null>(selected === 'NONE' ? null : selected);

    // Sync from Prop -> Local
    $effect(() => {
        const derivedVal = selected === 'NONE' ? null : selected;
        if (groupValue !== derivedVal) {
            groupValue = derivedVal;
        }
    });

</script>

<ArrowPopup placement="top" {triggerEl} padding="12px" usePortal={false}>
    <div class="flex flex-col items-center gap-2.5">
        <span class="text-xs font-semibold text-el-default-text">Set Page View Mode</span>
        
        <StyledRadioButtonGroup 
            {options} 
            bind:value={groupValue} 
            allowDeselect={true}
            onchange={(val) => onSelect((val || 'NONE') as ViewScaleType)}
            hasBorder={false}
        />

        <div class="text-xs text-gray-500 h-4">{label}</div>
    </div>
</ArrowPopup>
