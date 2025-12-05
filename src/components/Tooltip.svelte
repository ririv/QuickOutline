<script lang="ts">
    import { type Snippet } from 'svelte';

    interface Props {
        content?: string;
        className?: string;
        position?: 'top' | 'bottom';
        rightAligned?: boolean;
        children?: Snippet;
        popup?: Snippet; // New snippet prop for custom tooltip content
    }

    let { content = '', className = '', position = 'top', rightAligned = false, children, popup }: Props = $props();
</script>

<div class="tooltip-container {className}">
    {@render children?.()}
    <div class="tooltip {position === 'top' ? 'top' : 'bottom'}" class:right-aligned={rightAligned}>
        {#if popup}
            {@render popup()}
        {:else}
            {@html content}
        {/if}
    </div>
</div>

<style>
    .tooltip-container {
        position: relative;
        display: flex;
        align-items: center;
    }

    .tooltip {
        visibility: hidden;
        background-color: #333;
        color: #fff;
        text-align: center;
        border-radius: 4px;
        padding: 4px 8px;
        position: absolute;
        z-index: 10;
        font-size: 11px;
        white-space: nowrap;
        opacity: 0;
        transition: opacity 0.2s;
        pointer-events: none;
    }

    .tooltip.top {
        bottom: 125%; /* Position above */
        left: 50%;
        transform: translateX(-50%);
    }

    .tooltip.top::after {
        content: "";
        position: absolute;
        top: 100%; /* At the bottom of the tooltip */
        left: 50%;
        margin-left: -5px;
        border-width: 5px;
        border-style: solid;
        border-color: #333 transparent transparent transparent;
    }

    .tooltip.bottom {
        top: 125%; /* Position below */
        left: 50%;
        transform: translateX(-50%);
    }

    .tooltip.bottom::after {
        content: "";
        position: absolute;
        bottom: 100%; /* At the top of the tooltip */
        left: 50%;
        margin-left: -5px;
        border-width: 5px;
        border-style: solid;
        border-color: transparent transparent #333 transparent;
    }

    .tooltip.right-aligned {
        left: auto;
        right: 0;
        transform: translateX(0);
    }

    /* Adjust arrow position if rightAligned */
    .tooltip.right-aligned.top::after {
        left: auto;
        right: 4px;
        margin-left: 0;
        margin-right: 0; /* Remove margin-right */
        transform: translateX(0); /* Reset transform */
    }

    .tooltip.right-aligned.bottom::after {
        left: auto;
        right: 4px;
        margin-left: 0;
        margin-right: 0; /* Remove margin-right */
        transform: translateX(0); /* Reset transform */
    }


    .tooltip-container:hover .tooltip {
        visibility: visible;
        opacity: 1;
    }
</style>