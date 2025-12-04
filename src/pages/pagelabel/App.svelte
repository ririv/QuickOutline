<script lang="ts">
    import '@/assets/global.css';
    import SplitPane from '../../components/SplitPane.svelte';
    import ThumbnailPane from '../../components/ThumbnailPane.svelte';
    import { onMount } from 'svelte';
    import deleteIcon from '../../assets/icons/delete-item.svg';
    import StyledSelect from '../../components/controls/StyledSelect.svelte';
    import { ripple } from '@/lib/actions/ripple';
    import { messageStore } from '@/stores/messageStore';
    import { docStore } from '@/stores/docStore';
    import { pageLabelStore, type PageLabelRule } from '@/stores/pageLabelStore';

    const styles = [
        "1, 2, 3, ...", 
        "I, II, III, ...", 
        "i, ii, iii, ...", 
        "A, B, C, ...", 
        "a, b, c, ..."
    ];

    // Equivalent to initialize()
    onMount(() => {
        console.log("PageLabelTab mounted");
        // TODO: Fetch original page labels from backend if needed, 
        // currently store persists state in memory as long as the app is running.
    });

    function addRule() {
        if (!$pageLabelStore.startPage) {
             // Simple validation
             messageStore.add("Please enter Start Page", "WARNING");
             return;
        }

        const newRule: PageLabelRule = {
            id: Date.now().toString(),
            style: $pageLabelStore.numberingStyle,
            styleDisplay: $pageLabelStore.numberingStyle,
            prefix: $pageLabelStore.prefix,
            start: parseInt($pageLabelStore.startNumber) || 1,
            fromPage: parseInt($pageLabelStore.startPage) || 1
        };

        pageLabelStore.addRule(newRule);
        pageLabelStore.resetForm();

        simulate();
    }

    function deleteRule(ruleId: string) {
        pageLabelStore.deleteRule(ruleId);
        simulate();
    }

    function simulate() {
        console.log("Simulating labels with rules:", $pageLabelStore.rules);
        // TODO: Call backend to simulate and update thumbnails/page labels
    }

    function apply() {
        console.log("Applying rules:", $pageLabelStore.rules);
        // TODO: Call backend to apply changes
    }

</script>

<main class="h-full w-full overflow-hidden">
    <SplitPane initialSplit={30}>
        {#snippet left()}
        <div class="flex flex-col h-full p-4 bg-white box-border overflow-y-auto">
            <!-- Form Section -->
            <div class="flex flex-col gap-4">
                <div class="grid grid-cols-[120px_1fr] items-center gap-2.5">
                    <label for="style" class="text-right text-sm text-[#333]">Page Number Style</label>
                    <div class="w-full">
                        <StyledSelect options={styles} bind:value={$pageLabelStore.numberingStyle} />
                    </div>
                </div>
                
                <div class="grid grid-cols-[120px_1fr] items-center gap-2.5">
                    <label for="prefix" class="text-right text-sm text-[#333]">Prefix</label>
                    <input id="prefix" type="text" bind:value={$pageLabelStore.prefix} class="input" placeholder="Optional" />
                </div>

                <div class="grid grid-cols-[120px_1fr] items-center gap-2.5">
                    <label for="startNum" class="text-right text-sm text-[#333]">Start Number</label>
                    <input id="startNum" type="text" bind:value={$pageLabelStore.startNumber} placeholder="1" class="input" />
                </div>

                <div class="grid grid-cols-[120px_1fr] items-center gap-2.5">
                    <label for="startPage" class="text-right text-sm text-[#333]">Start Page</label>
                    <input id="startPage" type="text" bind:value={$pageLabelStore.startPage} class="input" placeholder="e.g. 1 (Required)" />
                </div>

                <div class="flex justify-center mt-2.5">
                    <button 
                        class="inline-flex items-center justify-center w-[110px] gap-1.5 px-3 py-2 text-sm font-medium text-gray-700 rounded-md transition-colors duration-200 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-2 focus-visible:ring-blue-500 hover:bg-gray-100"
                        use:ripple
                        onclick={addRule}
                    >
                        <svg class="w-4 h-4 opacity-70" viewBox="0 0 16 16" fill="currentColor" xmlns="http://www.w3.org/2000/svg">
                            <path d="M8 3V13M3 8H13" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                        </svg>
                        Add Rule
                    </button>
                </div>
            </div>

            <div class="h-px bg-gray-200 my-5"></div>

            <!-- Rule List Section -->
            <div class="flex-1 overflow-hidden flex flex-col min-h-[150px]">
                <h3 class="title">Rule List</h3>
                <div class="flex-1 overflow-y-auto border border-el-default-border p-2 bg-white rounded-md">
                    {#each $pageLabelStore.rules as rule (rule.id)}
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
                                        <span class="overflow-hidden text-ellipsis">{rule.styleDisplay}</span>
                                    </div>
                                    <div class="text-[10px] text-[#909399] leading-tight">
                                        Start: {rule.start}
                                    </div>
                                </div>
                            </div>

                            <button class="p-1 inline-flex items-center justify-center bg-transparent border-none cursor-pointer transition-colors rounded hover:bg-el-plain-important-bg-hover" onclick={() => deleteRule(rule.id)} title="Delete Rule">
                                <img src={deleteIcon} alt="Delete" class="w-4 h-4" />
                            </button>
                        </div>
                    {/each}
                    {#if $pageLabelStore.rules.length === 0}
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
                    <svg class="w-4 h-4" viewBox="0 0 16 16" fill="currentColor" xmlns="http://www.w3.org/2000/svg">
                        <path d="M8 4.5L12.5 9H10V14H6V9H3.5L8 4.5Z" />
                        <path d="M3 2H13V3.5H3V2Z" />
                    </svg>
                    Set Page Label
                </button>
            </div>
        </div>
        {/snippet}

        {#snippet right()}
        <div class="h-full bg-[#f5f5f5]">
            <ThumbnailPane pageCount={$docStore.pageCount} />
        </div>
        {/snippet}
    </SplitPane>
</main>
