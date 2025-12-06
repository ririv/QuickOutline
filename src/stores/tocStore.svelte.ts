export class TocState {
    content = $state('');
    filePath = $state<string | null>(null);
    
    // 更新内容（用户编辑时调用）
    updateContent(newContent: string) {
        this.content = newContent;
    }

    // 关联当前文件（加载新文件时调用）
    setFile(path: string | null, initialContent: string = '') {
        // 只有路径变化时才重置
        if (this.filePath !== path) {
            this.filePath = path;
            this.content = initialContent;
        }
    }

    // 检查 Store 中的内容是否属于该文件
    hasContentFor(path: string | null) {
        return this.filePath === path && this.content.length > 0;
    }
    
    // 获取当前内容
    getContent() {
        return this.content;
    }
}

export const tocStore = new TocState();
