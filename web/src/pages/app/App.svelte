<script lang="ts">
    import LeftPane from '../../components/LeftPane.svelte';
    import { appStore, FnTab } from '../../stores/appStore';

    let activeTab = $state(FnTab.bookmark);
    appStore.subscribe(state => {
        activeTab = state.activeTab;
    });
</script>

<main class="app-layout">
    <LeftPane />
    <div class="content-area">
        {#if activeTab === FnTab.bookmark}
            <h1>Bookmark Content</h1>
            <p>This is the content for the Bookmark tab.</p>
        {:else if activeTab === FnTab.label}
            <h1>Page Label Content</h1>
            <p>This is the content for the Page Label tab.</p>
        {:else if activeTab === FnTab.tocGenerator}
            <h1>TOC Generator Content</h1>
            <p>This is the content for the TOC Generator tab.</p>
        {:else if activeTab === FnTab.markdown}
            <h1>Markdown Content</h1>
            <p>This is the content for the Markdown tab.</p>
        {:else if activeTab === FnTab.preview}
            <h1>Preview Content</h1>
            <p>This is the content for the Preview tab.</p>
        {:else}
            <h1>Welcome</h1>
            <p>Select a tab from the left.</p>
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
        padding: 20px;
        overflow-y: auto; /* Allow content to scroll if it exceeds height */
        background-color: #f0f2f5;
        color: #333; /* Default text color */
    }
</style>
