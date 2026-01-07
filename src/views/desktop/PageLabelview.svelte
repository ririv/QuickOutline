<script lang="ts">
    import '@/assets/global.css';
    import SplitPane from '../../components/SplitPane.svelte';
    import PageLabelPreviewPane from '../../components/PageLabelPreviewPane.svelte';
    import Icon from "@/components/Icon.svelte";
    import deleteIcon from '../../assets/icons/delete-item.svg?raw';
    import trashIcon from '../../assets/icons/trash.svg';
    import resetIcon from '../../assets/icons/reset.svg';
    import applyIcon from '../../assets/icons/success.svg?raw';

    import { ripple } from '@/lib/actions/ripple.ts';
    import { docStore } from '@/stores/docStore.svelte.js';
    import { pageLabelStore } from '@/stores/pageLabelStore.svelte.js';
    import { pageLabelStyleMap } from '@/lib/types/page-label.ts';
    import GraphButton from "@/components/controls/GraphButton.svelte";

    import { usePageLabelActions } from '../shared/pagelabel.svelte.ts';
    import PageLabelForm from '@/components/pagelabel/PageLabelForm.svelte';
    import PageLabelFormModal from '@/components/pagelabel/PageLabelFormModal.svelte';

    const { deleteRule, clearRules, resetToOriginal, apply } = usePageLabelActions();

</script>

<main class="h-full w-full overflow-hidden">
    <PageLabelFormModal />
    <SplitPane initialSplit={67}>
        {#snippet left()}
        <div class="flex flex-col h-full p-4 bg-white box-border overflow-y-auto">
            <PageLabelForm />

            <div class="h-px bg-gray-200 my-5"></div>

            <div class="flex-1 overflow-hidden flex flex-col min-h-[150px]">
                <div class="flex items-center justify-between mb-2">
                    <h3 class="title m-0">Rule List</h3>
                    <div class="flex gap-1">
                        <GraphButton class="graph-button-important group"
                                     onclick={resetToOriginal}
                                     title="Reset to Original">
                            <img
                                src={resetIcon}
                                alt="Reset"
                                class="transition-[filter] duration-200 group-hover:[filter:invert(36%)_sepia(82%)_saturate(2268%)_hue-rotate(338deg)_brightness(95%)_contrast(94%)] group-active:[filter:invert(13%)_sepia(95%)_saturate(5686%)_hue-rotate(348deg)_brightness(82%)_contrast(106%)]"
                            />
                        </GraphButton>
                        <GraphButton class="graph-button-important group"
                                     onclick={clearRules}
                                     title="Clear All Rules (Delete)">
                            <img
                                src={trashIcon}
                                alt="Clear"
                                class="transition-[filter] duration-200 group-hover:[filter:invert(36%)_sepia(82%)_saturate(2268%)_hue-rotate(338deg)_brightness(95%)_contrast(94%)] group-active:[filter:invert(13%)_sepia(95%)_saturate(5686%)_hue-rotate(348deg)_brightness(82%)_contrast(106%)]"
                            />
                        </GraphButton>
                    </div>
                </div>
                <div class="flex-1 overflow-y-auto border border-el-default-border p-2 bg-white rounded-md">
                    {#each pageLabelStore.sortedRules as rule (rule.pageNum)}
                        <div class="flex items-center justify-between px-2 py-1 border-b border-[#f0f0f0] text-[13px] bg-transparent rounded mb-0.5 hover:bg-gray-50 transition-colors last:border-0 last:mb-0">
                            <div class="flex items-center gap-2 flex-1 overflow-hidden">
                                <span class="bg-el-plain-primary-bg text-el-primary border border-[#d9ecff] rounded px-1.5 py-0.5 text-xs font-semibold min-w-[32px] text-center shrink-0">
                                    P{rule.pageNum}
                                </span>

                                <div class="flex flex-col justify-center overflow-hidden">
                                    <div class="flex items-center gap-1 font-medium text-[#303133] whitespace-nowrap overflow-hidden text-ellipsis leading-tight">
                                        {#if rule.labelPrefix}
                                            <span class="text-[#606266] bg-[#f4f4f5] px-1 rounded-[3px] text-[11px] border border-[#e9e9eb]">{rule.labelPrefix}</span>
                                        {/if}
                                        <span class="overflow-hidden text-ellipsis">{pageLabelStyleMap.getDisplayText(rule.numberingStyle)}</span>
                                    </div>
                                    <div class="text-[10px] text-[#909399] leading-tight">
                                        Start: {rule.firstPage ?? 1}
                                    </div>
                                </div>
                            </div>

                            <button class="p-1 inline-flex items-center justify-center bg-transparent border-none cursor-pointer transition-colors rounded hover:bg-el-plain-important-bg-hover" onclick={() => deleteRule(rule.pageNum)} title="Delete Rule">
                                <Icon data={deleteIcon} class="w-4 h-4 text-red-500" />
                            </button>
                        </div>
                    {/each}
                    {#if pageLabelStore.sortedRules.length === 0}
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
            <PageLabelPreviewPane pageCount={docStore.pageCount} />
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