class OffsetStore {
    value = $state(0);

    /**
     * 根据页码标签自动探测 Offset
     * 策略：找到第一个标签完全匹配 "1" 的页面索引
     */
    autoDetect(labels: string[]) {
        if (!labels || labels.length === 0) {
            this.value = 0;
            return;
        }
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