<script lang="ts">
    import Icon from "@/components/Icon.svelte";
    import addIcon from '@/assets/icons/plus.svg?raw';
    import updateIcon from '@/assets/icons/edit.svg?raw';
    import StyledSelect from '@/components/controls/StyledSelect.svelte';
    import StyledInput from "@/components/controls/StyledInput.svelte";
    import { ripple } from '@/lib/actions/ripple.ts';
    import { pageLabelStore } from '@/stores/pageLabelStore.svelte.js';
    import { pageLabelStyleMap, type PageLabel } from '@/lib/types/page-label.ts';
    import { usePageLabelActions } from '@/views/shared/pagelabel.svelte.ts';
    import PageLabelPreviewRow from './PageLabelPreviewRow.svelte';

    const styles = pageLabelStyleMap.getAllStyles();
    const { addRule } = usePageLabelActions();

    interface Props {
        onSuccess?: () => void;
    }

    let { onSuccess }: Props = $props();

    let existingRule = $derived(pageLabelStore.getRuleByPage(parseInt(pageLabelStore.pageIndex) || 0));
    
    // Derived rule object for preview
    let previewRule = $derived({
        pageIndex: parseInt(pageLabelStore.pageIndex) || 1,
        numberingStyle: pageLabelStore.numberingStyle,
        labelPrefix: pageLabelStore.labelPrefix,
        startValue: pageLabelStore.startValue || 1
    } as PageLabel);

    function handleAdd() {
        addRule(); // This now calls addOrUpdateRule internally
        if (onSuccess) onSuccess();
    }
</script>

<div class="flex flex-col gap-4">
    <div class="grid grid-cols-[120px_1fr] items-center gap-2.5">
        <label for="startPage" class="text-right text-sm text-[#333]">Page Index</label>
        <StyledInput id="pageIndex" type="number" min="1" step="1" bind:value={pageLabelStore.pageIndex} placeholder="e.g. 1 (Required)" numericType="unsigned-integer" />
    </div>

    <div class="grid grid-cols-[120px_1fr] items-center gap-2.5">
        <label for="style" class="text-right text-sm text-[#333]">Numbering Style</label>
        <div class="w-full">
            <StyledSelect
                options={styles}
                displayKey="displayText"
                optionKey="displayText"
                valueKey="enumName"
                bind:value={pageLabelStore.numberingStyle}
                maxHeight="220px"
            />
        </div>
    </div>

    <div class="grid grid-cols-[120px_1fr] items-center gap-2.5">
        <label for="prefix" class="text-right text-sm text-[#333]">Label Prefix</label>
        <StyledInput id="prefix" type="text" bind:value={pageLabelStore.labelPrefix} placeholder="Optional" />
    </div>

    <div class="grid grid-cols-[120px_1fr] items-center gap-2.5">
        <label for="startNum" class="text-right text-sm text-[#333]">Start Value</label>
        <StyledInput id="startNum" type="number" min="1" step="1" bind:value={pageLabelStore.startValue} placeholder="Optional (default: 1)" numericType="unsigned-integer" />
    </div>

    <PageLabelPreviewRow rule={previewRule} labelWidth="120px" />

    <div class="flex justify-center mt-1">
        <button
            class="inline-flex items-center justify-center min-w-[110px] gap-1.5 px-3 py-2 text-sm font-medium rounded-md transition-colors duration-200 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-2 focus-visible:ring-blue-500 hover:bg-gray-100 {existingRule ? 'text-amber-600 hover:text-amber-700' : 'text-gray-700'}"
            use:ripple
            onclick={handleAdd}
        >
            <Icon data={existingRule ? updateIcon : addIcon} class="w-4 h-4 opacity-70" />
            {existingRule ? 'Update Rule' : 'Add Rule'}
        </button>
    </div>
</div>
