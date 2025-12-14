
export interface Block {
    id: string;
    type: 'p' | 'h1' | 'h2' | 'img';
    content: string;
    // 在真实场景中，这里可能包含更多的样式信息
}

export interface PageConfig {
    pageHeight: number;  // 页面总高度 (px)
    marginTop: number;   // 上边距 (px)
    marginBottom: number;// 下边距 (px)
    lineHeight: number;  // 基础行高 (px), 用于估算
}

export interface Page {
    index: number;
    blocks: Block[];
    heightUsed: number;
}

export class VirtualPager {
    private config: PageConfig;
    private measurementCache: Map<string, number> = new Map();

    constructor(config: PageConfig) {
        this.config = config;
    }

    /**
     * 核心分页方法
     * @param blocks 待分页的内容块列表
     * @param measureFn 测量函数：给定一个 Block，返回它的像素高度。
     *                  在浏览器中，这通常涉及创建一个隐藏的 DOM 元素来测量，
     *                  或者使用 Canvas measureText 进行估算。
     */
    public paginate(blocks: Block[], measureFn: (block: Block) => number): Page[] {
        const pages: Page[] = [];
        const contentHeightAvailable = this.config.pageHeight - this.config.marginTop - this.config.marginBottom;

        let currentPageBlocks: Block[] = [];
        let currentHeight = 0;
        let pageIndex = 0;

        for (const block of blocks) {
            // 1. 获取块高度 (优先使用缓存，但这里假设 measureFn 内部或调用者处理缓存，
            // 或者我们在这里做简单的 ID 缓存)
            let blockHeight = this.measurementCache.get(block.id);
            if (blockHeight === undefined) {
                blockHeight = measureFn(block);
                this.measurementCache.set(block.id, blockHeight);
            }

            // 2. 检查是否溢出
            // 简单的溢出逻辑：如果当前高度 + 新块高度 > 可用高度，则换页。
            // 真实场景可能需要处理：
            // - 单个块高度 > 页面高度（需要拆分块）
            // - 孤行寡行控制 (Widows/Orphans)
            if (currentHeight + blockHeight > contentHeightAvailable) {
                // 提交当前页
                pages.push({
                    index: pageIndex++,
                    blocks: currentPageBlocks,
                    heightUsed: currentHeight
                });

                // 重置下一页
                currentPageBlocks = [];
                currentHeight = 0;
            }

            // 添加块到当前页
            currentPageBlocks.push(block);
            currentHeight += blockHeight;
        }

        // 提交最后一页
        if (currentPageBlocks.length > 0) {
            pages.push({
                index: pageIndex++,
                blocks: currentPageBlocks,
                heightUsed: currentHeight
            });
        }

        return pages;
    }

    public clearCache() {
        this.measurementCache.clear();
    }
}

// --- 以下是模拟数据生成和测量工具，用于 Demo ---

export function generateMockBlocks(count: number): Block[] {
    const blocks: Block[] = [];
    const types: ('p' | 'h1' | 'h2')[] = ['p', 'p', 'p', 'h1', 'p', 'p', 'h2', 'p'];
    
    for (let i = 0; i < count; i++) {
        const type = types[Math.floor(Math.random() * types.length)];
        let content = `Block ${i}: This is some simulated content. `;
        // 随机增加长度
        if (type === 'p') {
            const loops = Math.floor(Math.random() * 5);
            for (let j = 0; j < loops; j++) {
                content += "The quick brown fox jumps over the lazy dog. ";
            }
        }

        blocks.push({
            id: `b-${i}`,
            type,
            content
        });
    }
    return blocks;
}

// 模拟测量函数 (纯逻辑，不依赖 DOM)
export function mockMeasure(block: Block): number {
    // 假设基础行高 20px
    const baseLineHeight = 20;
    
    if (block.type === 'h1') return 60; // H1 占 60px
    if (block.type === 'h2') return 40; // H2 占 40px
    
    // 对于 P，根据字符长度估算行数 (假设一行 80 字符)
    const charsPerLine = 80;
    const lines = Math.ceil(block.content.length / charsPerLine);
    return lines * baseLineHeight + 10; // +10 margin
}
