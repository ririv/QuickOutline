import type { BookmarkUI } from '../types/bookmark';

export function findNode(
    nodes: BookmarkUI[],
    id: string,
    parent: BookmarkUI | null = null
): { node: BookmarkUI; parent: BookmarkUI | null; index: number; list: BookmarkUI[] } | null {
    for (let i = 0; i < nodes.length; i++) {
        if (nodes[i].id === id) {
            return { node: nodes[i], parent, index: i, list: nodes };
        }
        if (nodes[i].children) {
            const found = findNode(nodes[i].children, id, nodes[i]);
            if (found) return found;
        }
    }
    return null;
}

export function isDescendant(parent: BookmarkUI, childId: string): boolean {
    if (parent.id === childId) return true;
    if (parent.children) {
        for (const child of parent.children) {
             if (isDescendant(child, childId)) return true;
        }
    }
    return false;
}

export function moveNode(
    tree: BookmarkUI[],
    draggedId: string,
    targetId: string,
    position: 'before' | 'after' | 'inside'
): BookmarkUI[] {
    // 1. Find dragged node
    const draggedResult = findNode(tree, draggedId);
    if (!draggedResult) return tree;

    // 2. Find target node
    const targetResult = findNode(tree, targetId);
    if (!targetResult) return tree;

    const { node: draggedNode, list: sourceList, index: sourceIndex } = draggedResult;
    const { node: targetNode, parent: targetParent, list: targetList, index: targetIndex } = targetResult;

    // 3. Check validity
    if (draggedId === targetId) return tree;
    if (isDescendant(draggedNode, targetId)) {
        console.warn("Cannot move node into its own descendant");
        return tree;
    }

    // 4. Remove dragged node
    sourceList.splice(sourceIndex, 1);

    // Adjust targetIndex if needed
    let newTargetIndex = targetIndex;
    if (sourceList === targetList && sourceIndex < targetIndex) {
        newTargetIndex--;
    }

    // 5. Insert
    if (position === 'inside') {
        if (!targetNode.children) targetNode.children = [];
        targetNode.children.push(draggedNode);
        targetNode.expanded = true;
        
        // Update level
        updateLevel(draggedNode, targetNode.level + 1);
    } else {
        // Before or After
        const listToInsert = targetList; 
        const insertIndex = position === 'before' ? newTargetIndex : newTargetIndex + 1;
        listToInsert.splice(insertIndex, 0, draggedNode);
        
        // Update level
        const newParentLevel = targetParent ? targetParent.level : 0; // If root (no parent), effectively level 0 parent implies node is level 1? 
        // Wait, if targetParent is null, we are at root list.
        // Existing nodes at root list should be level 1 (usually).
        // Let's verify `BookmarkNode` logic. `style="--level: {bookmark.level}"`.
        // If I move to root list, I should match the level of `targetNode`.
        updateLevel(draggedNode, targetNode.level); 
    }

    return tree;
}

function updateLevel(node: BookmarkUI, newLevel: number) {
    node.level = newLevel;
    if (node.children) {
        for (const child of node.children) {
            updateLevel(child, newLevel + 1);
        }
    }
}
