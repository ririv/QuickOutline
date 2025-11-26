<script lang="ts">
    import TextSubView from '../../components/bookmark/TextSubView.svelte';
    import TreeSubView from '../../components/bookmark/TreeSubView.svelte';
    import BookmarkBottomPane from '../../components/bookmark/BookmarkBottomPane.svelte';
    
    type View = 'text' | 'tree';
    let currentView = $state<View>('text');

</script>

<div class="bookmark-tab-container">
    <main class="main-content">
        {#if currentView === 'text'}
            <TextSubView />
        {:else}
            <TreeSubView />
        {/if}
    </main>
    <div class="divider"></div>
    <BookmarkBottomPane bind:view={currentView} />
</div>

<style>
    .bookmark-tab-container {
        display: flex;
        flex-direction: column;
        height: 100vh;
        width: 100vw;
        overflow: hidden;
        background-color: #f7f8fa; /* -my-window-color */
    }
    .main-content {
        flex: 1;
        overflow: hidden;
        display: flex; /* Ensure subviews fill the space */
    }
    .main-content > :global(*) {
        flex: 1; /* Ensure subviews fill the space */
    }
    .divider {
        height: 1px;
        background-color: #dfdfdf; /* -my-color-divding-line */
        flex-shrink: 0;
    }
</style>
