import { rpc } from '@/lib/api/rpc';
import { pdfRenderService } from '@/lib/services/PdfRenderService';
import { offsetStore } from './offsetStore.svelte';
import { pageLabelStore } from './pageLabelStore.svelte';
import { checkPdf } from '@/lib/pdfjs/pdfChecker.ts';
import { messageStore } from './messageStore.svelte';

class DocStore {
    // 响应式状态
    currentFilePath = $state<string | null>(null);
    pageCount = $state(0);
    version = $state(0);

    // 静态大型数据，使用 raw 提升性能
    originalPageLabels = $state.raw<string[]>([]);

    async openFile(path: string) {
        try {
            // 1. 预检 PDF 状态
            const checkResult = await checkPdf(path);
            
            if (!checkResult.isValid) {
                if (checkResult.isEncrypted) {
                    messageStore.add("The PDF is password protected. QuickOutline does not support encrypted files yet.", "ERROR");
                } else if (checkResult.isCorrupted) {
                    messageStore.add("The PDF file is corrupted or invalid.", "ERROR");
                } else {
                    messageStore.add(`Failed to open PDF: ${checkResult.errorName || 'Unknown error'}`, "ERROR");
                }
                return;
            }

            // 2. 调用后端打开文件
            await rpc.openFile(path);
            console.log("File opened in backend.");

            // 3. 获取文档信息
            const count = checkResult.pageCount; // 直接使用预检得到的页数
            const labels = await pdfRenderService.getPageLabels(path) || [];

            // 4. 更新状态
            this.currentFilePath = path;
            this.pageCount = count;
            this.originalPageLabels = labels;
            this.version = Date.now();

            // 5. 主动触发相关 Store 的初始化
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

    reset() {
        this.currentFilePath = null;
        this.pageCount = 0;
        this.originalPageLabels = [];
        this.version = 0;
        
        // 同时重置 Offset
        offsetStore.set(0);
    }
}

export const docStore = new DocStore();
