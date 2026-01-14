<script lang="ts">
    import { type Snippet } from 'svelte';

    interface Props {
        initialSplit?: number;
        left: Snippet;
        right: Snippet;
        hideLeft?: boolean;
        hideRight?: boolean;
    }

    let { 
        initialSplit = $bindable(50),
        left,
        right,
        hideLeft = false,
        hideRight = false
    }: Props = $props();

    let container: HTMLDivElement;
    let isResizing = false;
    
    function startResize(e: MouseEvent) {
        isResizing = true;
        document.body.classList.add('is-resizing'); // 关键：添加全局标记
        
        window.addEventListener('mousemove', handleMouseMove);
        window.addEventListener('mouseup', stopResize);
    }

    function handleMouseMove(e: MouseEvent) {
        if (!isResizing || !container) return;
        const rect = container.getBoundingClientRect();
        const x = e.clientX - rect.left;
        const w = rect.width;
        let percent = (x / w) * 100;
        percent = Math.max(10, Math.min(90, percent)); // Clamp 10-90%
        initialSplit = percent;
    }

    function stopResize() {
        isResizing = false;
        document.body.classList.remove('is-resizing'); // 移除标记
        
        window.removeEventListener('mousemove', handleMouseMove);
        window.removeEventListener('mouseup', stopResize);
    }
</script>

<div class="split-container" bind:this={container} class:hide-left={hideLeft} class:hide-right={hideRight}>
    <div class="pane left" style="width: {hideLeft ? '0' : hideRight ? '100%' : initialSplit + '%'}">
        {@render left()}
    </div>
    
    <!-- svelte-ignore a11y_no_static_element_interactions -->
    <div class="resizer" onmousedown={startResize}></div>
    
    <div class="pane right" style="width: {hideRight ? '0' : hideLeft ? '100%' : (100 - initialSplit) + '%'}">
        {@render right()}
    </div>
</div>

<style>
    .split-container {
        display: flex;
        width: 100%;
        height: 100%;
        overflow: hidden;
        position: relative;
    }
    .pane {
        height: 100%;
        overflow: hidden;
        transition: width 0.2s ease-in-out; /* 添加平滑过渡 */
    }

    .split-container.hide-left .resizer,
    .split-container.hide-right .resizer {
        display: none;
    }
    
    /* 
     * Resizer Strategy:
     * 1. The element itself is 0-width to minimize layout impact.
     * 2. ::after is the wide, transparent HIT AREA.
     * 3. ::before is the VISUAL LINE.
     */
    .resizer {
        width: 0;
        position: relative;
        z-index: 10;
        flex-shrink: 0;
        user-select: none;
    }
    
    /* Hit Area: Wide and transparent */
    .resizer::after {
        content: '';
        position: absolute;
        top: 0;
        bottom: 0;
        left: -5px; /* Center 10px area over 0px anchor */
        width: 10px;
        cursor: col-resize;
        background: transparent;
        z-index: 10;
    }

    /* Visual Line: Narrow and colored */
    .resizer::before {
        content: '';
        position: absolute;
        top: 0;
        bottom: 0;
        /* Center 1px line: -0.5px offset */
        left: -0.5px; 
        width: 1px;
        background-color: #e5e5e5;
        transition: all 0.15s ease-out;
        z-index: 9;
        pointer-events: none; /* Clicks go to ::after */
    }

    /* Interaction: Hover hit area -> Transform visual line */
    .resizer:hover::before, .resizer:active::before {
        background-color: #1677ff;
        width: 4px;
        /* Center 4px line: -2px offset */
        left: -2px;
    }

    /* --- Global Protection during Drag --- */
    :global(body.is-resizing) {
        cursor: col-resize !important;
    }

    :global(body.is-resizing *) {
        pointer-events: none !important;
        -webkit-user-select: none !important;
        user-select: none !important;
    }
</style>
