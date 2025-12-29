import { docStore } from './docStore';

class OffsetStore {
    value = $state(0);

    private lastFilePath: string | null = null;

    constructor() {
        // 自动订阅 docStore 的变化
        // 当文档被打开且有了 pageLabels 时，尝试自动探测 offset
        docStore.subscribe(doc => {
            if (doc.currentFilePath !== this.lastFilePath) {
                this.lastFilePath = doc.currentFilePath;
                
                if (doc.currentFilePath && doc.originalPageLabels.length > 0) {
                    this.autoDetect(doc.originalPageLabels);
                } else if (!doc.currentFilePath) {
                    // 文档关闭，重置
                    this.value = 0;
                }
            }
        });
    }

    /**
     * 根据页码标签自动探测 Offset
     * 策略：找到第一个标签完全匹配 "1" 的页面索引
     */
    autoDetect(labels: string[]) {
        const index = labels.indexOf('1');
        if (index > 0) {
            console.log(`[OffsetStore] Auto-detected offset: ${index}`);
            this.value = index;
        } else {
            // 如果没找到 "1"，或者 "1" 就是第 0 页，则 offset 默认为 0
            this.value = 0;
        }
    }

    /**
     * 手动设置 Offset
     */
    set(offset: number) {
        this.value = offset;
    }
}

export const offsetStore = new OffsetStore();
