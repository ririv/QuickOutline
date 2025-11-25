<script lang="ts">
  import { onMount } from 'svelte';
  import { handleSvgUpdate, onSvgViewChange, setDoubleBuffering } from '../lib/preview-engine/svg-engine';
  import { handleImageUpdate } from '../lib/preview-engine/image-engine';

  export let mode: 'combined' | 'preview-only' = 'preview-only';
  export let onrefresh: (() => void | Promise<void>) | undefined = undefined; // onrefresh might be async

  let container: HTMLDivElement;
  let viewport: HTMLDivElement;
  let slider: HTMLInputElement;
  
  let currentScale = 1.0;
  let isScrolling = false;
  let sliderPercent = '20%';
  let isRefreshing = false; // Refresh state for animation

  // Expose methods for parent to call
  export const renderSvg = (json: string) => {
    if (container && viewport) handleSvgUpdate(json, container, viewport);
  };
  
  export const renderImage = (json: string) => {
    if (container) handleImageUpdate(json, container);
  };

  export const setDoubleBuffer = (enable: boolean) => {
      setDoubleBuffering(enable);
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
          (container.style as any).zoom = currentScale;
          // Notify engine
          onSvgViewChange(container, viewport);
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
      if (!isScrolling) {
          window.requestAnimationFrame(() => {
              onSvgViewChange(container, viewport);
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
          const remainingAnimationTime = Math.max(0, animationDuration - elapsedTime);
          
          setTimeout(() => {
              isRefreshing = false;
          }, remainingAnimationTime);
      }
  }

  onMount(() => {
      updateSliderBackground(currentScale);
      setDoubleBuffer(true);
  });

</script>

<div class="preview-root" data-mode={mode}>
    <div id="viewport" bind:this={viewport} onwheel={handleWheel} onscroll={handleScroll}>
        <div id="pages-container" bind:this={container}>
            <!-- Pages will be injected here by engine -->
        </div>
    </div>

    <div id="toolbar-container">
        <div id="toolbar">
            <button class="icon-btn" onclick={() => adjustZoom(-0.1)} title="Zoom Out">−</button>
            <input 
                type="range" 
                bind:this={slider}
                min="0.5" max="3.0" step="0.1" 
                bind:value={currentScale} 
                oninput={() => setZoom(currentScale)}
                style="--percent: {sliderPercent};"
            >
            <button class="icon-btn" onclick={() => adjustZoom(0.1)} title="Zoom In">+</button>
            <span id="zoom-label">{Math.round(currentScale * 100)}%</span>
            <button class="text-btn" onclick={() => setZoom(1.0)} title="Reset">Reset</button>
        </div>
    </div>

    {#if onrefresh}
        <button class="refresh-fab {isRefreshing ? 'spinning' : ''}" onclick={handleRefreshClick} title="Refresh Preview">
            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 1024 1024" fill="currentColor"><path d="M896 198.4 896 198.4l0 179.2 0 0c0 19.2-6.4 32-19.2 44.8-12.8 12.8-32 19.2-44.8 19.2l0 0-179.2 0 0 0c-19.2 0-32-6.4-44.8-19.2-25.6-25.6-25.6-64 0-89.6C620.8 320 633.6 313.6 652.8 313.6l0 0 25.6 0C627.2 275.2 576 256 518.4 256 441.6 256 377.6 281.6 332.8 332.8l0 0c-25.6 25.6-64 25.6-89.6 0-25.6-25.6-25.6-64 0-89.6l0 0C313.6 172.8 409.6 128 518.4 128c96 0 185.6 38.4 249.6 96L768 198.4l0 0c0-19.2 6.4-32 19.2-44.8 25.6-25.6 64-25.6 89.6 0C889.6 160 896 179.2 896 198.4zM416 691.2c-12.8 12.8-32 19.2-44.8 19.2l0 0L352 710.4C396.8 748.8 448 768 505.6 768c70.4 0 134.4-25.6 179.2-76.8l0 0c25.6-25.6 64-25.6 89.6 0 25.6 25.6 25.6 64 0 89.6l0 0C710.4 851.2 614.4 896 505.6 896c-96 0-185.6-38.4-249.6-96l0 32 0 0c0 19.2-6.4 32-19.2 44.8-25.6 25.6-64 25.6-89.6 0C134.4 864 128 844.8 128 825.6l0 0 0-179.2 0 0c0-19.2 6.4-32 19.2-44.8C160 588.8 172.8 582.4 192 582.4l0 0 179.2 0 0 0c19.2 0 32 6.4 44.8 19.2C441.6 627.2 441.6 665.6 416 691.2z" p-id="4660"></path></svg>
        </button>
    {/if}
</div>

<style>
    /* ... existing styles ... */
    
    .refresh-fab {
        position: absolute;
        bottom: 15px; /* Adjusted position */
        right: 15px;  /* Adjusted position */
        width: 28px;  /* Button size */
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
        color: #666; /* Default icon color */
        transition: all 0.2s;
        padding: 0;
    }
    .refresh-fab svg {
        width: 20px; /* Icon size */
        height: 20px;
        transition: transform 0.3s ease-out; /* Smooth transform for rotation */
    }
    .refresh-fab:hover {
        background: rgba(0,0,0,0.1);
        color: #1677ff;
        box-shadow: none;
    }
    
    .refresh-fab.spinning svg { /* Use class to trigger animation */
        animation: spin 0.3s ease-out forwards; /* Spin once, then hold last frame */
    }
    
    @keyframes spin {
        from { transform: rotate(0deg); }
        to { transform: rotate(360deg); }
    }

    /* --- 双缓冲通用类 (仅在启用时生效) --- */
    /* 使用 :global() 包裹，因为 .double-buffer 类和内部的 svg/img 是动态生成的 */
    :global(.double-buffer .page-wrapper svg),
    :global(.double-buffer .page-wrapper img) {
        position: absolute;
        top: 0;
        left: 0;
        opacity: 0;
        transition: opacity 0.2s ease-in;
    }

    :global(.double-buffer .page-wrapper .current) {
        opacity: 1;
        z-index: 1;
    }

    :global(.double-buffer .page-wrapper .preload) {
        z-index: 2;
    }

    /* 允许用户选中页面内容 */
    :global(#pages-container) {
        user-select: text;
    }

    .preview-root {
        position: relative;
        width: 100%;
        height: 100%;
        overflow: hidden;
        background-color: #e3e4ea;
    }

    /* --- 悬浮工具栏 (胶囊样式 + 毛玻璃) --- */
    #toolbar-container {
        position: absolute; /* Changed from fixed to absolute */
        bottom: 30px; /* 悬浮在底部，不遮挡顶部内容 */
        left: 0;
        right: 0;
        display: flex;
        justify-content: center;
        z-index: 1000;
        pointer-events: none; /* 让容器本身不挡鼠标 */
    }

    #toolbar {
        pointer-events: auto; /* 让工具栏内部可点击 */
        background-color: rgba(0, 0, 0, 0.75); /* 半透明黑 */
        backdrop-filter: blur(10px); /* 毛玻璃效果 */
        padding: 8px 20px;
        border-radius: 50px; /* 胶囊圆角 */
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
        display: flex;
        align-items: center;
        gap: 15px;
        color: #fff;
        transition: opacity 0.3s;
    }

    /* 圆形图标按钮 */
    .icon-btn {
        background: transparent;
        border: 1px solid transparent;
        color: rgba(255, 255, 255, 0.85);
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
        background: rgba(255, 255, 255, 0.1);
        color: #fff;
    }
    .icon-btn:active {
        background: rgba(255, 255, 255, 0.2);
        transform: scale(0.95);
    }

    /* 缩放数值标签 */
    #zoom-label {
        font-size: 14px;
        font-variant-numeric: tabular-nums; /* 数字等宽，防止跳动 */
        min-width: 45px;
        text-align: center;
        color: #fff;
    }

    /* 文字按钮 (Reset) */
    .text-btn {
        background: #1677ff;
        border: none;
        color: white;
        border-radius: 4px;
        padding: 4px 8px;
        font-size: 12px;
        cursor: pointer;
        margin-left: 5px;
        font-family: inherit;
    }
    .text-btn:hover { background: #4096ff; }
    .text-btn:active { background: #0958d9; }

    /* --- Ant Design 风格滑动条 (CSS Magic) --- */
    input[type=range] {
        -webkit-appearance: none; /* 清除默认样式 */
        width: 120px;
        height: 4px; /* 轨道高度 */
        background: transparent;
        cursor: pointer;
        outline: none;
        margin: 0;
    }

    /* 轨道 (Track) - 动态背景色由 JS 变量 --percent 控制 */
    input[type=range]::-webkit-slider-runnable-track {
        width: 100%;
        height: 4px;
        border-radius: 2px;
        background: linear-gradient(to right, #1677ff 0%, #1677ff var(--percent), #5e5e5e var(--percent), #5e5e5e 100%);
    }

    /* 滑块 (Thumb) */
    input[type=range]::-webkit-slider-thumb {
        -webkit-appearance: none;
        height: 14px;
        width: 14px;
        border-radius: 50%;
        background: #ffffff;
        border: 2px solid #1677ff; /* 蓝色边框 */
        margin-top: -5px; /* 垂直居中修正: (轨道4 - 滑块14) / 2 */
        box-shadow: 0 2px 4px rgba(0,0,0,0.2);
        transition: transform 0.1s;
    }

    /* 滑块交互效果 */
    input[type=range]:hover::-webkit-slider-thumb {
        transform: scale(1.2);
        box-shadow: 0 0 0 3px rgba(22, 119, 255, 0.2); /* 蓝色光晕 */
    }
    input[type=range]:active::-webkit-slider-thumb {
        transform: scale(1.2);
        box-shadow: 0 0 0 5px rgba(22, 119, 255, 0.3);
    }
</style>