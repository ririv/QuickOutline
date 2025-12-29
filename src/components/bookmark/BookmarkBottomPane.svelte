<script lang="ts">
    import trashIcon from '@/assets/icons/trash.svg';
    import textEditIcon from '@/assets/icons/text-edit.svg';
    import treeDiagramIcon from '@/assets/icons/tree-diagram.svg';
    import doubleColumnIcon from '@/assets/icons/double-column.svg';
    import offsetIcon from '@/assets/icons/offset.svg';
    import downloadIcon from '@/assets/icons/download.svg';
    import uploadIcon from '@/assets/icons/upload.svg';

    import GetContentsPopup from './GetContentsPopup.svelte';
    import SetContentsPopup from './SetContentsPopup.svelte';
    import type { ViewScaleType } from './SetContentsPopup.svelte';
    import GraphButton from '../controls/GraphButton.svelte';
    import StyledInput from '../controls/StyledInput.svelte';
    import { ripple } from '@/lib/actions/ripple';
    
    import { rpc } from '@/lib/api/rpc';
    import { processText, serializeBookmarkTree } from '@/lib/outlineParser';
    import { bookmarkStore } from '@/stores/bookmarkStore.svelte';
    import { docStore } from '@/stores/docStore.svelte.ts';
    import { messageStore } from '@/stores/messageStore.svelte.ts';
    import type { BookmarkUI } from '@/lib/types/bookmark.ts';

    interface Props {
        view: 'text' | 'tree' | 'double';
    }
    let { view = $bindable() }: Props = $props();

    let activePopup = $state<'get' | 'set' | null>(null);
    let getContentsBtnEl = $state<HTMLButtonElement | undefined>();
    let setContentsBtnEl = $state<HTMLButtonElement | undefined>();
    let hideTimer: number | null = null;
    
    // State for Popups
    let getContentsMode = $state<'bookmark' | 'toc'>('bookmark'); 
    let viewMode = $state<ViewScaleType>('NONE'); 
    
    let offsetValue = $state('');
    let debounceTimer: number | undefined;

    // Simple debounce function
    function debounce(func: Function, delay: number) {
        return function(...args: any[]) {
            clearTimeout(debounceTimer);
            debounceTimer = setTimeout(() => func(...args), delay);
        };
    }

    function handleOffsetInput(e: Event) {
        const target = e.target as HTMLInputElement;
        offsetValue = target.value;
        const offset = parseInt(offsetValue, 10) || 0;
        bookmarkStore.offset = offset;
    }

    function setView(newView: 'text' | 'tree' | 'double') {
        view = newView;
    }

    function showPopup(popup: 'get' | 'set') {
        if (hideTimer) clearTimeout(hideTimer);
        activePopup = popup;
    }

    function hidePopup() {
        hideTimer = setTimeout(() => {
            activePopup = null;
        }, 200);
    }

    // --- Get Contents Logic ---

    function handleGetContentsModeChange(type: 'bookmark' | 'toc') {
        getContentsMode = type;
    }

    async function handleGetContentsClick() {
        console.log('Get contents from:', getContentsMode);
        
        try {
            const path = docStore.currentFilePath;
            if (!path) {
                messageStore.add('No file open.', 'WARNING');
                return;
            }

            const offset = parseInt(offsetValue, 10) || 0;
            // 1. Get Bookmark DTO from backend
            const bookmarkDto: BookmarkUI = await rpc.getOutlineAsBookmark(path, offset);
            
            // 3. Sync text locally
            const text = serializeBookmarkTree(bookmarkDto); 

            // 4. Update frontend store
            bookmarkStore.setText(text);
            bookmarkStore.setTree(bookmarkDto.children || []);
            bookmarkStore.offset = offset;
            
            messageStore.add('Outline loaded successfully', 'SUCCESS');
        } catch (e: any) {
            messageStore.add('Failed to load outline: ' + (e.message || String(e)), 'ERROR');
        }
    }

    // --- Set Contents Logic ---

    function handleViewModeChange(type: ViewScaleType) {
        viewMode = type;
    }

    async function handleSetContentsClick() {
        console.log('Set contents click with view scale:', viewMode);
        try {
            const path = docStore.currentFilePath;
            if (!path) {
                messageStore.add('No file open.', 'WARNING');
                return;
            }

            const text = bookmarkStore.text;
            if (!text || !text.trim()) {
                messageStore.add('No outline text to save. Please enter some text first.', 'WARNING');
                return;
            }
            
            const offset = parseInt(offsetValue, 10) || 0;

            // 2. Parse locally and save tree
            const tree = processText(text);
            await rpc.saveOutline(path, tree, null, offset, viewMode);
            
            messageStore.add('Outline saved successfully!', 'SUCCESS');
        } catch (e: any) {
            messageStore.add('Failed to save outline: ' + (e.message || String(e)), 'ERROR');
        }
    }

    function handleDelete() {
        bookmarkStore.reset(); // Clear frontend store (text, tree, offset)
        messageStore.add('Editor cleared.', 'INFO');
    }

    function getButtonClass(isActive: boolean) {
        const base = "flex items-center justify-center w-8 h-7 rounded-md transition-all duration-200";
        const active = "bg-white shadow-sm text-gray-900 opacity-100 scale-100";
        const inactive = "text-gray-500 hover:text-gray-700 hover:bg-gray-100 opacity-60 hover:opacity-100";
        return `${base} ${isActive ? active : inactive}`;
    }

</script>

<div class="flex items-center gap-[15px] py-[10px] pl-[15px] pr-[10px] bg-white border-none">
    <!-- Left Group: Delete and Load -->
    <div class="flex items-center gap-2">
        <GraphButton class="graph-button-important group" title="Clear Editor" onclick={handleDelete}>
            <img 
                src={trashIcon} 
                alt="Delete" 
                class="transition-[filter] duration-200 group-hover:[filter:invert(36%)_sepia(82%)_saturate(2268%)_hue-rotate(338deg)_brightness(95%)_contrast(94%)] group-active:[filter:invert(13%)_sepia(95%)_saturate(5686%)_hue-rotate(348deg)_brightness(82%)_contrast(106%)]"
            />
        </GraphButton>

        <!-- Vertical Divider -->
        <div class="w-px h-5 bg-gray-300 mx-1"></div>

        <div class="relative inline-flex" role="group" onmouseenter={() => showPopup('get')} onmouseleave={hidePopup}>
            <button 
                bind:this={getContentsBtnEl} 
                onclick={handleGetContentsClick}
                class="inline-flex items-center justify-center w-[100px] gap-1.5 px-3 py-2 text-sm font-medium text-gray-700 rounded-md transition-colors duration-200 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-2 focus-visible:ring-blue-500 hover:bg-gray-100"
                use:ripple
            >
                <svg class="w-4 h-4 opacity-70 group-hover:opacity-100" viewBox="0 0 16 16" fill="currentColor" xmlns="http://www.w3.org/2000/svg">
                    <path d="M8 11.5L3.5 7H6V2H10V7H12.5L8 11.5Z" />
                    <path d="M3 13H13V14.5H3V13Z" />
                </svg>
                Load
            </button>
            {#if activePopup === 'get' && getContentsBtnEl}
                <GetContentsPopup 
                    triggerEl={getContentsBtnEl} 
                    onSelect={handleGetContentsModeChange} 
                    selected={getContentsMode}
                />
            {/if}
        </div>
    </div>

    <div class="flex-1"></div>
    
    <!-- Center Group: Apply and Offset -->
    <div class="flex items-center gap-2">
        <div class="relative inline-flex" role="group" onmouseenter={() => showPopup('set')} onmouseleave={hidePopup}>
            <button 
                bind:this={setContentsBtnEl} 
                onclick={handleSetContentsClick}
                class="inline-flex items-center justify-center w-[100px] gap-1.5 px-3 py-2 text-sm font-medium text-[#409eff] rounded-md transition-colors duration-200 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 focus-visible:ring-offset-2 bg-[#ecf5ff] border border-[#d9ecff] hover:bg-[#d9ecff] active:bg-[#c6e2ff]"
                use:ripple={{ color: 'rgba(64,158,255,0.2)' }}
            >
                <svg class="w-4 h-4" viewBox="0 0 16 16" fill="currentColor" xmlns="http://www.w3.org/2000/svg">
                    <path d="M8 4.5L12.5 9H10V14H6V9H3.5L8 4.5Z" />
                    <path d="M3 2H13V3.5H3V2Z" />
                </svg>
                Apply
            </button>
            {#if activePopup === 'set' && setContentsBtnEl}
                <SetContentsPopup 
                    triggerEl={setContentsBtnEl} 
                    selected={viewMode}
                    onSelect={handleViewModeChange} 
                />
            {/if}
        </div>

        <!-- Offset Input Group -->
        <StyledInput 
            icon={offsetIcon}
            placeholder="Offset"
            bind:value={offsetValue}
            oninput={handleOffsetInput}
            width="100px"
            numericType="integer"
        />
    </div>

    <div class="flex-1"></div>

    <!-- Modern Segmented Control Group -->
    <div class="flex p-1 bg-gray-50 rounded-lg gap-1 select-none">
        <button 
            class={getButtonClass(view === 'text')}
            onclick={() => setView('text')} 
            title="文本视图"
        >
            <img src={textEditIcon} alt="Text View" class="w-4 h-4" />
        </button>
        <button 
            class={getButtonClass(view === 'tree')}
            onclick={() => setView('tree')} 
            title="树视图"
        >
            <img src={treeDiagramIcon} alt="Tree View" class="w-4 h-4" />
        </button>
        <button 
            class={getButtonClass(view === 'double')}
            onclick={() => setView('double')} 
            title="双栏视图"
        >
            <img src={doubleColumnIcon} alt="Double Column View" class="w-4 h-4" />
        </button>
    </div>
</div>


