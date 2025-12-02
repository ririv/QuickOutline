<script lang="ts">
    import type { Bookmark } from "./types";
    import BookmarkNode from "./BookmarkNode.svelte";
    import { onMount, onDestroy, setContext } from 'svelte';
    import { bookmarkStore } from '@/stores/bookmarkStore';
    import { rpc } from '@/lib/api/rpc';
    import { messageStore } from '@/stores/messageStore';
    import { get } from 'svelte/store';
    import { appStore } from '@/stores/appStore';
    import PreviewTooltip from '../PreviewTooltip.svelte';

    let bookmarks = $state<Bookmark[]>([]);
    let unsubscribeStore: () => void;
    let debounceTimer: number | undefined;
    
    // Preview State
    let hoveredPage = $state<{src: string, y: number, x: number} | null>(null);

    setContext('previewContext', {
        show: (src: string, y: number, x: number) => {
            hoveredPage = { src, y, x };
        },
        hide: () => {
            hoveredPage = null;
        }
    });

    let isAllExpanded = $state(true);

    function toggleAll() {
        isAllExpanded = !isAllExpanded;
        function traverse(nodes: Bookmark[]) {
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
    const debouncedSyncTreeWithBackend = debounce(async (tree: Bookmark[]) => {
        try {
            // Construct a virtual root BookmarkDto for sending to backend
            const rootDto: Bookmark = {
                id: 'virtual-root', // Use a consistent ID for the virtual root
                title: 'Virtual Root',
                page: null,
                level: 0,
                children: tree
            };
            const text = await rpc.syncFromTree(rootDto);
            bookmarkStore.setText(text); // Update text store with new text from tree
        } catch (e: any) {
            console.error("Failed to sync tree with backend:", e);
            messageStore.add('Failed to sync changes: ' + (e.message || String(e)), 'ERROR');
        }
    }, 500); // 500ms debounce delay


    onMount(() => {
        // Initialize bookmarks from store
        bookmarks = get(bookmarkStore).tree;

        // Subscribe to store changes from other sources (e.g., TextSubView, Get Contents)
        unsubscribeStore = bookmarkStore.subscribe(state => {
            // Check for deep equality to avoid unnecessary updates and re-renders if the tree is the same
            if (JSON.stringify(state.tree) !== JSON.stringify(bookmarks)) {
                bookmarks = state.tree;
            }
        });
    });

    onDestroy(() => {
        if (unsubscribeStore) {
            unsubscribeStore();
        }
        clearTimeout(debounceTimer); // Clear any pending debounced calls
    });

    // React to changes in the bookmarks array and trigger debounced sync
    $effect(() => {
        // We need a deep watch for changes in the tree structure
        // Svelte's $state reactivity tracks changes at the top level.
        // For nested objects, we rely on the fact that direct mutations to properties
        // of objects within the array are observed.
        // Stringify for simple deep comparison, but consider more efficient deep equality for large trees.
        // For now, simple stringify to detect any changes and trigger sync.
        debouncedSyncTreeWithBackend(bookmarks);
        bookmarkStore.setTree(bookmarks); // Keep the store's tree up-to-date with local mutations
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
        <div class="tree-column-page flex items-center justify-center">Page</div>
    </div>
    <div class="tree-body">
        {#each bookmarks as bookmark}
            <BookmarkNode {bookmark} />
        {/each}
    </div>
    
    {#if hoveredPage}
        <PreviewTooltip 
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