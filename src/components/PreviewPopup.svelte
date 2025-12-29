<script lang="ts">
    interface Props {
        src: string;
        y: number;
        anchorX: number;
        placement?: 'left' | 'right'; // New prop
        [key: string]: any; 
    }
    let { src, y, anchorX, placement = 'left', ...rest }: Props = $props();
    
    const PREVIEW_WIDTH = 500;
    const GAP = 9;
    const PREVIEW_HEIGHT_GUESS = 400;

    let top = $derived(Math.max(10, Math.min(y - PREVIEW_HEIGHT_GUESS / 2, window.innerHeight - PREVIEW_HEIGHT_GUESS - 10))); 
    let arrowTop = $derived(y - top);
    
    // Calculate left position based on placement
    let leftStyle = $derived(
        placement === 'left' 
            ? `left: ${anchorX - PREVIEW_WIDTH - GAP}px;`
            : `left: ${anchorX + GAP}px;`
    );
</script>

<div 
    class="fixed z-[9999] bg-white shadow-2xl rounded-lg border-2 border-gray-200 transition-opacity duration-200"
    style="
        width: {PREVIEW_WIDTH}px; 
        {leftStyle}
        top: {top}px;
        max-height: 80vh;
    "
    {...rest}
>
    <img {src} class="w-full h-auto block bg-gray-50 rounded-md" alt="Page Preview" />

    {#if placement === 'left'}
        <!-- Arrow pointing RIGHT (for left placement) -->
        <div 
            class="absolute w-4 h-4 bg-white border-t border-r border-gray-200"
            style="
                top: {arrowTop}px; 
                right: -9px;
                transform: translateY(-50%) rotate(45deg);
                z-index: 10;
            "
        ></div>
    {:else}
        <!-- Arrow pointing LEFT (for right placement) -->
        <div 
            class="absolute w-4 h-4 bg-white border-b border-l border-gray-200"
            style="
                top: {arrowTop}px; 
                left: -9px;
                transform: translateY(-50%) rotate(45deg);
                z-index: 10;
            "
        ></div>
    {/if}
</div>
