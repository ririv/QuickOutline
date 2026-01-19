<script lang="ts">
    import type { BookmarkUI } from "../../lib/types/bookmark.ts";
    import BookmarkNode from "./BookmarkNode.svelte"; // Self-import for recursion
    import { tick, getContext } from "svelte";
    import { bookmarkStore } from '@/stores/bookmarkStore.svelte';
    import { docStore } from '@/stores/docStore.svelte';
    import { pdfRenderService } from '@/lib/services/PdfRenderService';
    import { resolveLinkTarget, validatePageTarget } from '@/lib/services/PageLinkResolver';
    import Icon from "@/components/Icon.svelte";
    import arrowRightSolidIcon from '@/assets/icons/arrow-right-solid.svg?raw';
    import dragHandleIcon from '@/assets/icons/drag-handle.svg?raw';
    import { DragController } from "@/lib/drag-drop/DragController.svelte";
    import { getNodePadding, getGapIndent } from "@/lib/drag-drop/treeLayout";
    
    interface Props {
        bookmark: BookmarkUI;
        index: number;
    }
    let { bookmark = $bindable(), index }: Props = $props();

    let isEditingTitle = $state(false);
    let isEditingPage = $state(false);
    
    // Local state for preview tooltip
    let currentPreviewUrl: string | null = null; 

    let titleInput: HTMLInputElement | undefined = $state();
    let pageInput: HTMLInputElement | undefined = $state();
    
    // Initialize expanded state if missing
    if (bookmark.expanded === undefined) {
        bookmark.expanded = true;
    }

    const previewContext = getContext<{ show: (src: string, y: number, x: number) => void, hide: () => void }>('previewContext');
    const offsetContext = getContext<{ show: boolean }>('offsetContext');
    const treeContext = getContext<{ 
        openContextMenu: (e: MouseEvent, nodeId: string) => void,
        pendingFocusId: string | null,
        clearPendingFocus: () => void
    }>('treeContext');
    
    // Auto-focus logic for new nodes
    $effect(() => {
        if (treeContext.pendingFocusId === bookmark.id) {
            // Must use untrack if we don't want re-run on other dependencies? 
            // But pendingFocusId is the trigger.
            // We need to wait for DOM update (if just mounted).
            tick().then(() => {
                editTitle();
                treeContext.clearPendingFocus();
            });
        }
    });
    
    // Use DragController directly for robust reactivity
    const dragContext = getContext<DragController>('dragContext');

    // Drag & Drop State
    let isDragging = $derived(dragContext.draggedNodeId === bookmark.id);
    
    // Visual Feedback Logic derived from gapNodeId
    let isVisualActive = $derived(dragContext.gapNodeId === bookmark.id);
    let visualPos = $derived(isVisualActive ? dragContext.gapPosition : null);
    let targetLevel = $derived(isVisualActive ? dragContext.dropTargetLevel : 1);
    
    let expandTimer: number | undefined;

    // Auto-expand logic on hover
    $effect(() => {
        // Use logic target for auto-expand
        const activeDropPosition = dragContext.dropTargetId === bookmark.id ? dragContext.dropPosition : null;
        if (activeDropPosition === 'inside' && !bookmark.expanded && bookmark.children.length > 0) {
            expandTimer = setTimeout(() => {
                bookmark.expanded = true;
            }, 600);
        } else {
            clearTimeout(expandTimer);
        }
    });

    function handleDragStart(e: DragEvent) {
        e.stopPropagation();
        if (isEditingTitle || isEditingPage) {
            e.preventDefault();
            return;
        }
        dragContext.setDraggedNodeId(bookmark.id);
        if (e.dataTransfer) {
            e.dataTransfer.effectAllowed = 'move';
            e.dataTransfer.setData('text/plain', bookmark.id);
            
            // Create a custom drag preview
            const dragPreview = document.createElement('div');
            dragPreview.textContent = bookmark.title || 'Untitled';
            dragPreview.setAttribute('style', `
                position: absolute; 
                top: -1000px; 
                left: -1000px;
                padding: 6px 12px;
                background: white;
                border: 1px solid #e5e7eb;
                border-radius: 6px;
                box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
                font-size: 14px;
                font-family: sans-serif;
                font-weight: 500;
                color: #374151;
                width: max-content;
                max-width: 300px;
                z-index: 9999;
                white-space: nowrap;
                overflow: hidden;
                text-overflow: ellipsis;
            `);
            document.body.appendChild(dragPreview);
            e.dataTransfer.setDragImage(dragPreview, 0, 0);
            setTimeout(() => document.body.removeChild(dragPreview), 0);
        }
    }

    function handleDragEnd(e: DragEvent) {
        if (dragContext.dropTargetId && dragContext.dropPosition) {
             dragContext.move(bookmark.id, dragContext.dropTargetId, dragContext.dropPosition);
        }
        dragContext.setDraggedNodeId(null);
    }

    async function editTitle() {
        isEditingTitle = true;
        await tick(); 
        titleInput?.focus();
    }

    async function editPage() {
        handlePageMouseLeave();
        isEditingPage = true;
        await tick();
        pageInput?.focus();
    }

    function handlePageMouseEnter(e: MouseEvent) {
        if (isEditingPage || !bookmark.pageNum) return;
        const result = resolveLinkTarget(bookmark.pageNum, {
            offset: bookmarkStore.offset || 0,
            pageLabels: docStore.originalPageLabels,
            insertPos: 0
        });

        if (result && docStore.currentFilePath) {
            const rect = (e.currentTarget as HTMLElement).getBoundingClientRect();
            if (currentPreviewUrl) URL.revokeObjectURL(currentPreviewUrl);
            pdfRenderService.renderPage(docStore.currentFilePath, result.index, 'preview')
                .then(url => {
                    currentPreviewUrl = url;
                    if (currentPreviewUrl === url) {
                         previewContext.show(url, rect.top + rect.height / 2, rect.left);
                    } else {
                         URL.revokeObjectURL(url);
                    }
                })
        }
    }

    function handlePageMouseLeave() {
        previewContext.hide();
        if (currentPreviewUrl) {
            URL.revokeObjectURL(currentPreviewUrl);
            currentPreviewUrl = null;
        }
    }

    let displayedPage = $derived.by(() => {
        if (!bookmark.pageNum) return '';
        if (bookmark.pageNum.startsWith('#') || bookmark.pageNum.startsWith('@') || isNaN(parseInt(bookmark.pageNum, 10))) {
            return bookmark.pageNum;
        }
        const pageNum = parseInt(bookmark.pageNum, 10);
        const offset = bookmarkStore.offset || 0;
        return offsetContext.show ? String(pageNum + offset) : bookmark.pageNum;
    });

    let isOutOfRange = $derived.by(() => {
        if (!bookmark.pageNum) return false;
        return !validatePageTarget(bookmark.pageNum, {
            offset: bookmarkStore.offset || 0,
            totalPage: docStore.pageCount,
            pageLabels: docStore.originalPageLabels,
            insertPos: 0
        });
    });
</script>

<div class="node-container" style="--level: {bookmark.level}" data-id={bookmark.id}>
    <!-- Top Gap (Only for the very first item of the entire tree) -->
    {#if index === 0 && bookmark.level === 1}
        <div 
            class="drag-gap-trigger w-full relative z-50"
            style="
                height: 6px;
                padding-left: {(visualPos === 'before' ? getGapIndent(targetLevel) : 0)}px;
                background-color: transparent;
            "
        >
            <div class="gap-indicator" class:gap-active={visualPos === 'before'}></div>
        </div>
    {/if}

    <div 
        class="flex items-center border-b border-transparent transition-colors py-1 min-h-[28px] node-row group relative
            {visualPos === 'inside' ? '!bg-[#e6f7ff]' : 'hover:bg-[#f5f5f5]'}
            {isDragging ? 'opacity-50' : ''}"
        data-id={bookmark.id}
        role="treeitem"
        aria-selected="false"
        tabindex="-1"
        oncontextmenu={(e) => treeContext.openContextMenu(e, bookmark.id)}
    >
        <!-- Title Cell -->
        <div class="flex-[0.9] flex items-center w-full overflow-hidden" style="padding-left: {getNodePadding(bookmark.level)}px;">
            <div 
                class="cursor-grab text-gray-400 hover:text-gray-600 mr-0.5 shrink-0 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity"
                draggable="true"
                ondragstart={handleDragStart}
                ondragend={handleDragEnd}
                role="button"
                tabindex="-1"
                title="Drag to move"
            >
               <Icon data={dragHandleIcon} width={14} height={14} />
            </div>
            
            <button 
                class="bg-transparent border-none cursor-pointer flex items-center justify-center text-gray-400 hover:text-gray-600 transition-colors outline-none shrink-0"
                onclick={() => bookmark.expanded = !bookmark.expanded} 
                style="visibility: {bookmark.children.length > 0 ? 'visible' : 'hidden'}; width: 20px; height: 20px;"
                aria-label={bookmark.expanded ? "Collapse bookmark" : "Expand bookmark"}
            >
                <span class="inline-block transition-transform duration-200 origin-center {bookmark.expanded ? 'rotate-90' : ''}">
                    <Icon data={arrowRightSolidIcon} width={9} height={9} />
                </span>
            </button>
            
            {#if isEditingTitle}
                <div class="relative w-full">
                    <div class="absolute left-0.5 top-1/2 -translate-y-1/2 w-1 h-3 bg-[#409eff] rounded-full"></div>
                    <input 
                        type="text" 
                        autocomplete="off"
                        class="w-full outline-none pl-3 pr-1.5 py-0.5 text-sm leading-tight bg-transparent rounded font-normal text-gray-900 font-sans"
                        bind:value={bookmark.title} 
                        bind:this={titleInput}
                        onblur={() => isEditingTitle = false} 
                        onkeydown={e => e.key === 'Enter' && e.currentTarget.blur()} 
                    />
                </div>
            {:else}
                <div 
                    class="pl-3 pr-1.5 py-0.5 m-0 text-sm leading-tight text-gray-700 text-left cursor-text w-full truncate hover:text-gray-900 font-sans whitespace-pre"
                    onclick={editTitle}
                    role="button"
                    tabindex="0"
                    onkeydown={(e) => e.key === 'Enter' && editTitle()}
                >{bookmark.title}</div>
            {/if}
        </div>

        <!-- Page Cell -->
        <div class="flex-[0.1] min-w-[80px] flex items-center w-full px-2">
            {#if isEditingPage}
                <div class="relative w-full">
                    <div class="absolute left-0.5 top-1/2 -translate-y-1/2 w-1 h-3 bg-[#409eff] rounded-full"></div>
                    <input 
                        type="text" 
                        autocomplete="off"
                        class="w-full outline-none px-1.5 py-0.5 text-sm leading-tight text-center rounded-lg font-normal {isOutOfRange ? 'bg-[rgba(255,0,0,0.15)] text-gray-900' : 'bg-transparent text-gray-900'} font-sans border border-transparent"
                        bind:value={bookmark.pageNum}
                        bind:this={pageInput}
                        onblur={() => isEditingPage = false}
                    />
                </div>
            {:else}
                <div 
                    class="px-1.5 py-0.5 m-0 text-sm leading-tight text-center cursor-text w-full truncate hover:bg-gray-200 rounded-lg transition-colors font-sans whitespace-pre border border-transparent {isOutOfRange ? 'bg-[rgba(255,0,0,0.15)]' : ''} {offsetContext.show ? 'text-[#409eff] font-medium' : 'text-gray-500'}"
                    onclick={editPage}
                    onmouseenter={handlePageMouseEnter}
                    onmouseleave={handlePageMouseLeave}
                    role="button"
                    tabindex="0"
                    onkeydown={(e) => e.key === 'Enter' && editPage()}
                >{displayedPage}</div>
            {/if}
        </div>
    </div>

    <!-- Bottom Gap (For all items) -->
    <div
        class="drag-gap-trigger w-full relative z-50"
        style="
            height: 6px;
            padding-left: {(visualPos === 'after' ? getGapIndent(targetLevel) : 0)}px;
            background-color: transparent;
              "
    >
        <div class="gap-indicator" class:gap-active={visualPos === 'after'}></div>
    </div>

    {#if bookmark.expanded && bookmark.children.length > 0}
        <div>
            {#each bookmark.children as _, i (bookmark.children[i].id)}
                <BookmarkNode bind:bookmark={bookmark.children[i]} index={i} />
            {/each}
        </div>
    {/if}
</div>

<style>
    .gap-indicator {
        height: 100%;
        background-color: transparent;
        transition: background-color 75ms;
    }
    
    .gap-indicator.gap-active {
        background-color: #409eff !important;
    }
</style>
