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

    interface Props {
        view: 'text' | 'tree';
    }
    let { view = $bindable() }: Props = $props();

    let activePopup = $state<'get' | 'set' | null>('get'); // Revert to normal state
    let getContentsBtnEl = $state<HTMLElement>();
    let setContentsBtnEl = $state<HTMLElement>();
    let hideTimer: number | null = null;
    let getContentsPopupSelected = $state<'bookmark' | 'toc'>('bookmark'); // State for GetContentsPopup selection

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

    function handleGetContentsSelect(type: 'bookmark' | 'toc') {
        getContentsPopupSelected = type; // Update the state in parent
        console.log('Get contents from:', getContentsPopupSelected);
        // activePopup = null; // DO NOT close popup on select, hover logic will handle it.
        // TODO: post event
    }

    function handleSetContentsSelect(type: ViewScaleType) {
        console.log('Set contents with view scale:', type);
        // Don't close popup immediately to show selection
    }

</script>

<div class="bottom-pane">
    <GraphButton className="graph-button-important" title="Delete">
        <img src={trashIcon} alt="Delete" />
    </GraphButton>
    
    <div class="popup-wrapper" role="group" onmouseenter={() => showPopup('get')} onmouseleave={hidePopup}>
        <StyledButton type="primary" hoverEffect="darken" bind:this={getContentsBtnEl}>
            Get Contents
        </StyledButton>
        {#if activePopup === 'get' && getContentsBtnEl}
            <GetContentsPopup 
                triggerEl={getContentsBtnEl} 
                onSelect={handleGetContentsSelect} 
                selected={getContentsPopupSelected}
            />
        {/if}
    </div>

    <div class="popup-wrapper" role="group" onmouseenter={() => showPopup('set')} onmouseleave={hidePopup}>
        <StyledButton type="important" hoverEffect="elevation" bind:this={setContentsBtnEl}>
            Set Contents
        </StyledButton>
        {#if activePopup === 'set' && setContentsBtnEl}
            <SetContentsPopup triggerEl={setContentsBtnEl} onSelect={handleSetContentsSelect} />
        {/if}
    </div>

    <input type="text" class="input" placeholder="Offset" style="width: 120px;" />

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
        display: inline-block; /* Or just display: contents; */
    }
    .spacer {
        flex: 1;
    }
</style>
