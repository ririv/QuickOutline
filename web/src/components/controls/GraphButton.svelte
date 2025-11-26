<script lang="ts">
    interface Props {
        title?: string;
        className?: string;
        onclick?: (e: MouseEvent) => void;
        children?: import('svelte').Snippet;
    }
    let { title = '', className = '', onclick, children }: Props = $props();
</script>

<button 
    class="graph-button {className}" 
    {title} 
    onclick={onclick}
>
    {@render children?.()}
</button>

<style>
    .graph-button {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        background-color: transparent;
        border: none;
        cursor: pointer;
        padding: 6px;
        border-radius: 4px;
        transition: background-color 0.2s;
    }
    .graph-button:hover {
        background-color: rgba(0,0,0,0.05);
    }

    /* Make sure icons inside scale correctly */
    :global(.graph-button > *) {
        display: block; /* Ensures imgs/svgs inside behave well */
    }

    /* Variants from global.css */
    :global(.graph-button.graph-button-important:hover) {
        background-color: var(--el-color-plain-important-bg);
    }
    :global(.graph-button.active) {
        background-color: var(--el-color-plain-primary-bg);
    }
    :global(.graph-button.active img), :global(.graph-button.active svg) {
        opacity: 1;
    }
</style>
