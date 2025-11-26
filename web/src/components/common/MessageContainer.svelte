<script lang="ts">
    import { flip } from 'svelte/animate';
    import { messageStore } from '@/stores/messageStore';
    import Message from './Message.svelte';
</script>

<div 
    class="message-container"
    aria-live="polite"
    role="status"
>
    {#each $messageStore as message (message.id)}
        <div animate:flip={{ duration: 500 }}>
            <Message {...message} />
        </div>
    {/each}
</div>

<style>
    .message-container {
        position: fixed;
        top: 20px;
        left: 50%;
        transform: translateX(-50%);
        z-index: 9999;
        display: flex;
        flex-direction: column;
        gap: 10px;
        align-items: center;
    }
</style>
