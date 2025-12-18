<script lang="ts">
    interface Props {
        src: string;
        y: number;
        anchorX: number;
        [key: string]: any; // Allow extra props (events)
    }
    let { src, y, anchorX, ...rest }: Props = $props();
    
    const PREVIEW_WIDTH = 500;
    const GAP = 9;
    const PREVIEW_HEIGHT_GUESS = 400; // Guess a reasonable height for centering

    let top = $derived(Math.max(10, Math.min(y - PREVIEW_HEIGHT_GUESS / 2, window.innerHeight - PREVIEW_HEIGHT_GUESS - 10))); 
    let arrowTop = $derived(y - top);
</script>

<div 
    class="fixed z-[9999] bg-white shadow-2xl rounded-lg border-2 border-gray-200 transition-opacity duration-200"
    style="
        width: {PREVIEW_WIDTH}px; 
        left: {anchorX - PREVIEW_WIDTH - GAP}px; 
        top: {top}px;
        max-height: 80vh;
    "
    {...rest}
>
    <img {src} class="w-full h-auto block bg-gray-50 rounded-md" alt="Page Preview" />

    <!-- Arrow pointing RIGHT (Rotated Square) -->
    <div 
        class="absolute w-4 h-4 bg-white border-t border-r border-gray-200"
        style="
            top: {arrowTop}px; 
            right: -9px;
            transform: translateY(-50%) rotate(45deg);
            z-index: 10;
        "
    ></div>
</div>
