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

class AppStore {
    activeTab = $state(FnTab.bookmark);
    serverPort = $state(0);
    connectionStatus = $state<ConnectionStatus>('init');
    
    // User preference for external editor (auto, code, etc.)
    externalEditor = $state('auto');

    switchTab(tab: FnTab) {
        this.activeTab = tab;
    }

    setServerPort(port: number) {
        this.serverPort = port;
    }

    setConnectionStatus(status: ConnectionStatus) {
        this.connectionStatus = status;
    }
}

export const appStore = new AppStore();