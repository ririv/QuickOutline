<script lang="ts">
    import { onMount, onDestroy } from 'svelte';

    export let show: boolean;
    export let title: string = '';

    function close() {
        show = false;
    }

    function handleKeydown(event: KeyboardEvent) {
        if (event.key === 'Escape') {
            close();
        }
    }

    onMount(() => {
        window.addEventListener('keydown', handleKeydown);
    });

    onDestroy(() => {
        window.removeEventListener('keydown', handleKeydown);
    });
</script>

{#if show}
<div class="modal-overlay" on:click|self={close} role="presentation" tabindex="-1">
    <div class="modal-content" role="dialog" aria-modal="true" aria-labelledby="modal-title">
        <div class="modal-header">
            <h3 id="modal-title">{title}</h3>
            <button class="modal-close-btn" on:click={close} aria-label="Close modal">
                <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="lucide lucide-x"><path d="M18 6 6 18"/><path d="m6 6 12 12"/></svg>
            </button>
        </div>
        <div class="modal-body">
            <slot></slot>
        </div>
    </div>
</div>
{/if}

<style>
    .modal-overlay {
        position: fixed;
        top: 0;
        left: 0;
        width: 100vw;
        height: 100vh;
        background-color: rgba(0, 0, 0, 0.5);
        display: flex;
        justify-content: center;
        align-items: center;
        z-index: 1000;
        backdrop-filter: blur(2px);
    }

    .modal-content {
        background-color: white;
        border-radius: 8px;
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
        padding: 20px;
        max-width: 500px;
        max-height: 80vh;
        overflow-y: auto;
        display: flex;
        flex-direction: column;
    }

    .modal-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        border-bottom: 1px solid #eee;
        padding-bottom: 10px;
        margin-bottom: 15px;
    }

    .modal-header h3 {
        margin: 0;
        font-size: 1.2em;
        color: #333;
    }

    .modal-close-btn {
        background: none;
        border: none;
        cursor: pointer;
        font-size: 1.5em;
        color: #666;
        padding: 0;
        line-height: 1;
        transition: color 0.2s;
    }
    .modal-close-btn:hover {
        color: #333;
    }

    .modal-body {
        flex-grow: 1;
    }
</style>
