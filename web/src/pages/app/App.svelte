<script lang="ts">
    import LeftPane from '../../components/LeftPane.svelte';
    import { appStore, FnTab } from '@/stores/appStore';
    import BookmarkTab from '../bookmark/App.svelte';
    import PageLabelTab from '../pagelabel/App.svelte';
    import TocGeneratorTab from '../toc/App.svelte';
    import MarkdownTab from '../markdown/App.svelte';
    import RpcProvider from '../../components/RpcProvider.svelte';
    import MessageContainer from '../../components/common/MessageContainer.svelte';
    import FileHeader from '../../components/FileHeader.svelte';
    import { rpc } from '@/lib/api/rpc';

    let activeTab = $state(FnTab.bookmark);
    appStore.subscribe(state => {
        activeTab = state.activeTab;
    });

    let isDragging = $state(false);

    function handleDragEnter(e: DragEvent) {
        e.preventDefault();
        if (e.dataTransfer?.types.includes('Files')) {
            isDragging = true;
        }
    }

    function handleDragOver(e: DragEvent) {
        e.preventDefault();
    }

    function handleDragLeave(e: DragEvent) {
        e.preventDefault();
        isDragging = false;
    }

    async function handleDrop(e: DragEvent) {
        e.preventDefault();
        isDragging = false;
        
        if (e.dataTransfer?.files && e.dataTransfer.files.length > 0) {
            const file = e.dataTransfer.files[0];
            if (file.name.toLowerCase().endsWith('.pdf')) {
                // @ts-ignore
                const path = file.path; // Tauri specific
                if (path) {
                    await rpc.openFile(path);
                } else {
                    console.warn("No file path available (Browser mode?)");
                    const manualPath = prompt("Browser mode detected: Please enter PDF file path manually:");
                    if (manualPath) {
                        await rpc.openFile(manualPath);
                    }
                }
            }
        }
    }
</script>

<main class="app-container" ondragenter={handleDragEnter} ondragover={handleDragOver} role="application">
    {#if isDragging}
        <div class="drag-overlay" 
             ondragleave={handleDragLeave} 
             ondrop={handleDrop}
             ondragover={handleDragOver} 
             role="presentation">
            <div class="drag-message">Drop PDF File Here</div>
        </div>
    {/if}

    <MessageContainer />
    <FileHeader />
    <div class="app-body">
        <LeftPane />
        <div class="content-area">
            <RpcProvider>
                {#if activeTab === FnTab.bookmark}
                    <BookmarkTab />
                {:else if activeTab === FnTab.label}
                    <PageLabelTab />
                {:else if activeTab === FnTab.tocGenerator}
                    <TocGeneratorTab />
                {:else if activeTab === FnTab.markdown}
                    <MarkdownTab />
                {:else if activeTab === FnTab.preview}
                    <div class="placeholder">
                        <h1>Preview Content</h1>
                        <p>This is the content for the Preview tab.</p>
                    </div>
                {:else if activeTab === FnTab.settings}
                    <div class="placeholder">
                        <h1>Settings Content</h1>
                        <p>This is the content for the Settings tab.</p>
                    </div>
                {:else}
                    <div class="placeholder">
                        <h1>Welcome</h1>
                        <p>Select a tab from the left.</p>
                    </div>
                {/if}
            </RpcProvider>
        </div>
    </div>
</main>

<style>
    :global(body) {
        margin: 0;
        padding: 0;
        overflow: hidden; /* Ensure body does not scroll */
    }

    .app-container {
        display: flex;
        flex-direction: column;
        height: 100vh;
        width: 100vw;
        overflow: hidden;
    }

    .app-body {
        display: flex;
        flex: 1;
        overflow: hidden; /* Container for sidebar and content */
    }

    .content-area {
        flex-grow: 1;
        padding: 0; /* Remove padding to let sub-apps control layout */
        overflow: hidden; /* Let sub-apps control scrolling */
        background-color: #f0f2f5;
        color: #333; /* Default text color */
        display: flex;
        flex-direction: column;
    }
    
    .content-area > :global(*) {
        flex-grow: 1;
    }

    .placeholder {
        padding: 20px;
    }

    .drag-overlay {
        position: absolute;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        background: rgba(255, 255, 255, 0.9);
        z-index: 9999;
        display: flex;
        align-items: center;
        justify-content: center;
        border: 4px dashed #1677ff;
        box-sizing: border-box;
    }
    
    .drag-message {
        font-size: 24px;
        color: #1677ff;
        font-weight: 600;
        pointer-events: none;
    }
</style>