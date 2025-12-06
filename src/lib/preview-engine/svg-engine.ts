// --- 类型定义 ---
export interface SvgPageUpdateData {
    pageIndex: number;
    svgContent?: string; // 变为可选，通常为空
    widthPt: number;
    heightPt: number;
    totalPages?: number;
    version: number; // 新增版本号
}

interface PageCacheItem {
    content: string;
    width: number;
    height: number;
    version: number;
}

// --- 内部状态 ---
const pageCache: Record<number, PageCacheItem> = {};
let isDoubleBufferingEnabled = true;

export function setDoubleBuffering(enable: boolean) {
    isDoubleBufferingEnabled = enable;
    const container = document.getElementById('pages-container');
    if (container) {
        if (enable) {
            container.classList.add('double-buffer');
        } else {
            container.classList.remove('double-buffer');
        }
    }
    console.log(`[SVG] Double buffering set to: ${enable}`);
}

/**
 * 核心：处理 Java 传来的 SVG JSON 元数据
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

    // 1. 同步容器结构
    syncContainerForSvg(container, totalPages);

    // 2. 处理更新
    updates.forEach(u => {
        const pageDiv = container.querySelector('#page-' + u.pageIndex) as HTMLElement;
        if (pageDiv) {
            // 更新尺寸
            pageDiv.style.width = u.widthPt + 'px';
            pageDiv.style.height = u.heightPt + 'px';
        }

        // 检查缓存是否匹配当前版本
        const cached = pageCache[u.pageIndex];
        
        // 如果缓存存在且版本一致，跳过 Fetch
        if (cached && cached.version === u.version && cached.content) {
            // 强制刷新当前视图（如果该页在视野内但未加载）
            renderIfVisible(u.pageIndex, container, viewport);
            return;
        }

        // 需要更新：发起 Fetch
        // 默认 DocId 为 "default"，如果支持多文档需从 Context 获取
        // 这里使用简单的 /page_svg/{page}.svg 路径，Java 端会解析为默认文档
        const url = `/page_svg/${u.pageIndex}.svg?v=${u.version}`;

        // 预先更新缓存结构（占位），防止重复处理
        pageCache[u.pageIndex] = {
            content: '', // 待填充
            width: u.widthPt,
            height: u.heightPt,
            version: u.version
        };

        fetch(url)
            .then(res => {
                if (!res.ok) throw new Error(res.statusText);
                return res.text();
            })
            .then(text => {
                // 检查版本一致性（防止竞态）
                if (pageCache[u.pageIndex] && pageCache[u.pageIndex].version === u.version) {
                    pageCache[u.pageIndex].content = text;
                    // 数据就绪，尝试渲染
                    renderIfVisible(u.pageIndex, container, viewport);
                }
            })
            .catch(err => console.error(`[SVG] Failed to load page ${u.pageIndex}`, err));
    });

    // 3. 立即触发一次可视区域渲染（处理那些不需要 Fetch 的页面）
    renderVisiblePages(container, viewport);
}

/**
 * 响应滚动或缩放事件（对外暴露）
 */
export function onSvgViewChange(container: HTMLElement, viewport: HTMLElement) {
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
 * 内部：渲染特定页面（如果可见）
 */
function renderIfVisible(pageIndex: number, container: HTMLElement, viewport: HTMLElement) {
    const pageDiv = container.querySelector('#page-' + pageIndex) as HTMLElement;
    if (!pageDiv) return;

    if (isElementVisible(pageDiv, viewport)) {
        const data = pageCache[pageIndex];
        // 只有当内容已下载且当前未加载时才渲染
        // 或者如果是强制刷新（版本更新），无论 loaded 状态如何都要渲染？
        // 为了简化，我们认为只要 loaded=true 且版本匹配就不动。
        // 但这里是 update 回调，说明版本变了，所以如果 visible，就应该刷新。
        // 问题是：怎么判断 DOM 里的内容是不是旧版本的？
        // 简单策略：只要由 handleSvgUpdate 触发，我们假设 DOM 是旧的或空的，直接覆盖。
        
        if (data && data.content) {
             renderPageNode(pageDiv, data.content);
             pageDiv.dataset.loaded = "true";
        }
    }
}

/**
 * 内部：虚拟渲染循环
 */
function renderVisiblePages(container: HTMLElement, viewport: HTMLElement) {
    const pages = container.children;
    for (let i = 0; i < pages.length; i++) {
        const pageDiv = pages[i] as HTMLElement;
        
        if (isElementVisible(pageDiv, viewport)) {
            if (pageDiv.dataset.loaded === "false") {
                const data = pageCache[i];
                if (data && data.content) {
                    renderPageNode(pageDiv, data.content);
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

function isElementVisible(el: HTMLElement, viewport: HTMLElement): boolean {
    const viewTop = viewport.scrollTop;
    const viewBottom = viewTop + viewport.clientHeight;
    const buffer = 600;

    const pageTop = el.offsetTop;
    const pageBottom = pageTop + el.offsetHeight;

    return (pageBottom >= viewTop - buffer) && (pageTop <= viewBottom + buffer);
}

/**
 * 统一渲染逻辑
 */
function renderPageNode(pageDiv: HTMLElement, svgContent: string) {
    if (isDoubleBufferingEnabled) {
        // --- 双缓冲加载逻辑 ---
        const parser = new DOMParser();
        const doc = parser.parseFromString(svgContent, "image/svg+xml");
        const newSvg = doc.documentElement;

        if (newSvg.tagName.toLowerCase() === 'svg') {
            newSvg.classList.add('preload');
            pageDiv.appendChild(newSvg);

            requestAnimationFrame(() => {
                newSvg.classList.add('current');
                newSvg.classList.remove('preload');

                const oldSvgs = Array.from(pageDiv.querySelectorAll('svg')).filter(el => el !== newSvg);
                if (oldSvgs.length > 0) {
                    setTimeout(() => oldSvgs.forEach(el => el.remove()), 300); // 与 CSS transition 保持一致
                }
            });
        }
    } else {
        // --- 直接替换逻辑 ---
        pageDiv.innerHTML = svgContent;
        const svg = pageDiv.querySelector('svg');
        if (svg) {
            svg.classList.add('current');
        }
    }
}