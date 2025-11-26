<script lang="ts">
    import trashIcon from '@/assets/icons/trash.svg';
    import textEditIcon from '@/assets/icons/text-edit.svg';
    import treeDiagramIcon from '@/assets/icons/tree-diagram.svg';
    import switchIcon from '@/assets/icons/switch.svg';

    import GetContentsPopup from './GetContentsPopup.svelte';
    import SetContentsPopup from './SetContentsPopup.svelte';
    import type { ViewScaleType } from './SetContentsPopup.svelte';

    interface Props {
        view: 'text' | 'tree';
    }
    let { view = $bindable() }: Props = $props();

    let activePopup = $state<'get' | 'set' | null>(null);
    let getContentsBtnEl: HTMLElement;
    let setContentsBtnEl: HTMLElement;
    let hideTimer: number | null = null;

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
        console.log('Get contents from:', type);
        activePopup = null;
        // TODO: post event
    }

    function handleSetContentsSelect(type: ViewScaleType) {
        console.log('Set contents with view scale:', type);
        // Don't close popup immediately to show selection
    }

</script>

<div class="bottom-pane">
    <button class="graph-button graph-button-important" title="Delete">
        <img src={trashIcon} alt="Delete" />
    </button>
    
    <div class="popup-wrapper" onmouseenter={() => showPopup('get')} onmouseleave={hidePopup}>
        <button class="my-button plain-button-primary" bind:this={getContentsBtnEl}>
            Get Contents
        </button>
        {#if activePopup === 'get' && getContentsBtnEl}
            <GetContentsPopup triggerEl={getContentsBtnEl} onSelect={handleGetContentsSelect} />
        {/if}
    </div>

    <div class="popup-wrapper" onmouseenter={() => showPopup('set')} onmouseleave={hidePopup}>
        <button class="my-button plain-button-important" bind:this={setContentsBtnEl}>
            Set Contents
        </button>
        {#if activePopup === 'set' && setContentsBtnEl}
            <SetContentsPopup triggerEl={setContentsBtnEl} onSelect={handleSetContentsSelect} />
        {/if}
    </div>

    <input type="text" class="input" placeholder="Offset" style="width: 120px;" />

    <div class="spacer"></div>

    <button class="graph-button" title="Switch View" onclick={toggleView}>
        {#if view === 'text'}
            <img src={treeDiagramIcon} alt="Tree View" />
        {:else}
            <img src={textEditIcon} alt="Text View" />
        {/if}
        <img src={switchIcon} alt="Switch" style="margin-left: 5px;" />
    </button>
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