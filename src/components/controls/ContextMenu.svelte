<script lang="ts">
    import { onMount, onDestroy } from 'svelte';

    export interface MenuItem {
        label: string;
        onClick: () => void;
        icon?: string; // Optional icon data
        variant?: 'danger' | 'default';
    }

    interface Props {
        x: number;
        y: number;
        items: MenuItem[];
        onClose: () => void;
    }

    let { x, y, items, onClose }: Props = $props();
    
    let menuEl: HTMLDivElement | undefined = $state();

    function handleClickOutside(e: MouseEvent) {
        if (menuEl && !menuEl.contains(e.target as Node)) {
            onClose();
        }
    }

    onMount(() => {
        // Use capture phase to ensure we catch clicks before they trigger other actions
        document.addEventListener('mousedown', handleClickOutside, true);
        document.addEventListener('contextmenu', handleClickOutside, true);
    });

    onDestroy(() => {
        document.removeEventListener('mousedown', handleClickOutside, true);
        document.removeEventListener('contextmenu', handleClickOutside, true);
    });

    // Ensure menu stays within viewport
    let adjustedX = $state(x);
    let adjustedY = $state(y);

    $effect(() => {
        if (menuEl) {
            const rect = menuEl.getBoundingClientRect();
            const padding = 10;
            
            if (x + rect.width > window.innerWidth) {
                adjustedX = window.innerWidth - rect.width - padding;
            } else {
                adjustedX = x;
            }
            
            if (y + rect.height > window.innerHeight) {
                adjustedY = window.innerHeight - rect.height - padding;
            } else {
                adjustedY = y;
            }
        }
    });
</script>

<div 
    bind:this={menuEl}
    class="fixed z-[1000] bg-white border border-gray-200 rounded-lg shadow-xl p-1 min-w-[100px] animate-in fade-in zoom-in duration-100"
    style="top: {adjustedY}px; left: {adjustedX}px;"
    role="menu"
    tabindex="-1"
>
    {#each items as item}
        <button 
            class="w-full text-left px-3 py-1.5 text-sm flex items-center gap-2 rounded transition-colors border-none bg-transparent cursor-pointer
                   {item.variant === 'danger' 
                       ? 'text-red-600 hover:bg-red-50' 
                       : 'text-gray-700 hover:bg-[#f2f2f4] hover:text-gray-900'}"
            onclick={() => { item.onClick(); onClose(); }}
            role="menuitem"
        >
            {#if item.icon}
                <span class="opacity-70">{@html item.icon}</span>
            {/if}
            {item.label}
        </button>
    {/each}
</div>

<style>
    /* Ensure no focus outline on the menu itself */
    div:focus {
        outline: none;
    }
</style>
