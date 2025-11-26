<script lang="ts">
    import type { Bookmark } from "./types";
    import BookmarkNode from "./BookmarkNode.svelte"; // Self-import for recursion
    import { tick } from "svelte";

    interface Props {
        bookmark: Bookmark;
    }
    let { bookmark }: Props = $props();

    let isEditingTitle = $state(false);
    let isEditingPage = $state(false);
    let isExpanded = $state(true);

    async function editTitle() {
        isEditingTitle = true;
        await tick(); // Wait for the DOM to update
        // Find and focus the input, could be improved with a more specific selector if needed
        const inputEl = document.querySelector('.title-cell input');
        (inputEl as HTMLElement)?.focus();
    }

    async function editPage() {
        isEditingPage = true;
        await tick();
        const inputEl = document.querySelector('.page-cell input');
        (inputEl as HTMLElement)?.focus();
    }

</script>

<div class="node-container" style="--level: {bookmark.level}">
    <div class="node-row">
        <div class="node-cell title-cell" style="padding-left: {bookmark.level * 20}px;">
            <button class="expand-btn" onclick={() => isExpanded = !isExpanded} style="visibility: {bookmark.children.length > 0 ? 'visible' : 'hidden'}">
                {isExpanded ? '▼' : '►'}
            </button>
            
            {#if isEditingTitle}
                <input type="text" bind:value={bookmark.title} onblur={() => isEditingTitle = false} onkeydown={e => e.key === 'Enter' && e.currentTarget.blur()} />
            {:else}
                <button class="editable-text" onclick={editTitle}>{bookmark.title}</button>
            {/if}
        </div>
        <div class="node-cell page-cell">
            {#if isEditingPage}
                <input type="text" bind:value={bookmark.page} onblur={() => isEditingPage = false} onkeydown={e => e.key === 'Enter' && e.currentTarget.blur()} />
            {:else}
                <button class="editable-text" onclick={editPage}>{bookmark.page}</button>
            {/if}
        </div>
    </div>

    {#if isExpanded && bookmark.children.length > 0}
        <div class="children-container">
            {#each bookmark.children as child}
                <BookmarkNode bookmark={child} />
            {/each}
        </div>
    {/if}
</div>

<style>
    .node-row {
        display: flex;
        border-bottom: 1px solid #f0f0f0;
    }
    .node-row:hover {
        background-color: #f4f4f4;
    }
    .node-cell {
        padding: 6px 12px;
        display: flex;
        align-items: center;
        width: 100%; /* Allow cell to fill space */
    }
    .title-cell {
        flex: 0.9;
    }
    .page-cell {
        flex: 0.1;
        min-width: 80px;
        border-left: 1px solid #f0f0f0;
    }
    .expand-btn {
        background: none;
        border: none;
        cursor: pointer;
        padding: 0 8px 0 0;
        font-size: 10px;
        color: #666;
    }
    input[type="text"] {
        width: 100%;
        border: 1px solid #409eff;
        outline: none;
        padding: 2px;
        font-family: inherit;
        font-size: inherit;
    }
    .editable-text {
        /* Reset button styles */
        appearance: none;
        -webkit-appearance: none;
        background: none;
        border: none;
        padding: 0;
        margin: 0;
        font: inherit;
        color: inherit;
        text-align: left;
        cursor: text;
        width: 100%;
        /* Add a subtle underline on hover to indicate clickability */
    }
    .editable-text:hover {
        text-decoration: underline;
    }
</style>
