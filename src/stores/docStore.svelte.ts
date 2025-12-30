import { pdfRenderService } from '@/lib/services/PdfRenderService';
import { offsetStore } from './offsetStore.svelte';
import { pageLabelStore } from './pageLabelStore.svelte';
import { checkPdf } from '@/lib/pdfjs/pdfChecker';
import { messageStore } from './messageStore.svelte';
import type { PDFDocumentProxy } from 'pdfjs-dist';

class DocStore {
    // 响应式状态
    currentFilePath = $state<string | null>(null);
    pageCount = $state(0);
    version = $state(0);

    // 托管的 PDF.js 文档实例 (使用 .raw 避免深度代理)
    pdfDoc = $state.raw<PDFDocumentProxy | null>(null);

    // 静态大型数据
    originalPageLabels = $state.raw<string[]>([]);

    async openFile(path: string) {
        try {
            // 1. 资源清理：如果已经打开了一个文件，先销毁它释放内存
            if (this.pdfDoc) {
                console.log("Cleaning up old PDF document instance...");
                await this.pdfDoc.destroy();
                this.pdfDoc = null;
            }

            // 2. 预检并获取文档实例 (整个流程仅此一次加载)
            const checkResult = await checkPdf(path);
            
            if (!checkResult.isValid || !checkResult.doc) {
                if (checkResult.isEncrypted) {
                    messageStore.add("The PDF is password protected. QuickOutline does not support encrypted files yet.", "ERROR");
                } else if (checkResult.isCorrupted) {
                    messageStore.add("The PDF file is corrupted or invalid.", "ERROR");
                } else {
                    messageStore.add(`Failed to open PDF: ${checkResult.errorName || 'Unknown error'}`, "ERROR");
                }
                return;
            }

            // 3. 托管实例
            this.pdfDoc = checkResult.doc;
            console.log("PDF document loaded and managed by DocStore.");

            // 4. 获取文档信息 (复用实例)
            const count = checkResult.pageCount; 
            const labels = await pdfRenderService.getPageLabels(path) || [];

            // 5. 更新状态
            this.currentFilePath = path;
            this.pageCount = count;
            this.originalPageLabels = labels;
            this.version = Date.now();

            // 6. 主动触发相关 Store 的初始化
            offsetStore.autoDetect(labels);
            pageLabelStore.init(labels);

        } catch (e: any) {
            console.error("Failed to open file:", e);
            messageStore.add("Failed to open file: " + (e.message || String(e)), "ERROR");
            this.reset();
        }
    }

    /**
     * 仅设置当前文件路径（通常用于初始化或恢复状态）
     */
    setCurrentFile(path: string | null) {
        this.currentFilePath = path;
    }

    async reset() {
        if (this.pdfDoc) {
            await this.pdfDoc.destroy();
            this.pdfDoc = null;
        }
        this.currentFilePath = null;
        this.pageCount = 0;
        this.originalPageLabels = [];
        this.version = 0;
        offsetStore.set(0);
    }
}

export const docStore = new DocStore();