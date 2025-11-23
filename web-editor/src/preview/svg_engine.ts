// --- 类型定义 ---
interface SvgPageUpdateData {
    pageIndex: number;
    svgContent: string;
    widthPt: number;
    heightPt: number;
    totalPages?: number;
}

interface PageCacheItem {
    content: string;
    width: number;
    height: number;
}

// --- 内部状态 ---
const pageCache: Record<number, PageCacheItem> = {};

/**
 * 核心：处理 Java 传来的 SVG JSON 数据
 */
export function handleSvgUpdate(jsonString: string, container: HTMLElement, viewport: HTMLElement) {
    let updates: SvgPageUpdateData[];
    try {
        updates = JSON.parse(jsonString);
    } catch (e) {
        console.error("SVG JSON Parse Error", e);
        return;
    }

    if (!updates || updates.length === 0) return;

    const totalPages = updates[0].totalPages || updates.length;

    // 1. 更新缓存
    updates.forEach(u => {
        pageCache[u.pageIndex] = {
            content: u.svgContent,
            width: u.widthPt,
            height: u.heightPt
        };
    });

    // 2. 同步容器结构
    syncContainerForSvg(container, totalPages);

    // 3. 更新尺寸 & 强制刷新可视区
    updates.forEach(u => {
        const pageDiv = document.getElementById('page-' + u.pageIndex);
        if (pageDiv) {
            // 注意：SVG 通常使用 px 或 pt，这里保持逻辑一致性
            pageDiv.style.width = u.widthPt + 'px';
            pageDiv.style.height = u.heightPt + 'px';

            // 如果当前正好在显示，强制刷新内容
            if (pageDiv.dataset.loaded === "true") {
                pageDiv.innerHTML = u.svgContent;
            }
        }
    });

    // 4. 立即触发一次渲染
    renderVisiblePages(container, viewport);
}

/**
 * 响应滚动或缩放事件（对外暴露）
 */
export function onSvgViewChange(container: HTMLElement, viewport: HTMLElement) {
    // 只有当缓存有数据时才执行，避免在图片模式下浪费计算资源
    if (Object.keys(pageCache).length > 0) {
        renderVisiblePages(container, viewport);
    }
}

/**
 * 内部：同步 DOM 节点数量
 */
function syncContainerForSvg(container: HTMLElement, totalPages: number) {
    // 增
    while (container.children.length < totalPages) {
        const div = document.createElement('div');
        div.className = 'page-wrapper';
        div.id = 'page-' + container.children.length;
        div.dataset.loaded = "false"; // 虚拟列表标记
        container.appendChild(div);
    }
    // 减 (同时清理缓存)
    while (container.children.length > totalPages) {
        delete pageCache[container.children.length - 1];
        if (container.lastChild) {
            container.removeChild(container.lastChild);
        }
    }
}

/**
 * 内部：虚拟渲染实现
 */
function renderVisiblePages(container: HTMLElement, viewport: HTMLElement) {
    const viewTop = viewport.scrollTop;
    const viewBottom = viewTop + viewport.clientHeight;
    const buffer = 600;

    const pages = container.children;

    for (let i = 0; i < pages.length; i++) {
        const pageDiv = pages[i] as HTMLElement;
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
                pageDiv.innerHTML = ""; // 卸载 DOM 节省内存
                pageDiv.dataset.loaded = "false";
            }
        }
    }
}