<script lang="ts">
    import Icon from "@/components/Icon.svelte";
    import addIcon from '@/assets/icons/plus.svg?raw';
    import StyledSelect from '@/components/controls/StyledSelect.svelte';
    import StyledInput from "@/components/controls/StyledInput.svelte";
    import { ripple } from '@/lib/actions/ripple.ts';
    import { pageLabelStore } from '@/stores/pageLabelStore.svelte.js';
    import { pageLabelStyleMap } from '@/lib/types/page-label.ts';
    import { usePageLabelActions } from '@/views/shared/pagelabel.svelte.ts';

    const styles = pageLabelStyleMap.getAllStyles();
    const { addRule } = usePageLabelActions();

    interface Props {
        onSuccess?: () => void;
    }

    let { onSuccess }: Props = $props();

    function handleAdd() {
        addRule();
        if (onSuccess) onSuccess();
    }
</script>

<div class="flex flex-col gap-4">
    <div class="grid grid-cols-[120px_1fr] items-center gap-2.5">
        <label for="style" class="text-right text-sm text-[#333]">Style</label>
        <div class="w-full">
            <StyledSelect
                options={styles}
                displayKey="displayText"
                optionKey="displayText"
                valueKey="enumName"
                bind:value={pageLabelStore.numberingStyle}
            />
        </div>
    </div>

    <div class="grid grid-cols-[120px_1fr] items-center gap-2.5">
        <label for="prefix" class="text-right text-sm text-[#333]">Prefix</label>
        <StyledInput id="prefix" type="text" bind:value={pageLabelStore.prefix} placeholder="Optional" />
    </div>

    <div class="grid grid-cols-[120px_1fr] items-center gap-2.5">
        <label for="startNum" class="text-right text-sm text-[#333]">Start Number</label>
        <StyledInput id="startNum" type="number" min="1" step="1" bind:value={pageLabelStore.startNumber} placeholder="1" numericType="unsigned-integer" />
    </div>

    <div class="grid grid-cols-[120px_1fr] items-center gap-2.5">
        <label for="startPage" class="text-right text-sm text-[#333]">Start Page</label>
        <StyledInput id="startPage" type="number" min="1" step="1" bind:value={pageLabelStore.startPage} placeholder="e.g. 1 (Required)" numericType="unsigned-integer" />
    </div>

    <div class="flex justify-center mt-2.5">
        <button
            class="inline-flex items-center justify-center w-[110px] gap-1.5 px-3 py-2 text-sm font-medium text-gray-700 rounded-md transition-colors duration-200 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-2 focus-visible:ring-blue-500 hover:bg-gray-100"
            use:ripple
            onclick={handleAdd}
        >
            <Icon data={addIcon} class="w-4 h-4 opacity-70" />
            Add Rule
        </button>
    </div>
</div>
