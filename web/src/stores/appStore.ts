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
        currentFilePath: null as string | null,
        pageCount: 0,
        serverPort: 0
    });

    return {
        subscribe,
        switchTab: (tab: FnTab) => update(state => ({ ...state, activeTab: tab })),
        setCurrentFile: (path: string | null) => update(state => ({ ...state, currentFilePath: path })),
        setPageCount: (count: number) => update(state => ({ ...state, pageCount: count })),
        setServerPort: (port: number) => update(state => ({ ...state, serverPort: port })),
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
