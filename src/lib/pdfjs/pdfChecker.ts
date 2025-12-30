import { loadPdfFromPath } from '@/lib/pdfjs/index.ts';
import type { PDFDocumentProxy } from 'pdfjs-dist';

export interface PdfCheckResult {
    isValid: boolean;      // 文件可被正常解析
    isEncrypted: boolean;  // 文件受密码保护
    isCorrupted: boolean;  // 文件损坏 (Invalid PDF)
    pageCount: number;     // 总页数 (仅在有效时)
    errorName?: string;    // 原始错误名
    doc?: PDFDocumentProxy; // 交付加载好的文档实例
}

/**
 * 检查 PDF 状态。
 * 
 * 优化：加载成功后直接返回 doc 实例给 docStore 托管，实现全应用单次加载。
 */
export async function checkPdf(filePath: string): Promise<PdfCheckResult> {
    try {
        // 复用现有的加载函数
        const doc = await loadPdfFromPath(filePath);
        
        return {
            isValid: true,
            isEncrypted: false,
            isCorrupted: false,
            pageCount: doc.numPages,
            doc: doc
        };
    } catch (error: unknown) {
        const pdfError = error as { name?: string };
        const errorName = pdfError.name || '';

        // 依据 PDF.js 错误名判定状态
        const isEncrypted = errorName === 'PasswordException';
        const isCorrupted = errorName === 'InvalidPDFException';

        return {
            isValid: false,
            isEncrypted,
            isCorrupted,
            pageCount: 0,
            errorName: errorName
        };
    }
}