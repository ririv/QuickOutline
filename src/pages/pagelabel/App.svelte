<script lang="ts">
    import '@/assets/global.css';
    import SplitPane from '../../components/SplitPane.svelte';
    import ThumbnailPane from '../../components/ThumbnailPane.svelte';
    import Icon from "@/components/Icon.svelte";
    
    import deleteIcon from '../../assets/icons/delete-item.svg?raw';
    import trashIcon from '../../assets/icons/trash.svg';
    import addIcon from '../../assets/icons/plus.svg?raw';
    import applyIcon from '../../assets/icons/success.svg?raw';

    import StyledSelect from '../../components/controls/StyledSelect.svelte';
    import StyledInput from "@/components/controls/StyledInput.svelte";
    import { ripple } from '@/lib/actions/ripple';
    import { messageStore } from '@/stores/messageStore.svelte.ts';
    import { docStore } from '@/stores/docStore.svelte';
    import { pageLabelStore } from '@/stores/pageLabelStore.svelte';
    import { PageLabelNumberingStyle, pageLabelStyleMap } from '@/lib/styleMaps';
    import { simulatePageLabelsLocal, type PageLabel } from '@/lib/pdf-processing/page-label';
    import { setPageLabels } from '@/lib/api/rust_pdf';
    import GraphButton from "@/components/controls/GraphButton.svelte";

    const styles = pageLabelStyleMap.getAllStyles();

    function addRule() {
        if (!pageLabelStore.startPage) {
             messageStore.add("Please enter Start Page", "WARNING");
             return;
        }

        const newRule = {
            id: Date.now().toString(),
            numberingStyleDisplay: pageLabelStore.numberingStyle == PageLabelNumberingStyle.NONE ? "" : pageLabelStyleMap.getDisplayText(pageLabelStore.numberingStyle),
            prefix: pageLabelStore.prefix,
            start: parseInt(pageLabelStore.startNumber) || 1,
            fromPage: parseInt(pageLabelStore.startPage) || 1
        };

        pageLabelStore.addRule(newRule);
        pageLabelStore.resetForm();
        simulate();
    }

    function deleteRule(ruleId: string) {
        pageLabelStore.deleteRule(ruleId);
        simulate();
    }

    function clearRules() {
        pageLabelStore.removeAllRules();
        pageLabelStore.setSimulatedLabels(docStore.originalPageLabels);
        pageLabelStore.resetForm();
    }

    function simulate() {
        const rules: PageLabel[] = pageLabelStore.rules.map(r => ({
            pageNum: r.fromPage,
            firstPage: r.start,
            labelPrefix: r.prefix,
            numberingStyle: pageLabelStyleMap.getEnumName(r.numberingStyleDisplay) || PageLabelNumberingStyle.DECIMAL_ARABIC_NUMERALS
        }));

        try {
            const labels = simulatePageLabelsLocal(rules, docStore.pageCount);
            pageLabelStore.setSimulatedLabels(labels);
        } catch (e) {
            console.error("Simulation failed", e);
        }
    }

    function apply() {
        if (!docStore.currentFilePath) {
            messageStore.add("No file opened.", "ERROR");
            return;
        }

        const rules: PageLabel[] = pageLabelStore.rules.map(r => ({
            pageNum: r.fromPage,
            firstPage: r.start,
            labelPrefix: r.prefix,
            numberingStyle: pageLabelStyleMap.getEnumName(r.numberingStyleDisplay) || PageLabelNumberingStyle.DECIMAL_ARABIC_NUMERALS
        }));
        
        setPageLabels(docStore.currentFilePath, rules, null).then(() => {
             messageStore.add("Page labels applied successfully!", "SUCCESS");
        }).catch(e => {
             messageStore.add("Failed to apply page labels: " + e.message, "ERROR");
        });
    }
</script>

<main class="h-full w-full overflow-hidden">
    <SplitPane initialSplit={30}>
        {#snippet left()}
        <div class="flex flex-col h-full p-4 bg-white box-border overflow-y-auto">
            <div class="flex flex-col gap-4">
                <div class="grid grid-cols-[120px_1fr] items-center gap-2.5">
                    <label for="style" class="text-right text-sm text-[#333]">Page Number Style</label>
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
                        onclick={addRule}
                    >
                        <Icon data={addIcon} class="w-4 h-4 opacity-70" />
                        Add Rule
                    </button>
                </div>
            </div>

            <div class="h-px bg-gray-200 my-5"></div>

            <div class="flex-1 overflow-hidden flex flex-col min-h-[150px]">
                <div class="flex items-center justify-between mb-2">
                    <h3 class="title m-0">Rule List</h3>
                    <GraphButton class="graph-button-important group"
                                 onclick={clearRules}
                                 title="Clear All Rules">
                        <img
                            src={trashIcon}
                            alt="Delete"
                            class="transition-[filter] duration-200 group-hover:[filter:invert(36%)_sepia(82%)_saturate(2268%)_hue-rotate(338deg)_brightness(95%)_contrast(94%)] group-active:[filter:invert(13%)_sepia(95%)_saturate(5686%)_hue-rotate(348deg)_brightness(82%)_contrast(106%)]"
                        />
                    </GraphButton>
                </div>
                <div class="flex-1 overflow-y-auto border border-el-default-border p-2 bg-white rounded-md">
                    {#each pageLabelStore.rules as rule (rule.id)}
                        <div class="flex items-center justify-between px-2 py-1 border-b border-[#f0f0f0] text-[13px] bg-transparent rounded mb-0.5 hover:bg-gray-50 transition-colors last:border-0 last:mb-0">
                            <div class="flex items-center gap-2 flex-1 overflow-hidden">
                                <span class="bg-el-plain-primary-bg text-el-primary border border-[#d9ecff] rounded px-1.5 py-0.5 text-xs font-semibold min-w-[32px] text-center shrink-0">
                                    P{rule.fromPage}
                                </span>
                                
                                <div class="flex flex-col justify-center overflow-hidden">
                                    <div class="flex items-center gap-1 font-medium text-[#303133] whitespace-nowrap overflow-hidden text-ellipsis leading-tight">
                                        {#if rule.prefix}
                                            <span class="text-[#606266] bg-[#f4f4f5] px-1 rounded-[3px] text-[11px] border border-[#e9e9eb]">{rule.prefix}</span>
                                        {/if}
                                        <span class="overflow-hidden text-ellipsis">{rule.numberingStyleDisplay}</span>
                                    </div>
                                    <div class="text-[10px] text-[#909399] leading-tight">
                                        Start: {rule.start}
                                    </div>
                                </div>
                            </div>

                            <button class="p-1 inline-flex items-center justify-center bg-transparent border-none cursor-pointer transition-colors rounded hover:bg-el-plain-important-bg-hover" onclick={() => deleteRule(rule.id)} title="Delete Rule">
                                <Icon data={deleteIcon} class="w-4 h-4 text-red-500" />
                            </button>
                        </div>
                    {/each}
                    {#if pageLabelStore.rules.length === 0}
                        <div class="p-4 text-center text-xs text-gray-400 italic">
                            No rules added yet.
                        </div>
                    {/if}
                </div>
            </div>

            <div class="mt-4 flex justify-center">
                 <button 
                    class="inline-flex items-center justify-center min-w-[140px] gap-1.5 px-4 py-2 text-sm font-medium text-el-primary rounded-md transition-colors duration-200 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 focus-visible:ring-offset-2 bg-el-plain-primary-bg border border-el-plain-primary-border hover:bg-el-plain-primary-bg-hover active:bg-el-plain-primary-border"
                    use:ripple={{ color: 'var(--color-el-primary-shadow)' }}
                    onclick={apply}
                >
                    <Icon data={applyIcon} class="w-4 h-4" />
                    Set Page Label
                </button>
            </div>
        </div>
        {/snippet}

        {#snippet right()}
        <div class="h-full bg-[#f5f5f5]">
            <ThumbnailPane pageCount={docStore.pageCount} />
        </div>
        {/snippet}
    </SplitPane>
</main>

<style>
    .title {
        font-size: 12px;
        font-weight: bold;
        color: #9198a1;
        margin-bottom: 10px;
        display: block;
    }
</style>
