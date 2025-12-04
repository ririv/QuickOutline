<script lang="ts">
    import { type Snippet } from 'svelte';

    interface Props {
        initialSplit?: number;
        left: Snippet;
        right: Snippet;
    }

    let { 
        initialSplit = $bindable(50),
        left,
        right
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

<div class="split-container" bind:this={container}>
    <div class="pane left" style="width: {initialSplit}%">
        {@render left()}
    </div>
    
    <!-- svelte-ignore a11y_no_static_element_interactions -->
    <div class="resizer" onmousedown={startResize}></div>
    
    <div class="pane right" style="width: {100 - initialSplit}%">
        {@render right()}
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
        cursor: col-resize; /* 重新添加 */
        z-index: 10;
        transition: background 0.2s;
    }
    .resizer:hover, .resizer:active {
        background: #1677ff;
    }
</style>
