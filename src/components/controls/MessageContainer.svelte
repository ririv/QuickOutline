<script lang="ts">
    import { flip } from 'svelte/animate';
    import MessageComponent, { type Message } from './Message.svelte';

    // 显式通过 Prop 接收 Store 实例
    interface Props {
        store: {
            list: Message[];
            remove: (id: number) => void;
        };
    }

    let { store }: Props = $props();
</script>

<div 
    class="message-container"
    aria-live="polite"
    role="status"
>
    {#each store.list as message (message.id)}
        <div animate:flip={{ duration: 500 }}>
            <MessageComponent {...message} onClose={() => store.remove(message.id)} />
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
