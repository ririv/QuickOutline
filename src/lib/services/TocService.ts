import type { BookmarkUI } from "@/lib/types/bookmark";
import { INDENT_UNIT } from "@/lib/outlineParser/constants";

export class TocService {
    /**
     * Serializes a bookmark tree into the specific text format required by the TOC Editor.
     * Format: Indent + Title + " ... " + PageNum
     */
    serializeForTocEditor(root: BookmarkUI): string {
        let result = "";
        
        const traverse = (node: BookmarkUI) => {
            if (node.level > 0) {
                const pageNumStr = node.pageNum ? node.pageNum : "";
                result += this.buildLineForTocEditor(node.level, node.title, pageNumStr);
            }
            
            if (node.children) {
                node.children.forEach(child => traverse(child));
            }
        };

        traverse(root);
        return result;
    }

    private buildLineForTocEditor(level: number, title: string, pageNum: string): string {
        if (level < 1) return "";
        const indent = INDENT_UNIT.repeat(level - 1);
        return `${indent}${title} ... ${pageNum}\n`;
    }
}

export const tocService = new TocService();
