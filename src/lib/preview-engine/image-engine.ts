import { renderPdfPageAsUrl } from '@/lib/api/pdf-render';

// --- 类型定义 ---
interface ImagePageUpdateData {
    pageIndex: number;
    widthPt: number;
    heightPt: number;
    totalPages: number;
    version: string | number;
}

/**
 * 核心：处理 Java 传来的图片 JSON 数据 (现改为从 Rust 获取)
 * @param jsonString JSON 字符串，包含页面更新数据
 * @param container DOM 容器元素
 * @param pdfFilePath 当前 PDF 文件的路径
 * @param currentScale 当前的缩放比例
 */
export function handleImageUpdate(jsonString: string, container: HTMLElement, pdfFilePath: string, currentScale: number) {
    if (!container) return;
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
    // const baseUrl = `http://127.0.0.1:${rpc.port}`; // Get base URL from rpc client

    // 1. 同步容器结构
    syncContainerForImage(container, totalPages);

    // 2. 更新页面
    updates.forEach(u => {
        const pageDiv = container.querySelector('#page-' + u.pageIndex) as HTMLElement;
        if (!pageDiv) return;

        // 设置尺寸
        pageDiv.style.width = u.widthPt + 'pt';
        pageDiv.style.height = u.heightPt + 'pt';

        // 标记为已加载
        pageDiv.dataset.loaded = "true";

        // const newUrl = `${baseUrl}/preview_images/${u.pageIndex}.png?v=${u.version}`;
        const currentImg = pageDiv.querySelector('img.current') as HTMLImageElement;

        // Ensure we have a file path
        if (!pdfFilePath) {
            console.warn("PDF file path is not available for rendering image.");
            return;
        }

        // Cache hit check (based on previously loaded image src, which will be a blob URL)
        // This is tricky because blob URLs are unique. A better cache would be based on version + pdfFilePath + scale
        // For now, if currentImg exists and we are *not* updating its src, just skip.
        // If currentImg does not exist, or its src is different, we proceed to render.
        // This check is simplified; a robust caching mechanism for blob URLs would be more complex.
        // For now, if the version changed, we always re-render.
        // @ts-ignore
        if (currentImg && currentImg.dataset.version === u.version.toString() && currentImg.dataset.scale === currentScale.toString()) {
            return; // No need to re-render if version and scale match
        }

        // --- 双缓冲加载 ---
        const newImg = document.createElement('img');
        newImg.className = 'preload';
        newImg.dataset.version = u.version.toString();
        newImg.dataset.scale = currentScale.toString();
        
                        // Use an array to store object URLs created for this image, and revoke them later
                        const oldObjectUrls: string[] = [];
                        if (currentImg && currentImg.src.startsWith('blob:')) {
                            oldObjectUrls.push(currentImg.src);
                        }
        
                        renderPdfPageAsUrl(pdfFilePath, u.pageIndex, currentScale)
                            .then(blobUrl => {
                                console.log(`[Image Engine] Setting src for page ${u.pageIndex} to: ${blobUrl}`);
                                newImg.src = blobUrl;
                                                newImg.onload = () => {
                    console.log(`[Image Engine] Image loaded for page ${u.pageIndex}`);
                    // Revoke old object URLs after new image is loaded and displayed
                    oldObjectUrls.forEach(URL.revokeObjectURL);
                    
                    // 下一帧触发动画
                    requestAnimationFrame(() => {
                        newImg.className = 'current';
                        
                        // 延迟移除旧图以显示淡入效果
                        const oldImages = Array.from(pageDiv.querySelectorAll('img:not(.current)'));
                        if (oldImages.length > 0) {
                            setTimeout(() => {
                                oldImages.forEach(img => {
                                    if (img !== newImg && img.src.startsWith('blob:')) {
                                        URL.revokeObjectURL(img.src); // Revoke URL for old image
                                    }
                                    img.remove();
                                });
                            }, 300); // 与 CSS transition 保持一致
                        }
                    });
                };

                newImg.onerror = (e) => {
                    console.error(`Failed to load image for page ${u.pageIndex} from blob URL for ${pdfFilePath}`, e);
                    // Fallback or error display
                };
            })
            .catch(err => {
                console.error(`Error rendering PDF page ${u.pageIndex} via Rust for ${pdfFilePath}:`, err);
                // Display error image or message
            });

        pageDiv.appendChild(newImg);
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
