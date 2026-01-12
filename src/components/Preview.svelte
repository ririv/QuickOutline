<script lang="ts">
    import { onMount } from "svelte";
    import {
        handleSvgUpdate,
        onSvgViewChange,
        setDoubleBuffering,
    } from "@/lib/preview-engine/svg-engine";
    import { handleImageUpdate } from "@/lib/preview-engine/image-engine";
    import PagedRenderer from "./renderers/PagedRenderer.svelte";
    import { docStore } from '@/stores/docStore.svelte'; // Import docStore
    import Icon from "@/components/Icon.svelte";

    interface Props {
        mode?: "svg" | "image" | "paged";
        onrefresh?: () => void | Promise<void>;
        onScroll?: (top: number) => void;
        onRenderStats?: (stats: { duration: number }) => void;
        isActive?: boolean;
        pagedPayload?: {
            html: string;
            styles: string;
            header: any;
            footer: any;
        } | null;
    }

    let {
        mode = "paged",
        onrefresh,
        onScroll,
        onRenderStats,
        isActive = true,
        pagedPayload = null
    }: Props = $props();

    let container: HTMLDivElement | undefined = $state();
    let viewport: HTMLDivElement | undefined = $state();
    let slider: HTMLInputElement | undefined = $state();

    let currentScale = $state(1.0);
    let isScrolling = false;
    let sliderPercent = $state("20%");
    let isRefreshing = $state(false); // Refresh state for animation
    let isDoublePage = false; // Restore Double Page mode state
    let isHovering = $state(false); // Track mouse hover state for toolbar visibility

    let currentPdfFilePath = $derived(docStore.currentFilePath);

    const zoomResetIcon = `<svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg"><path d="M3.5 11C3.5 11 4.5 7 8 4.5C11.5 2 17 3.5 20 7.5C23 11.5 21.5 17.5 17.5 20.5C13.5 23.5 7.5 22.5 4.5 18.5" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/><path d="M3.5 5V11H9.5" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>`;

    // Expose methods for parent to call (SVG/Image Engine)
    // Note: json string still comes from Java part, containing page metadata
    export const renderSvg = (json: string) => {
        if (mode === "svg" && container && viewport)
            handleSvgUpdate(json, container, viewport);
    };

    export const renderImage = (json: string, pdfFilePath: string, scale: number) => {
        if (mode === "image" && container) handleImageUpdate(json, container, pdfFilePath, scale); // Pass pdfFilePath and scale
    };
    export const setDoubleBuffer = (enable: boolean) => {
        setDoubleBuffering(enable);
    };
    export const restoreScroll = (top: number) => {
        if (viewport) viewport.scrollTop = top;
    };

    function updateSliderBackground(val: number) {
        const min = 0.5;
        const max = 3.0;
        const percent = ((val - min) / (max - min)) * 100;
        sliderPercent = `${percent}%`;
    }

    function setZoom(scale: number) {
        scale = Math.max(0.5, Math.min(3.0, scale));
        currentScale = scale;
        if (container) {
            // Apply zoom to container using CSS zoom property
            // This handles layout and scrollbars automatically in WebKit/Blink
            container.style.zoom = `${currentScale}`;

            // Notify svg-engine if mode is svg (it might need to know for internal calculations, 
            // though zoom usually handles it seamlessly)
            if (mode === "svg" && container && viewport) onSvgViewChange(container, viewport);
        }
        updateSliderBackground(currentScale);
    }

    function adjustZoom(delta: number) {
        let newScale = Math.round((currentScale + delta) * 10) / 10;
        setZoom(newScale);
    }

    function handleWheel(e: WheelEvent) {
        if (e.ctrlKey || e.metaKey) {
            e.preventDefault();
            const delta = e.deltaY > 0 ? -0.1 : 0.1;
            adjustZoom(delta);
        }
    }

    function handleScroll() {
        if (onScroll && viewport) {
            onScroll(viewport.scrollTop);
        }

        // Only SVG mode needs scroll notification to manage virtual rendering
        if (mode === "svg" && !isScrolling && container && viewport) {
            window.requestAnimationFrame(() => {
                if (container && viewport) {
                    onSvgViewChange(container, viewport);
                }
                isScrolling = false;
            });
            isScrolling = true;
        }
    }

    async function handleRefreshClick() {
        if (isRefreshing || !onrefresh) return;
        isRefreshing = true;
        const animationDuration = 300; // 0.3s for one spin
        const startClickTime = Date.now();
        try {
            await Promise.resolve(onrefresh());
        } finally {
            const elapsedTime = Date.now() - startClickTime;
            const remainingAnimationTime = Math.max(
                0,
                animationDuration - elapsedTime,
            );
            setTimeout(() => {
                isRefreshing = false;
            }, remainingAnimationTime);
        }
    }

    onMount(() => {
        updateSliderBackground(currentScale);
        setDoubleBuffer(true);
        // Apply initial zoom
        setZoom(currentScale);
    });
</script>

<div
    class="preview-root"
    data-mode={mode}
    onmouseenter={() => isHovering = true}
    onmouseleave={() => isHovering = false}
    role="region"
    aria-label="Document Preview"
>
    <div
        id="viewport"
        bind:this={viewport}
        onwheel={handleWheel}
        onscroll={handleScroll}
    >
        <div id="pages-container" bind:this={container}>
            {#if mode === "paged"}
                {#if pagedPayload}
                    <PagedRenderer
                        payload={pagedPayload}
                        isActive={isActive}
                        onRenderComplete={(duration) => {
                            if (onRenderStats) onRenderStats({ duration });
                        }}
                    />
                {/if}
            {:else if mode === "svg" || mode === "image"}
                <!-- SVG/Image engines render directly into container -->
            {/if}
        </div>
    </div>

    <div id="toolbar-container" style:opacity={isHovering ? 1 : 0}>
        <div id="toolbar">
            <button
                class="icon-btn"
                onclick={() => adjustZoom(-0.1)}
                title="Zoom Out">−</button
            >
            <input
                type="range"
                bind:this={slider}
                min="0.5"
                max="3.0"
                step="0.1"
                bind:value={currentScale}
                oninput={() => setZoom(currentScale)}
                style="--percent: {sliderPercent};"
            />
            <button
                class="icon-btn"
                onclick={() => adjustZoom(0.1)}
                title="Zoom In">+</button
            >
            <span id="zoom-label">{Math.round(currentScale * 100)}%</span>
            <button class="icon-btn" onclick={() => setZoom(1.0)} title="Reset to 100%">
                <Icon data={zoomResetIcon} width="18" height="18" />
            </button>
        </div>
    </div>

    {#if onrefresh}
        <button
            class="refresh-fab {isRefreshing ? 'spinning' : ''}"
            onclick={handleRefreshClick}
            title="Refresh Preview"
            style:opacity={isHovering ? 1 : 0}
            style:pointer-events={isHovering ? 'auto' : 'none'}
        >
            <Icon name="refresh" width="20" height="20" />
        </button>
    {/if}
</div>

<style>
    /* Unified Paper Styles for All Engines */
    /* .page-sheet: General class for future use */
    /* .pagedjs_page: Paged.js generated pages */
    /* .page-wrapper: SVG/Image engine pages */
    :global(.page-sheet),
    :global(.pagedjs_page),
    :global(.page-wrapper) {
        background: white;
        box-shadow: 0 4px 15px rgba(0, 0, 0, 0.15); /* Slightly darker for better contrast */
        border-radius: 2px; /* Slight radius */
        margin-bottom: 20px; /* Spacing between pages */
        flex: none; /* Prevent shrinking */
        display: flex; /* Removes bottom gap for images */

        /* Performance & Layout */
        contain: content;
        transform: translate3d(0, 0, 0);
        will-change: transform;
        position: relative;
        overflow: hidden;
    }

    /* Paged.js specific sizing (A4 default) - REMOVED to allow dynamic sizing */
    :global(.pagedjs_page) {
        /* width: 595pt; */
        /* min-height: 842pt; */
        background-color: white; /* Ensure background matches */
        margin-bottom: 20px;
    }

    :global(.page-wrapper) {
        width: 595pt;
        height: 842pt;
    }

    /* SVG/Image Engine Specific Styles - Restored */
    :global(.page-wrapper svg) {
        display: block;
        width: 100%;
        height: 100%;
        position: absolute; /* Ensure overlap for double buffering */
        top: 0;
        left: 0;
        pointer-events: auto;
        shape-rendering: auto;
        text-rendering: geometricPrecision;
        mix-blend-mode: normal;
    }

    :global(.page-wrapper img) {
        display: block;
        width: 100%;
        height: 100%;
        object-fit: contain;
        position: absolute;
        top: 0;
        left: 0;
        image-rendering: auto;
    }

    /* Double Buffering Animation States */
    :global(.page-wrapper .current) {
        opacity: 1;
        transition: opacity 0.3s ease-out;
        z-index: 2;
    }

    :global(.page-wrapper .preload) {
        opacity: 0;
        z-index: 3; /* New content loads on top */
    }

    /* Container Styles */
    .preview-root {
        position: relative;
        width: 100%;
        height: 100%;
        overflow: hidden;
        background-color: #f5f7fa; /* Reverted to previous background */
        display: flex;
        flex-direction: column;
    }

    #viewport {
        flex: 1;
        overflow: auto;
        display: flex;
        /* Center pages horizontally */
        justify-content: center;
        padding: 40px; /* Reduced padding since we are centered */
        /* Force left alignment to override any global center styles */
        text-align: left !important;
    }

    #pages-container {
        /* Zoom property handles scaling without transform hacks */
        display: flex;
        flex-direction: column;
        gap: 20px;
        /* Ensure container width wraps content */
        width: fit-content;
        height: fit-content;
        -webkit-user-select: text;
        user-select: text;
    }

    /* Toolbar Styles */

    #toolbar-container {
        position: absolute;
        bottom: 30px;
        left: 0;
        right: 0;
        display: flex;
        justify-content: center;
        z-index: 1000;
        pointer-events: none;
        transition: opacity 0.3s; /* Reverted to original transition */
    }

    #toolbar {
        pointer-events: auto;
        /* Idle State: Even more subtle */
        background-color: rgba(0, 0, 0, 0.3); /* Changed from 0.5 to 0.3 */
        backdrop-filter: blur(4px);
        padding: 8px 20px;
        border-radius: 50px;
        box-shadow: none;
        display: flex;
        align-items: center;
        gap: 15px;
        color: rgba(255, 255, 255, 0.8); /* Slightly brighter text */
        transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    }

    #toolbar:hover {
        /* Active State: High contrast */
        background-color: rgba(0, 0, 0, 0.85);
        backdrop-filter: blur(10px);
        color: #fff;
        box-shadow: 0 8px 24px rgba(0, 0, 0, 0.25);
        transform: translateY(-2px);
    }

    .icon-btn {
        background: transparent;
        border: 1px solid transparent;
        color: inherit; /* Inherit from parent for transition */
        width: 28px;
        height: 28px;
        border-radius: 50%;
        cursor: pointer;
        font-size: 18px;
        display: flex;
        align-items: center;
        justify-content: center;
        transition: all 0.2s;
        font-weight: bold;
        line-height: 1;
        padding: 0;
    }

    .icon-btn:hover {
        background: rgba(255, 255, 255, 0.15);
        color: #fff;
    }

    .icon-btn:active {
        background: rgba(255, 255, 255, 0.25);
        transform: scale(0.95);
    }

    #zoom-label {
        font-size: 14px;
        font-variant-numeric: tabular-nums;
        min-width: 45px;
        text-align: center;
        color: inherit; /* Inherit from parent */
    }

    input[type="range"] {
        appearance: none;
        width: 120px;
        height: 4px;
        background: transparent;
        cursor: pointer;
        outline: none;
        margin: 0;
        opacity: 0.8;
        transition: opacity 0.3s;
    }

    #toolbar:hover input[type="range"] {
        opacity: 1;
    }

    input[type="range"]::-webkit-slider-runnable-track {
        width: 100%;
        height: 4px;
        border-radius: 2px;
        /* Default: Neutral/Grayish track */
        background: linear-gradient(
            to right,
            rgba(255, 255, 255, 0.6) 0%,
            rgba(255, 255, 255, 0.6) var(--percent),
            rgba(255, 255, 255, 0.2) var(--percent),
            rgba(255, 255, 255, 0.2) 100%
        );
        transition: background 0.3s;
    }

    #toolbar:hover input[type="range"]::-webkit-slider-runnable-track {
        /* Hover: Blue track */
        background: linear-gradient(
            to right,
            #1677ff 0%,
            #1677ff var(--percent),
            rgba(255, 255, 255, 0.3) var(--percent),
            rgba(255, 255, 255, 0.3) 100%
        );
    }

    input[type="range"]::-webkit-slider-thumb {
        -webkit-appearance: none;
        height: 14px;
        width: 14px;
        border-radius: 50%;
        background: #ffffff;
        border: 2px solid rgba(255, 255, 255, 0.6); /* Neutral border */
        margin-top: -5px;
        box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
        transition: transform 0.1s, border-color 0.3s;
    }

    #toolbar:hover input[type="range"]::-webkit-slider-thumb {
        border-color: #1677ff; /* Blue border on hover */
    }

    input[type="range"]:hover::-webkit-slider-thumb {
        transform: scale(1.2);
    }

    input[type="range"]:active::-webkit-slider-thumb {
        transform: scale(1.2);
        box-shadow: 0 0 0 5px rgba(22, 119, 255, 0.3);
        border-color: #1677ff;
    }

    .refresh-fab {
        position: absolute;
        bottom: 15px;
        right: 15px;
        width: 28px;
        height: 28px;
        border-radius: 50%;
        background: transparent;
        box-shadow: none;
        border: none;
        cursor: pointer;
        display: flex;
        align-items: center;
        justify-content: center;
        z-index: 900;
        color: rgba(0, 0, 0, 0.3);
        transition: all 0.3s;
        padding: 0;
    }

    .refresh-fab:hover {
        background: rgba(0, 0, 0, 0.1);
        color: #1677ff;
    }

    :global(.refresh-fab.spinning svg) {
        animation: spin 0.3s ease-out forwards;
    }

    @keyframes spin {
        from {
            transform: rotate(0deg);
        }
        to {
            transform: rotate(360deg);
        }
    }

    /* --- PRINT STYLES for all modes --- */
    /* Hides UI elements. Specific page layout is handled by PagedRenderer or Engine */
    @media print {
        :global(body > *:not(.preview-root)),
        #toolbar-container,
        .refresh-fab {
            display: none !important;
        }

        :global(body),
        :global(html),
        .preview-root,
        #viewport,
        #pages-container {
            width: 100%;
            height: auto !important;
            margin: 0;
            padding: 0;
            background: white;
            overflow: visible !important;
            display: block !important;
        }

        #viewport,
        #pages-container {
            padding: 0 !important;
            text-align: left !important;
            transform: none !important; /* Disable zoom on print */
        }
    }
</style>
