export enum FnTab {
    bookmark = 'bookmark',
    tocGenerator = 'tocGenerator',
    label = 'label',
    viewer = 'viewer',
    markdown = 'markdown',
    settings = 'settings',
    experimental = 'experimental'
}

class AppStore {
    activeTab = $state(FnTab.bookmark);
    
    // User preference for external editor (auto, code, etc.)
    externalEditor = $state('auto');

    switchTab(tab: FnTab) {
        this.activeTab = tab;
    }
}

export const appStore = new AppStore();