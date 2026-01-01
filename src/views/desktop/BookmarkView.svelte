<script lang="ts">
    import TextSubView from '../../components/bookmark/TextSubView.svelte';
    import TreeSubView from '../../components/bookmark/TreeSubView.svelte';
    import BookmarkBottomPane from '../../components/bookmark/BookmarkBottomPane.svelte';
    import SplitPane from '../../components/SplitPane.svelte';
    import { useBookmarkActions, type BookmarkViewMode } from '../shared/bookmark.svelte.ts';
    
    let currentView = $state<BookmarkViewMode>('text');
    const {} = useBookmarkActions();

</script>

<div class="bookmark-tab-container">
    <main class="main-content">
        {#if currentView === 'text'}
            <TextSubView />
        {:else if currentView === 'tree'}
            <TreeSubView />
        {:else}
            <SplitPane initialSplit={50}>
                {#snippet left()}
                <div style="height: 100%; width: 100%;">
                    <TextSubView />
                </div>
                {/snippet}
                
                {#snippet right()}
                <div style="height: 100%; width: 100%;">
                    <TreeSubView />
                </div>
                {/snippet}
            </SplitPane>
        {/if}
    </main>
    <BookmarkBottomPane bind:view={currentView} />
</div>

<style>
    .bookmark-tab-container {
        display: flex;
        flex-direction: column;
        height: 100%;
        width: 100%;
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
</style>
