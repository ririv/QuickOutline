<script lang="ts">
    import '@/assets/global.css';
    import SplitPane from '../../components/SplitPane.svelte';
    import ThumbnailPane from '../../components/ThumbnailPane.svelte';
    import { onMount } from 'svelte';
    import deleteIcon from '../../assets/icons/delete-item.svg';
    import StyledSelect from '../../components/controls/StyledSelect.svelte';
    import StyledButton from '../../components/controls/StyledButton.svelte';
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
                    <input id="prefix" type="text" bind:value={prefix} class="input" />
                </div>

                <div class="form-group">
                    <label for="startNum">Start Number</label>
                    <input id="startNum" type="text" bind:value={startNumber} placeholder="1" class="input" />
                </div>

                <div class="form-group">
                    <label for="startPage">Start Page</label>
                    <input id="startPage" type="text" bind:value={startPage} class="input" />
                </div>

                <div class="actions">
                    <StyledButton type="primary" onclick={addRule}>Add Rule</StyledButton>
                </div>
            </div>

            <div class="separator"></div>

            <div class="rule-list-section">
                <h3 class="title">Rule List</h3>
                <div class="rule-list">
                    {#each rules as rule (rule.id)}
                        <div class="rule-item">
                            <span class="rule-text">
                                From Page {rule.fromPage}: Style={rule.styleDisplay}, Prefix='{rule.prefix}', Start={rule.start}
                            </span>
                            <button class="graph-button graph-button-important" onclick={() => deleteRule(rule.id)} title="Delete Rule">
                                <img src={deleteIcon} alt="Delete" />
                            </button>
                        </div>
                    {/each}
                </div>
            </div>

            <div class="bottom-actions">
                 <StyledButton type="important" onclick={apply}>Set Page Label</StyledButton>
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
