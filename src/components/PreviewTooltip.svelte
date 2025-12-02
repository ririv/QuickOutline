<script lang="ts">
    interface Props {
        src: string;
        y: number;
        anchorX: number;
    }
    let { src, y, anchorX }: Props = $props();
    
    const PREVIEW_WIDTH = 500;
    const GAP = 15;
    
    // Calculate top to try to center it relative to the mouse/item, but keep in viewport
    // We assume a max height or let it flow. 
    // Simple clamp: keep at least 10px from top, and don't go too far down.
    // Since we don't know image height yet, we guess or center on Y.
    let top = $derived(Math.max(10, Math.min(y - 150, window.innerHeight - 600))); 
</script>

<div 
    class="fixed z-[9999] bg-white shadow-2xl rounded-lg border-4 border-white overflow-hidden pointer-events-none"
    style="
        width: {PREVIEW_WIDTH}px; 
        left: {anchorX - PREVIEW_WIDTH - GAP}px; 
        top: {top}px;
        max-height: 80vh;
    "
>
    <img {src} class="w-full h-auto block bg-gray-50" alt="Preview" />
    <div class="absolute bottom-2 right-2 px-2 py-1 bg-black/60 text-white text-xs rounded backdrop-blur-sm">
        Preview
    </div>
</div>
