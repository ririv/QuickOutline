import type { Bookmark } from '@/components/bookmark/types';

class BookmarkStore {
	text = $state('');
	tree = $state<Bookmark[]>([]);
	offset = $state(0);

	setText(text: string) {
		this.text = text;
	}

	setTree(tree: Bookmark[]) {
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