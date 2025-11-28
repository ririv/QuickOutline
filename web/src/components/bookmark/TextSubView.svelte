<script lang="ts">
    import StyledButton from '../controls/StyledButton.svelte';
    import StyledRadioGroup from '../controls/StyledRadioGroup.svelte';
    import { bookmarkStore } from '@/stores/bookmarkStore';
    import { rpc } from '@/lib/api/rpc';
    import { messageStore } from '@/stores/messageStore';

    let method = $state('sequential');
    const methodOptions = [
        { value: 'sequential', label: 'Sequential' },
        { value: 'indent', label: 'Indent' }
    ];

    let textValue = $state('');

    // Subscribe to store changes
    bookmarkStore.subscribe(state => {
        // Avoid cursor jumping loop if needed, but for now simple sync
        if (state.text !== textValue) {
            textValue = state.text;
        }
    });

    // Update store when text changes
    function handleInput(e: Event) {
        const target = e.target as HTMLTextAreaElement;
        textValue = target.value;
        bookmarkStore.setText(textValue);
    }

    async function handleAutoFormat() {
        if (!textValue) return;
        try {
            const formatted = await rpc.autoFormat(textValue);
            textValue = formatted;
            bookmarkStore.setText(formatted);
            messageStore.add('Auto-format successful', 'SUCCESS');
        } catch (e: any) {
            messageStore.add('Auto-format failed: ' + e.message, 'ERROR');
        }
    }
</script>

<div class="text-subview-container">
    <div class="editor-area">
        <textarea 
            placeholder="Enter bookmarks here..." 
            value={textValue} 
            oninput={handleInput}
        ></textarea>
    </div>
    <div class="sidebar">
        <StyledButton type="primary">VSCode</StyledButton>
        <StyledButton type="primary" onclick={handleAutoFormat}>Auto-Format</StyledButton>

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