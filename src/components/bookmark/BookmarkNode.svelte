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

    interface Props {
        bookmark: BookmarkUI;
    }
    let { bookmark }: Props = $props();

    let isEditingTitle = $state(false);
    let isEditingPage = $state(false);
    
    // Local state for preview tooltip
    let currentPreviewUrl: string | null = null; // Track current blob URL for cleanup

    let titleInput: HTMLInputElement | undefined = $state();
    let pageInput: HTMLInputElement | undefined = $state();
    
    // Initialize expanded state if missing
    if (bookmark.expanded === undefined) {
        bookmark.expanded = true;
    }

    const previewContext = getContext<{ show: (src: string, y: number, x: number) => void, hide: () => void }>('previewContext');
    const offsetContext = getContext<{ show: boolean }>('offsetContext');
    const dragContext = getContext<{ 
        draggedNodeId: string | null, 
        dropTargetId: string | null,
        dropPosition: 'before' | 'after' | 'inside' | null,
        setDraggedNodeId: (id: string | null) => void,
        move: (draggedId: string, targetId: string, pos: 'before' | 'after' | 'inside') => void
    }>('dragContext');

    // Drag & Drop State
    let isDragging = $derived(dragContext.draggedNodeId === bookmark.id);
    let activeDropPosition = $derived(dragContext.dropTargetId === bookmark.id ? dragContext.dropPosition : null);
    let expandTimer: number | undefined;

    // Auto-expand logic on hover
    $effect(() => {
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
            
            // Create a transparent drag image
            const img = new Image();
            img.src = 'data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7';
            e.dataTransfer.setDragImage(img, 0, 0);
        }
    }

    function handleDragEnd(e: DragEvent) {
        // Execute move if we have a valid target (Fallback for Tauri internal drops)
        // In Tauri, tauri://drag-drop might not fire for internal elements, but drag-over does update the state.
        if (dragContext.dropTargetId && dragContext.dropPosition) {
             dragContext.move(bookmark.id, dragContext.dropTargetId, dragContext.dropPosition);
        }
        
        dragContext.setDraggedNodeId(null);
    }

    async function editTitle() {
        isEditingTitle = true;
        await tick(); // Wait for the DOM to update
        titleInput?.focus();
    }

    async function editPage() {
        // Hide preview when editing starts
        handlePageMouseLeave();
        isEditingPage = true;
        await tick();
        pageInput?.focus();
    }

    function handlePageMouseEnter(e: MouseEvent) {
        if (isEditingPage || !bookmark.pageNum) return;
        
        // Use real offset for preview, regardless of display toggle
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
                    // Only show if still hovering (simple check: logic in mouseleave handles the nulling)
                    // We check if currentPreviewUrl is still valid (not nulled by leave)
                    if (currentPreviewUrl === url) {
                         previewContext.show(url, rect.top + rect.height / 2, rect.left);
                    } else {
                         // Mouse left before render finished
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

    // Computed page to display
    let displayedPage = $derived.by(() => {
        if (!bookmark.pageNum) return '';
        
        // If it's a special syntax or label, show as is
        if (bookmark.pageNum.startsWith('#') || bookmark.pageNum.startsWith('@') || isNaN(parseInt(bookmark.pageNum, 10))) {
            return bookmark.pageNum;
        }

        // It's a standard logical number
        const pageNum = parseInt(bookmark.pageNum, 10);
        const offset = bookmarkStore.offset || 0;
        return offsetContext.show ? String(pageNum + offset) : bookmark.pageNum;
    });

    let isOutOfRange = $derived.by(() => {
        if (!bookmark.pageNum) return false;
        
        // Validation must always use the real offset to match the editor's behavior
        return !validatePageTarget(bookmark.pageNum, {
            offset: bookmarkStore.offset || 0,
            totalPage: docStore.pageCount,
            pageLabels: docStore.originalPageLabels,
            insertPos: 0
        });
    });

</script>

<div class="node-container" style="--level: {bookmark.level}">
    <div 
        class="flex items-center border-b border-transparent transition-colors py-1 min-h-[28px] node-row
            {activeDropPosition === 'inside' ? '!bg-[#e6f7ff]' : 'hover:bg-[#f5f5f5]'}
            {activeDropPosition === 'before' ? '!border-t-2 !border-t-[#409eff]' : ''}
            {activeDropPosition === 'after' ? '!border-b-2 !border-b-[#409eff]' : ''}
            {isDragging ? 'opacity-50' : ''}"
        draggable="true"
        data-id={bookmark.id}
        ondragstart={handleDragStart}
        ondragend={handleDragEnd}
        role="treeitem"
        aria-selected="false"
        tabindex="-1"
    >
        <!-- Title Cell -->
        <div class="flex-[0.9] flex items-center w-full overflow-hidden" style="padding-left: {(bookmark.level - 1) * 24 + 4}px;">
            <button 
                class="bg-transparent border-none cursor-pointer flex items-center justify-center text-gray-400 hover:text-gray-600 transition-colors outline-none shrink-0"
                onclick={() => bookmark.expanded = !bookmark.expanded} 
                style="visibility: {bookmark.children.length > 0 ? 'visible' : 'hidden'}; width: 24px; height: 24px;"
                aria-label={bookmark.expanded ? "Collapse bookmark" : "Expand bookmark"}
            >
                <span class="inline-block transition-transform duration-200 origin-center {bookmark.expanded ? 'rotate-90' : ''}">
                    <Icon data={arrowRightSolidIcon} width={10} height={10} />
                </span>
            </button>
            
            {#if isEditingTitle}
                <div class="relative w-full">
                    <div class="absolute left-0.5 top-1/2 -translate-y-1/2 w-1 h-3 bg-[#409eff] rounded-full"></div>
                    <input 
                        type="text" 
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
                >{bookmark.title}</div><!-- whitespace-pre: 避免空格折叠，保持与编辑态 input 宽度一致，消除布局跳动 -->
            {/if}
        </div>

        <!-- Page Cell -->
        <div class="flex-[0.1] min-w-[80px] flex items-center w-full px-2">
            {#if isEditingPage}
                <div class="relative w-full">
                    <div class="absolute left-0.5 top-1/2 -translate-y-1/2 w-1 h-3 bg-[#409eff] rounded-full"></div>
                    <input 
                        type="text" 
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
                >{displayedPage}</div><!-- whitespace-pre: 避免空格折叠，保持与编辑态 input 宽度一致，消除布局跳动 -->
            {/if}
        </div>
    </div>

    {#if bookmark.expanded && bookmark.children.length > 0}
        <div>
            {#each bookmark.children as child (child.id)}
                <BookmarkNode bookmark={child} />
            {/each}
        </div>
    {/if}
</div>
