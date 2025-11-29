<script lang="ts">
    import type { Bookmark } from "./types";
    import BookmarkNode from "./BookmarkNode.svelte";
    import { onMount, onDestroy } from 'svelte';
    import { bookmarkStore } from '@/stores/bookmarkStore';
    import { rpc } from '@/lib/api/rpc';
    import { get } from 'svelte/store';

    let bookmarks = $state<Bookmark[]>([]);
    let rootBookmark: any = null; // Keep the root structure for serialization

    onMount(async () => {
        const text = get(bookmarkStore).text;
        if (text) {
            try {
                const root = await rpc.parseTextToTree(text);
                if (root) {
                    rootBookmark = root;
                    bookmarks = root.children || [];
                }
            } catch (e) {
                console.error("Failed to parse bookmarks", e);
            }
        }
    });

    onDestroy(async () => {
        // Sync back to text store when switching views
        if (rootBookmark) {
            // Update children in root object
            rootBookmark.children = $state.snapshot(bookmarks); 
            try {
                const text = await rpc.serializeTreeToText(rootBookmark);
                bookmarkStore.setText(text);
            } catch (e) {
                console.error("Failed to serialize bookmarks", e);
            }
        }
    });

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