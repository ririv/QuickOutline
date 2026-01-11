<script lang="ts">
  import ArrowPopup from '../controls/ArrowPopup.svelte';
  import { type PageLabel, PageLabelNumberingStyle, pageLabelStyleMap } from "@/lib/types/page-label.ts";
  import StyledSelect from '@/components/controls/StyledSelect.svelte';
  import StyledInput from '@/components/controls/StyledInput.svelte';
  import PageLabelPreviewRow from '@/components/pagelabel/PageLabelPreviewRow.svelte';

  interface Props {
    pageLabel?: PageLabel;
    onchange?: () => void;
    triggerEl: HTMLElement | undefined;
  }

  let { 
    pageLabel = $bindable({
        pageIndex: 1,
        numberingStyle: PageLabelNumberingStyle.NONE,
        labelPrefix: '',
        startValue: 1
    }),
    onchange,
    triggerEl
  }: Props = $props();

  const styles = pageLabelStyleMap.getAllStyles();

  function handleChange() {
      onchange?.();
  }
</script>

<ArrowPopup 
  placement="top" 
  minWidth="280px" 
  padding="12px"
  {triggerEl}
>
    <div class="flex flex-col gap-4">
        <!-- Numbering Style -->
        <div class="grid grid-cols-[100px_1fr] items-center gap-2.5">
            <label for="style" class="text-right text-sm text-[#333]">Style</label>
            <div class="w-full">
                <StyledSelect
                    options={styles}
                    displayKey="displayText"
                    optionKey="displayText"
                    valueKey="enumName"
                    bind:value={pageLabel.numberingStyle}
                    onchange={handleChange}
                    placement="top"
                    maxHeight="220px"
                />
            </div>
        </div>

        <!-- Label Prefix -->
        <div class="grid grid-cols-[100px_1fr] items-center gap-2.5">
            <label for="prefix" class="text-right text-sm text-[#333]">Prefix</label>
            <StyledInput 
                id="prefix" 
                type="text" 
                value={pageLabel.labelPrefix ?? ''} 
                placeholder="Optional" 
                oninput={(e: Event) => {
                    const target = e.target as HTMLInputElement;
                    pageLabel.labelPrefix = target.value;
                    handleChange();
                }}
            />
        </div>

        <!-- Start Value -->
        <div class="grid grid-cols-[100px_1fr] items-center gap-2.5">
            <label for="startNum" class="text-right text-sm text-[#333]">Start Value</label>
            <StyledInput 
                id="startNum" 
                type="number" 
                min="1" 
                step="1" 
                bind:value={pageLabel.startValue} 
                placeholder="Default: 1" 
                numericType="unsigned-integer"
                oninput={handleChange}
            />
        </div>

        <!-- Preview -->
        <PageLabelPreviewRow rule={pageLabel} labelWidth="100px" />
    </div>
</ArrowPopup>