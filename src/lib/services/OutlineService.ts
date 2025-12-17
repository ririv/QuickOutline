import { rpc } from '@/lib/api/rpc';
import { serializeBookmarkTree } from '@/lib/outlineParser';
import { tocStore } from '@/stores/tocStore.svelte';

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
                const { loadPdfFromPath, getBookmarks } = await import('@/lib/pdfjs');
                
                console.log("[OutlineService] Loading outline via PDF.js...");
                // 1. Load PDF
                const pdf = await loadPdfFromPath(path);
                // 2. Get Bookmarks
                const bookmarks = await getBookmarks(pdf);
                
                // 3. Serialize to Text (for the editor)
                // Wrap bookmarks in a virtual root
                const root: any = { 
                    id: 'virtual-root',
                    title: 'Outlines',
                    level: 0,
                    children: bookmarks,
                    pageNum: null
                };
                outline = serializeBookmarkTree(root);
                
                // Cleanup
                pdf.destroy();

            } else {
                // Fallback to lopdf (Rust)
                console.log("[OutlineService] Loading outline via lopdf (Rust)...");
                const dto = await rpc.getOutlineAsBookmark(path, 0);
                outline = serializeBookmarkTree(dto);
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
