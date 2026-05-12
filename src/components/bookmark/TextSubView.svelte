<script lang="ts">
    import { onMount, onDestroy, untrack } from 'svelte';
    import { bookmarkStore } from '@/stores/bookmarkStore.svelte';
    import { docStore } from '@/stores/docStore.svelte';
    import { useExternalEditor } from '@/lib/bridge/useExternalEditor.svelte';
    import { processText, autoFormat, Method } from 'outline-parser';
    import { messageStore } from '@/stores/messageStore.svelte.ts';
    import type { BookmarkUI } from 'outline-parser/bookmark';
    import formatIcon from '@/assets/icons/format.svg'; 
    import sequentialIcon from '@/assets/icons/mode-sequential.svg';
    import indentIcon from '@/assets/icons/mode-indent.svg';
    import IconSwitch from 'shared-kit/controls/IconSwitch.svelte';
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
    let isUpdatingFromStore = false; // Lock to prevent circular updates

    const externalEditor = useExternalEditor();
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

    $effect(() => {
        if (!isFocused || isExternalEditing) { 
            if (bookmarkStore.text !== textValue) {
                isUpdatingFromStore = true;
                try {
                    textValue = bookmarkStore.text;
                } finally {
                    // CodeMirror updates are synchronous, so this lock works
                    isUpdatingFromStore = false;
                }
                
                if (isExternalEditing) {
                    debouncedSyncWithBackend(textValue);
                }
            }
        }
    });

    function handleFocus() { isFocused = true; }
    function handleBlur() { isFocused = false; }

    function handleEditorChange(newText: string, changedLines: number[]) {
        // Prevent infinite loop if the update came from the store (via prop binding)
        if (newText === bookmarkStore.text) return;
        
        if (isUpdatingFromStore) {
            // console.log(`[TextSubView] Loop prevented. Store len: ${bookmarkStore.text.length}, Editor len: ${newText.length}`);
            return;
        }

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
    <div class="bookmark-toolbar flex items-center justify-between shrink-0 py-2 px-2 bg-white">
        <IconSwitch
            class="parse-mode-switch"
            bind:value={bookmarkStore.method}
            options={modeOptions}
            highlightValue={highlightedMode}
        />
        
        <div class="toolbar-actions flex items-center gap-2">
            <button 
                class="toolbar-action-btn flex items-center gap-1.5 px-2 py-1.5 rounded hover:bg-gray-100 text-xs text-gray-600 font-medium transition-colors border border-transparent hover:border-gray-200"
                onclick={handleAutoFormat} 
                title="Auto Format Indentation"
                disabled={isExternalEditing}
            >
                <img src={formatIcon} alt="Auto Format" class="w-4 h-4 opacity-70 shrink-0" />
                <span class="toolbar-action-label">Auto-Format</span>
            </button>
            <button
                class="toolbar-action-btn flex items-center gap-1.5 px-2 py-1.5 rounded transition-all duration-300 text-xs font-medium border
                       {isExternalEditing 
                        ? 'bg-blue-50 text-blue-600 border-blue-200' 
                        : 'hover:bg-gray-100 text-gray-600 border-transparent hover:border-gray-200'}"
                onclick={handleOpenExternalEditor}
                title={isExternalEditing ? `Syncing with ${editorName}` : `Open in ${editorName}`}
                disabled={isExternalEditing}
            >
                {#if isExternalEditing}
                    <span class="relative flex h-2 w-2 shrink-0">
                        <span class="animate-ping absolute inline-flex h-full w-full rounded-full bg-blue-400 opacity-75"></span>
                        <span class="relative inline-flex rounded-full h-2 w-2 bg-blue-500"></span>
                    </span>
                {:else}
                    <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="lucide lucide-code opacity-70 shrink-0"><polyline points="16 18 22 12 16 6"></polyline><polyline points="8 6 2 12 8 18"></polyline></svg>
                {/if}
                <span class="toolbar-action-label">{isExternalEditing ? 'Syncing' : editorName}</span>
            </button>
        </div>
    </div>

    <!-- Editor Area -->
    <div class="flex-1 min-h-0 relative group
                border rounded-lg mx-2 transition-all duration-500 overflow-hidden
                {isExternalEditing ? 'border-blue-400/20 shadow-inner' : 'border-gray-200'}">
        
        {#if isExternalEditing}
            <!-- Transparent Subtle Overlay (No Blur) -->
            <div class="absolute inset-0 z-20 pointer-events-none flex flex-col items-center justify-center animate-in fade-in duration-500 bg-gray-400/10">
                <div class="flex flex-col items-center gap-2 text-gray-500">
                    <svg xmlns="http://www.w3.org/2000/svg" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" class="lucide lucide-link opacity-60"><path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"></path><path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"></path></svg>
                    <span class="text-[11px] font-semibold opacity-90 tracking-tight text-gray-600">Syncing with {editorName}</span>
                </div>
            </div>
            <!-- Invisible Click Blocker -->
            <div class="absolute inset-0 z-10 cursor-not-allowed"></div>
        {/if}

        <div class="h-full transition-all duration-500 {isExternalEditing ? 'opacity-70' : ''}">
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
</div>

<style>
    .bookmark-toolbar {
        container-type: inline-size;
        gap: 8px;
        overflow-x: auto;
        scrollbar-width: none;
    }

    .bookmark-toolbar::-webkit-scrollbar {
        display: none;
    }

    .toolbar-actions,
    .toolbar-action-btn {
        flex: 0 0 auto;
    }

    .toolbar-action-btn {
        white-space: nowrap;
    }

    .bookmark-toolbar :global(.parse-mode-switch) {
        flex: 0 0 auto;
    }

    @container (max-width: 430px) {
        .bookmark-toolbar {
            gap: 4px;
        }

        .toolbar-actions {
            gap: 4px;
        }

        .bookmark-toolbar :global(.icon-switch-label) {
            display: none;
        }

        .bookmark-toolbar :global(.icon-switch-root) {
            gap: 0;
        }
    }

    @container (max-width: 360px) {
        .toolbar-action-label {
            display: none;
        }

        .toolbar-action-btn {
            padding-inline: 8px;
        }
    }
</style>
