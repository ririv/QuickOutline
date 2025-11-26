<script lang="ts">
    import type { Bookmark } from "./types";
    import BookmarkNode from "./BookmarkNode.svelte";

    let bookmarks = $state<Bookmark[]>([
        { 
            id: '1', title: 'Chapter 1: Introduction', page: 1, level: 1, children: [
                { id: '1.1', title: '1.1 Overview', page: 2, level: 2, children: [] },
                { id: '1.2', title: '1.2 History', page: 5, level: 2, children: [] }
            ] 
        },
        { id: '2', title: 'Chapter 2: Advanced Topics', page: 10, level: 1, children: [] },
    ]);

</script>

<div class="tree-subview-container">
    <div class="tree-header">
        <div class="tree-column-title">Title</div>
        <div class="tree-column-page">Page</div>
    </div>
    <div class="tree-body">
        {#each bookmarks as bookmark}
            <BookmarkNode {bookmark} />
        {/each}
    </div>
</div>

<style>
    .tree-subview-container {
        width: 100%;
        height: 100%;
        display: flex;
        flex-direction: column;
        background-color: white;
        font-size: 14px;
    }
    .tree-header {
        display: flex;
        border-bottom: 1px solid #d1d1d1;
        background-color: white;
        color: #888888;
        font-weight: 500;
        flex-shrink: 0;
    }
    .tree-column-title {
        flex: 0.9;
        padding: 8px 12px;
    }
    .tree-column-page {
        flex: 0.1;
        min-width: 80px;
        padding: 8px 12px;
        border-left: 1px solid #d1d1d1;
    }
    .tree-body {
        flex: 1;
        overflow-y: auto;
    }
</style>