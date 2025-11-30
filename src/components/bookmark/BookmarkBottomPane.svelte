<script lang="ts">
    import trashIcon from '@/assets/icons/trash.svg';
    import textEditIcon from '@/assets/icons/text-edit.svg';
    import treeDiagramIcon from '@/assets/icons/tree-diagram.svg';
    import switchIcon from '@/assets/icons/switch.svg';

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
        view: 'text' | 'tree';
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

    function toggleView() {
        view = view === 'text' ? 'tree' : 'text';
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

            // TODO: Pass viewMode to backend
            await rpc.saveOutlineFromText(state.text, null, offset);
            
            messageStore.add('Outline saved successfully!', 'SUCCESS');
        } catch (e: any) {
            messageStore.add('Failed to save outline: ' + (e.message || String(e)), 'ERROR');
        }
    }

    function handleDelete() {
        // Implement delete outline functionality if API supports it, 
        // or just clear the store.
        // rpc.deleteOutline? (Not in API yet)
        // For now just clear editor
        bookmarkStore.setText('');
        messageStore.add('Editor cleared', 'INFO');
    }

</script>

<div class="bottom-pane">
    <GraphButton class="graph-button-important" title="Clear Editor" onclick={handleDelete}>
        <img src={trashIcon} alt="Delete" />
    </GraphButton>
    
    <div class="popup-wrapper" role="group" onmouseenter={() => showPopup('get')} onmouseleave={hidePopup}>
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

    <div class="popup-wrapper" role="group" onmouseenter={() => showPopup('set')} onmouseleave={hidePopup}>
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
        class="input" 
        placeholder="Offset" 
        style="width: 120px;" 
        bind:value={offsetValue}
        oninput={handleOffsetInput}
    />

    <div class="spacer"></div>

    <GraphButton title="Switch View" onclick={toggleView}>
        {#if view === 'text'}
            <img src={treeDiagramIcon} alt="Tree View" />
        {:else}
            <img src={textEditIcon} alt="Text View" />
        {/if}
        <img src={switchIcon} alt="Switch" style="margin-left: 5px;" />
    </GraphButton>
</div>

<style>
    .bottom-pane {
        display: flex;
        align-items: center;
        gap: 15px;
        padding: 10px;
        background-color: #f7f8fa;
    }
    .popup-wrapper {
        position: relative;
        display: inline-flex;
    }
    .spacer {
        flex: 1;
    }
    .input {
        padding: 6px 10px;
        border: 1px solid #dfdfdf;
        border-radius: 4px;
        font-size: 14px;
        outline: none;
    }
    .input:focus {
        border-color: #007bff;
    }
</style>