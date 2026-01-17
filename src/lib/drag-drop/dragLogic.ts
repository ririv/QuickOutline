import type { BookmarkUI } from '../types/bookmark.ts';

export interface DragState {
    dropTargetId: string | null;
    dropPosition: 'before' | 'after' | 'inside' | null;
    dropTargetLevel: number;
    gapNodeId: string | null;
    gapPosition: 'before' | 'after' | 'inside' | null;
}

export function calculateDragState(
    mouseY: number,
    nodeHeight: number,
    mouseX: number, 
    hitNodeId: string,
    draggedNodeId: string,
    visibleNodes: BookmarkUI[]
): DragState | null {
    const TOP_ZONE = 0.25;
    const BOTTOM_ZONE = 0.75;
    const INDENT_WIDTH = 20;
    const INDENT_BASE = 4;

    const relY = mouseY / nodeHeight;
    const hitIndex = visibleNodes.findIndex(n => n.id === hitNodeId);
    if (hitIndex === -1) return null;

    // 1. Inside Zone Handling
    if (relY > TOP_ZONE && relY < BOTTOM_ZONE) {
        if (hitNodeId === draggedNodeId) return null;
        const hitNode = visibleNodes[hitIndex];
        return {
            dropTargetId: hitNodeId,
            dropPosition: 'inside',
            dropTargetLevel: hitNode.level + 1,
            gapNodeId: hitNodeId,
            gapPosition: 'inside'
        };
    }

    // 2. Identify the Physical Gap
    let refNode: BookmarkUI | null = null;
    let nextNode: BookmarkUI | null = null;
    let gapNodeId = hitNodeId;
    let gapPosition: 'before' | 'after' = 'after';

    if (relY <= TOP_ZONE) {
        if (hitIndex > 0) refNode = visibleNodes[hitIndex - 1];
        nextNode = visibleNodes[hitIndex];
        if (refNode) {
            gapNodeId = refNode.id;
            gapPosition = 'after';
        } else {
            gapNodeId = nextNode.id;
            gapPosition = 'before';
        }
    } else {
        refNode = visibleNodes[hitIndex];
        if (hitIndex < visibleNodes.length - 1) nextNode = visibleNodes[hitIndex + 1];
        gapNodeId = refNode.id;
        gapPosition = 'after';
    }

    // Filter interaction with self (Inside only)
    // For Gap interactions (Before/After), we allow returning a state even if it's a no-op
    // This ensures visual feedback (the line) is always visible, which is better UX.
    // The actual 'move' operation will be a no-op if logic determines start === end.
    
    // 3. Determine Allowed Level Range at this Gap
    // Min Level is always 1.
    // Max Level is RefNode.level + 1 (can only be 1 deeper than the node before the gap).
    // If no refNode (top of list), Max Level is 1.
    const maxLevel = refNode ? refNode.level + 1 : 1;
    
    // Calculate Target Level based on X
    let targetLevel = Math.floor((mouseX - INDENT_BASE + (INDENT_WIDTH / 2)) / INDENT_WIDTH) + 1;
    targetLevel = Math.max(1, Math.min(targetLevel, maxLevel));

    // 4. Resolve Logical Operation
    let dropTargetId = '';
    let dropPosition: 'before' | 'after' | 'inside' = 'after';

    if (!refNode) {
        // Very top
        if (nextNode) {
            dropTargetId = nextNode.id;
            dropPosition = 'before';
        }
    } else {
        // Standard Gap
        if (targetLevel === refNode.level) {
            dropTargetId = refNode.id;
            dropPosition = 'after';
        } else if (targetLevel === refNode.level + 1) {
            if (nextNode) {
                dropTargetId = nextNode.id;
                dropPosition = 'before';
            } else {
                dropTargetId = refNode.id;
                dropPosition = 'inside';
            }
        } else {
            // Outdent: Find the ancestor sibling
            // Search back for the last node that had this level.
            let found = false;
            for (let i = hitIndex; i >= 0; i--) {
                if (visibleNodes[i].level === targetLevel) {
                    dropTargetId = visibleNodes[i].id;
                    dropPosition = 'after';
                    found = true;
                    break;
                }
            }
            if (!found) {
                dropTargetId = refNode.id;
                dropPosition = 'after';
            }
        }
    }

    return {
        dropTargetId,
        dropPosition,
        dropTargetLevel: targetLevel,
        gapNodeId,
        gapPosition
    };
}