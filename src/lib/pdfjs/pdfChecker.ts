import { loadPdfFromPath } from '@/lib/pdfjs/index.ts';

export interface PdfCheckResult {
    isValid: boolean;      // 文件可被正常解析
    isEncrypted: boolean;  // 文件受密码保护
    isCorrupted: boolean;  // 文件损坏 (Invalid PDF)
    pageCount: number;     // 总页数 (仅在有效时)
    errorName?: string;    // 原始错误名
}

/**
 * 检查 PDF 状态。
 * 直接复用 @/lib/pdfjs 中的成熟加载逻辑。
 */
export async function checkPdf(filePath: string): Promise<PdfCheckResult> {
    try {
        // 直接复用现有的加载函数，它会自动处理静态服务器和 CMap
        const doc = await loadPdfFromPath(filePath);
        
        return {
            isValid: true,
            isEncrypted: false,
            isCorrupted: false,
            pageCount: doc.numPages
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
