<script lang="ts">
    import SidebarNav from './views/desktop/SidebarNav.svelte';
    import { appStore, FnTab } from '@/stores/appStore.svelte';
    import { docStore } from '@/stores/docStore.svelte';
    import BookmarkViewView from './views/desktop/BookmarkView.svelte';
    import PageLabelview from './views/desktop/PageLabelview.svelte';
    import TocViewView from './views/desktop/TocView.svelte';
    import MarkDownView from './views/desktop/MarkDownView.svelte';
    import ViewerView from './views/desktop/ViewerView.svelte';
    import MessageContainer from './components/common/MessageContainer.svelte';
    import FileHeader from './views/desktop/FileHeader.svelte';
    import Settings from './components/Settings.svelte';
    import ConfirmDialog from './components/ConfirmDialog.svelte';
    import { provideExternalEditor } from '@/lib/bridge/useExternalEditor.svelte';
    import { onMount, onDestroy } from 'svelte';

    let activeTab = $derived(appStore.activeTab);

    // Automatic lifecycle: initialization and destruction are handled internally
    provideExternalEditor();

</script>

<main class="app-container" role="application">
    <ConfirmDialog />
    <MessageContainer />
    <FileHeader />
    <div class="app-body">
        <SidebarNav />
        <div class="content-area">
                <div style="display: {activeTab === FnTab.bookmark ? 'block' : 'none'}; height: 100%;">
                    <BookmarkViewView />
                </div>
                <div style="display: {activeTab === FnTab.label ? 'block' : 'none'}; height: 100%;">
                    <PageLabelview />
                </div>
                <div style="display: {activeTab === FnTab.tocGenerator ? 'block' : 'none'}; height: 100%;">
                    <TocViewView />
                </div>
                <div style="display: {activeTab === FnTab.markdown ? 'block' : 'none'}; height: 100%;">
                    <MarkDownView />
                </div>
                <div style="display: {activeTab === FnTab.viewer ? 'block' : 'none'}; height: 100%;">
                    <ViewerView />
                </div>
                <div style="display: {activeTab === FnTab.settings ? 'block' : 'none'}; height: 100%;">
                    <Settings />
                </div>
                <!-- Experimental Tab - Only show in DEV mode, otherwise show a default welcome -->
                <div style="display: {activeTab === FnTab.experimental ? 'block' : 'none'}; height: 100%;">
                    <div class="placeholder">
                        <h1>Experimental Features Disabled</h1>
                        <p>This feature is only available in development mode.</p>
                    </div>
                </div>
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
</style>