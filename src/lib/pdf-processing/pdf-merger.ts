import { PDFDocument } from 'pdf-lib';

/**
 * 将源 PDF 的页面插入到目标 PDF 的指定位置。
 *
 * @param targetPdfBytes - 目标 PDF (原始文档) 的 ArrayBuffer。
 * @param sourcePdfBytes - 源 PDF (要插入的文档) 的 ArrayBuffer。
 * @param insertAtIndex - 插入位置（基于 0 的索引）。
 * @returns 合并后的 PDFDocument 对象（尚未保存，方便后续添加链接等操作）。
 */
export async function insertPdfToPdf(
    targetPdfBytes: ArrayBuffer,
    sourcePdfBytes: ArrayBuffer,
    insertAtIndex: number
): Promise<PDFDocument> {
    const targetDoc = await PDFDocument.load(targetPdfBytes);
    const sourceDoc = await PDFDocument.load(sourcePdfBytes);

    const newDoc = await PDFDocument.create();

    const targetPageCount = targetDoc.getPageCount();
    const sourcePageCount = sourceDoc.getPageCount();

    // 1. 复制目标文档的前半部分 (0 到 insertAtIndex - 1)
    if (insertAtIndex > 0) {
        const prePagesIndices = Array.from({ length: Math.min(insertAtIndex, targetPageCount) }, (_, i) => i);
        if (prePagesIndices.length > 0) {
            const prePages = await newDoc.copyPages(targetDoc, prePagesIndices);
            prePages.forEach(page => newDoc.addPage(page));
        }
    }

    // 2. 复制源文档的所有页面
    if (sourcePageCount > 0) {
        const sourcePageIndices = sourceDoc.getPageIndices();
        const sourcePages = await newDoc.copyPages(sourceDoc, sourcePageIndices);
        sourcePages.forEach(page => newDoc.addPage(page));
    }

    // 3. 复制目标文档的后半部分 (insertAtIndex 到结尾)
    if (insertAtIndex < targetPageCount) {
        const postPagesIndices = Array.from({ length: targetPageCount - insertAtIndex }, (_, i) => i + insertAtIndex);
        if (postPagesIndices.length > 0) {
            const postPages = await newDoc.copyPages(targetDoc, postPagesIndices);
            postPages.forEach(page => newDoc.addPage(page));
        }
    }

    return newDoc;
}
