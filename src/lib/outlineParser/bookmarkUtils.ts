import type {BookmarkData, BookmarkUI} from "../types/bookmark.ts";

type LinkedBookmark = BookmarkUI & {
    parent?: LinkedBookmark;
};

export function toBookmarkData(bookmark: BookmarkUI): BookmarkData {
    return {
        id: bookmark.id,
        title: bookmark.title,
        pageNum: bookmark.pageNum,
        level: bookmark.level,
        children: bookmark.children.map(toBookmarkData)
    };
}

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

function transferState(oldNode: BookmarkUI, newNode: BookmarkUI) {
    newNode.id = oldNode.id;
    if (oldNode.expanded !== undefined) {
        newNode.expanded = oldNode.expanded;
    }
}

export function reconcileTrees(oldRoots: BookmarkUI[], newRoots: BookmarkUI[]) {
    const oldList = flattenWithPath(oldRoots);
    const newList = flattenWithPath(newRoots);
    const oldByPath = new Map<string, FlattenedBookmark>();
    const oldBySignature = new Map<string, FlattenedBookmark[]>();
    const signatureCursor = new Map<string, number>();
    const usedOldNodes = new Set<BookmarkUI>();

    for (const oldItem of oldList) {
        oldByPath.set(oldItem.path, oldItem);

        const signature = bookmarkSignature(oldItem.node);
        const bucket = oldBySignature.get(signature);
        if (bucket) {
            bucket.push(oldItem);
        } else {
            oldBySignature.set(signature, [oldItem]);
        }
    }

    for (const newItem of newList) {
        const signature = bookmarkSignature(newItem.node);
        const exactMatch = takeUnused(oldBySignature.get(signature), usedOldNodes, signatureCursor, signature);
        if (exactMatch) {
            transferState(exactMatch.node, newItem.node);
            usedOldNodes.add(exactMatch.node);
            continue;
        }

        const pathMatch = oldByPath.get(newItem.path);
        if (pathMatch && !usedOldNodes.has(pathMatch.node) && isLikelySameNode(pathMatch.node, newItem.node)) {
            transferState(pathMatch.node, newItem.node);
            usedOldNodes.add(pathMatch.node);
        }
    }
}

type FlattenedBookmark = {
    node: BookmarkUI;
    path: string;
};

function flattenWithPath(nodes: BookmarkUI[], parentPath = ""): FlattenedBookmark[] {
    const result: FlattenedBookmark[] = [];

    nodes.forEach((node, index) => {
        const path = parentPath ? `${parentPath}/${index}` : String(index);
        result.push({ node, path });
        result.push(...flattenWithPath(node.children || [], path));
    });

    return result;
}

function bookmarkSignature(node: BookmarkUI): string {
    return JSON.stringify([node.level, node.title, node.pageNum || ""]);
}

function takeUnused(
    items: FlattenedBookmark[] | undefined,
    usedOldNodes: Set<BookmarkUI>,
    signatureCursor: Map<string, number>,
    signature: string
): FlattenedBookmark | undefined {
    if (!items) return undefined;

    let index = signatureCursor.get(signature) || 0;
    while (index < items.length && usedOldNodes.has(items[index].node)) {
        index++;
    }
    signatureCursor.set(signature, index + 1);

    return items[index];
}

function isLikelySameNode(oldNode: BookmarkUI, newNode: BookmarkUI): boolean {
    if (oldNode.level !== newNode.level) return false;

    const oldPage = oldNode.pageNum || "";
    const newPage = newNode.pageNum || "";
    if (oldPage && newPage && oldPage === newPage) return true;

    const oldTitle = oldNode.title.trim();
    const newTitle = newNode.title.trim();
    const shorterTitleLength = Math.min(oldTitle.length, newTitle.length);
    return shorterTitleLength >= 3 && (oldTitle.includes(newTitle) || newTitle.includes(oldTitle));
}
