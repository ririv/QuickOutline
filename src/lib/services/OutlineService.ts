import { rpc } from '@/lib/api/rpc';
import { tocStore } from '@/stores/tocStore.svelte';
import { tocService } from './TocService';

export class OutlineService {
    // Engine Switch (Internal Config)
    // Options: 'lopdf' (Rust Backend) | 'pdfjs' (Frontend JS)
    private engine: 'lopdf' | 'pdfjs' = 'pdfjs';

    async loadOutline(path: string): Promise<string | undefined> {
        if (!path) return;

        try {
            let outline = '';
            
            if (this.engine === 'pdfjs') {
                // Dynamic import to keep bundle size optimized
                const { loadPdfFromPath, getBookmarks, getPageLabels } = await import('@/lib/pdfjs');
                
                console.log("[OutlineService] Loading outline via PDF.js...");
                // 1. Load PDF
                const pdf = await loadPdfFromPath(path);
                // 2. Get Bookmarks
                const bookmarks = await getBookmarks(pdf);
                // 3. Get Page Labels
                const labels = await getPageLabels(pdf);

                // 4. Post-process bookmarks to include labels
                if (labels) {
                    const updatePageNums = (nodes: any[]) => {
                        for (const node of nodes) {
                            if (node.pageNum) {
                                const physical = parseInt(node.pageNum, 10);
                                if (!isNaN(physical) && physical >= 1 && physical <= labels.length) {
                                    const label = labels[physical - 1];
                                    // If label matches physical page (e.g. "5" == 5), keep it simple so Offset works.
                                    // Otherwise (e.g. "iv" != 4), use explicit target to ensure accuracy.
                                    if (label === String(physical)) {
                                        node.pageNum = label;
                                    } else {
                                        node.pageNum = `${label} | #${physical}`;
                                    }
                                }
                            }
                            if (node.children) {
                                updatePageNums(node.children);
                            }
                        }
                    };
                    updatePageNums(bookmarks);
                }
                
                // 5. Serialize to Text (for the editor)
                // Wrap bookmarks in a virtual root
                const root: any = { 
                    id: 'virtual-root',
                    title: 'Outlines',
                    level: 0,
                    children: bookmarks,
                    pageNum: null
                };
                outline = tocService.serializeForTocEditor(root);
                
                // Cleanup
                pdf.destroy();

            } else {
                // Fallback to lopdf (Rust)
                console.log("[OutlineService] Loading outline via lopdf (Rust)...");
                const dto = await rpc.getOutlineAsBookmark(path, 0);
                outline = tocService.serializeForTocEditor(dto);
            }

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
