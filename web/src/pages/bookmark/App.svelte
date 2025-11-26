<script lang="ts">
    import TextSubView from '../../components/bookmark/TextSubView.svelte';
    import TreeSubView from '../../components/bookmark/TreeSubView.svelte';
    import BookmarkBottomPane from '../../components/bookmark/BookmarkBottomPane.svelte';
    import MessageContainer from '../../components/common/MessageContainer.svelte';
    import { messageStore } from '@/stores/messageStore';
    
    type View = 'text' | 'tree';
    let currentView = $state<View>('text');

    function testMessages() {
        messageStore.add('This is an info message.', 'INFO');
        setTimeout(() => messageStore.add('This is a success message!', 'SUCCESS'), 300);
        setTimeout(() => messageStore.add('This is a warning message, be careful, this is a long message to test wrapping.', 'WARNING'), 600);
        setTimeout(() => messageStore.add('This is an error message.', 'ERROR'), 900);
    }

</script>

<MessageContainer />

<!-- Temporary test button -->
<button onclick={testMessages} style="position:fixed; top: 10px; right: 10px; z-index: 9999;">Test Msgs</button>

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
