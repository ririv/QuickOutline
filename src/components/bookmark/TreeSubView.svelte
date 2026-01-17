<script lang="ts">
    import type { BookmarkUI } from "@/lib/types/bookmark.ts";
    import BookmarkNode from "./BookmarkNode.svelte";
    import { onMount, onDestroy, setContext, untrack, tick } from 'svelte';
    import { bookmarkStore } from '@/stores/bookmarkStore.svelte';
    import { serializeBookmarkTree } from '@/lib/outlineParser';
    import { messageStore } from '@/stores/messageStore.svelte.ts';
    import { moveNode, getVisibleNodes } from '@/lib/utils/treeUtils';
    import { calculateDragState } from '@/lib/drag-drop/dragLogic';
    import PreviewPopup from '../PreviewPopup.svelte';
    import offsetIconRaw from '@/assets/icons/offset.svg?raw';
    import { DragController } from '@/lib/drag-drop/DragController.svelte';
    import { setupTauriDragDrop } from '@/lib/drag-drop/tauriDragDrop';
    import Icon from '../Icon.svelte';

    let bookmarks = $state<BookmarkUI[]>([]);
    let debounceTimer: number | undefined;
    
    // Drag Controller Instance
    const dragController = new DragController(
        () => bookmarks,
        (b) => { bookmarks = b; }
    );
    
    setContext('dragContext', dragController);

    let unlistenFns: (() => void)[] = [];

    onMount(async () => {
        // Initialize bookmarks from store
        isUpdatingFromStore = true;
        bookmarks = bookmarkStore.tree;
        tick().then(() => isUpdatingFromStore = false);

        // Tauri Drag & Drop Strategy
        const cleanup = await setupTauriDragDrop({
            getDraggedId: () => dragController.draggedNodeId,
            onDragOver: (targetId, position, coords) => {
                if (targetId && position && coords) {
                    // We need to use calculateDragState to get the correct level/gap
                    // We need to find the node element to get relative coordinates
                    // But setupTauriDragDrop already found the element to determine targetId.
                    // We can query it again.
                    const nodeElement = document.querySelector(`.node-row[data-id="${targetId}"]`) as HTMLElement;
                    if (nodeElement) {
                        const rect = nodeElement.getBoundingClientRect();
                        const visibleNodes = getVisibleNodes(bookmarks);
                        
                        const state = calculateDragState(
                            coords.y - rect.top,
                            rect.height,
                            coords.x - rect.left,
                            targetId,
                            dragController.draggedNodeId!,
                            visibleNodes
                        );
                        
                        if (state) {
                            dragController.updateState(
                                state.dropTargetId,
                                state.dropPosition,
                                state.dropTargetLevel,
                                state.gapNodeId,
                                state.gapPosition
                            );
                            return;
                        }
                    }
                    
                    // Fallback if calculation fails (e.g. element not found)
                    dragController.updateState(targetId, position, 1, targetId, position);
                } else {
                    dragController.reset();
                }
            },
            onDrop: (targetId, position) => {
                if (dragController.draggedNodeId) {
                    dragController.move(dragController.draggedNodeId, targetId, position);
                }
                dragController.reset();
            },
            onDragLeave: () => {
                dragController.reset();
            }
        });
        unlistenFns.push(cleanup);
    });

    onDestroy(() => {
        clearTimeout(debounceTimer); // Clear any pending debounced calls
        unlistenFns.forEach(fn => fn());
    });

    function handleDragOver(e: DragEvent) {
        if (!dragController.draggedNodeId) return;
        e.preventDefault();
        e.stopPropagation();

        const element = document.elementFromPoint(e.clientX, e.clientY);
        const containerElement = element?.closest('.node-container') as HTMLElement;
        const visibleNodes = getVisibleNodes(bookmarks);
        
        if (!containerElement) {
            // Drop in empty space
            if (visibleNodes.length > 0) {
                const lastNode = visibleNodes[visibleNodes.length - 1];
                if (lastNode.id !== dragController.draggedNodeId) {
                    dragController.updateState(lastNode.id, 'after', 1, lastNode.id, 'after');
                }
            }
            return;
        }

        const hitId = containerElement.dataset.id;
        if (!hitId) return;

        // Use the row inside container for precise coordinate calculation
        const nodeRow = containerElement.querySelector('.node-row') as HTMLElement;
        if (!nodeRow) return;

        const rect = nodeRow.getBoundingClientRect();
        const state = calculateDragState(
            e.clientY - rect.top,
            rect.height,
            e.clientX - rect.left,
            hitId,
            dragController.draggedNodeId,
            visibleNodes
        );

        if (state) {
            dragController.updateState(
                state.dropTargetId,
                state.dropPosition,
                state.dropTargetLevel,
                state.gapNodeId,
                state.gapPosition
            );
        } else {
            dragController.reset();
        }
    }

    function handleDrop(e: DragEvent) {
        e.preventDefault();
        if (dragController.draggedNodeId && dragController.dropTargetId && dragController.dropPosition) {
            dragController.move(dragController.draggedNodeId, dragController.dropTargetId, dragController.dropPosition);
        }
        dragController.setDraggedNodeId(null);
    }

    function handleDragLeave(e: DragEvent) {
         const related = e.relatedTarget as HTMLElement;
         const current = e.currentTarget as HTMLElement;
         if (!current.contains(related)) {
             dragController.reset();
         }
    }
    
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


    let isUpdatingFromStore = false;

    onMount(() => {
        // Initialize bookmarks from store
        isUpdatingFromStore = true;
        bookmarks = bookmarkStore.tree;
        tick().then(() => isUpdatingFromStore = false);
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
                 isUpdatingFromStore = true;
                 bookmarks = storeTree;
                 tick().then(() => isUpdatingFromStore = false);
             }
        });
    });

    // Sync from Local to Store & Backend
    $effect(() => {
        // Track local bookmarks changes (including deep changes due to JSON.stringify usage implicitly or just access)
        JSON.stringify(bookmarks); // Explicitly track deep changes
        const currentBookmarks = bookmarks;
        
        untrack(() => {
            if (isUpdatingFromStore) return;

            // Always sync when local changes are detected, assuming they are user interactions.
            // We skip the JSON comparison against the store because shared references (e.g. from drag & drop mutations)
            // can make the store and local state appear identical even when a sync is needed.
            bookmarkStore.setTree(currentBookmarks); // Keep the store's tree up-to-date with local mutations
            debouncedSyncTreeWithBackend(currentBookmarks);
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
                    class="w-4 h-4 flex items-center justify-center text-gray-400 hover:text-gray-600 cursor-pointer border-none bg-transparent p-0 transition-colors group relative"
                    class:!text-[#409eff]={showOffsetPage}
                    onclick={() => showOffsetPage = !showOffsetPage}
                    title={showOffsetPage ? "当前显示偏移后的页码" : "当前显示原始页码"}
                >
                    <Icon data={offsetIconRaw} width={16} height={16} />
                </button>
        </div>
    </div>
    <div class="tree-body" 
         ondragover={handleDragOver}
         ondrop={handleDrop}
         ondragleave={handleDragLeave}
         role="tree"
         tabindex="0">
        {#each bookmarks as _, i (bookmarks[i].id)}
            <BookmarkNode bind:bookmark={bookmarks[i]} index={i} />
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
        position: relative;
    }
</style>