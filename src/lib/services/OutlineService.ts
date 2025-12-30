import { getOutlineAsBookmark } from '@/lib/api/rust_pdf';
import { tocStore } from '@/stores/tocStore.svelte';
import { tocService } from './TocService';
import type { BookmarkUI } from '@/lib/types/bookmark';

export class OutlineService {
    // Engine Options: 'lopdf' (Rust Backend) | 'pdfjs' (Frontend JS)
    private engine: 'pdfjs' | 'lopdf' = 'pdfjs';

    /**
     * Common method to fetch and process bookmarks from a PDF.
     */
    async fetchBookmarks(path: string): Promise<BookmarkUI> {
        const { docStore } = await import('@/stores/docStore.svelte');
        
        if (this.engine === 'pdfjs') {
            const { getBookmarks, getPageLabels } = await import('@/lib/pdfjs');
            
            const pdf = docStore.currentFilePath === path ? docStore.pdfDoc : null;
            if (!pdf) {
                console.error("[OutlineService] Document not available in docStore");
                throw new Error("Document not loaded");
            }

            console.log("[OutlineService] Fetching via shared PDF.js instance...");
            const bookmarks = await getBookmarks(pdf) as BookmarkUI[];
            const labels = await getPageLabels(pdf);

            if (labels) {
                const updatePageNums = (nodes: BookmarkUI[]) => {
                    for (const node of nodes) {
                        if (node.pageNum) {
                            const physical = parseInt(node.pageNum, 10);
                            if (!isNaN(physical) && physical >= 1 && physical <= labels.length) {
                                const label = labels[physical - 1];
                                node.pageNum = label;
                            }
                        }
                        if (node.children) updatePageNums(node.children);
                    }
                };
                updatePageNums(bookmarks);
            }
            
            const root: BookmarkUI = { 
                id: 'virtual-root',
                title: 'Outlines',
                level: 0,
                children: bookmarks,
                pageNum: null
            };
            return root;
        } else {
            console.log("[OutlineService] Fetching via lopdf (Rust)...");
            return await getOutlineAsBookmark(path, 0) as BookmarkUI;
        }
    }

    async loadOutline(path: string): Promise<string | undefined> {
        if (!path) return;

        try {
            const dto = await this.fetchBookmarks(path);
            const outline = tocService.serializeForTocEditor(dto);

            // Initialize store with new file and default config
            tocStore.setFile(path, outline || '');
            
            return outline;
        } catch (e) {
            console.error("[OutlineService] Failed to load outline", e);
            throw e;
        }
    }
}

export const outlineService = new OutlineService();