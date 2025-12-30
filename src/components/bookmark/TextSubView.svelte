<script lang="ts">
    import { onMount, onDestroy, untrack } from 'svelte';
    import { bookmarkStore } from '@/stores/bookmarkStore.svelte';
    import { docStore } from '@/stores/docStore.svelte';
    import { appStore } from '@/stores/appStore.svelte';
    import { openExternalEditor } from '@/lib/api/rust_pdf';
    import { listen, type UnlistenFn } from '@tauri-apps/api/event';
    import { processText, autoFormat, Method } from '@/lib/outlineParser';
    import { messageStore } from '@/stores/messageStore.svelte.ts';
    import type { BookmarkUI } from '@/lib/types/bookmark.ts';
    import formatIcon from '@/assets/icons/format.svg'; // Using text-edit for format
    import sequentialIcon from '@/assets/icons/mode-sequential.svg';
    import indentIcon from '@/assets/icons/mode-indent.svg';
    import IconSwitch from '../controls/IconSwitch.svelte';
    import BookmarkEditor from '../editor/BookmarkEditor.svelte';

    // Using bookmarkStore.method directly with Enum
    const modeOptions = [
        { value: Method.SEQ, label: 'Sequential', icon: sequentialIcon, title: 'Parse by Sequential Numbers (e.g., 1, 1.1, 2)' },
        { value: Method.INDENT, label: 'Indent', icon: indentIcon, title: 'Parse by Indentation Levels' }
    ];

    let textValue = $state('');
    let debounceTimer: number | undefined;
    let highlightedMode = $state<Method | null>(null); // State for highlight value
    let isExternalEditing = $state(false); // State for external editor mode
    let isFocused = $state(false); // New state to track editor focus
    let unlistenFunctions: UnlistenFn[] = [];
    let editor = $state<ReturnType<typeof BookmarkEditor> | undefined>();

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
            // Use the global method from store directly
            const bookmarkDto: BookmarkUI = processText(newText, bookmarkStore.method);
            bookmarkStore.setTree(bookmarkDto.children || []);
            // No need to setText here, as it's already done instantly by handleInput
        } catch (e: any) {
            console.error("Failed to sync text with backend:", e);
            messageStore.add('Failed to sync changes: ' + (e.message || String(e)), 'ERROR');
        }
    }, 500); // 500ms debounce delay

    // React to method changes
    $effect(() => {
        const method = bookmarkStore.method;
        untrack(() => {
            if (!textValue) return;
            try {
                // Use method directly
                const bookmarkDto: BookmarkUI = processText(textValue, method);
                bookmarkStore.setTree(bookmarkDto.children || []);
            } catch (e: any) {
                console.error("Failed to re-process text after method change:", e);
            }
        });
    });

    onMount(async () => {
        // Initialize textValue from store
        textValue = bookmarkStore.text;

        // Register Tauri listeners
        unlistenFunctions.push(await listen<string>('external-editor-sync', (event) => {
            console.log('Received external update');
            const newText = event.payload;
            textValue = newText;
            bookmarkStore.setText(newText);
            debouncedSyncWithBackend(newText); 
        }));

        unlistenFunctions.push(await listen('external-editor-start', () => {
            isExternalEditing = true;
            messageStore.add('External editor connected.', 'INFO');
        }));

        unlistenFunctions.push(await listen('external-editor-end', () => {
            isExternalEditing = false;
            messageStore.add('External editor disconnected.', 'INFO');
        }));
    });

    onDestroy(() => {
        clearTimeout(debounceTimer); // Clear any pending debounced calls
        
        // Unregister listeners
        for (const unlisten of unlistenFunctions) {
            unlisten();
        }
    });

    // Sync from Store to Local (only when not focused, or when external editor is active)
    $effect(() => {
        if (!isFocused || isExternalEditing) { // Only update if not focused or external editing is happening
            if (bookmarkStore.text !== textValue) {
                textValue = bookmarkStore.text;
            }
        }
    });

    function handleFocus() {
        isFocused = true;
    }

    function handleBlur() {
        isFocused = false;
        // When blur, force sync in case there were updates while focused
        if (bookmarkStore.text !== textValue) {
            textValue = bookmarkStore.text;
        }
    }

    function handleEditorChange(newText: string, changedLines: number[]) {
        textValue = newText; // Update local state immediately for UI feedback
        bookmarkStore.setText(newText); // Update store immediately

        debouncedSyncWithBackend(newText); // Debounced call to backend
    }

    async function handleAutoFormat() {
        if (!textValue) return;
        try {
            const formatted = autoFormat(textValue);
            
            // Sync formatted text to backend and get updated tree
            // Force Indent mode when parsing auto-formatted text
            const bookmarkDto: BookmarkUI = processText(formatted, Method.INDENT);

            textValue = formatted;
            bookmarkStore.setText(formatted);
            bookmarkStore.setTree(bookmarkDto.children || []);
            
            // Update Method in store
            if (bookmarkStore.method !== Method.INDENT) {
                bookmarkStore.setMethod(Method.INDENT);
                // Only highlight if method actually changed
                highlightedMode = Method.INDENT;
                setTimeout(() => {
                    highlightedMode = null;
                }, 1500); // Highlight for 1.5 seconds
            }

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
            const { line, ch } = editor?.getCursor() || { line: 1, ch: 1 };
            await openExternalEditor(textValue, line, ch, appStore.externalEditor);
            // UI state will be updated by events
        } catch (e: any) {
            messageStore.add('Failed to open in external editor: ' + (e.message || String(e)), 'ERROR');
        }
    }
</script>

<div class="flex flex-col h-full w-full bg-white">
    <!-- Top Toolbar -->
    <div class="flex items-center justify-between shrink-0 py-2 px-2 bg-white">
        <!-- Mode Selector (IconSwitch) -->
        <IconSwitch
            bind:value={bookmarkStore.method}
            options={modeOptions}
            highlightValue={highlightedMode}
        />
        
        <div class="flex items-center gap-2">
            <button 
                class="flex items-center gap-1.5 px-2 py-1.5 rounded hover:bg-gray-100 text-xs text-gray-600 font-medium transition-colors border border-transparent hover:border-gray-200"
                onclick={handleAutoFormat} 
                title="Auto Format Indentation"
                disabled={isExternalEditing}
            >
                <img src={formatIcon} alt="Auto Format" class="w-4 h-4 opacity-70" />
                <span>Auto-Format</span>
            </button>
            <button
                class="flex items-center gap-1.5 px-2 py-1.5 rounded hover:bg-gray-100 text-xs text-gray-600 font-medium transition-colors border border-transparent hover:border-gray-200"
                onclick={handleOpenInVSCode}
                title="Open in VS Code"
                disabled={isExternalEditing}
            >
                <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="lucide lucide-code opacity-70"><polyline points="16 18 22 12 16 6"></polyline><polyline points="8 6 2 12 8 18"></polyline></svg>
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

        <BookmarkEditor 
            bind:this={editor}
            bind:value={textValue} 
            offset={bookmarkStore.offset}
            totalPage={docStore.pageCount}
            pageLabels={docStore.originalPageLabels}
            onchange={handleEditorChange}
            onFocus={() => isFocused = true}
            onBlur={() => isFocused = false}
        />
    </div>
</div>
<style>
    /* No extra styles needed, Tailwind covers it */
</style>