import { writable } from 'svelte/store';

export enum FnTab {
    bookmark = 'bookmark',
    tocGenerator = 'tocGenerator',
    label = 'label',
    preview = 'preview',
    markdown = 'markdown'
}

function createAppStore() {
    const { subscribe, set, update } = writable({
        activeTab: FnTab.bookmark
    });

    return {
        subscribe,
        switchTab: (tab: FnTab) => update(state => ({ ...state, activeTab: tab }))
    };
}

export const appStore = createAppStore();
