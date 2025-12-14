import { writable } from 'svelte/store';
// import { rpc } from '@/lib/api/rpc'; // rpc no longer needed here

export enum FnTab {
    bookmark = 'bookmark',
    tocGenerator = 'tocGenerator',
    label = 'label',
    preview = 'preview',
    markdown = 'markdown',
    settings = 'settings',
    experimental = 'experimental'
}

// 定义连接状态类型
export type ConnectionStatus = 'init' | 'connecting' | 'connected' | 'error';

function createAppStore() {
    const { subscribe, set, update } = writable({
        activeTab: FnTab.bookmark,
        serverPort: 0,
        connectionStatus: 'init' as ConnectionStatus // 添加连接状态
    });

    return {
        subscribe,
        switchTab: (tab: FnTab) => update(state => ({ ...state, activeTab: tab })),
        setServerPort: (port: number) => update(state => ({ ...state, serverPort: port })),
        setConnectionStatus: (status: ConnectionStatus) => update(state => ({ ...state, connectionStatus: status })),
    };
}

export const appStore = createAppStore();
