<script lang="ts">
    import { onMount, onDestroy } from 'svelte';
    import { get } from 'svelte/store'; // Import get
    import StyledButton from '../controls/StyledButton.svelte';
    import StyledRadioGroup from '../controls/StyledRadioGroup.svelte';
    import { bookmarkStore } from '@/stores/bookmarkStore';
    import { rpc } from '@/lib/api/rpc';
    import { messageStore } from '@/stores/messageStore';
    import type { Bookmark } from './types';
    import formatIcon from '@/assets/icons/text-edit.svg'; // Using text-edit for format

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
        textValue = get(bookmarkStore).text;

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

<div class="flex flex-col h-full w-full bg-gray-50">
    <!-- Top Toolbar -->
    <div class="flex items-center justify-between shrink-0 px-3 py-1 border-b border-gray-200 bg-white">
        <div class="flex items-center gap-3">
            <span class="text-xs font-medium text-gray-500 uppercase tracking-wider">Mode:</span>
            <StyledRadioGroup 
                bind:value={method} 
                options={methodOptions}
                name="method" 
                layout="horizontal"
            />
        </div>
        
        <div class="flex items-center gap-1">
            <button 
                class="p-1.5 rounded hover:bg-gray-200 transition-colors"
                onclick={handleAutoFormat} 
                title="Auto Format Indentation"
            >
                <img src={formatIcon} alt="Auto Format" class="w-4 h-4" />
            </button>
            <button
                class="p-1.5 rounded hover:bg-gray-200 transition-colors"
                title="Open in VS Code"
            >
                <!-- Placeholder for VS Code icon -->
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="lucide lucide-code"><polyline points="16 18 22 12 16 6"></polyline><polyline points="8 6 2 12 8 18"></polyline></svg>
            </button>
        </div>
    </div>

    <!-- Editor Area -->
    <div class="flex-1 min-h-0 relative group
                border border-gray-200 rounded-lg m-2 
                focus-within:border-el-primary focus-within:ring-2 focus-within:ring-el-primary/20
                transition-all duration-200 overflow-hidden">
        <textarea 
            class="w-full h-full resize-none outline-none border-none rounded-none shadow-none
                   font-mono text-sm leading-relaxed
                   bg-white text-gray-800 p-4"
            placeholder="Enter bookmarks here..." 
            value={textValue} 
            oninput={handleInput}
            spellcheck="false"
        ></textarea>
    </div>
</div>
<style>
    /* No extra styles needed, Tailwind covers it */
</style>