<script lang="ts">
    import type { BookmarkData, BookmarkUI } from "outline-parser/bookmark";
    import BookmarkNode from "./BookmarkNode.svelte";
    import { onMount, onDestroy, setContext, untrack, tick } from 'svelte';
    import { bookmarkStore } from '@/stores/bookmarkStore.svelte';
    import { serializeBookmarkTree } from 'outline-parser';
    import { toBookmarkData } from 'outline-parser/bookmarkUtils';
    import { messageStore } from '@/stores/messageStore.svelte.ts';
    import { getVisibleNodes } from '@/lib/utils/treeUtils';
    import { calculateDragState, getDragTargetInfo } from '@/lib/drag-drop/dragLogic';
    import PreviewPopup from '../PreviewPopup.svelte';
    import offsetIconRaw from '@/assets/icons/offset.svg?raw';
    import { DragController } from '@/lib/drag-drop/DragController.svelte';
    import { setupTauriDragDrop } from '@/lib/drag-drop/tauriDragDrop';
    import Icon from '../Icon.svelte';
    import ContextMenu from 'shared-kit/controls/ContextMenu.svelte';
    import { removeNode, insertNode } from '@/lib/utils/treeUtils';
    let bookmarks = $state<BookmarkUI[]>([]);
    let debounceTimer: number | undefined;
    
    // Context Menu State
    let contextMenu = $state<{ x: number, y: number, nodeId: string } | null>(null);
    let pendingFocusId = $state<string | null>(null);

    function handleDelete(id: string) {
        removeNode(bookmarks, id);
        bookmarks = [...bookmarks]; // Trigger update and sync
    }

    function handleAdd(targetId: string, position: 'before' | 'after' | 'inside') {
        const newNode: BookmarkUI = {
            id: crypto.randomUUID(),
            title: 'New Bookmark',
            pageNum: '',
            children: [],
            level: 1, // Will be updated by insertNode
            expanded: true
        };
        
        insertNode(bookmarks, targetId, position, newNode);
        bookmarks = [...bookmarks];
        
        // Schedule focus
        pendingFocusId = newNode.id;
        // Reset after a delay to allow other interactions? 
        // Actually, BookmarkNode should consume this and then we can clear it?
        // Or just keep it as "last focused request".
    }

    setContext('treeContext', {
        openContextMenu: (e: MouseEvent, nodeId: string) => {
            e.preventDefault();
            e.stopPropagation();
            contextMenu = { x: e.clientX, y: e.clientY, nodeId };
        },
        get pendingFocusId() { return pendingFocusId; },
        clearPendingFocus: () => { pendingFocusId = null; }
    });
    
    // Drag Controller Instance
    const dragController = new DragController(
        () => bookmarks,
        (b) => { bookmarks = b; }
    );
    
    setContext('dragContext', dragController);
    let bookmarkData = $derived(bookmarks.map(toBookmarkData));

    let unlistenFns: (() => void)[] = [];

    onMount(async () => {
        // Initialize bookmarks from store
        isUpdatingFromStore = true;
        bookmarks = bookmarkStore.tree;
        tick().then(() => isUpdatingFromStore = false);

        // Tauri Drag & Drop Strategy
        const cleanup = await setupTauriDragDrop({
            getDraggedId: () => dragController.draggedNodeId,
            onDragOver: (_targetId, _position, coords) => {
                if (coords) {
                    const target = getDragTargetInfo(coords.x, coords.y);
                    if (target) {
                        const visibleNodes = getVisibleNodes(bookmarks);
                        const state = calculateDragState(
                            target.relY,
                            target.rect.height,
                            target.relX,
                            target.id,
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
                }
                dragController.reset();
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

        const visibleNodes = getVisibleNodes(bookmarks);
        const target = getDragTargetInfo(e.clientX, e.clientY);
        
        if (!target) {
            // Drop in empty space
            if (visibleNodes.length > 0) {
                const lastNode = visibleNodes[visibleNodes.length - 1];
                if (lastNode.id !== dragController.draggedNodeId) {
                    dragController.updateState(lastNode.id, 'after', 1, lastNode.id, 'after');
                }
            }
            return;
        }

        const state = calculateDragState(
            target.relY,
            target.rect.height,
            target.relX,
            target.id,
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

    // 树内容变化后再同步文本，避免编辑过程中频繁序列化整棵树。
    const debouncedSyncTreeWithText = debounce((tree: BookmarkData[]) => {
        try {
            const rootDto: BookmarkData = {
                id: 'virtual-root',
                title: 'Virtual Root',
                pageNum: null,
                level: 0,
                children: tree
            };
            const text = serializeBookmarkTree(rootDto);
            if (bookmarkStore.text !== text) {
                bookmarkStore.setText(text);
            }
        } catch (e: any) {
            console.error("Failed to sync tree with text:", e);
            messageStore.add('Failed to sync changes: ' + (e.message || String(e)), 'ERROR');
        }
    }, 500); // 500ms 防抖


    let isUpdatingFromStore = false;

    // 从 Store 同步到本地树
    $effect(() => {
        const storeTree = bookmarkStore.tree;
        untrack(() => {
             if (storeTree !== bookmarks) {
                  isUpdatingFromStore = true;
                  bookmarks = storeTree;
                  tick().then(() => isUpdatingFromStore = false);
             }
        });
    });

    // 从本地树同步到 Store 和文本
    $effect(() => {
        const currentBookmarks = bookmarks;
        const currentBookmarkData = bookmarkData;

        untrack(() => {
            if (isUpdatingFromStore) return;

            if (bookmarkStore.tree !== currentBookmarks) {
                bookmarkStore.setTree(currentBookmarks);
            }
            debouncedSyncTreeWithText(currentBookmarkData);
        });
    });
</script>

<div class="tree-subview-container">
    <div class="tree-header">
        <div class="tree-column-title relative flex items-center justify-center">
            <div class="absolute left-[22px] flex gap-1">
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
    
    {#if contextMenu}
        {#snippet AddSiblingIcon()} <Icon name="add-sibling" size="16" /> {/snippet}
        {#snippet AddChildIcon()} <Icon name="add-child" size="16" /> {/snippet}
        {#snippet DeleteIcon()} <Icon name="delete" size="16" /> {/snippet}

        <ContextMenu 
            x={contextMenu.x} 
            y={contextMenu.y} 
            items={[
                { 
                    label: 'Add Sibling', 
                    icon: AddSiblingIcon,
                    onClick: () => handleAdd(contextMenu!.nodeId, 'after') 
                },
                { 
                    label: 'Add Child', 
                    icon: AddChildIcon,
                    onClick: () => handleAdd(contextMenu!.nodeId, 'inside') 
                },
                { 
                    label: 'Delete', 
                    variant: 'danger', 
                    icon: DeleteIcon,
                    onClick: () => handleDelete(contextMenu!.nodeId) 
                }
            ]}
            onClose={() => contextMenu = null}
        />
    {/if}
    
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
