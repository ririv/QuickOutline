<script lang="ts">
    import { onMount, onDestroy } from 'svelte';
    import { get } from 'svelte/store'; // Import get
    import StyledRadioGroup from '../controls/StyledRadioGroup.svelte';
    import { bookmarkStore } from '@/stores/bookmarkStore';
    import { rpc } from '@/lib/api/rpc';
    import { messageStore } from '@/stores/messageStore';
    import type { Bookmark } from './types';
    import formatIcon from '@/assets/icons/format.svg'; // Using text-edit for format

    let method = $state('sequential');
    const methodOptions = [
        { value: 'sequential', label: 'Sequential' },
        { value: 'indent', label: 'Indent' }
    ];

    let textValue = $state('');
    let unsubscribeStore: () => void;
    let debounceTimer: number | undefined;
    let highlightedMode = $state<string | null>(null); // State for highlight value
    let isExternalEditing = $state(false); // State for external editor mode

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

    // Handlers for RPC events
    const onExternalUpdate = (payload: { text: string }) => {
        console.log('Received external update');
        textValue = payload.text;
        bookmarkStore.setText(payload.text);
        // Also trigger tree update? The backend syncService usually updates backend state, 
        // but we might need to fetch the tree. 
        // However, `syncFromText` does both. 
        // Since backend state is updated by `syncService`, we can just fetch tree or trust backend sent everything.
        // For now, just update text. The tree view will react if we update store.tree?
        // We should probably ask backend for tree update or have backend push it.
        // Let's trigger a sync to be sure the tree view gets updated.
        debouncedSyncWithBackend(payload.text); 
    };

    const onExternalStart = () => {
        isExternalEditing = true;
        messageStore.add('External editor connected.', 'INFO');
    };

    const onExternalEnd = () => {
        isExternalEditing = false;
        messageStore.add('External editor disconnected.', 'INFO');
    };

    const onExternalError = (msg: string) => {
        isExternalEditing = false;
        messageStore.add('External editor error: ' + msg, 'ERROR');
    };

    onMount(() => {
        // Initialize textValue from store
        textValue = get(bookmarkStore).text;

        // Subscribe to store changes from other sources (e.g., TreeSubView, Get Contents)
        unsubscribeStore = bookmarkStore.subscribe(state => {
            if (state.text !== textValue) {
                textValue = state.text;
            }
        });

        // Register RPC listeners
        rpc.on('external-editor-update', onExternalUpdate);
        rpc.on('external-editor-start', onExternalStart);
        rpc.on('external-editor-end', onExternalEnd);
        rpc.on('external-editor-error', onExternalError);
    });

    onDestroy(() => {
        if (unsubscribeStore) {
            unsubscribeStore();
        }
        clearTimeout(debounceTimer); // Clear any pending debounced calls
        
        // Unregister RPC listeners
        rpc.off('external-editor-update', onExternalUpdate);
        rpc.off('external-editor-start', onExternalStart);
        rpc.off('external-editor-end', onExternalEnd);
        rpc.off('external-editor-error', onExternalError);
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
            
            // Update Method and show highlight
            method = 'indent';
            highlightedMode = 'indent';
            setTimeout(() => {
                highlightedMode = null;
            }, 1500); // Highlight for 1.5 seconds

        } catch (e: any) {
            messageStore.add('Auto-format failed: ' + e.message, 'ERROR');
        }
    }

    async function handleOpenInVSCode() {
        if (!textValue) {
            messageStore.add('Editor is empty, nothing to open.', 'WARNING');
            return;
        }
        try {
            await rpc.openExternalEditor(textValue);
            // UI state will be updated by events
        } catch (e: any) {
            messageStore.add('Failed to open in external editor: ' + (e.message || String(e)), 'ERROR');
        }
    }
</script>

<div class="flex flex-col h-full w-full bg-white">
    <!-- Top Toolbar -->
    <div class="flex items-center justify-between shrink-0 py-1 bg-white">
        <div class="flex items-center ml-2 pl-2">
            <span class="text-xs font-medium text-gray-500 uppercase tracking-wider">Mode:</span>
            <StyledRadioGroup 
                bind:value={method} 
                options={methodOptions}
                name="method" 
                layout="horizontal"
                highlightValue={highlightedMode}
            />
        </div>
        
        <div class="flex items-center gap-1 mr-2">
            <button 
                class="flex items-center gap-1.5 p-1.5 rounded hover:bg-gray-200 transition-colors text-xs text-gray-600 font-medium"
                onclick={handleAutoFormat} 
                title="Auto Format Indentation"
                disabled={isExternalEditing}
            >
                <img src={formatIcon} alt="Auto Format" class="w-4 h-4" />
                <span>Auto-Format</span>
            </button>
            <button
                class="flex items-center gap-1.5 p-1.5 rounded hover:bg-gray-200 transition-colors text-xs text-gray-600 font-medium"
                onclick={handleOpenInVSCode}
                title="Open in VS Code"
                disabled={isExternalEditing}
            >
                <!-- Placeholder for VS Code icon -->
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="lucide lucide-code"><polyline points="16 18 22 12 16 6"></polyline><polyline points="8 6 2 12 8 18"></polyline></svg>
                <span>{isExternalEditing ? 'Connected' : 'VS Code'}</span>
            </button>
        </div>
    </div>

    <!-- Editor Area -->
    <div class="flex-1 min-h-0 relative group
                border border-gray-200 rounded-lg mx-2
                focus-within:border-el-primary focus-within:ring-2 focus-within:ring-el-primary/20
                transition-all duration-200 overflow-hidden">
        
        {#if isExternalEditing}
            <div class="absolute inset-0 z-10 bg-gray-50/90 flex flex-col items-center justify-center text-gray-500">
                <div class="animate-spin mb-2">
                    <svg class="w-6 h-6" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                    </svg>
                </div>
                <span class="text-sm font-medium">Editing in VS Code...</span>
            </div>
        {/if}

        <textarea 
            class="w-full h-full resize-none outline-none border-none rounded-none shadow-none
                   font-mono text-sm leading-relaxed
                   bg-white text-gray-800 p-4 disabled:text-gray-400"
            placeholder="Enter bookmarks here..." 
            value={textValue} 
            oninput={handleInput}
            spellcheck="false"
            disabled={isExternalEditing}
        ></textarea>
    </div>
</div>
<style>
    /* No extra styles needed, Tailwind covers it */
</style>