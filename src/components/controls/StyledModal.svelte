<script lang="ts">
    import { fade, scale } from 'svelte/transition';
    import { cubicOut } from 'svelte/easing';

    interface Props {
        isOpen: boolean;
        onClose: () => void;
        children: any;
        blur?: boolean;
        width?: string;
        zIndex?: string;
    }

    let { 
        isOpen, 
        onClose, 
        children, 
        blur = false, 
        width = 'max-w-sm',
        zIndex = 'z-50'
    }: Props = $props();

    function handleBackdropClick(e: MouseEvent) {
        if (e.target === e.currentTarget) {
            onClose();
        }
    }

    function handleKeydown(e: KeyboardEvent) {
        if (e.key === 'Escape' && isOpen) onClose();
    }
</script>

<svelte:window onkeydown={handleKeydown} />

{#if isOpen}
    <!-- Backdrop -->
    <!-- svelte-ignore a11y_click_events_have_key_events -->
    <!-- svelte-ignore a11y_no_noninteractive_element_interactions -->
    <div 
        class="fixed inset-0 {zIndex} flex items-center justify-center bg-black/40 {blur ? 'backdrop-blur-[2px]' : ''}" 
        transition:fade={{ duration: 150 }} 
        onclick={handleBackdropClick} 
        role="dialog" 
        aria-modal="true"
        tabindex="-1"
    >
        <!-- Dialog Container -->
        <div 
            class="bg-white rounded-lg shadow-2xl w-full {width} mx-4 flex flex-col overflow-hidden relative" 
            transition:scale={{ start: 0.96, duration: 150, easing: cubicOut }}
            onclick={(e) => e.stopPropagation()} 
            role="document"
        >
            {@render children()}
        </div>
    </div>
{/if}
