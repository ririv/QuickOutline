<script lang="ts">
    import type { Bookmark } from "./types";
    import BookmarkNode from "./BookmarkNode.svelte"; // Self-import for recursion
    import { tick, getContext } from "svelte";
    import { appStore } from '@/stores/appStore';

    interface Props {
        bookmark: Bookmark;
    }
    let { bookmark }: Props = $props();

    let isEditingTitle = $state(false);
    let isEditingPage = $state(false);
    let isExpanded = $state(true);

    const previewContext = getContext<{ show: (src: string, y: number, x: number) => void, hide: () => void }>('previewContext');

    async function editTitle() {
        isEditingTitle = true;
        await tick(); // Wait for the DOM to update
        // Find and focus the input, could be improved with a more specific selector if needed
        const inputEl = document.querySelector('.title-cell input');
        (inputEl as HTMLElement)?.focus();
    }

    async function editPage() {
        previewContext.hide(); // Hide preview when editing starts
        isEditingPage = true;
        await tick();
        const inputEl = document.querySelector('.page-cell input');
        (inputEl as HTMLElement)?.focus();
    }

    function handlePageMouseEnter(e: MouseEvent) {
        // console.log('Hover page:', bookmark.page);
        if (isEditingPage || !bookmark.page) return;
        
        const pageNum = parseInt(bookmark.page, 10);
        if (isNaN(pageNum)) {
            // console.warn('Page is not a number:', bookmark.page);
            return;
        }

        const pageIndex = pageNum - 1;
        
        if ($appStore.serverPort && $appStore.serverPort > 0) {
            const src = `http://127.0.0.1:${$appStore.serverPort}/page_images/${pageIndex}.png`;
            const rect = (e.currentTarget as HTMLElement).getBoundingClientRect();
            previewContext.show(src, rect.top + rect.height / 2, rect.left);
        } else {
            // console.warn('Server port not ready');
        }
    }

    function handlePageMouseLeave() {
        previewContext.hide();
    }

</script>

<div class="node-container" style="--level: {bookmark.level}">
    <div class="flex items-center border-b border-transparent hover:bg-[#f5f5f5] transition-colors py-1 min-h-[28px]">
        <!-- Title Cell -->
        <div class="flex-[0.9] flex items-center w-full overflow-hidden" style="padding-left: {(bookmark.level - 1) * 24 + 4}px;">
            <button 
                class="bg-transparent border-none cursor-pointer flex items-center justify-center text-gray-400 hover:text-gray-600 transition-colors outline-none shrink-0"
                onclick={() => isExpanded = !isExpanded} 
                style="visibility: {bookmark.children.length > 0 ? 'visible' : 'hidden'}; width: 24px; height: 24px;"
            >
                <span class="inline-block transition-transform duration-200 origin-center {isExpanded ? 'rotate-90' : ''}">
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
                        onblur={() => isEditingTitle = false} 
                        onkeydown={e => e.key === 'Enter' && e.currentTarget.blur()} 
                        autofocus
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
                        class="w-full outline-none pl-3 pr-1.5 py-0.5 text-sm leading-tight bg-transparent text-center rounded font-normal text-gray-900 font-sans"
                        bind:value={bookmark.page} 
                        onblur={() => isEditingPage = false} 
                        onkeydown={e => e.key === 'Enter' && e.currentTarget.blur()} 
                        oninput={(e) => {
                            const target = e.currentTarget as HTMLInputElement;
                            target.value = target.value.replace(/[^0-9]/g, '');
                            bookmark.page = target.value;
                        }}
                        autofocus
                    />
                </div>
            {:else}
                <div 
                    class="pl-3 pr-1.5 py-0.5 m-0 text-sm leading-tight text-gray-500 text-center cursor-text w-full truncate hover:bg-gray-200 rounded-full transition-colors text-center font-sans whitespace-pre" 
                    onclick={editPage}
                    onmouseenter={handlePageMouseEnter}
                    onmouseleave={handlePageMouseLeave}
                    role="button"
                    tabindex="0"
                    onkeydown={(e) => e.key === 'Enter' && editPage()}
                >{bookmark.page}</div><!-- whitespace-pre: 避免空格折叠，保持与编辑态 input 宽度一致，消除布局跳动 -->
            {/if}
        </div>
    </div>

    {#if isExpanded && bookmark.children.length > 0}
        <div>
            {#each bookmark.children as child}
                <BookmarkNode bookmark={child} />
            {/each}
        </div>
    {/if}
</div>
