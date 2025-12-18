import type { BookmarkUI } from "../../components/bookmark/types";
import { INDENT_UNIT, FOUR_NORM_SPACE } from "./constants";

export type LinkedBookmark = BookmarkUI & {
    parent?: LinkedBookmark;
};

export function createBookmark(title: string, page: string | null, level: number): LinkedBookmark {
    return {
        id: crypto.randomUUID(),
        title,
        pageNum: page,
        level,
        children: [],
        expanded: true
    };
}

export function createRoot(): LinkedBookmark {
    return {
        id: "root",
        title: "Outlines",
        pageNum: null,
        level: 0,
        children: [],
        expanded: true
    };
}

export function convertListToBookmarkTree(linearList: BookmarkUI[]): BookmarkUI {
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

export function serializeBookmarkTree(root: BookmarkUI): string {
    let result = "";
    
    function traverse(node: BookmarkUI) {
        if (node.level > 0) {
            const pageNumStr = node.pageNum ? node.pageNum : "";
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

function flatten(nodes: BookmarkUI[]): BookmarkUI[] {
    const result: BookmarkUI[] = [];
    function traverse(list: BookmarkUI[]) {
        for (const node of list) {
            result.push(node);
            if (node.children) traverse(node.children);
        }
    }
    traverse(nodes);
    return result;
}

function isSameContent(a: BookmarkUI, b: BookmarkUI): boolean {
    return a.title === b.title && a.pageNum === b.pageNum && a.level === b.level;
}

function transferState(oldNode: BookmarkUI, newNode: BookmarkUI) {
    newNode.id = oldNode.id;
    if (oldNode.expanded !== undefined) {
        newNode.expanded = oldNode.expanded;
    }
}

export function reconcileTrees(oldRoots: BookmarkUI[], newRoots: BookmarkUI[]) {
    const oldList = flatten(oldRoots);
    const newList = flatten(newRoots);
    
    const m = oldList.length;
    const n = newList.length;
    
    // Costs (local to reconcileTrees)
    const INS_DEL_COST = 2;
    const MODIFY_COST = 1;

    function getEditCost(oldNode: BookmarkUI, newNode: BookmarkUI): number {
        if (oldNode.level !== newNode.level) {
            return Infinity; // Cannot match across levels
        }
        return isSameContent(oldNode, newNode) ? 0 : MODIFY_COST;
    }

    // dp[i][j] stores the minimum edit distance between oldList[0..i] and newList[0..j]
    const dp: number[][] = Array.from({ length: m + 1 }, () => Array(n + 1).fill(0));

    // Initialize base cases
    for (let i = 0; i <= m; i++) dp[i][0] = i * INS_DEL_COST;
    for (let j = 0; j <= n; j++) dp[0][j] = j * INS_DEL_COST;

    for (let i = 1; i <= m; i++) {
        for (let j = 1; j <= n; j++) {
            const oldNode = oldList[i-1];
            const newNode = newList[j-1];
            
            const matchCost = getEditCost(oldNode, newNode);
            
            dp[i][j] = Math.min(
                dp[i-1][j] + INS_DEL_COST, // Delete from old
                dp[i][j-1] + INS_DEL_COST, // Insert into new
                dp[i-1][j-1] + matchCost   // Match or Modify
            );
        }
    }

    // Backtrack to apply state transfer for Matched or Modified nodes
    let i = m, j = n;
    while (i > 0 && j > 0) {
        const oldNode = oldList[i-1];
        const newNode = newList[j-1];
        
        const matchCost = getEditCost(oldNode, newNode);
        const currentScore = dp[i][j];
        
        // Prioritize diagonal (Match/Modify) if the score matches
        if (matchCost !== Infinity && currentScore === dp[i-1][j-1] + matchCost) {
            // We found a correspondence!
            // Whether it's an exact match (0) or a modification (1), we reuse the ID.
            transferState(oldNode, newNode);
            i--;
            j--;
        } else if (currentScore === dp[i-1][j] + INS_DEL_COST) {
            // Deletion from old list (oldNode is gone)
            i--;
        } else {
            // Insertion into new list (newNode is fresh)
            j--;
        }
    }
}
