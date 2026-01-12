import type { PageLayout, HeaderFooterLayout, SectionConfig } from '@/lib/types/page';
import PagedPrepareWorker from '@/workers/paged-prepare.worker?worker';

export interface PagedPayload {
    html: string;
    styles: string;
    header: SectionConfig;
    footer: SectionConfig;
    pageLayout?: PageLayout;
    hfLayout?: HeaderFooterLayout;
}

/**
 * 专门负责与 Paged.js 预处理 Worker 通信的客户端
 */
class PrepareWorkerClient {
    private worker: Worker;
    private pendingRequests = new Map<string, { resolve: (data: any) => void, reject: (err: any) => void }>();

    constructor() {
        this.worker = new PagedPrepareWorker();
        this.worker.onmessage = (e) => {
            const { type, id, payload, error } = e.data;
            const request = this.pendingRequests.get(id);
            if (request) {
                if (type === 'success') {
                    request.resolve(payload);
                } else {
                    request.reject(new Error(error));
                }
                this.pendingRequests.delete(id);
            }
        };
    }

    /**
     * 将排版所需的原材料发给 Worker 拼接处理
     */
    public async prepare(payload: PagedPayload): Promise<{ pageCss: string, contentWithStyle: string }> {
        const id = Math.random().toString(36).substring(7);
        
        // 性能优化：解构出大字符串和配置项
        const { html, styles, ...configs } = payload;
        
        // 只对体积很小的配置对象进行“脱水”（剥离 Svelte Proxy），避免 DataCloneError
        // 巨大的 html 字符串不参与 JSON.stringify，从而避免二次序列化开销
        const cleanConfigs = JSON.parse(JSON.stringify(configs));
        
        const workerData = { 
            id, 
            ...cleanConfigs, 
            html, 
            styles 
        };
        
        return new Promise((resolve, reject) => {
            this.pendingRequests.set(id, { resolve, reject });
            this.worker.postMessage(workerData);
        });
    }

    public terminate() {
        this.worker.terminate();
    }
}

// 导出单例，避免重复创建 Worker 线程
export const workerClient = new PrepareWorkerClient();
