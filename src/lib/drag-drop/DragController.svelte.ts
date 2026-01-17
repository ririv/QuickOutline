import { moveNode } from '../utils/treeUtils.ts';
import type { BookmarkUI } from '../types/bookmark.ts';

export class DragController {
    draggedNodeId = $state<string | null>(null);
    dropTargetId = $state<string | null>(null);
    dropPosition = $state<'before' | 'after' | 'inside' | null>(null);
    dropTargetLevel = $state<number>(1);
    
    // Visual state
    gapNodeId = $state<string | null>(null);
    gapPosition = $state<'before' | 'after' | 'inside' | null>(null);

    // Context references (to operate on data)
    private getBookmarks: () => BookmarkUI[];
    private setBookmarks: (b: BookmarkUI[]) => void;

    constructor(getBookmarks: () => BookmarkUI[], setBookmarks: (b: BookmarkUI[]) => void) {
        this.getBookmarks = getBookmarks;
        this.setBookmarks = setBookmarks;
    }

    setDraggedNodeId(id: string | null) {
        this.draggedNodeId = id;
        if (!id) {
            this.reset();
        }
    }

    updateState(
        targetId: string | null, 
        pos: 'before' | 'after' | 'inside' | null, 
        level: number,
        visNodeId: string | null,
        visPos: 'before' | 'after' | 'inside' | null
    ) {
        this.dropTargetId = targetId;
        this.dropPosition = pos;
        this.dropTargetLevel = level;
        this.gapNodeId = visNodeId;
        this.gapPosition = visPos;
    }

    reset() {
        this.dropTargetId = null;
        this.dropPosition = null;
        this.gapNodeId = null;
        this.gapPosition = null;
    }

    move(draggedId: string, targetId: string, position: 'before' | 'after' | 'inside') {
        const bookmarks = this.getBookmarks();
        moveNode(bookmarks, draggedId, targetId, position);
        this.setBookmarks([...bookmarks]); // Trigger update
    }
}
