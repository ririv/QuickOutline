<script lang="ts">
    import LeftPane from './components/LeftPane.svelte';
    import { appStore, FnTab } from '@/stores/appStore';
    import { docStore } from '@/stores/docStore';
    import BookmarkTab from './pages/bookmark/App.svelte';
    import PageLabelTab from './pages/pagelabel/App.svelte';
    import TocGeneratorTab from './pages/toc/App.svelte';
    import MarkdownTab from './pages/markdown/App.svelte';
    import RpcProvider from './components/RpcProvider.svelte';
    import MessageContainer from './components/common/MessageContainer.svelte';
    import FileHeader from './components/FileHeader.svelte';
    import Settings from './components/Settings.svelte';
    import { listen } from '@tauri-apps/api/event';
    import { onMount, onDestroy } from 'svelte';

    let activeTab = $state(FnTab.bookmark);
    appStore.subscribe(state => {
        activeTab = state.activeTab;
    });

    let isDragging = $state(false);
    let unlistenFns: (() => void)[] = [];
    let isTauri = false;

    onMount(async () => {
        // @ts-ignore
        isTauri = !!(window.__TAURI_INTERNALS__ || window.__TAURI__);

        if (isTauri) {
            // Tauri Strategy
            try {
                unlistenFns.push(await listen('tauri://drag-enter', () => {
                    isDragging = true;
                }));

                unlistenFns.push(await listen('tauri://drag-leave', () => {
                    isDragging = false;
                }));

                unlistenFns.push(await listen<{ paths: string[] }>('tauri://drag-drop', (event) => {
                    isDragging = false;
                    const paths = event.payload.paths;
                    if (paths && paths.length > 0) {
                        const pdfPath = paths.find(p => p.toLowerCase().endsWith('.pdf'));
                        if (pdfPath) {
                            docStore.openFile(pdfPath);
                        }
                    }
                }));
            } catch (e) {
                console.warn("Failed to setup Tauri drag events", e);
            }
        }
    });

    onDestroy(() => {
        unlistenFns.forEach(fn => fn());
    });

    // HTML5 Fallback Handlers
    function handleHtmlDragEnter(e: DragEvent) {
        e.preventDefault();
        if (!isTauri && e.dataTransfer?.types.includes('Files')) {
            isDragging = true;
        }
    }

    function handleHtmlDragOver(e: DragEvent) {
        e.preventDefault();
    }

    function handleHtmlDragLeave(e: DragEvent) {
        e.preventDefault();
        if (!isTauri) {
            isDragging = false;
        }
    }

    function handleHtmlDrop(e: DragEvent) {
        e.preventDefault();
        if (!isTauri) {
            isDragging = false;
            const manualPath = prompt(
                "Browser Restriction: Cannot access file path from drag-and-drop.\n\n" +
                "If the Java backend is running locally, please enter the absolute file path manually:"
            );
            if (manualPath) {
                docStore.openFile(manualPath);
            }
        }
    }
</script>

<main class="app-container" 
      ondragenter={handleHtmlDragEnter} 
      ondragover={handleHtmlDragOver} 
      role="application">
    {#if isDragging}
        <!-- Overlay handles events to prevent flickering in HTML5 mode -->
        <div class="drag-overlay" 
             ondragleave={handleHtmlDragLeave} 
             ondrop={handleHtmlDrop}
             ondragover={handleHtmlDragOver}
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
                    <Settings />
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
        pointer-events: none; /* Critical for preventing flickering */
    }
</style>
