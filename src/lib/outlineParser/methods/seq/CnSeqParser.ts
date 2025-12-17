import type { BookmarkUI } from "../../../../components/bookmark/types";
import { BaseParser } from "../../Parser";
import { createBookmark } from "../../bookmarkUtils";
import { getLevelByStandardSeq } from "./Seq";
import { TWO_NORM_SPACE, NORM_SPACE } from "../../constants";

export class CnSeqParser extends BaseParser {
    // Java: ^(\s*)?(\S?\s?[零一二三四五六七八九十百千0-9]+\s?(篇|章|节|部分)|[0-9.]+)?\s*(.*?)[\s.]*(-?[0-9]+)?\s*$
    // JS:
    // Group 1: indent
    // Group 2: seq
    // Group 3: title
    // Group 4: pageNum
    // Note: I used non-capturing group for (篇|章|节|部分) inside group 2? No, Java code captures it as group 3.
    // Let's adjust groups to match my logic or just use named groups if possible, but standard regex is fine.
    
    // My Groups:
    // 1: indent
    // 2: seq (outer)
    // 3: title
    // 4: pageNum
    
    // Using [\s\u3000] for whitespace to be safe with Chinese input
    private readonly cnPattern = /^([\s\u3000]*)?(\S?[\s\u3000]?[零一二三四五六七八九十百千0-9]+[\s\u3000]?(?:篇|章|节|部分)|[0-9.]+)?[\s\u3000]*(.*?)[\s\u3000.]*(-?[0-9]+)?[\s\u3000]*$/;

    parseLine(line: string, linearBookmarkList: BookmarkUI[]): BookmarkUI {
        const match = line.match(this.cnPattern);
        
        if (match) {
            let rawSeq = match[2] || "";
            // rawSeq = rawSeq.replaceAll(NORM_SPACE, ""); 
            // NORM_SPACE is " ". Java code removes spaces from seq.
            rawSeq = rawSeq.replace(/ /g, "");

            const seq = this.standardizeSeq(rawSeq);
            const titlePart = match[3] || "";
            
            const titleWithSeq = (rawSeq + TWO_NORM_SPACE + titlePart).trim();
            const pageNumStr = match[4];
            
            const page = pageNumStr ? pageNumStr : null;
            const level = getLevelByStandardSeq(seq);

            return createBookmark(titleWithSeq, page, level);
        } else {
             throw new Error(`Bookmark format error: "${line}" is not valid.`);
        }
    }

    private standardizeSeq(rawSeq: string): string {
        const match = rawSeq.match(/[0-9.]+/);
        return match ? match[0] : "";
    }
}
