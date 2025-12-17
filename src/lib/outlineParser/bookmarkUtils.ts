import type { Bookmark } from "../../components/bookmark/types";
import { INDENT_UNIT, FOUR_NORM_SPACE } from "./constants";

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
        const current = item as LinkedBookmark;
        if (!current.children) current.children = [];
        last = addLinearlyToBookmarkTree(current, last);
    }
    
    cleanupParentReferences(rootBookmark);

    return rootBookmark;
}

function addLinearlyToBookmarkTree(current: LinkedBookmark, last: LinkedBookmark): LinkedBookmark {
    const currentLevel = current.level;
    const lastLevel = last.level;

    if (lastLevel === currentLevel) {
        if (!last.parent) {
             console.warn("Found node without parent while adding sibling", last);
             return current; 
        }
        addChild(last.parent, current);
    } else if (lastLevel < currentLevel) {
        addChild(last, current);
    } else {
        let parent = last.parent;
        if (!parent) {
             console.warn("Found node without parent while traversing up", last);
             return current;
        }

        const diff = lastLevel - currentLevel;
        for (let i = 0; i < diff; i++) {
            if (parent && parent.parent) {
                parent = parent.parent;
            } else if (parent && parent.level === 0) {
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

export function serializeBookmarkTree(root: Bookmark): string {
    let result = "";
    
    function traverse(node: Bookmark) {
        if (node.level > 0) {
            const pageNumStr = node.page ? node.page : "";
            result += buildLine(node.level, node.title, pageNumStr);
        }
        
        if (node.children) {
            node.children.forEach(child => traverse(child));
        }
    }

    traverse(root);
    return result;
}

function buildLine(level: number, title: string, pageNum: string): string {
    if (level < 1) return "";
    const indent = INDENT_UNIT.repeat(level - 1);
    return `${indent}${title}${FOUR_NORM_SPACE}${pageNum}\n`;
}
