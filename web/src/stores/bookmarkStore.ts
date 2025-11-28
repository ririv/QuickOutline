import { writable } from 'svelte/store';

interface BookmarkState {
    text: string;
    offset: number;
}

function createBookmarkStore() {
    const { subscribe, set, update } = writable<BookmarkState>({
        text: '',
        offset: 0
    });

    return {
        subscribe,
        setText: (text: string) => update(s => ({ ...s, text })),
        setOffset: (offset: number) => update(s => ({ ...s, offset })),
        reset: () => set({ text: '', offset: 0 })
    };
}

export const bookmarkStore = createBookmarkStore();
