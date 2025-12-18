<script lang="ts">
    import type { BookmarkUI } from "./types";
    import BookmarkNode from "./BookmarkNode.svelte";
    import { onMount, onDestroy, setContext, untrack } from 'svelte';
    import { bookmarkStore } from '@/stores/bookmarkStore.svelte';
    import { rpc } from '@/lib/api/rpc';
    import { serializeBookmarkTree } from '@/lib/outlineParser';
    import { messageStore } from '@/stores/messageStore';
    import { appStore } from '@/stores/appStore';
    import PreviewPopup from '../PreviewPopup.svelte';

    let bookmarks = $state<BookmarkUI[]>([]);
    let debounceTimer: number | undefined;
    
    // Preview State
    let hoveredPage = $state<{src: string, y: number, x: number} | null>(null);
    let showOffsetPage = $state(false);

    setContext('previewContext', {
        show: (src: string, y: number, x: number) => {
            hoveredPage = { src, y, x };
        },
        hide: () => {
            hoveredPage = null;
        }
    });

    setContext('offsetContext', {
        get show() { return showOffsetPage; }
    });

    let isAllExpanded = $state(true);

    function toggleAll() {
        isAllExpanded = !isAllExpanded;
        function traverse(nodes: BookmarkUI[]) {
            for (const node of nodes) {
                node.expanded = isAllExpanded;
                if (node.children && node.children.length > 0) {
                    traverse(node.children);
                }
            }
        }
        traverse(bookmarks);
    }

    // Simple debounce function (copied from TextSubView for consistency)
    function debounce<T extends any[]>(func: (...args: T) => void, delay: number) {
        return function(this: any, ...args: T) {
            clearTimeout(debounceTimer);
            debounceTimer = setTimeout(() => func.apply(this, args), delay);
        };
    }

    // Debounced function to sync tree changes with backend and update text
    const debouncedSyncTreeWithBackend = debounce(async (tree: BookmarkUI[]) => {
        try {
            // Construct a virtual root BookmarkDto for sending to backend
            const rootDto: BookmarkUI = {
                id: 'virtual-root', // Use a consistent ID for the virtual root
                title: 'Virtual Root',
                pageNum: null,
                level: 0,
                children: tree
            };
            const text = serializeBookmarkTree(rootDto);
            // Only update text if it's different to avoid loops
            if (bookmarkStore.text !== text) {
                bookmarkStore.setText(text);
            }
        } catch (e: any) {
            console.error("Failed to sync tree with backend:", e);
            messageStore.add('Failed to sync changes: ' + (e.message || String(e)), 'ERROR');
        }
    }, 500); // 500ms debounce delay


    onMount(() => {
        // Initialize bookmarks from store
        bookmarks = bookmarkStore.tree;
    });

    onDestroy(() => {
        clearTimeout(debounceTimer); // Clear any pending debounced calls
    });

    // Sync from Store to Local
    $effect(() => {
        const storeTree = bookmarkStore.tree; // Track store
        untrack(() => {
             // Check for deep equality to avoid unnecessary updates and re-renders if the tree is the same
             // Using JSON stringify is a bit expensive but robust for deep structures
             if (JSON.stringify(storeTree) !== JSON.stringify(bookmarks)) {
                 bookmarks = storeTree;
             }
        });
    });

    // Sync from Local to Store & Backend
    $effect(() => {
        // Track local bookmarks changes (including deep changes due to JSON.stringify usage implicitly or just access)
        JSON.stringify(bookmarks); // Explicitly track deep changes
        const currentBookmarks = bookmarks;
        
        untrack(() => {
            // Check if store is already same as local to prevent loop
            const isDifferent = JSON.stringify(bookmarkStore.tree) !== JSON.stringify(currentBookmarks);
            
            if (isDifferent) {
                bookmarkStore.setTree(currentBookmarks); // Keep the store's tree up-to-date with local mutations
                debouncedSyncTreeWithBackend(currentBookmarks);
            }
        });
    });
</script>

<div class="tree-subview-container">
    <div class="tree-header">
        <div class="tree-column-title relative flex items-center justify-center">
            <div class="absolute left-2 flex gap-1">
                <button 
                    class="w-4 h-4 flex items-center justify-center text-gray-400 hover:text-gray-600 cursor-pointer border-none bg-transparent p-0" 
                    onclick={toggleAll}
                    title={isAllExpanded ? "Collapse All" : "Expand All"}
                >
                    {#if isAllExpanded}
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                            <path d="M7 4L12 9L17 4"/>
                            <path d="M7 20L12 15L17 20"/>
                        </svg>
                    {:else}
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                            <path d="M7 9L12 4L17 9"/>
                            <path d="M7 15L12 20L17 15"/>
                        </svg>
                    {/if}
                </button>
            </div>
            <span>Title</span>
        </div>
        <div class="tree-column-page flex items-center justify-center gap-1">
            <span>Page</span>
            <button 
                class="w-4 h-4 flex items-center justify-center text-gray-400 hover:text-gray-600 cursor-pointer border-none bg-transparent p-0" 
                onclick={() => showOffsetPage = !showOffsetPage}
                title={showOffsetPage ? "Show Original Page Numbers" : "Show Offset Page Numbers"}
            >
                {#if showOffsetPage}
                    <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="lucide lucide-hash">
                        <line x1="4" x2="20" y1="9" y2="9" />
                        <line x1="4" x2="20" y1="15" y2="15" />
                        <line x1="10" x2="8" y1="3" y2="21" />
                        <line x1="16" x2="14" y1="3" y2="21" />
                    </svg>
                {:else}
                    <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="lucide lucide-file-digit">
                        <path d="M4 22h14a2 2 0 0 0 2-2V7.5L14.5 2H6a2 2 0 0 0-2 2v4" />
                        <path d="M14 2v6h6" />
                        <path d="M5 12h3a2 2 0 1 1 0 4H5v4" />
                    </svg>
                {/if}
            </button>
        </div>
    </div>
    <div class="tree-body">
        {#each bookmarks as bookmark (bookmark.id)}
            <BookmarkNode {bookmark} />
        {/each}
    </div>
    
    {#if hoveredPage}
        <PreviewPopup
            src={hoveredPage.src} 
            y={hoveredPage.y} 
            anchorX={hoveredPage.x} 
        />
    {/if}
</div>

<style>
    .tree-subview-container {
        width: 100%;
        height: 100%;
        display: flex;
        flex-direction: column;
        background-color: white;
        font-size: 14px;
    }
    .tree-header {
        display: flex;
        background-color: white;
        color: #888888;
        font-weight: 500;
        flex-shrink: 0;
    }
    .tree-column-title {
        flex: 0.9;
        padding: 8px 12px;
    }
    .tree-column-page {
        flex: 0.1;
        min-width: 80px;
        padding: 8px 12px;
    }
    .tree-body {
        flex: 1;
        overflow-y: auto;
    }
</style>