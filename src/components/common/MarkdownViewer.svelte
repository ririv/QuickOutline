<script lang="ts">
    import { onMount } from 'svelte';
    import { markdownService } from '@/lib/services/MarkdownService';

    let { src } = $props<{ src: string }>();

    let guideContent = $state('');
    let loading = $state(true);
    let error = $state<string | null>(null);

    onMount(async () => {
        try {
            const response = await fetch(src);
            if (!response.ok) {
                throw new Error(`Failed to load guide: ${response.statusText}`);
            }
            const text = await response.text();
            // Render markdown to HTML
            guideContent = markdownService.compileHtml(text);
        } catch (e) {
            error = e instanceof Error ? e.message : 'Unknown error occurred';
            console.error(`Error loading markdown from ${src}:`, e);
        } finally {
            loading = false;
        }
    });
</script>

<div class="guide-container markdown-body">
    {#if loading}
        <div class="loading">Loading guide...</div>
    {:else if error}
        <div class="error">
            <p>Error loading guide:</p>
            <p>{error}</p>
        </div>
    {:else}
        {@html guideContent}
    {/if}
</div>

<style>
    .guide-container {
        padding: 16px;
        font-size: 13px;
        color: #444;
        overflow-y: auto;
    }

    .loading, .error {
        padding: 20px;
        text-align: center;
        color: #666;
    }

    .error {
        color: #d32f2f;
    }

    /* Basic Markdown Styles Adaptation */
    :global(.markdown-body h1) { font-size: 1.5em; margin-bottom: 0.5em; border-bottom: 1px solid #eee; padding-bottom: 0.3em; }
    :global(.markdown-body h2) { font-size: 1.3em; margin-top: 1em; margin-bottom: 0.5em; border-bottom: 1px dashed #eee; padding-bottom: 0.3em; }
    :global(.markdown-body h3) { font-size: 1.1em; margin-top: 1em; margin-bottom: 0.5em; }
    :global(.markdown-body h4) { font-size: 1em; margin-top: 0.8em; margin-bottom: 0.4em; font-weight: bold; }
    :global(.markdown-body p) { margin-bottom: 0.8em; line-height: 1.5; }
    :global(.markdown-body ul), :global(.markdown-body ol) { padding-left: 1.5em; margin-bottom: 0.8em; }
    :global(.markdown-body li) { margin-bottom: 0.3em; }
    :global(.markdown-body code) { background-color: #f4f4f4; padding: 2px 4px; border-radius: 3px; font-family: monospace; font-size: 0.9em; color: #c7254e; }
    :global(.markdown-body pre) { background-color: #f6f8fa; padding: 10px; border-radius: 4px; overflow-x: auto; margin-bottom: 1em; }
    :global(.markdown-body pre code) { background-color: transparent; padding: 0; color: inherit; font-size: 0.9em; }
    :global(.markdown-body table) { width: 100%; border-collapse: collapse; margin-bottom: 1em; font-size: 0.9em; }
    :global(.markdown-body th), :global(.markdown-body td) { border: 1px solid #dfe2e5; padding: 6px 13px; }
    :global(.markdown-body th) { background-color: #f6f8fa; font-weight: 600; }
    :global(.markdown-body tr:nth-child(2n)) { background-color: #f8f8f8; }
    :global(.markdown-body blockquote) { color: #6a737d; border-left: 0.25em solid #dfe2e5; padding: 0 1em; margin: 0 0 1em 0; }
</style>
