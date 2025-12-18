import * as pdfjsLib from 'pdfjs-dist';
import type { BookmarkUI } from '@/lib/types/bookmark.ts';

// Recursively converts PDF.js outline format to our BookmarkUI format
async function convertToBookmarkUI(pdfDocument: pdfjsLib.PDFDocumentProxy, pdfjsOutline: any[], level: number = 0): Promise<BookmarkUI[]> {
    const bookmarks: BookmarkUI[] = [];
    if (!pdfjsOutline || pdfjsOutline.length === 0) {
        return bookmarks;
    }

    for (const item of pdfjsOutline) {
        let pageNum: string | null = null;
        
        try {
            let destArray = null;
            if (typeof item.dest === 'string') {
                destArray = await pdfDocument.getDestination(item.dest);
            } else if (Array.isArray(item.dest)) {
                destArray = item.dest;
            }

            if (destArray && destArray.length > 0) {
                const pageRef = destArray[0];
                if (typeof pageRef === 'object' && pageRef !== null) {
                    const pageIndex = await pdfDocument.getPageIndex(pageRef);
                    pageNum = String(pageIndex + 1);
                } else if (typeof pageRef === 'number') {
                    pageNum = String(pageRef + 1);
                }
            }
        } catch (e) {
            console.warn(`Failed to resolve destination for bookmark '${item.title}':`, e);
        }

        const newBookmark: BookmarkUI = {
            id: crypto.randomUUID(), 
            title: item.title,
            pageNum: pageNum, 
            level: level + 1, 
            children: [],
            expanded: true 
        };

        if (item.items && item.items.length > 0) {
            newBookmark.children = await convertToBookmarkUI(pdfDocument, item.items, level + 1);
        }
        bookmarks.push(newBookmark);
    }
    return bookmarks;
}

export async function getBookmarks(pdfDocument: pdfjsLib.PDFDocumentProxy): Promise<BookmarkUI[]> {
    const outline = await pdfDocument.getOutline();
    if (!outline) {
        return [];
    }
    return convertToBookmarkUI(pdfDocument, outline);
}
