<script lang="ts">
    import LeftPane from '../../components/LeftPane.svelte';
    import { appStore, FnTab } from '@/stores/appStore';
    import BookmarkTab from '../bookmark/App.svelte';
    import PageLabelTab from '../pagelabel/App.svelte';
    import TocGeneratorTab from '../toc/App.svelte';
    import MarkdownTab from '../markdown/App.svelte';

    let activeTab = $state(FnTab.bookmark);
    appStore.subscribe(state => {
        activeTab = state.activeTab;
    });
</script>

<main class="app-layout">
    <LeftPane />
    <div class="content-area">
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
    </div>
</main>

<style>
    :global(body) {
        margin: 0;
        padding: 0;
        overflow: hidden; /* Ensure body does not scroll */
    }

    .app-layout {
        display: flex;
        height: 100vh;
        width: 100vw;
        overflow: hidden; /* Ensure the main layout itself handles overflow */
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
