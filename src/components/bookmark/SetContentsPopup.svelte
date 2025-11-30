<script lang="ts">
    import ArrowPopup from "../controls/ArrowPopup.svelte";
    import fitToHeightIcon from '@/assets/icons/fit-to-height.svg';
    import fitToWidthIcon from '@/assets/icons/fit-to-width.svg';
    import actualSizeIcon from '@/assets/icons/actual-size.svg';

    export type ViewScaleType = 'NONE' | 'FIT_TO_HEIGHT' | 'FIT_TO_WIDTH' | 'ACTUAL_SIZE';

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
            case 'ACTUAL_SIZE': return 'Actual Size';
            default: return 'Default';
        }
    }

    function handleSelect(type: ViewScaleType) {
        // Toggle behavior: if clicking already selected, revert to NONE
        const newType = selected === type ? 'NONE' : type;
        onSelect(newType);
    }
</script>
<ArrowPopup placement="top" {triggerEl} padding="12px">
    <div class="flex flex-col items-center gap-2.5">
        <span class="text-xs font-semibold text-el-default-text">Set Page View Mode</span>

        <!-- Button Group: Tightly packed, no outer border, rounded corners for the group -->
        <div class="flex rounded-md overflow-hidden">
            <button
                    class="p-2 transition-colors focus:outline-none
                       hover:bg-gray-200
                       {selected === 'FIT_TO_HEIGHT' ? 'bg-blue-200 text-blue-800' : 'bg-transparent text-gray-600'}"
                    onclick={() => handleSelect('FIT_TO_HEIGHT')}
                    title="Fit to Height"
            >
                <img src={fitToHeightIcon} alt="Fit to Height" class="w-4 h-4" />
            </button>

            <button
                    class="p-2 transition-colors focus:outline-none
                       hover:bg-gray-200
                       {selected === 'FIT_TO_WIDTH' ? 'bg-blue-200 text-blue-800' : 'bg-transparent text-gray-600'}"
                    onclick={() => handleSelect('FIT_TO_WIDTH')}
                    title="Fit to Width"
            >
                <img src={fitToWidthIcon} alt="Fit to Width" class="w-4 h-4" />
            </button>

            <button
                    class="p-2 transition-colors focus:outline-none
                       hover:bg-gray-200
                       {selected === 'ACTUAL_SIZE' ? 'bg-blue-200 text-blue-800' : 'bg-transparent text-gray-600'}"
                    onclick={() => handleSelect('ACTUAL_SIZE')}
                    title="Actual Size"
            >
                <img src={actualSizeIcon} alt="Actual Size" class="w-4 h-4" />
            </button>
        </div>

        <div class="text-xs text-gray-500 h-4">{label}</div>
    </div>
</ArrowPopup>
