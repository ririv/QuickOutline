import { detectPageOffset } from 'outline-parser/pageOffset';

class OffsetStore {
    value = $state(0);

    /**
     * 根据页码标签自动探测 Offset
     * 策略：找到第一个标签完全匹配 "1" 的页面索引
     */
    autoDetect(labels: string[]) {
        const offset = detectPageOffset(labels);
        if (offset > 0) console.log(`[OffsetStore] Auto-detected offset: ${offset}`);
        this.value = offset;
    }

    /**
     * 手动设置 Offset
     */
    set(offset: number) {
        this.value = offset;
    }
}

export const offsetStore = new OffsetStore();
