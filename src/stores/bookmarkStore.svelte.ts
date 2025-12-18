import type { BookmarkUI } from '@/components/bookmark/types';
import { reconcileTrees } from '@/lib/outlineParser/bookmarkUtils';

class BookmarkStore {
	text = $state('');
	tree = $state<BookmarkUI[]>([]);
	offset = $state(0);

	setText(text: string) {
		this.text = text;
	}

	setTree(tree: BookmarkUI[]) {
		// Try to preserve IDs and state from the old tree
		if (this.tree.length > 0 && tree.length > 0) {
			reconcileTrees(this.tree, tree);
		}
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