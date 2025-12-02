<script lang="ts">
    import '@/assets/global.css';
    import SplitPane from '../../components/SplitPane.svelte';
    import ThumbnailPane from '../../components/ThumbnailPane.svelte';
    import { onMount } from 'svelte';
    import deleteIcon from '../../assets/icons/delete-item.svg';
    import plusIcon from '@/assets/icons/plus.svg';
    import uploadIcon from '@/assets/icons/upload.svg';
    import StyledSelect from '../../components/controls/StyledSelect.svelte';
    import { ripple } from '@/lib/actions/ripple';
    import { messageStore } from '@/stores/messageStore';
    import { appStore } from '@/stores/appStore';

    // Models
    interface PageLabelRule {
        id: string; // unique id for UI list
        style: string;
        styleDisplay: string;
        prefix: string;
        start: number;
        fromPage: number;
    }

    // State (Svelte 5 Runes)
    let rules = $state<PageLabelRule[]>([]);
    let numberingStyle = $state("1, 2, 3, ..."); // Default
    let prefix = $state("");
    let startNumber = $state("");
    let startPage = $state("");
    
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
        // TODO: Fetch original page labels from backend
    });

    function addRule() {
        if (!startPage) {
             // Simple validation
             messageStore.add("Please enter Start Page", "WARNING");
             return;
        }

        const newRule: PageLabelRule = {
            id: Date.now().toString(),
            style: numberingStyle, // simplified for now
            styleDisplay: numberingStyle,
            prefix: prefix,
            start: parseInt(startNumber) || 1,
            fromPage: parseInt(startPage) || 1
        };

        rules = [...rules, newRule];
        
        // Reset fields
        startPage = "";
        prefix = "";
        startNumber = "";

        simulate();
    }

    function deleteRule(ruleId: string) {
        rules = rules.filter(r => r.id !== ruleId);
        simulate();
    }

    function simulate() {
        console.log("Simulating labels with rules:", $state.snapshot(rules));
        // TODO: Call backend to simulate and update thumbnails/page labels
    }

    function apply() {
        console.log("Applying rules:", $state.snapshot(rules));
        // TODO: Call backend to apply changes
    }

</script>

<main>
    <SplitPane initialSplit={30}>
        {#snippet left()}
        <div class="control-pane">
            <div class="form-section">
                <div class="form-group">
                    <label for="style">Page Number Style</label>
                    <div style="width: 100%">
                        <StyledSelect options={styles} bind:value={numberingStyle} />
                    </div>
                </div>
                
                <div class="form-group">
                    <label for="prefix">Prefix</label>
                    <input id="prefix" type="text" bind:value={prefix} class="input" placeholder="Optional" />
                </div>

                <div class="form-group">
                    <label for="startNum">Start Number</label>
                    <input id="startNum" type="text" bind:value={startNumber} placeholder="1" class="input" />
                </div>

                <div class="form-group">
                    <label for="startPage">Start Page</label>
                    <input id="startPage" type="text" bind:value={startPage} class="input" placeholder="e.g. 1 (Required)" />
                </div>

                <div class="actions">
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

            <div class="separator bg-gray-200 h-px my-4"></div>

            <div class="rule-list-section">
                <h3 class="title">Rule List</h3>
                <div class="rule-list rounded-md border border-gray-200 bg-gray-50 p-0 overflow-hidden">
                    {#each rules as rule (rule.id)}
                        <div class="rule-item hover:bg-gray-100 transition-colors border-b border-gray-200 last:border-0 px-3 py-2">
                            <span class="rule-text text-sm text-gray-600">
                                <strong>P{rule.fromPage}:</strong> {rule.prefix}{rule.styleDisplay} (Start {rule.start})
                            </span>
                            <button class="graph-button graph-button-important" onclick={() => deleteRule(rule.id)} title="Delete Rule">
                                <img src={deleteIcon} alt="Delete" />
                            </button>
                        </div>
                    {/each}
                    {#if rules.length === 0}
                        <div class="p-4 text-center text-xs text-gray-400 italic">
                            No rules added yet.
                        </div>
                    {/if}
                </div>
            </div>

            <div class="bottom-actions">
                 <button 
                    class="inline-flex items-center justify-center min-w-[140px] gap-1.5 px-4 py-2 text-sm font-medium text-[#409eff] rounded-md transition-colors duration-200 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 focus-visible:ring-offset-2 bg-[#ecf5ff] border border-[#d9ecff] hover:bg-[#d9ecff] active:bg-[#c6e2ff]"
                    use:ripple={{ color: 'rgba(64,158,255,0.2)' }}
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
        <div class="preview-pane">
            <ThumbnailPane pageCount={$appStore.pageCount} />
        </div>
        {/snippet}
    </SplitPane>
</main>

<style>
    main {
        height: 100%;
        width: 100%;
        overflow: hidden;
    }
    .control-pane {
        display: flex;
        flex-direction: column;
        height: 100%;
        padding: 15px;
        background: white;
        box-sizing: border-box;
        overflow-y: auto;
    }
    .form-section {
        display: flex;
        flex-direction: column;
        gap: 15px;
    }
    .form-group {
        display: grid;
        grid-template-columns: 120px 1fr; /* Increased label width */
        align-items: center;
        gap: 10px;
    }
    .form-group label {
        text-align: right;
        font-size: 14px;
        color: #333;
    }
    /* .input style removed - provided by global.css */
    
    .actions {
        display: flex;
        justify-content: center;
        margin-top: 10px;
    }
    .separator {
        height: 1px;
        background: #eee;
        margin: 20px 0;
    }
    /* .title style removed - provided by global.css */

    .rule-list-section {
        flex: 1;
        overflow: hidden;
        display: flex;
        flex-direction: column;
        min-height: 150px; /* Ensure list has space */
    }
    .rule-list {
        flex: 1;
        overflow-y: auto;
        border: 1px solid #dcdfe6; /* using default border color var from global context manually here for now */
        padding: 5px;
        background: #fafafa;
    }
    .rule-item {
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: 8px;
        border-bottom: 1px solid #f0f0f0;
        font-size: 13px;
        background: white;
    }
    .rule-text {
        flex: 1;
        margin-right: 10px;
    }
    
    .bottom-actions {
        margin-top: 15px;
        display: flex;
        justify-content: center;
    }
    
    /* Button Styles removed - provided by global.css */
    
    .preview-pane {
        height: 100%;
        background: #f5f5f5;
    }
</style>
