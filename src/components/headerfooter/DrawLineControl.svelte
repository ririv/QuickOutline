<script lang="ts">
    import ArrowPopup from '../controls/ArrowPopup.svelte';

    interface Props {
        drawLine?: boolean;
        padding?: number;
        type?: 'header' | 'footer';
        isHovered?: boolean;
        justToggled?: boolean;
        onChange?: () => void;
    }

    let {
        drawLine = $bindable(false),
        padding = $bindable(0),
        type = 'header',
        isHovered = $bindable(false),
        justToggled = $bindable(false),
        onChange
    }: Props = $props();

    let triggerEl = $state<HTMLElement | undefined>();
    let showPadPopup = $state(false);
    let padHideTimer: number | null = null;

    function openPadPopup() {
        if (padHideTimer) {
            clearTimeout(padHideTimer);
            padHideTimer = null;
        }
        showPadPopup = true;
    }

    function closePadPopup() {
        padHideTimer = setTimeout(() => {
            showPadPopup = false;
        }, 200);
    }

    function toggle() {
        drawLine = !drawLine;
        justToggled = true;

        if (drawLine && isHovered) {
            openPadPopup();
        } else if (!drawLine) {
            showPadPopup = false;
        }

        if (onChange) onChange();
    }
    
    function handlePaddingInput() {
        if (onChange) onChange();
    }
</script>

<div class="btn-wrapper">
    <button
        bind:this={triggerEl}
        class="toggle-line-btn" class:active={drawLine}
        onclick={toggle}
        onmouseenter={() => {
            isHovered = true;
            if (drawLine) {
                openPadPopup();
            }
        }}
        onmouseleave={() => {
            isHovered = false;
            justToggled = false;
            closePadPopup();
        }}
        title="Show Divider Line"
    >
        <svg fill="none" height="14" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round"
             stroke-width="2" viewBox="0 0 24 24" width="14" xmlns="http://www.w3.org/2000/svg">
            {#if type === 'header'}
                <rect x="2" y="4" width="18" height="12" rx="2" stroke-opacity="0.3" stroke-dasharray="2 2"
                      stroke={drawLine ? '#1677ff' : '#999'}></rect>
                <line x1="2" y1="20" x2="20" y2="20" stroke={drawLine ? '#1677ff' : '#999'}
                      stroke-dasharray={drawLine ? '0' : '2 2'}></line>
            {:else}
                <rect x="2" y="8" width="18" height="12" rx="2" stroke-opacity="0.3" stroke-dasharray="2 2"
                      stroke={drawLine ? '#1677ff' : '#999'}></rect>
                <line x1="2" y1="4" x2="20" y2="4" stroke={drawLine ? '#1677ff' : '#999'}
                      stroke-dasharray={drawLine ? '0' : '2 2'}></line>
            {/if}
        </svg>
    </button>
    
    {#if showPadPopup}
        <ArrowPopup
            usePortal={false}
            placement={type === 'header' ? 'bottom' : 'top'}
            triggerEl={triggerEl}
            onmouseenter={openPadPopup}
            onmouseleave={closePadPopup}
        >
            <div class="pad-popup">
                <span>Pad</span>
                <input type="number" bind:value={padding} min="0" oninput={handlePaddingInput} />
            </div>
        </ArrowPopup>
    {/if}
</div>

<style>
    .btn-wrapper {
        position: relative;
    }

    .toggle-line-btn {
        background: transparent;
        border: none;
        padding: 2px;
        cursor: pointer;
        display: flex;
        align-items: center;
        justify-content: center;
        border-radius: 3px;
        color: #999;
        transition: all 0.2s;
        width: 24px;
        height: 24px;
    }

    .toggle-line-btn:hover {
        background-color: #f0f0f0;
        color: #666;
    }

    .toggle-line-btn.active {
        background-color: #e6f7ff;
        color: #1677ff;
    }

    .toggle-line-btn svg {
        width: 100%;
        height: 100%;
        stroke: currentColor;
    }

    .pad-popup {
        display: flex;
        flex-direction: column;
        gap: 4px;
        padding: 4px;
        align-items: center;
    }

    .pad-popup span {
        font-size: 11px;
        color: #888;
    }

    .pad-popup input {
        width: 40px;
        padding: 2px;
        text-align: center;
        border: 1px solid #ddd;
        border-radius: 3px;
        font-size: 12px;
    }
    
    /* Hide popup by default logic handled by svelte if block + ArrowPopup, 
       but we keep the hover-popup class logic from HeaderFooterEditor if needed?
       Wait, in HeaderFooterEditor the .hover-popup class was used for other buttons.
       Here we manually control showPadPopup, so we don't need CSS hover tricks.
    */
</style>