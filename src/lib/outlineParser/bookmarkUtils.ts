import type { Bookmark } from "../../components/bookmark/types";

export type LinkedBookmark = Bookmark & {
    parent?: LinkedBookmark;
};

export function createBookmark(title: string, page: string | null, level: number): LinkedBookmark {
    return {
        id: crypto.randomUUID(),
        title,
        page,
        level,
        children: [],
        expanded: true
    };
}

export function createRoot(): LinkedBookmark {
    return {
        id: "root",
        title: "Outlines",
        page: null,
        level: 0,
        children: [],
        expanded: true
    };
}

export function convertListToBookmarkTree(linearList: Bookmark[]): Bookmark {
    const rootBookmark = createRoot();
    let last: LinkedBookmark = rootBookmark;

    for (const item of linearList) {
        // Convert to LinkedBookmark (just casting as we will add parent)
        const current = item as LinkedBookmark;
        // Ensure children array exists
        if (!current.children) current.children = [];
        
        last = addLinearlyToBookmarkTree(current, last);
    }
    
    // Clean up parent references before returning if we want strictly JSON-serializable tree without circles
    // Although in JS, having circular references is fine for objects, but JSON.stringify will fail.
    // The UI likely doesn't need 'parent'.
    cleanupParentReferences(rootBookmark);

    return rootBookmark;
}

function addLinearlyToBookmarkTree(current: LinkedBookmark, last: LinkedBookmark): LinkedBookmark {
    const currentLevel = current.level;
    const lastLevel = last.level;

    if (lastLevel === currentLevel) {
        // Sibling
        if (!last.parent) {
             // Should not happen if structure is valid and root is 0
             // But if we have multiple roots at level 1, last is one of them, its parent is root(0).
             console.warn("Found node without parent while adding sibling", last);
             return current; 
        }
        addChild(last.parent, current);
    } else if (lastLevel < currentLevel) {
        // Child (Next level)
        // Usually currentLevel is lastLevel + 1. 
        // If it jumps (e.g. 1 -> 3), we still add it to last.
        addChild(last, current);
    } else {
        // Back up (Return to upper level)
        // lastLevel > currentLevel
        let parent = last.parent;
        if (!parent) {
             console.warn("Found node without parent while traversing up", last);
             return current;
        }

        // We want to find the parent that effectively contains this new 'current' node.
        // The Java logic:
        // for (int dif = last.getLevel() - currentLevel; dif != 0; dif--) { parent = parent.getParent(); }
        // Basically, go up until we find a node that is one level above 'current' (level == current.level - 1)
        // OR, since we want to add as sibling to a node at 'current.level', we find the parent at 'current.level - 1'.
        
        // Let's trace Java logic carefully:
        // dif = lastLevel - currentLevel.
        // if last=2, current=1. dif=1. parent = parent(1).getParent() -> parent(0).
        // parent(0) is root. we add current(1) to parent(0). Correct.
        
        // We traverse up (lastLevel - currentLevel) times.
        const diff = lastLevel - currentLevel;
        for (let i = 0; i < diff; i++) {
            if (parent && parent.parent) {
                parent = parent.parent;
            } else if (parent && parent.level === 0) {
                 // Stop at root
                 break;
            }
        }
        
        if (parent) {
            addChild(parent, current);
        } else {
             console.warn("Could not find suitable parent for", current);
        }
    }

    return current;
}

function addChild(parent: LinkedBookmark, child: LinkedBookmark) {
    parent.children.push(child);
    child.parent = parent;
}

function cleanupParentReferences(node: LinkedBookmark) {
    delete node.parent;
    if (node.children) {
        for (const child of node.children) {
            cleanupParentReferences(child as LinkedBookmark);
        }
    }
}
