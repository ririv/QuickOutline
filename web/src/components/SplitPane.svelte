<script lang="ts">
    // Simple resizeable split pane
    let container: HTMLDivElement;
    let isResizing = false;
    
    export let initialSplit = 50; // Percentage
    
    function startResize(e: MouseEvent) {
        isResizing = true;
        document.body.style.cursor = 'col-resize';
        document.body.style.userSelect = 'none';
        
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
        document.body.style.cursor = '';
        document.body.style.userSelect = '';
        window.removeEventListener('mousemove', handleMouseMove);
        window.removeEventListener('mouseup', stopResize);
    }
</script>

<div class="split-container" bind:this={container}>
    <div class="pane left" style="width: {initialSplit}%">
        <slot name="left"></slot>
    </div>
    
    <!-- svelte-ignore a11y-no-static-element-interactions -->
    <div class="resizer" on:mousedown={startResize}></div>
    
    <div class="pane right" style="width: {100 - initialSplit}%">
        <slot name="right"></slot>
    </div>
</div>

<style>
    .split-container {
        display: flex;
        width: 100%;
        height: 100%;
        overflow: hidden;
    }
    .pane {
        height: 100%;
        overflow: hidden;
    }
    .resizer {
        width: 5px;
        background: #ddd;
        cursor: col-resize;
        z-index: 10;
        transition: background 0.2s;
    }
    .resizer:hover, .resizer:active {
        background: #1677ff;
    }
</style>
