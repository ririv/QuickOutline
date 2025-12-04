<script lang="ts">
    import TextSubView from '../../components/bookmark/TextSubView.svelte';
    import TreeSubView from '../../components/bookmark/TreeSubView.svelte';
    import BookmarkBottomPane from '../../components/bookmark/BookmarkBottomPane.svelte';
    import SplitPane from '../../components/SplitPane.svelte';
    import { messageStore } from '@/stores/messageStore';
    
    type View = 'text' | 'tree' | 'double';
    let currentView = $state<View>('text');

    function testMessages() {
        messageStore.add('This is an info message.', 'INFO');
        setTimeout(() => messageStore.add('This is a success message!', 'SUCCESS'), 300);
        setTimeout(() => messageStore.add('This is a warning message, be careful, this is a long message to test wrapping.', 'WARNING'), 600);
        setTimeout(() => messageStore.add('This is an error message.', 'ERROR'), 900);
    }

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
    .divider {
        height: 1px;
        background-color: #dfdfdf; /* -my-color-divding-line */
        flex-shrink: 0;
    }
</style>
