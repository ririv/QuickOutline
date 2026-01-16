<script lang="ts">
    import { onMount, onDestroy } from 'svelte';
    import { open } from '@tauri-apps/plugin-dialog';
    import { listen } from '@tauri-apps/api/event';
    import { docStore } from '@/stores/docStore.svelte.ts';
    import Icon from "@/components/Icon.svelte";

    const currentFilePath = $derived(docStore.currentFilePath);
    let fileName = $derived(currentFilePath ? getFileName(currentFilePath) : 'Click to Open or Drop PDF');
    
    let isHovering = $state(false);
    let unlistenFns: (() => void)[] = [];
    let headerElement: HTMLElement | undefined = $state();

    function getFileName(path: string) {
        return path.split(/[\/\\]/).pop() || path;
    }

    async function handleOpen() {
        try {
            const selected = await open({
                multiple: false,
                filters: [{ name: 'PDF Files', extensions: ['pdf'] }]
            });
            if (selected) {
                await docStore.openFile(selected as string);
            }
        } catch (e) {
            console.error("File open error:", e);
            const path = prompt("Please enter PDF file path manually:");
            if (path) await docStore.openFile(path);
        }
    }

    onMount(async () => {
        try {
            // @ts-ignore
            const isTauri = !!(window.__TAURI_INTERNALS__ || window.__TAURI__);
            if (isTauri) {
                // Use robust coordinate-based hit testing
                unlistenFns.push(await listen<{ paths: string[], position: { x: number, y: number } }>('tauri://drag-drop', (event) => {
                    const { paths, position } = event.payload;
                    
                    if (headerElement && paths && paths.length > 0) {
                        // 1. Convert Physical to Logical Coordinates
                        const scaleFactor = window.devicePixelRatio || 1;
                        const logicalX = position.x / scaleFactor;
                        const logicalY = position.y / scaleFactor;
                        
                        // 2. Perform Hit Test
                        // Check what element is at this precise screen location
                        const elementAtPoint = document.elementFromPoint(logicalX, logicalY);
                        
                        // 3. Verify Ownership
                        if (elementAtPoint && headerElement.contains(elementAtPoint)) {
                            const pdfPath = paths.find(p => p.toLowerCase().endsWith('.pdf'));
                            if (pdfPath) {
                                docStore.openFile(pdfPath);
                            }
                        }
                    }
                    // Always reset visual state
                    isHovering = false;
                }));

                // Keep cancel listener just for visual cleanup
                unlistenFns.push(await listen('tauri://drag-leave', () => {
                    isHovering = false;
                }));

                // 2. Visual Feedback - Sync with Tauri drag-over coordinates
                unlistenFns.push(await listen<{ position: { x: number, y: number } }>('tauri://drag-over', (event) => {
                    const { position } = event.payload;
                    if (headerElement) {
                        const scaleFactor = window.devicePixelRatio || 1;
                        const logicalX = position.x / scaleFactor;
                        const logicalY = position.y / scaleFactor;
                        
                        const elementAtPoint = document.elementFromPoint(logicalX, logicalY);
                        const isOver = elementAtPoint ? headerElement.contains(elementAtPoint) : false;
                        
                        if (isHovering !== isOver) {
                            isHovering = isOver;
                        }
                    }
                }));
            }
        } catch (e) {
            console.warn("Failed to setup Tauri drag events in FileHeader", e);
        }
    });

    onDestroy(() => {
        unlistenFns.forEach(fn => fn());
    });

    // 使用 HTML5 事件来精准控制 "是否在 Header 上" 的状态
    function onDragEnter(e: DragEvent) {
        e.preventDefault();
        isHovering = true;
    }

    function onDragLeave(e: DragEvent) {
        e.preventDefault();
        // 只有当真正离开 header 区域时才取消高亮
        const target = e.currentTarget as HTMLElement;
        if (!target) return;
        
        const rect = target.getBoundingClientRect();
        if (e.clientX < rect.left || e.clientX >= rect.right || e.clientY < rect.top || e.clientY >= rect.bottom) {
            isHovering = false;
        }
    }

    function onDragOver(e: DragEvent) {
        e.preventDefault();
        isHovering = true;
    }

    function onDrop(e: DragEvent) {
        e.preventDefault();
        isHovering = false;
    }
    
    function handleKeydown(e: KeyboardEvent) {
        if (!currentFilePath && (e.key === 'Enter' || e.key === ' ')) {
            handleOpen();
        }
    }
</script>

<header class="file-header" 
        bind:this={headerElement}
        class:dragging={isHovering}
        ondragenter={onDragEnter}
        ondragleave={onDragLeave}
        ondragover={onDragOver}
        ondrop={onDrop}
        role="region" 
        aria-label="File Header and Drop Zone">
    
    <!-- Drag Region for Tauri Window Movement -->
    <div class="drag-region" data-tauri-drag-region></div>

    <div class="file-info" 
         class:clickable={!currentFilePath}
         onclick={!currentFilePath ? handleOpen : undefined}
         onkeydown={handleKeydown}
         role="button"
         tabindex="0">
        <span class="filename" title={currentFilePath || 'Open File'}>{fileName}</span>
    </div>

    <div class="actions">
        <button class="icon-btn open-btn" onclick={handleOpen} title="Open PDF File">
            <Icon name="folder-open" width="18" height="18" />
        </button>
    </div>

    <!-- Drag Overlay -->
    {#if isHovering}
        <div class="drop-overlay">
            <div class="drop-content">
                <Icon name="folder-open" width="24" height="24" />
                <span>Drop to Open</span>
            </div>
        </div>
    {/if}
</header>

<style>
    .file-header {
        height: 40px;
        display: flex;
        align-items: center;
        justify-content: flex-end;
        padding: 0 12px;
        background: #fff;
        border-bottom: 1px solid #e0e0e0;
        flex-shrink: 0;
        position: relative;
        transition: all 0.2s;
        /* Ensure overlay is contained */
        overflow: hidden; 
    }
    
    .file-header.dragging {
        background-color: #f0f9ff;
    }

    .drag-region {
        position: absolute;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        z-index: 0;
    }
    
    .drop-overlay {
        position: absolute;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        background-color: rgba(240, 249, 255, 0.95);
        border: 2px dashed #1890ff;
        box-sizing: border-box;
        display: flex;
        align-items: center;
        justify-content: center;
        z-index: 10;
        animation: fadeIn 0.15s ease-out;
    }

    .drop-content {
        display: flex;
        align-items: center;
        gap: 8px;
        color: #1890ff;
        font-weight: 500;
        pointer-events: none;
    }

    @keyframes fadeIn {
        from { opacity: 0; }
        to { opacity: 1; }
    }
    
    .file-info {
        position: absolute;
        left: 50%;
        transform: translateX(-50%);
        max-width: 60%;
        display: flex;
        align-items: center;
        justify-content: center;
        overflow: hidden;
        z-index: 1;
    }
    
    .file-info.clickable {
        cursor: pointer;
        padding: 0px 8px;
        border-radius: 4px;
        transition: all 0.2s ease-in-out;
    }
    
    .file-info.clickable:hover {
        background: #f0f0f0;
        color: #333;
    }
    
    .filename {
        font-size: 14px;
        font-weight: 500;
        color: #333;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
        pointer-events: none; /* 防止干扰拖拽事件 */
    }
    
    .file-info.clickable .filename {
        color: #888;
        font-weight: normal;
        font-size: 13px;
    }

    .actions {
        display: flex;
        align-items: center;
        z-index: 1;
    }

    .icon-btn {
        width: 28px;
        height: 28px;
        border: 1px solid transparent;
        background: transparent;
        border-radius: 4px;
        cursor: pointer;
        display: flex;
        align-items: center;
        justify-content: center;
        color: #555;
        transition: all 0.2s;
        padding: 0;
    }
    
    .icon-btn:hover {
        background: rgba(0,0,0,0.06);
        color: #333;
    }
</style>