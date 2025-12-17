import type { Bookmark } from "../../../components/bookmark/types";
import { BaseParser } from "../Parser";
import { createBookmark } from "../bookmarkUtils";

export class IndentParser extends BaseParser {
    private isChecked = false;
    private recognizedSingleIndent = "";
    
    // JS Regex:
    // Group 1: Indent (optional)
    // Group 2: Title (non-greedy)
    // Group 3: Page Number (optional, integer)
    // Note: [\s.]* handles dots/spaces between title and page number
    private readonly indentPattern = /^(\s*)?(.*?)[\s.]*(-?[0-9]+)?\s*$/;

    parseLine(line: string, linearBookmarkList: Bookmark[]): Bookmark {
        const match = line.match(this.indentPattern);
        
        if (match) {
            const linePrefix = match[1] || "";
            const title = match[2].trim();
            const pageNumStr = match[3];

            this.checkSingleIndentStr(linePrefix);
            const level = this.getLevelByLinePrefix(linePrefix);
            
            // In Java code: offsetPageNum = Integer.parseInt(matcher.group(3))
            // Here we keep it as string or null to match Bookmark interface
            const page = pageNumStr ? pageNumStr : null;

            return createBookmark(title, page, level);
        } else {
             throw new Error(`Bookmark format error: "${line}" is not valid.`);
        }
    }

    private checkSingleIndentStr(linePrefix: string) {
        if (!this.isChecked && linePrefix.length > 0) {
            this.recognizedSingleIndent = linePrefix;
            this.isChecked = true;
        }
    }

    private getLevelByLinePrefix(linePrefix: string): number {
        let level = 1;
        
        // If we haven't recognized an indent unit yet, we assume level 1
        // unless linePrefix itself is the first indent unit we see (handled in checkSingleIndentStr)
        
        if (this.recognizedSingleIndent.length === 0) {
            return 1;
        }

        let tempPrefix = linePrefix;
        while (tempPrefix.startsWith(this.recognizedSingleIndent)) {
            tempPrefix = tempPrefix.substring(this.recognizedSingleIndent.length);
            level++;
        }

        return level;
    }
}
