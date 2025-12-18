<script lang="ts">
    import type { BookmarkUI } from "../../lib/types/bookmark.ts";
    import BookmarkNode from "./BookmarkNode.svelte"; // Self-import for recursion
    import { tick, getContext } from "svelte";
    import { appStore } from '@/stores/appStore';
    import { bookmarkStore } from '@/stores/bookmarkStore.svelte';
    import { docStore } from '@/stores/docStore';
    import { pdfRenderService } from '@/lib/services/PdfRenderService';

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
        // console.log('Hover page:', bookmark.page);
        if (isEditingPage || !bookmark.pageNum) return;
        
        const pageNum = parseInt(bookmark.pageNum, 10);
        if (isNaN(pageNum)) {
            // console.warn('Page is not a number:', bookmark.page);
            return;
        }

        const offset = bookmarkStore.offset || 0;
        const showOffset = offsetContext.show;

        const effectivePageNum = showOffset ? (pageNum + offset) : pageNum;
        const pageIndex = effectivePageNum - 1;
        
        if ($docStore.currentFilePath) {
            const rect = (e.currentTarget as HTMLElement).getBoundingClientRect();
            
            // Clean up any previous URL just in case
            if (currentPreviewUrl) URL.revokeObjectURL(currentPreviewUrl);
            
            pdfRenderService.renderPage($docStore.currentFilePath, pageIndex, 'preview')
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
        const pageNum = parseInt(bookmark.pageNum, 10);
        if (isNaN(pageNum)) return bookmark.pageNum;
        const offset = bookmarkStore.offset || 0;
        return offsetContext.show ? String(pageNum + offset) : bookmark.pageNum;
    });

    let isOutOfRange = $derived.by(() => {
        if (!bookmark.pageNum) return false;
        const pageNum = parseInt(bookmark.pageNum, 10);
        if (isNaN(pageNum)) return false;
        
        const offset = bookmarkStore.offset || 0;
        const count = $docStore.pageCount;
        
        const effectivePage = pageNum + offset;
        if (count > 0) {
            return effectivePage > count || effectivePage < 1;
        }
        return effectivePage < 1;
    });

</script>

<div class="node-container" style="--level: {bookmark.level}">
    <div class="flex items-center border-b border-transparent hover:bg-[#f5f5f5] transition-colors py-1 min-h-[28px]">
        <!-- Title Cell -->
        <div class="flex-[0.9] flex items-center w-full overflow-hidden" style="padding-left: {(bookmark.level - 1) * 24 + 4}px;">
            <button 
                class="bg-transparent border-none cursor-pointer flex items-center justify-center text-gray-400 hover:text-gray-600 transition-colors outline-none shrink-0"
                onclick={() => bookmark.expanded = !bookmark.expanded} 
                style="visibility: {bookmark.children.length > 0 ? 'visible' : 'hidden'}; width: 24px; height: 24px;"
                aria-label={bookmark.expanded ? "Collapse bookmark" : "Expand bookmark"}
            >
                <span class="inline-block transition-transform duration-200 origin-center {bookmark.expanded ? 'rotate-90' : ''}">
                    <svg width="10" height="10" viewBox="0 0 10 10" fill="currentColor" xmlns="http://www.w3.org/2000/svg">
                        <path d="M2.5 1.66667L7.5 5L2.5 8.33333L2.5 1.66667Z" />
                    </svg>
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
                        class="w-full outline-none px-1.5 py-0.5 text-sm leading-tight bg-transparent text-center rounded font-normal {isOutOfRange ? 'text-[rgba(255,0,0,0.7)]' : 'text-gray-900'} font-sans"
                        bind:value={bookmark.pageNum}
                        bind:this={pageInput}
                        onblur={() => isEditingPage = false}
                    />
                </div>
            {:else}
                <div 
                    class="px-1.5 py-0.5 m-0 text-sm leading-tight text-center cursor-text w-full truncate hover:bg-gray-200 rounded-full transition-colors font-sans whitespace-pre {isOutOfRange ? 'text-[rgba(255,0,0,0.7)] font-medium' : 'text-gray-500'}"
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
