import { writable } from 'svelte/store';
import { rpc } from '@/lib/api/rpc'; // docStore 内部需要调用 RPC

interface DocState {
    currentFilePath: string | null;
    pageCount: number;
    originalPageLabels: string[];
    version: number; // Timestamp to force refresh images
}

const initialState: DocState = {
    currentFilePath: null,
    pageCount: 0,
    originalPageLabels: [],
    version: 0,
};

function createDocStore() {
    const { subscribe, set, update } = writable<DocState>(initialState);

    return {
        subscribe,
        setCurrentFile: (path: string | null) => update(state => ({ ...state, currentFilePath: path })),
        setPageCount: (count: number) => update(state => ({ ...state, pageCount: count })),
        
        /**
         * 打开文件，并通过 RPC 获取页数等信息。
         * 如果打开失败，将文件路径和页数重置。
         */
        openFile: async (path: string) => {
            await rpc.openFile(path); // 通知 Java 端打开文件
            try {
                const count = await rpc.getPageCount(); // 获取页数
                const labels = await rpc.getPageLabels(null); // 获取原始页码标签
                const ver = Date.now(); // Generate a new version for cache busting
                update(state => ({ ...state, currentFilePath: path, pageCount: count, originalPageLabels: labels, version: ver }));
            } catch (e) {
                console.error("Failed to get page count or labels after opening file:", e);
                // 如果获取页数失败，则清除文件信息
                update(state => ({ ...state, currentFilePath: null, pageCount: 0, originalPageLabels: [], version: 0 }));
            }
        },

        // 重置文档状态
        reset: () => set(initialState)
    };
}

export const docStore = createDocStore();
