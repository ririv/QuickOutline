import type { BookmarkUI } from '@/lib/types/bookmark.ts';
import { reconcileTrees } from '@/lib/outlineParser/bookmarkUtils';
import { Method } from '@/lib/outlineParser';
import { offsetStore } from './offsetStore.svelte';

class BookmarkStore {
	text = $state('');
	tree = $state<BookmarkUI[]>([]);
	
	get offset() {
		return offsetStore.value;
	}

	set offset(val: number) {
		offsetStore.set(val);
	}
	
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

	// setOffset is removed, assign directly to .offset

	setMethod(method: Method) {
		this.method = method;
	}

	reset() {
		this.text = '';
		this.tree = [];
		// offset is managed globally
		this.method = Method.SEQ;
	}
}

export const bookmarkStore = new BookmarkStore();