import {FOUR_NORM_SPACE, INDENT_UNIT} from "@/lib/outlineParser/constants.ts";
import type {BookmarkUI} from "@/lib/types/bookmark.ts";

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