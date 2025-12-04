import { writable } from 'svelte/store';
import { rpc } from '@/lib/api/rpc';

export enum FnTab {
    bookmark = 'bookmark',
    tocGenerator = 'tocGenerator',
    label = 'label',
    preview = 'preview',
    markdown = 'markdown',
    settings = 'settings'
}

// 定义连接状态类型
export type ConnectionStatus = 'init' | 'connecting' | 'connected' | 'error';

function createAppStore() {
    const { subscribe, set, update } = writable({
        activeTab: FnTab.bookmark,
        currentFilePath: null as string | null,
        pageCount: 0,
        serverPort: 0,
        connectionStatus: 'init' as ConnectionStatus // 添加连接状态
    });

    return {
        subscribe,
        switchTab: (tab: FnTab) => update(state => ({ ...state, activeTab: tab })),
        setCurrentFile: (path: string | null) => update(state => ({ ...state, currentFilePath: path })),
        setPageCount: (count: number) => update(state => ({ ...state, pageCount: count })),
        setServerPort: (port: number) => update(state => ({ ...state, serverPort: port })),
        // 新增：设置连接状态
        setConnectionStatus: (status: ConnectionStatus) => update(state => ({ ...state, connectionStatus: status })),
        openFile: async (path: string) => {
            await rpc.openFile(path);
            try {
                const count = await rpc.getPageCount();
                update(state => ({ ...state, currentFilePath: path, pageCount: count }));
            } catch (e) {
                console.error("Failed to get page count", e);
                update(state => ({ ...state, currentFilePath: path, pageCount: 0 }));
            }
        }
    };
}

export const appStore = createAppStore();
