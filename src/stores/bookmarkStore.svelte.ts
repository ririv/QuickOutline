import { writable } from 'svelte/store';
import type { Bookmark } from '@/components/bookmark/types'; // Import Bookmark type

interface BookmarkState {
    text: string;
    tree: Bookmark[]; // Add tree property
    offset: number;
}

function createBookmarkStore() {
    const { subscribe, set, update } = writable<BookmarkState>({
        text: '',
        tree: [], // Initialize with empty array
        offset: 0
    });

    return {
        subscribe,
        setText: (text: string) => update(s => ({ ...s, text })),
        setTree: (tree: Bookmark[]) => update(s => ({ ...s, tree })), // New setter for tree
        setOffset: (offset: number) => update(s => ({ ...s, offset })),
        reset: () => set({ text: '', tree: [], offset: 0 }) // Reset also clears tree
    };
}

export const bookmarkStore = createBookmarkStore();
