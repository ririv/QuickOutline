<script lang="ts">
    import trashIcon from '@/assets/icons/trash.svg';
    import textEditIcon from '@/assets/icons/text-edit.svg';
    import treeDiagramIcon from '@/assets/icons/tree-diagram.svg';
    import doubleColumnIcon from '@/assets/icons/double-column.svg';

    import GetContentsPopup from './GetContentsPopup.svelte';
    import SetContentsPopup from './SetContentsPopup.svelte';
    import type { ViewScaleType } from './SetContentsPopup.svelte';
    import GraphButton from '../controls/GraphButton.svelte';
    import StyledButton from '../controls/StyledButton.svelte';
    
    import { rpc } from '@/lib/api/rpc';
    import { bookmarkStore } from '@/stores/bookmarkStore';
    import { messageStore } from '@/stores/messageStore';
    import { get } from 'svelte/store';
    import type { Bookmark } from '@/components/bookmark/types';

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

    const debouncedUpdateOffset = debounce(async (val: string) => {
        const offset = parseInt(val, 10) || 0;
        try {
            await rpc.updateOffset(offset);
        } catch (e) {
            console.error("Failed to sync offset", e);
        }
    }, 500);

    // Sync offsetValue with bookmarkStore
    bookmarkStore.subscribe(state => {
        const currentOffset = state.offset === 0 ? '' : String(state.offset);
        if (offsetValue !== currentOffset) {
            offsetValue = currentOffset;
        }
    });

    function handleOffsetInput(e: Event) {
        const target = e.target as HTMLInputElement;
        offsetValue = target.value;
        const offset = parseInt(offsetValue, 10) || 0;
        bookmarkStore.setOffset(offset);
        debouncedUpdateOffset(offsetValue);
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
            const offset = parseInt(offsetValue, 10) || 0;
            // 1. Update backend offset state
            await rpc.updateOffset(offset);
            
            // TODO: Handle 'toc' mode if backend supports it distinctively
            // For now, we use standard getOutlineAsBookmark for 'bookmark' mode
            
            // 2. Get Bookmark DTO from backend
            const bookmarkDto: Bookmark = await rpc.getOutlineAsBookmark(offset);
            
            // 3. Sync text
            const text = await rpc.syncFromTree(bookmarkDto); 

            // 4. Update frontend store
            bookmarkStore.setText(text);
            bookmarkStore.setTree(bookmarkDto.children || []);
            bookmarkStore.setOffset(offset);
            
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
            const state = get(bookmarkStore);
            if (!state.text || !state.text.trim()) {
                messageStore.add('No outline text to save. Please enter some text first.', 'WARNING');
                return;
            }
            
            const offset = parseInt(offsetValue, 10) || 0;
            await rpc.updateOffset(offset);

            // 2. Use saveOutlineFromText which updates backend state AND saves in one go.
            await rpc.saveOutlineFromText(state.text, null, offset, viewMode);
            
            messageStore.add('Outline saved successfully!', 'SUCCESS');
        } catch (e: any) {
            messageStore.add('Failed to save outline: ' + (e.message || String(e)), 'ERROR');
        }
    }

    function handleDelete() {
        bookmarkStore.reset(); // Clear frontend store (text, tree, offset)
        // Sync empty state with backend
        rpc.syncFromText('').then(() => {
            messageStore.add('Editor cleared and backend state reset.', 'INFO');
        }).catch(e => {
            messageStore.add('Failed to clear editor and reset backend state: ' + (e.message || String(e)), 'ERROR');
        });
    }

    function getButtonClass(isActive: boolean) {
        const base = "flex items-center justify-center w-8 h-7 rounded-md transition-all duration-200";
        const active = "bg-gray-200 text-gray-900 opacity-100 scale-100";
        const inactive = "text-gray-500 hover:text-gray-700 hover:bg-gray-100 opacity-60 hover:opacity-100";
        return `${base} ${isActive ? active : inactive}`;
    }

</script>

<div class="flex items-center gap-[15px] p-[10px] bg-white border-none">
    <GraphButton class="graph-button-important" title="Clear Editor" onclick={handleDelete}>
        <img src={trashIcon} alt="Delete" />
    </GraphButton>
    
    <div class="relative inline-flex" role="group" onmouseenter={() => showPopup('get')} onmouseleave={hidePopup}>
        <StyledButton type="primary" hoverEffect="darken" bind:element={getContentsBtnEl} onclick={handleGetContentsClick}>
            Get Contents
        </StyledButton>
        {#if activePopup === 'get' && getContentsBtnEl}
            <GetContentsPopup 
                triggerEl={getContentsBtnEl} 
                onSelect={handleGetContentsModeChange} 
                selected={getContentsMode}
            />
        {/if}
    </div>

    <div class="relative inline-flex" role="group" onmouseenter={() => showPopup('set')} onmouseleave={hidePopup}>
        <StyledButton type="important" hoverEffect="elevation" bind:element={setContentsBtnEl} onclick={handleSetContentsClick}>
            Set Contents
        </StyledButton>
        {#if activePopup === 'set' && setContentsBtnEl}
            <SetContentsPopup 
                triggerEl={setContentsBtnEl} 
                selected={viewMode}
                onSelect={handleViewModeChange} 
            />
        {/if}
    </div>

    <input 
        type="text" 
        class="px-3 py-1.5 bg-gray-50 border border-gray-200 rounded-md text-sm outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100 w-[100px] transition-all" 
        placeholder="Offset" 
        bind:value={offsetValue}
        oninput={handleOffsetInput}
    />

    <div class="flex-1"></div>

    <!-- Modern Segmented Control Group -->
    <div class="flex p-1 rounded-lg gap-1 select-none">
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
