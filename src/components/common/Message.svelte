<script lang="ts">
    import { fade, slide } from 'svelte/transition';
    import { onMount } from 'svelte';
    import { messageStore, type MessageType } from '@/stores/messageStore';

    interface Props {
        id: number;
        text: string;
        type: MessageType;
        duration?: number;
    }
    let { id, text, type, duration = 3000 }: Props = $props();

    const icons = {
        SUCCESS: `<svg viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path d="M9.00002 16.0001L5.00002 12.0001L6.41002 10.5901L9.00002 13.1701L17.59 4.59009L19 6.00009L9.00002 16.0001Z" fill="currentColor"/></svg>`,
        INFO: `<svg viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path fill-rule="evenodd" clip-rule="evenodd" d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm-1-5h2v2h-2v-2zm0-8h2v6h-2V7z" fill="currentColor"/></svg>`,
        WARNING: `<svg viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path d="M1 21H23L12 2L1 21ZM13 18H11V16H13V18ZM13 14H11V10H13V14Z" fill="currentColor"/></svg>`,
        ERROR: `<svg viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path d="M12 2C6.47 2 2 6.47 2 12C2 17.53 6.47 22 12 22C17.53 22 22 17.53 22 12C22 6.47 17.53 2 12 2ZM17 15.59L15.59 17L12 13.41L8.41 17L7 15.59L10.59 12L7 8.41L8.41 7L12 10.59L15.59 7L17 8.41L13.41 12L17 15.59Z" fill="currentColor"/></svg>`
    };

    let timer: number;
    let remaining = duration;
    let startTime: number;

    function startTimer() {
        if (remaining <= 0) return;
        startTime = Date.now();
        clearTimeout(timer);
        timer = setTimeout(() => messageStore.remove(id), remaining);
    }

    function pauseTimer() {
        clearTimeout(timer);
        remaining -= (Date.now() - startTime);
    }

    onMount(() => {
        startTimer();
    });

</script>

<div 
    role="status"
    class="message {type.toLowerCase()}" 
    transition:slide|local={{duration: 300}}
    onmouseenter={pauseTimer}
    onmouseleave={startTimer}
>
    <div class="icon">
        {@html icons[type]}
    </div>
    <div class="text">
        {text}
    </div>
    <button class="close-btn" onclick={() => messageStore.remove(id)}>
        &times;
    </button>
</div>

<style>
    .message {
        width: 320px;
        min-height: 42px;
        border-radius: 4px;
        box-shadow: 0 4px 12px rgba(0,0,0,0.15);
        display: flex;
        align-items: center;
        padding: 8px 15px;
        box-sizing: border-box;
    }
    .icon {
        width: 20px;
        height: 20px;
        margin-right: 10px;
        flex-shrink: 0;
    }
    .text {
        font-size: 14px;
        line-height: 1.4;
        flex: 1; /* Allow text to take up space */
        padding-right: 10px; /* Space before close button */
    }
    .close-btn {
        background: none;
        border: none;
        padding: 0;
        margin: 0;
        cursor: pointer;
        font-size: 20px;
        line-height: 1;
        opacity: 0.6;
        color: inherit; /* Inherit color from parent */
    }
    .close-btn:hover {
        opacity: 1;
    }

    /* Type Variants - Set background and text color for the message container */
    .message.success { background-color: #f0f9eb; color: #67c23a; }
    .message.info    { background-color: #edf2fc; color: #409eff; }
    .message.warning { background-color: #fdf6ec; color: #e6a23c; }
    .message.error   { background-color: #fef0f0; color: #f56c6c; }
</style>
