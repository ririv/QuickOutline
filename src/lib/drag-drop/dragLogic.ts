import type { BookmarkUI } from '../types/bookmark';
import { calculateLevelFromX } from './treeLayout';

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
    
    // 3. 确定允许的层级范围 (Max Level)
    // 默认情况下，横线层级被锁定为上一个节点的层级 (只能做兄弟，不能做子节点)。
    // 但是，如果下一个节点是子节点 (说明父节点已展开)，我们允许横线向右缩进。
    let maxLevel = refNode ? refNode.level : 1;
    if (refNode && nextNode && nextNode.level > refNode.level) {
        maxLevel = nextNode.level;
    }

    // 根据鼠标 X 坐标计算目标层级
    let targetLevel = calculateLevelFromX(mouseX);
    targetLevel = Math.max(1, Math.min(targetLevel, maxLevel));

    // 4. 解析逻辑操作
    let dropTargetId = '';
    let dropPosition: 'before' | 'after' | 'inside' = 'after';

    if (!refNode) {
        // 绝对列表最顶部
        if (nextNode) {
            dropTargetId = nextNode.id;
            dropPosition = 'before';
        }
    } else {
        // 情况 A：匹配了下方子节点的层级 (作为第一个子节点插入)
        if (nextNode && targetLevel === nextNode.level && nextNode.level > refNode.level) {
             dropTargetId = nextNode.id;
             dropPosition = 'before';
        } 
        // 情况 B：标准层级 (作为上一个节点的兄弟插入)
        else if (targetLevel === refNode.level) {
            dropTargetId = refNode.id;
            dropPosition = 'after';
        } else {
            // 情况 C：向左缩进 (Outdent)，寻找对应层级的祖先兄弟
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

/**
 * Shared hit-testing logic.
 * Finds the bookmark node info at the given screen coordinates.
 */
export function getDragTargetInfo(x: number, y: number): { id: string, rect: DOMRect, relX: number, relY: number, row: HTMLElement, container: HTMLElement } | null {
    const element = document.elementFromPoint(x, y);
    // Find the container first to catch both gaps and content rows
    const container = element?.closest('.node-container') as HTMLElement;
    if (!container) return null;

    // Retrieve the row logic data
    const id = container.dataset.id;
    const row = container.querySelector('.node-row') as HTMLElement;

    if (id && row) {
        const rect = row.getBoundingClientRect();
        return {
            id,
            rect,
            relX: x - rect.left,
            relY: y - rect.top,
            row,
            container
        };
    }
    return null;
}