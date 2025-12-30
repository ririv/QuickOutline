<script lang="ts">
    import { onMount, onDestroy, untrack } from 'svelte';
    import { bookmarkStore } from '@/stores/bookmarkStore.svelte';
    import { docStore } from '@/stores/docStore.svelte';
    import { useExternalEditor } from '@/lib/bridge/useExternalEditor.svelte';
    import { processText, autoFormat, Method } from '@/lib/outlineParser';
    import { messageStore } from '@/stores/messageStore.svelte.ts';
    import type { BookmarkUI } from '@/lib/types/bookmark.ts';
    import formatIcon from '@/assets/icons/format.svg'; 
    import sequentialIcon from '@/assets/icons/mode-sequential.svg';
    import indentIcon from '@/assets/icons/mode-indent.svg';
    import IconSwitch from '../controls/IconSwitch.svelte';
    import BookmarkEditor from '../editor/BookmarkEditor.svelte';

    const modeOptions = [
        { value: Method.SEQ, label: 'Sequential', icon: sequentialIcon, title: 'Parse by Sequential Numbers (e.g., 1, 1.1, 2)' },
        { value: Method.INDENT, label: 'Indent', icon: indentIcon, title: 'Parse by Indentation Levels' }
    ];

    let textValue = $state('');
    let debounceTimer: number | undefined;
    let highlightedMode = $state<Method | null>(null);
    let isFocused = $state(false);
    let editor = $state<ReturnType<typeof BookmarkEditor> | undefined>();

    // Consume the bridge from context
    const externalEditor = useExternalEditor();

    // Derived values from bridge
    let isExternalEditing = $derived(externalEditor.isEditing);
    let editorName = $derived(externalEditor.editorName);

    // Simple debounce function
    function debounce<T extends any[]>(func: (...args: T) => void, delay: number) {
        return function(this: any, ...args: T) {
            clearTimeout(debounceTimer);
            debounceTimer = setTimeout(() => func.apply(this, args), delay);
        };
    }

    const debouncedSyncWithBackend = debounce(async (newText: string) => {
        try {
            const bookmarkDto: BookmarkUI = processText(newText, bookmarkStore.method);
            bookmarkStore.setTree(bookmarkDto.children || []);
        } catch (e: any) {
            console.error("Failed to sync text with backend:", e);
            messageStore.add('Failed to sync changes: ' + (e.message || String(e)), 'ERROR');
        }
    }, 500);

    // React to method changes
    $effect(() => {
        const method = bookmarkStore.method;
        untrack(() => {
            if (!textValue) return;
            try {
                const bookmarkDto: BookmarkUI = processText(textValue, method);
                bookmarkStore.setTree(bookmarkDto.children || []);
            } catch (e: any) {
                console.error("Failed to re-process text after method change:", e);
            }
        });
    });

    onMount(() => {
        textValue = bookmarkStore.text;
    });

    onDestroy(() => {
        clearTimeout(debounceTimer);
    });

    // Sync from Store to Local (only when not focused, or when external editor is active)
    $effect(() => {
        if (!isFocused || isExternalEditing) { 
            if (bookmarkStore.text !== textValue) {
                textValue = bookmarkStore.text;
                // Force a tree re-sync when text comes from external editor
                if (isExternalEditing) {
                    debouncedSyncWithBackend(textValue);
                }
            }
        }
    });

    function handleFocus() { isFocused = true; }
    function handleBlur() { isFocused = false; }

    function handleEditorChange(newText: string, changedLines: number[]) {
        textValue = newText;
        bookmarkStore.setText(newText);
        debouncedSyncWithBackend(newText);
    }

    async function handleAutoFormat() {
        if (!textValue) return;
        try {
            const formatted = autoFormat(textValue);
            const bookmarkDto: BookmarkUI = processText(formatted, Method.INDENT);
            textValue = formatted;
            bookmarkStore.setText(formatted);
            bookmarkStore.setTree(bookmarkDto.children || []);
            if (bookmarkStore.method !== Method.INDENT) {
                bookmarkStore.setMethod(Method.INDENT);
                highlightedMode = Method.INDENT;
                setTimeout(() => { highlightedMode = null; }, 1500);
            }
        } catch (e: any) {
            messageStore.add('Auto-format failed: ' + e.message, 'ERROR');
        }
    }

    async function handleOpenExternalEditor() {
        const { line, ch } = editor?.getCursor() || { line: 1, ch: 1 };
        await externalEditor.open(textValue, line, ch);
    }
</script>

<div class="flex flex-col h-full w-full bg-white">
    <!-- Top Toolbar -->
    <div class="flex items-center justify-between shrink-0 py-2 px-2 bg-white">
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
                class="flex items-center gap-1.5 px-2 py-1.5 rounded transition-all duration-300 text-xs font-medium border
                       {isExternalEditing 
                        ? 'bg-blue-50 text-blue-600 border-blue-200' 
                        : 'hover:bg-gray-100 text-gray-600 border-transparent hover:border-gray-200'}"
                onclick={handleOpenExternalEditor}
                title={isExternalEditing ? `Syncing with ${editorName}` : `Open in ${editorName}`}
                disabled={isExternalEditing}
            >
                {#if isExternalEditing}
                    <span class="relative flex h-2 w-2">
                        <span class="animate-ping absolute inline-flex h-full w-full rounded-full bg-blue-400 opacity-75"></span>
                        <span class="relative inline-flex rounded-full h-2 w-2 bg-blue-500"></span>
                    </span>
                {:else}
                    <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="lucide lucide-code opacity-70"><polyline points="16 18 22 12 16 6"></polyline><polyline points="8 6 2 12 8 18"></polyline></svg>
                {/if}
                <span>{isExternalEditing ? 'Syncing' : editorName}</span>
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
                <span class="text-sm font-medium">Editing in {editorName}...</span>
            </div>
        {/if}

        <BookmarkEditor 
            bind:this={editor}
            bind:value={textValue} 
            offset={bookmarkStore.offset}
            totalPage={docStore.pageCount}
            pageLabels={docStore.originalPageLabels}
            onchange={handleEditorChange}
            onFocus={handleFocus}
            onBlur={handleBlur}
        />
    </div>
</div>
