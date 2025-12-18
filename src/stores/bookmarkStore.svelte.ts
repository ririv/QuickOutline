import type { BookmarkUI } from '@/components/bookmark/types';
import { reconcileTrees } from '@/lib/outlineParser/bookmarkUtils';
import { Method } from '@/lib/outlineParser';

class BookmarkStore {
	text = $state('');
	tree = $state<BookmarkUI[]>([]);
	offset = $state(0);
	method = $state<Method>(Method.SEQ);

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

	setMethod(method: Method) {
		this.method = method;
	}

	reset() {
		this.text = '';
		this.tree = [];
		this.offset = 0;
		this.method = Method.SEQ;
	}
}

export const bookmarkStore = new BookmarkStore();