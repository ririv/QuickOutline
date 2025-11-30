<script lang="ts">
    import { onMount, onDestroy } from 'svelte';
    import StyledButton from '../controls/StyledButton.svelte';
    import StyledRadioGroup from '../controls/StyledRadioGroup.svelte';
    import { bookmarkStore } from '@/stores/bookmarkStore';
    import { rpc } from '@/lib/api/rpc';
    import { messageStore } from '@/stores/messageStore';
    import type { Bookmark } from './types';

    let method = $state('sequential');
    const methodOptions = [
        { value: 'sequential', label: 'Sequential' },
        { value: 'indent', label: 'Indent' }
    ];

    let textValue = $state('');
    let unsubscribeStore: () => void;
    let debounceTimer: number | undefined;

    // Simple debounce function
    function debounce<T extends any[]>(func: (...args: T) => void, delay: number) {
        return function(this: any, ...args: T) {
            clearTimeout(debounceTimer);
            debounceTimer = setTimeout(() => func.apply(this, args), delay);
        };
    }

    // Debounced function to sync text changes with backend and update tree
    const debouncedSyncWithBackend = debounce(async (newText: string) => {
        try {
            const bookmarkDto: Bookmark = await rpc.syncFromText(newText);
            bookmarkStore.setTree(bookmarkDto.children || []);
            // No need to setText here, as it's already done instantly by handleInput
        } catch (e: any) {
            console.error("Failed to sync text with backend:", e);
            messageStore.add('Failed to sync changes: ' + (e.message || String(e)), 'ERROR');
        }
    }, 500); // 500ms debounce delay

    onMount(() => {
        // Initialize textValue from store
        textValue = bookmarkStore.text;

        // Subscribe to store changes from other sources (e.g., TreeSubView, Get Contents)
        unsubscribeStore = bookmarkStore.subscribe(state => {
            if (state.text !== textValue) {
                textValue = state.text;
            }
        });
    });

    onDestroy(() => {
        if (unsubscribeStore) {
            unsubscribeStore();
        }
        clearTimeout(debounceTimer); // Clear any pending debounced calls
    });


    function handleInput(e: Event) {
        const target = e.target as HTMLTextAreaElement;
        const newText = target.value;
        
        textValue = newText; // Update local state immediately for UI feedback
        bookmarkStore.setText(newText); // Update store immediately

        debouncedSyncWithBackend(newText); // Debounced call to backend
    }

    async function handleAutoFormat() {
        if (!textValue) return;
        try {
            const formatted = await rpc.autoFormat(textValue);
            
            // Sync formatted text to backend and get updated tree
            const bookmarkDto: Bookmark = await rpc.syncFromText(formatted);

            textValue = formatted;
            bookmarkStore.setText(formatted);
            bookmarkStore.setTree(bookmarkDto.children || []);
            
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