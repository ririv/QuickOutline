import type { BookmarkUI } from "../../../types/bookmark.ts";
import { BaseParser } from "../../Parser";
import { createBookmark } from "../../bookmarkUtils";
import { getLevelByStandardSeq } from "./Seq";
import { TWO_NORM_SPACE } from "../../constants";

export class StdSeqParser extends BaseParser {
    // Java: ^(\s*)?(\d+(\.\d+)*\.?)?\s*(.*?)[\s.]*(-?[0-9]+)?\s*$
    // JS:
    // Group 1: indent (unused)
    // Group 2: seq (digits.digits...)
    // Group 4: title
    // Group 5: pageNum
    private readonly stdPattern = /^(\s*)?(\d+(?:\.\d+)*\.?)?\s*(.*?)["\s.]*(-?[0-9]+)?\s*$/;

    parseLine(line: string, linearBookmarkList: BookmarkUI[]): BookmarkUI {
        const match = line.match(this.stdPattern);
        
        if (match) {
            const seq = match[2] || "";
            // Java: String titleWithSeq = (seq + TWO_NORM_SPACE + matcher.group(4)).trim();
            const rawTitle = match[3] || ""; // Group 4 in Java is 3rd capturing group if we count correctly? 
            // Java Groups:
            // 1: (\s*)
            // 2: (\d+(\.\d+)*\.?)"  -- Nested group 3 is (\.\d+)
            // 4: (.*?)
            // 5: (-?[0-9]+)
            
            // My JS regex groups:
            // 1: (\s*)
            // 2: (\d+(?:\.\d+)*\.?)
            // 3: (.*?)
            // 4: (-?[0-9]+)
            // Note: I used non-capturing group for inner dot digits `(?:\.\d+)*`. So indexes shift.
            
            // Let's verify indexes:
            // match[0]: full string
            // match[1]: indent
            // match[2]: seq
            // match[3]: title
            // match[4]: pageNum
            
            const titlePart = match[3] || "";
            const pageNumStr = match[4];

            // Reconstruct title with seq prefix if seq exists
            let titleWithSeq = titlePart;
            if (seq) {
                 titleWithSeq = (seq + TWO_NORM_SPACE + titlePart).trim();
            } else {
                 titleWithSeq = titlePart.trim();
            }

            const page = pageNumStr ? pageNumStr : null;
            const level = getLevelByStandardSeq(seq);

            return createBookmark(titleWithSeq, page, level);
        } else {
             throw new Error(`Bookmark format error: "${line}" is not valid.`);
        }
    }
}
