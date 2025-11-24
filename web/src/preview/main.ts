// 引入样式
import './style.css';

// 引入子引擎
import { handleSvgUpdate, onSvgViewChange, setDoubleBuffering } from './svg_engine';
import { handleImageUpdate } from './image_engine';

// --- 全局定义 ---
declare global {
    interface Window {
        updateSvgPages: (jsonString: string) => void;
        updateImagePages: (jsonString: string) => void;
        setSvgDoubleBuffering: (enable: boolean) => void; // 仅对svg有效，img只有双缓冲模式
    }
}

// --- DOM 元素获取 ---
const slider = document.getElementById('zoom-slider') as HTMLInputElement;
const label = document.getElementById('zoom-label') as HTMLSpanElement;
const container = document.getElementById('pages-container') as HTMLDivElement;
const viewport = document.getElementById('viewport') as HTMLDivElement;

const btnZoomOut = document.getElementById('btn-zoom-out') as HTMLButtonElement;
const btnZoomIn = document.getElementById('btn-zoom-in') as HTMLButtonElement;
const btnReset = document.getElementById('btn-reset') as HTMLButtonElement;

// --- 状态管理 ---
let currentScale: number = 1.0;
let isScrolling = false;

// --- 初始化 ---
updateSliderBackground(1.0);

// ============================
// 1. 注册全局 API (Bridge)
// ============================

// SVG 入口
window.updateSvgPages = (jsonString: string) => {
    handleSvgUpdate(jsonString, container, viewport);
};

// 图片入口
window.updateImagePages = (jsonString: string) => {
    handleImageUpdate(jsonString, container);
};

// 双缓冲开关入口
window.setSvgDoubleBuffering = (enable: boolean) => {
    setDoubleBuffering(enable);
};


// ============================
// 2. UI 事件监听 (Controller)
// ============================

// 缩放按钮
btnZoomOut.addEventListener('click', () => adjustZoom(-0.1));
btnZoomIn.addEventListener('click', () => adjustZoom(0.1));
btnReset.addEventListener('click', () => setZoom(1.0));

// 滑动条
slider.addEventListener('input', function() {
    setZoom(parseFloat(this.value));
});

// 鼠标滚轮 (Ctrl + Wheel)
viewport.addEventListener('wheel', function(e: WheelEvent) {
    if (e.ctrlKey || e.metaKey) {
        e.preventDefault();
        const delta = e.deltaY > 0 ? -0.1 : 0.1;
        adjustZoom(delta);
    }
}, { passive: false });

// 滚动监听 (节流后通知 SVG 引擎)
viewport.addEventListener('scroll', () => {
    if (!isScrolling) {
        window.requestAnimationFrame(() => {
            // 通知 SVG 引擎视口变了，它内部会判断是否有 SVG 数据需要渲染
            onSvgViewChange(container, viewport);
            isScrolling = false;
        });
        isScrolling = true;
    }
});


// ============================
// 3. 共享 UI 逻辑
// ============================

function adjustZoom(delta: number) {
    let newScale = Math.round((currentScale + delta) * 10) / 10;
    newScale = Math.max(0.5, Math.min(3.0, newScale));
    setZoom(newScale);
}

function setZoom(scale: number) {
    currentScale = scale;

    // 应用缩放
    (container.style as any).zoom = currentScale;

    // 更新 UI
    slider.value = currentScale.toString();
    label.innerText = Math.round(currentScale * 100) + "%";
    updateSliderBackground(currentScale);

    // 缩放改变了视口内容，通知 SVG 引擎可能需要加载新块
    onSvgViewChange(container, viewport);
}

function updateSliderBackground(val: number) {
    const min = parseFloat(slider.min);
    const max = parseFloat(slider.max);
    const percent = ((val - min) / (max - min)) * 100;
    slider.style.setProperty('--percent', percent + '%');
}