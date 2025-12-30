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
export type ExternalEditorType = 'auto' | 'code' | 'code-insiders' | 'zed';

class AppStore {
    activeTab = $state(FnTab.bookmark);
    serverPort = $state(0);
    connectionStatus = $state<ConnectionStatus>('init');
    externalEditor = $state<ExternalEditorType>('auto');

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