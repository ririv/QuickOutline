import * as pdfjsLib from 'pdfjs-dist';
import type { BookmarkUI } from '@/components/bookmark/types';

// Recursively converts PDF.js outline format to our BookmarkUI format
// pdfjsOutline parameter type will be inferred from pdfjsLib.PDFDocumentProxy['getOutline'] result
function convertToBookmarkUI(pdfjsOutline: any[], level: number = 0): BookmarkUI[] {
    const bookmarks: BookmarkUI[] = [];
    if (!pdfjsOutline || pdfjsOutline.length === 0) {
        return bookmarks;
    }

    for (const item of pdfjsOutline) {
        let pageNum: string | null = null;
        // PDF.js's dest (destination) can be a string (named dest) or an array
        // Common format for array dest: [pageRef, /XYZ, left, top, zoom]
        // pageRef is usually an object {num: number, gen: number} or sometimes a page index (0-based)
        if (Array.isArray(item.dest) && item.dest.length > 0) {
            const pageRef = item.dest[0]; // This is the page reference
            if (typeof pageRef === 'object' && pageRef !== null && 'num' in pageRef && 'gen' in pageRef) {
                // This is a page dictionary reference. We can't get page number directly here.
                // We would need pdfDocument.getPageIndex(pageRef) which is async.
                // For now, we will leave it as null. A more complete implementation would resolve this.
                console.warn("PDF.js Named Destination (object ref) encountered, not resolved to page number:", item.dest);
            } else if (typeof pageRef === 'number') {
                // Sometimes it's a direct 0-based page index.
                pageNum = String(pageRef + 1); // PDF.js is 0-based, convert to 1-based
            }
        } else if (typeof item.dest === 'string') {
            // This is a named destination string. Requires pdfDocument.getDestination(item.dest) then resolve.
            console.warn("PDF.js Named Destination (string) encountered, not resolved to page number:", item.dest);
        }
        

        const newBookmark: BookmarkUI = {
            id: crypto.randomUUID(), // Generate a unique ID for frontend
            title: item.title,
            pageNum: pageNum, // Page number (1-based string)
            level: level + 1, // PDF.js outline root is implicitly level 0. Our bookmarks start at 1.
            children: [],
            expanded: true // Default to expanded
        };

        if (item.items && item.items.length > 0) {
            newBookmark.children = convertToBookmarkUI(item.items, level + 1);
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
    return convertToBookmarkUI(outline);
}
