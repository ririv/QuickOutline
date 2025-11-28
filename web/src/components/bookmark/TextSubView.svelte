<script lang="ts">
    import StyledButton from '../controls/StyledButton.svelte';
    import StyledRadioGroup from '../controls/StyledRadioGroup.svelte';

    let method = $state('sequential');
    const methodOptions = [
        { value: 'sequential', label: 'Sequential' },
        { value: 'indent', label: 'Indent' }
    ];
</script>

<div class="text-subview-container">
    <div class="editor-area">
        <textarea placeholder="Enter bookmarks here..."></textarea>
    </div>
    <div class="sidebar">
        <StyledButton type="primary">VSCode</StyledButton>
        <StyledButton type="primary">Auto-Format</StyledButton>

        <StyledRadioGroup 
            name="method"
            bind:value={method}
            options={methodOptions}
            class="mt-auto flex flex-col gap-2.5 text-sm"
        />
    </div>
</div>

<style>
    .text-subview-container {
        display: flex;
        height: 100%;
        width: 100%;
    }
    .editor-area {
        flex: 1;
    }
    textarea {
        width: 100%;
        height: 100%;
        border: none;
        resize: none;
        outline: none;
        font-family: monospace;
        font-size: 14px;
        background-color: white; /* Match JavaFX */
        padding: 10px;
        box-sizing: border-box;
    }
    .sidebar {
        width: 150px;
        padding: 20px;
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 20px;
        border-left: 1px solid #dfdfdf;
        background-color: white; /* Match JavaFX */
    }
</style>
