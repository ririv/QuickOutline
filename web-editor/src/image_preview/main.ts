// 引入公共样式 (假设你有) 和 独有样式
import '../shared/common.css';
import './style.css';

// --- 类型定义 ---

interface ImageUpdateData {
    pageIndex: number;
    widthPt: number;
    heightPt: number;
    totalPages: number;
    version: string | number; // 支持时间戳或版本号
}

declare global {
    interface Window {
        updateImagePages: (jsonString: string) => void;
    }
}

// --- DOM 元素 ---
const slider = document.getElementById('zoom-slider') as HTMLInputElement;
const label = document.getElementById('zoom-label') as HTMLSpanElement;
const container = document.getElementById('pages-container') as HTMLDivElement;
const viewport = document.getElementById('viewport') as HTMLDivElement;
const btnZoomOut = document.getElementById('btn-zoom-out') as HTMLButtonElement;
const btnZoomIn = document.getElementById('btn-zoom-in') as HTMLButtonElement;
const btnReset = document.getElementById('btn-reset') as HTMLButtonElement;

// --- 状态 ---
let currentScale: number = 1.0;

// --- 初始化 ---
updateSliderBackground(1.0);

// --- 事件监听 ---

btnZoomOut.addEventListener('click', () => adjustZoom(-0.1));
btnZoomIn.addEventListener('click', () => adjustZoom(0.1));
btnReset.addEventListener('click', () => setZoom(1.0));

slider.addEventListener('input', function() {
    setZoom(parseFloat(this.value));
});

// 滚轮缩放
viewport.addEventListener('wheel', function(e: WheelEvent) {
    if (e.ctrlKey || e.metaKey) {
        e.preventDefault();
        const delta = e.deltaY > 0 ? -0.1 : 0.1;
        adjustZoom(delta);
    }
}, { passive: false });

// --- 缩放逻辑 ---

function adjustZoom(delta: number) {
    let newScale = Math.round((currentScale + delta) * 10) / 10;
    newScale = Math.max(0.5, Math.min(3.0, newScale));
    setZoom(newScale);
}

function setZoom(scale: number) {
    currentScale = scale;

    // 使用 zoom 非标准属性，需要强转类型
    (container.style as any).zoom = currentScale;

    slider.value = currentScale.toString();
    label.innerText = Math.round(currentScale * 100) + "%";
    updateSliderBackground(currentScale);
}

function updateSliderBackground(val: number) {
    const min = parseFloat(slider.min);
    const max = parseFloat(slider.max);
    const percent = ((val - min) / (max - min)) * 100;
    slider.style.setProperty('--percent', percent + '%');
}

// --- 核心：无感更新逻辑 ---

window.updateImagePages = function(jsonString: string) {
    let updates: ImageUpdateData[];
    try {
        updates = JSON.parse(jsonString);
    } catch (e) {
        console.error("JSON Parse Error", e);
        return;
    }

    if (!updates || updates.length === 0) return;

    const totalPages = updates[0].totalPages;

    // 1. 调整 DOM 骨架 (只增删容器)
    while (container.children.length < totalPages) {
        const div = document.createElement('div');
        div.className = 'page-wrapper';
        div.id = 'page-' + container.children.length;
        container.appendChild(div);
    }
    while (container.children.length > totalPages) {
        if (container.lastChild) {
            container.removeChild(container.lastChild);
        }
    }

    // 2. 智能更新每页内容
    updates.forEach(u => {
        const pageDiv = document.getElementById('page-' + u.pageIndex);
        if (!pageDiv) return;

        // 设置尺寸
        pageDiv.style.width = u.widthPt + 'pt';
        pageDiv.style.height = u.heightPt + 'pt';

        // 构建 URL
        // 假设你的服务器路径结构是 /page_images/{index}.png
        const newUrl = `/page_images/${u.pageIndex}.png?v=${u.version}`;

        // 检查当前显示的图片
        // 注意：img.src 获取的是绝对路径，包含域名，所以判断时最好用 includes 或 endsWith
        const currentImg = pageDiv.querySelector('img.current') as HTMLImageElement;

        // 简单的缓存命中检查：如果当前 URL 包含这个版本号，就不刷新
        // @ts-ignore
        if (currentImg && currentImg.src.includes(`v=${u.version}`)) {
            return;
        }

        // --- 双缓冲加载策略 ---

        const newImg = document.createElement('img');
        newImg.className = 'preload'; // 初始状态 class

        // 加载完成回调
        newImg.onload = () => {
            // 1. 标记为当前，这会触发 CSS 的 opacity: 1
            newImg.className = 'current';

            // 2. 移除旧图片
            // 为什么不直接 remove? 为了保险起见，查找所有非当前的图片移除
            const oldImages = pageDiv.querySelectorAll('img:not(.current)');
            oldImages.forEach(img => {
                if (img !== newImg) {
                    pageDiv.removeChild(img);
                }
            });
        };

        newImg.onerror = () => {
            console.error(`Failed to load image for page ${u.pageIndex}`);
            // 可以在这里放一个占位图
        };

        // 将新图片加入 DOM (此时它是 opacity: 0)
        pageDiv.appendChild(newImg);

        // 开始加载
        newImg.src = newUrl;
    });
};