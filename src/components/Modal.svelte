<script lang="ts">
    import { fade, scale } from 'svelte/transition';

    let { isOpen = $bindable(false), title, blur = false, children }: { isOpen: boolean, title: string, blur?: boolean, children: any } = $props();

    function close() {
        isOpen = false;
    }
    
    function handleKeydown(e: KeyboardEvent) {
        if (e.key === 'Escape') close();
    }
</script>

<svelte:window onkeydown={handleKeydown} />

{#if isOpen}
    <!-- Backdrop -->
    <!-- svelte-ignore a11y_click_events_have_key_events -->
    <!-- svelte-ignore a11y_no_noninteractive_element_interactions -->
    <div class="fixed inset-0 z-50 flex items-center justify-center bg-black/40 {blur ? 'backdrop-blur-[2px]' : ''}" 
         transition:fade={{ duration: 150 }} 
         onclick={close} 
         role="dialog" 
         aria-modal="true"
         tabindex="-1">
        <!-- Dialog -->
        <!-- svelte-ignore a11y_click_events_have_key_events -->
        <!-- svelte-ignore a11y_no_noninteractive_element_interactions -->
        <div class="bg-white rounded-lg shadow-2xl w-full max-w-sm mx-4 flex flex-col overflow-hidden" 
             transition:scale={{ start: 0.96, duration: 150 }} 
             onclick={(e) => e.stopPropagation()} 
             role="document">
            <div class="flex items-center justify-between px-4 py-3 border-b border-gray-100 bg-gray-50/50">
                <h3 class="text-sm font-semibold text-gray-700">{title}</h3>
                <button onclick={close} class="text-gray-400 hover:text-gray-600 transition-colors p-1 rounded-md hover:bg-gray-200/50" aria-label="Close">
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path></svg>
                </button>
            </div>
            <div class="p-5">
                {@render children()}
            </div>
        </div>
    </div>
{/if}
