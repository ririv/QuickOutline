<script lang="ts">
    import landscapeIcon from '../assets/icons/landscape.svg';
    import StyledSlider from './controls/StyledSlider.svelte';
    import { docStore } from '@/stores/docStore.svelte';
    import PreviewPopup from './PreviewPopup.svelte';
    import { pageLabelStore } from '@/stores/pageLabelStore.svelte';
    import Tooltip from './Tooltip.svelte';
    import { pdfRenderService } from '@/lib/services/PdfRenderService';
    import { onDestroy } from 'svelte';

    const DEFAULT_ASPECT_RATIO = 1.414; // Fallback to A4

    interface Props {
        pageCount?: number;
        zoom?: number;
    }

    let { pageCount = 0, zoom = $bindable(1.0) }: Props = $props();

    // --- State ---
    // Cache for aspect ratios to maintain layout stability when images are unloaded
    let aspectRatios = $state<number[]>([]);
    
    // Cache for URLs (Non-reactive to avoid loops)
    const thumbnailCache = new Map<number, string>();
    const previewCache = new Map<number, string>();
    
    let hoveredImage = $state<{src: string, y: number, x: number} | null>(null);

    // --- Layout Constants ---
    const CARD_BASE_WIDTH = 120;

    // --- Action: Smart Visibility Loader ---
    function smartImage(node: HTMLElement, index: number) {
        let imgElement: HTMLImageElement | null = null;
        let observer: IntersectionObserver;

        const load = () => {
            if (imgElement) return; // Already loaded

            const path = docStore.currentFilePath;
            if (!path) return;

            // Check cache first
            if (thumbnailCache.has(index)) {
                createImg(thumbnailCache.get(index)!);
                return;
            }

            pdfRenderService.renderPage(path, index, 'thumbnail')
                .then(url => {
                    thumbnailCache.set(index, url);
                    createImg(url);
                })
                .catch(console.error);
        };

        const createImg = (url: string) => {
            if (!node) return;
            // Create img tag
            imgElement = document.createElement('img');
            imgElement.src = url;
            imgElement.alt = `Page ${index + 1}`;
            imgElement.className = "absolute top-0 left-0 w-full h-full object-contain transition-opacity duration-200 opacity-0";
            
            imgElement.onload = () => {
                if (imgElement) {
                    imgElement.style.opacity = '1';
                    // Update aspect ratio cache from natural dimensions
                    if (imgElement.naturalWidth) {
                        const ratio = imgElement.naturalHeight / imgElement.naturalWidth;
                        // Only update if significantly different to avoid layout trashing loops
                        if (!aspectRatios[index] || Math.abs(aspectRatios[index] - ratio) > 0.01) {
                            aspectRatios[index] = ratio;
                        }
                    }
                }
            };
            
            node.appendChild(imgElement);
        };

        const unload = () => {
            if (imgElement) {
                imgElement.remove();
                imgElement = null;
            }
        };

        // Setup Observer with large margin to preload/keep images
        observer = new IntersectionObserver((entries) => {
            const entry = entries[0];
            if (entry.isIntersecting) {
                load();
            } else {
                // Unload when out of view (Memory Virtualization)
                unload();
            }
        }, { 
            rootMargin: "600px 0px" // Keep ~3 screens of images
        });
        
        observer.observe(node);

        return {
            destroy() {
                observer.disconnect();
                unload();
            }
        };
    }

    // --- Lifecycle ---
    $effect(() => {
        const _v = docStore.version; 
        const _p = pageCount; // Dependency to reset
        
        // Reset everything on file change
        thumbnailCache.forEach(url => URL.revokeObjectURL(url));
        thumbnailCache.clear();
        previewCache.forEach(url => URL.revokeObjectURL(url));
        previewCache.clear();
        
        // Reset ratios
        aspectRatios = new Array(pageCount).fill(DEFAULT_ASPECT_RATIO);
    });

    onDestroy(() => {
        thumbnailCache.forEach(url => URL.revokeObjectURL(url));
        previewCache.forEach(url => URL.revokeObjectURL(url));
    });

    // --- Helpers ---
    function isLabelModified(index: number, currentLabel: string) {
        const orig = docStore.originalPageLabels?.[index];
        if (orig === undefined) return currentLabel !== String(index + 1);
        return currentLabel !== orig;
    }

    const displayedPageLabels = $derived(
        (pageLabelStore.simulatedLabels && pageLabelStore.simulatedLabels.length > 0)
            ? pageLabelStore.simulatedLabels
            : Array.from({ length: docStore.pageCount }, (_, i) => String(i + 1))
    );

    async function handleMouseEnter(e: MouseEvent, index: number) {
        const target = e.currentTarget as HTMLElement;
        const rect = target.getBoundingClientRect();
        
        let src = previewCache.get(index) || thumbnailCache.get(index);
        if (!src && docStore.currentFilePath) {
             try {
                src = await pdfRenderService.renderPage(docStore.currentFilePath, index, 'preview');
                previewCache.set(index, src);
             } catch(e) { console.error(e); }
        }

        if (src) {
            hoveredImage = {
                src,
                y: rect.top + rect.height / 2,
                x: rect.left
            };
        }
    }

    function handleMouseLeave() { hoveredImage = null; }
</script>

<div class="flex flex-col h-full bg-[#f3f4f6] border-l border-[#e5e7eb] font-sans">
    <!-- Toolbar -->
    <div class="flex items-center p-3 gap-3 border-b border-[#e5e7eb] bg-white shadow-sm z-10">
        <img src={landscapeIcon} class="block opacity-40 w-3 h-3" alt="Zoom Out" />
        <StyledSlider
            min={0.5}
            max={5.0}
            step={0.01}
            bind:value={zoom}
        />
        <img src={landscapeIcon} class="block opacity-40 w-5 h-5" alt="Zoom In" />
    </div>

    <!-- Scroll Container -->
    <div class="flex-1 overflow-y-auto p-5 scrollbar-thin">
        <!-- Flex Grid Container -->
        <div class="flex flex-wrap justify-center gap-5">
            {#each { length: pageCount } as _, i (i)}
                {@const label = displayedPageLabels[i] || String(i + 1)}
                {@const ratio = aspectRatios[i] || DEFAULT_ASPECT_RATIO}
                
                <div 
                    class="flex-none flex flex-col items-center gap-2 group transition-[width] duration-100"
                    style="width: {CARD_BASE_WIDTH * zoom}px;"
                >
                    <!-- Card: Dynamic Height via Padding-Top -->
                    <div 
                        class="w-full bg-white relative transition-all duration-300 shadow-[0_2px_8px_rgba(0,0,0,0.2)] hover:shadow-[0_4px_12px_rgba(0,0,0,0.3)]"
                        style="padding-top: {ratio * 100}%;"
                        use:smartImage={i}
                        onmouseenter={(e) => handleMouseEnter(e, i)}
                        onmouseleave={handleMouseLeave}
                        role="img"
                        aria-label="Page {i + 1}"
                    >
                        <!-- Image injected by action -->
                        <!-- Placeholder/Skeleton Background -->
                        <div class="absolute inset-0 bg-white -z-10"></div>


                    </div>

                    <div class="w-full flex justify-center">
                        <Tooltip content="{i + 1} / {pageCount}" position="top">
                             <div 
                                class="text-xs font-medium whitespace-nowrap overflow-hidden text-ellipsis max-w-full px-2 py-0.5 rounded text-gray-700 {isLabelModified(i, label) ? 'text-blue-600 bg-blue-50' : ''}"
                            >
                                {label}
                            </div>
                        </Tooltip>
                    </div>
                </div>
            {/each}
            
            {#if pageCount === 0}
                <div class="w-full text-center text-gray-400 mt-10">No thumbnails available</div>
            {/if}
        </div>
    </div>
    
    {#if hoveredImage}
        <PreviewPopup
            src={hoveredImage.src} 
            y={hoveredImage.y} 
            anchorX={hoveredImage.x} 
        />
    {/if}
</div>

<style>
    .scrollbar-thin::-webkit-scrollbar { width: 6px; height: 6px; }
    .scrollbar-thin::-webkit-scrollbar-track { background: transparent; }
    .scrollbar-thin::-webkit-scrollbar-thumb { background: #d1d5db; border-radius: 3px; }
    .scrollbar-thin::-webkit-scrollbar-thumb:hover { background: #9ca3af; }
</style>