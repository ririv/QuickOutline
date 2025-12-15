import { PDFDocument } from 'pdf-lib';
import { insertPdfToPdf } from './pdf-merger';

export interface TocLink {
    tocPageIndex: number;
    x: number;
    y: number;
    width: number;
    height: number;
    targetPage: string;
}

/**
 * 将 TOC PDF 合并到原始 PDF，并添加跳转链接。
 *
 * @param originalPdfBytes - 原始 PDF 数据。
 * @param tocPdfBytes - TOC PDF 数据。
 * @param insertAtIndex - 插入位置。
 * @param links - 跳转链接信息。
 * @returns 合并后的 PDF 数据 (Uint8Array)。
 */
export async function mergeTocWithLinks(
    originalPdfBytes: ArrayBuffer,
    tocPdfBytes: ArrayBuffer,
    insertAtIndex: number,
    links: TocLink[] = []
): Promise<Uint8Array> {
    // 1. 执行物理合并
    // insertPdfToPdf 返回的是 PDFDocument 对象
    const newDoc = await insertPdfToPdf(originalPdfBytes, tocPdfBytes, insertAtIndex);

    // 2. 为了计算 TOC 页数和位置，我们需要加载原始数据来获取页数信息
    // 这是一个轻量级的加载，主要为了获取页数
    const tocDocTemp = await PDFDocument.load(tocPdfBytes);
    const tocPageCount = tocDocTemp.getPageCount();

    const targetDocTemp = await PDFDocument.load(originalPdfBytes);
    const targetPageCount = targetDocTemp.getPageCount();
    
    // 3. 计算 TOC 在新文档中的起始索引
    const prePagesCount = Math.min(insertAtIndex, targetPageCount);
    const tocStartInNewDoc = prePagesCount;

    // 4. 处理链接
    if (links && links.length > 0 && tocPageCount > 0) {
        // 假设 scale = 0.75 (96DPI -> 72DPI)
        // 注意：如果预览引擎不是 96DPI，这里可能需要调整。
        const scale = 0.75; 
        
        // 获取第一页 TOC 的高度用于坐标转换 (假设所有 TOC 页面大小一致)
        // 注意：这里的 page0 是 TOC 的第一页
        const page0 = newDoc.getPage(tocStartInNewDoc);
        const { height: pdfHeight } = page0.getSize();

        for (const link of links) {
            if (link.tocPageIndex >= tocPageCount) continue;

            const newDocPageIndex = tocStartInNewDoc + link.tocPageIndex;
            if (newDocPageIndex >= newDoc.getPageCount()) continue;

            const page = newDoc.getPage(newDocPageIndex);

            const targetPageNum = parseInt(link.targetPage, 10);
            if (isNaN(targetPageNum)) continue;

            // 计算目标页在新文档中的索引
            // 原始文档 index = targetPageNum - 1
            const originalIndex = targetPageNum - 1;
            let targetNewIndex = originalIndex;
            
            // 如果目标页在插入点之后，需要加上 TOC 的页数
            if (originalIndex >= insertAtIndex) {
                targetNewIndex += tocPageCount;
            }

            // 检查目标页是否在范围内
            if (targetNewIndex < 0 || targetNewIndex >= newDoc.getPageCount()) continue;

            const targetPageRef = newDoc.getPage(targetNewIndex).ref;

            // 坐标转换 (HTML top-left -> PDF bottom-left)
            const rectX = link.x * scale;
            const rectW = link.width * scale;
            const rectH = link.height * scale;
            const rectY = pdfHeight - (link.y * scale) - rectH;

            // 创建 Link Annotation
            const linkAnnot = newDoc.context.obj({
                Type: 'Annot',
                Subtype: 'Link',
                Rect: [rectX, rectY, rectX + rectW, rectY + rectH],
                Border: [0, 0, 0], // 无边框
                Dest: [targetPageRef, 'XYZ', null, null, null], // 跳转并保持缩放
            });

            const linkAnnotRef = newDoc.context.register(linkAnnot);
            page.node.addAnnot(linkAnnotRef);
        }
    }

    return await newDoc.save();
}
