// --- 类型定义 ---
interface ImagePageUpdateData {
    pageIndex: number;
    widthPt: number;
    heightPt: number;
    totalPages: number;
    version: string | number;
}

/**
 * 核心：处理 Java 传来的图片 JSON 数据
 */
export function handleImageUpdate(jsonString: string, container: HTMLElement) {
    // 确保容器拥有 .double-buffer 类以激活 CSS 双缓冲样式
    if (!container.classList.contains('double-buffer')) {
        container.classList.add('double-buffer');
    }

    let updates: ImagePageUpdateData[];
    try {
        updates = JSON.parse(jsonString);
    } catch (e) {
        console.error("Image JSON Parse Error", e);
        return;
    }

    if (!updates || updates.length === 0) return;

    const totalPages = updates[0].totalPages;

    // 1. 同步容器结构
    syncContainerForImage(container, totalPages);

    // 2. 更新页面
    updates.forEach(u => {
        const pageDiv = container.querySelector('#page-' + u.pageIndex) as HTMLElement;
        if (!pageDiv) return;

        // 设置尺寸
        pageDiv.style.width = u.widthPt + 'pt';
        pageDiv.style.height = u.heightPt + 'pt';

        // 标记为已加载，防止 SVG 逻辑误判（虽然现在逻辑分离了，但保持一致性是好的）
        pageDiv.dataset.loaded = "true";

        const newUrl = `/page_images/${u.pageIndex}.png?v=${u.version}`;
        const currentImg = pageDiv.querySelector('img.current') as HTMLImageElement;

        // 缓存命中检查
        // @ts-ignore
        if (currentImg && currentImg.src.includes(`v=${u.version}`)) {
            return;
        }

        // --- 双缓冲加载 ---
        const newImg = document.createElement('img');
        newImg.className = 'preload';

        newImg.onload = () => {
            newImg.className = 'current';
            // 移除旧图
            const oldImages = pageDiv.querySelectorAll('img:not(.current)');
            oldImages.forEach(img => {
                if (img !== newImg) pageDiv.removeChild(img);
            });
        };

        newImg.onerror = () => {
            console.error(`Failed to load image for page ${u.pageIndex}`);
        };

        pageDiv.appendChild(newImg);
        newImg.src = newUrl;
    });
}

/**
 * 内部：同步 DOM 节点数量 (图片版不需要处理缓存清理)
 */
function syncContainerForImage(container: HTMLElement, totalPages: number) {
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
}
