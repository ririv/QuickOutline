// src/main.ts
import './style.css'; // 引入 CSS，esbuild 会处理它

// --- 类型定义 ---

// 对应 Java 传过来的 JSON 对象结构
interface PageUpdateData {
    pageIndex: number;
    svgContent: string;
    widthPt: number;
    heightPt: number;
    totalPages?: number; // 通常只在第一项或者作为元数据传递
}

interface PageCacheItem {
    content: string;
    width: number;
    height: number;
}

// 声明全局 window 上的方法，防止 TS 报错
declare global {
    interface Window {
        updateSvgPages: (jsonString: string) => void;
    }
}

// --- 状态管理 ---
let currentScale: number = 1.0;
const pageCache: Record<number, PageCacheItem> = {};

// --- DOM 元素获取 (使用类型断言) ---
const slider = document.getElementById('zoom-slider') as HTMLInputElement;
const label = document.getElementById('zoom-label') as HTMLSpanElement;
const container = document.getElementById('pages-container') as HTMLDivElement;
const viewport = document.getElementById('viewport') as HTMLDivElement;
const btnZoomOut = document.getElementById('btn-zoom-out') as HTMLButtonElement;
const btnZoomIn = document.getElementById('btn-zoom-in') as HTMLButtonElement;
const btnReset = document.getElementById('btn-reset') as HTMLButtonElement;

// --- 初始化 ---
updateSliderBackground(1.0);

// --- 事件绑定 ---

// 1. 按钮事件
btnZoomOut.addEventListener('click', () => adjustZoom(-0.1));
btnZoomIn.addEventListener('click', () => adjustZoom(0.1));
btnReset.addEventListener('click', resetZoom);

// 2. 滑动条拖动
slider.addEventListener('input', function() {
    setZoom(parseFloat(this.value));
});

// 3. 鼠标滚轮缩放 (Ctrl + Wheel)
viewport.addEventListener('wheel', function(e: WheelEvent) {
    if (e.ctrlKey || e.metaKey) {
        e.preventDefault();
        const delta = e.deltaY > 0 ? -0.1 : 0.1;
        adjustZoom(delta);
    }
}, { passive: false });

// 4. 滚动监听 (虚拟渲染)
let isScrolling = false;
viewport.addEventListener('scroll', () => {
    if (!isScrolling) {
        window.requestAnimationFrame(() => {
            renderVisiblePages();
            isScrolling = false;
        });
        isScrolling = true;
    }
});

// --- 逻辑函数 ---

function adjustZoom(delta: number) {
    let newScale = Math.round((currentScale + delta) * 10) / 10;
    newScale = Math.max(0.5, Math.min(3.0, newScale));
    setZoom(newScale);
}

function resetZoom() {
    setZoom(1.0);
}

function setZoom(scale: number) {
    currentScale = scale;

    // 注意：style.zoom 不是标准 CSS 属性，但在 WebKit/Blink (Android WebView) 中有效
    // TS 可能会报错，使用 any 或者扩展 CSSStyleDeclaration 接口，这里用 cast 绕过
    (container.style as any).zoom = currentScale;

    slider.value = currentScale.toString();
    label.innerText = Math.round(currentScale * 100) + "%";
    updateSliderBackground(currentScale);

    renderVisiblePages();
}

function updateSliderBackground(val: number) {
    const min = parseFloat(slider.min);
    const max = parseFloat(slider.max);
    const percent = ((val - min) / (max - min)) * 100;
    slider.style.setProperty('--percent', percent + '%');
}

// --- 核心业务逻辑 ---

// 暴露给 Java 调用的全局函数
window.updateSvgPages = function(jsonString: string) {
    let updates: PageUpdateData[];
    try {
        updates = JSON.parse(jsonString);
    } catch (e) {
        console.error("Failed to parse JSON:", e);
        return;
    }

    if (!updates || updates.length === 0) return;

    // 假设第一项包含了 totalPages 信息，或者你需要从 updates 的长度判断
    // 这里沿用你之前的逻辑，取 updates[0].totalPages
    const totalPages = updates[0].totalPages || updates.length;

    // 1. 更新缓存
    updates.forEach(u => {
        pageCache[u.pageIndex] = {
            content: u.svgContent,
            width: u.widthPt,
            height: u.heightPt
        };
    });

    // 2. 调整 DOM 骨架
    // 增加新页
    while (container.children.length < totalPages) {
        let div = document.createElement('div');
        div.className = 'page-wrapper';
        // 设置 id 方便查找，假设 id 格式为 "page-0", "page-1"
        div.id = 'page-' + container.children.length;
        div.dataset.loaded = "false";
        container.appendChild(div);
    }
    // 删除多余页
    while (container.children.length > totalPages) {
        delete pageCache[container.children.length - 1];
        if (container.lastChild) {
            container.removeChild(container.lastChild);
        }
    }

    // 3. 更新容器尺寸
    updates.forEach(u => {
        const pageDiv = document.getElementById('page-' + u.pageIndex);
        if (pageDiv) {
            pageDiv.style.width = u.widthPt + 'px';
            pageDiv.style.height = u.heightPt + 'px';

            if (pageDiv.dataset.loaded === "true") {
                pageDiv.innerHTML = u.svgContent;
            }
        }
    });

    // 4. 渲染检查
    renderVisiblePages();
};

/**
 * 虚拟渲染引擎
 */
function renderVisiblePages() {
    const viewTop = viewport.scrollTop;
    const viewBottom = viewTop + viewport.clientHeight;
    const buffer = 600;

    // HTMLCollection 并不是数组，需要转换或使用传统 for 循环
    const pages = container.children;

    for (let i = 0; i < pages.length; i++) {
        const pageDiv = pages[i] as HTMLElement; // 断言为 HTMLElement 才能访问 offsetTop

        const pageTop = pageDiv.offsetTop;
        const pageBottom = pageTop + pageDiv.offsetHeight;

        const isVisible = (pageBottom >= viewTop - buffer) && (pageTop <= viewBottom + buffer);

        if (isVisible) {
            if (pageDiv.dataset.loaded === "false") {
                const data = pageCache[i];
                if (data && data.content) {
                    pageDiv.innerHTML = data.content;
                    pageDiv.dataset.loaded = "true";
                }
            }
        } else {
            if (pageDiv.dataset.loaded === "true") {
                pageDiv.innerHTML = "";
                pageDiv.dataset.loaded = "false";
            }
        }
    }
}