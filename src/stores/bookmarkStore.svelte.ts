import type { BookmarkUI } from '@/components/bookmark/types';

class BookmarkStore {
	text = $state('');
	tree = $state<BookmarkUI[]>([]);
	offset = $state(0);

	setText(text: string) {
		this.text = text;
	}

	setTree(tree: BookmarkUI[]) {
		this.tree = tree;
	}

	setOffset(offset: number) {
		this.offset = offset;
	}

	reset() {
		this.text = '';
		this.tree = [];
		this.offset = 0;
	}
}

export const bookmarkStore = new BookmarkStore();