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

function createAppStore() {
    const { subscribe, set, update } = writable({
        activeTab: FnTab.bookmark,
        currentFilePath: null as string | null
    });

    return {
        subscribe,
        switchTab: (tab: FnTab) => update(state => ({ ...state, activeTab: tab })),
        setCurrentFile: (path: string | null) => update(state => ({ ...state, currentFilePath: path })),
        openFile: async (path: string) => {
            await rpc.openFile(path);
            update(state => ({ ...state, currentFilePath: path }));
        }
    };
}

export const appStore = createAppStore();
