import type { Bookmark } from "../../../../components/bookmark/types";
import { BaseParser } from "../../Parser";
import { createBookmark } from "../../bookmarkUtils";
import { getLevelByStandardSeq } from "./Seq";

export class EnSeqParser extends BaseParser {
    private readonly keywords = ["Chapter", "Section", "Part", "Appendix", "Label"];

    parseLine(line: string, linearBookmarkList: Bookmark[]): Bookmark {
        const trimmedLine = line.trim();
        const pageNumIndex = this.findPageNumberIndex(trimmedLine);
        
        let titleWithSeq: string;
        let pageNumStr: string | null = null;

        if (pageNumIndex !== -1) {
            titleWithSeq = trimmedLine.substring(0, pageNumIndex).trim();
            pageNumStr = trimmedLine.substring(pageNumIndex).trim();
        } else {
            titleWithSeq = trimmedLine;
            pageNumStr = null;
        }

        // indent is calculated but unused in Java code? 
        // Java: String indent = getIndentation(line); 
        // It is called but 'indent' variable is not used in Java EnSeq logic. 
        // Logic relies on seq or previous level.

        const [seq, title] = this.divideTitle(titleWithSeq);
        
        // Use 'title' ? Java code returns `new Bookmark(title, ...)` BUT wait.
        // Java code:
        // String[] titleInfo = divideTitle(titleWithSeq);
        // if titleInfo != null: seq=titleInfo[0], title=titleInfo[1].
        // else: seq="", title=titleWithSeq.
        // return new Bookmark(title, ...)
        
        // This seems to DROP the sequence (Chapter 1) from the final bookmark title if divideTitle succeeds?
        // Let's check Java Code again.
        // Yes: return new Bookmark(title, ...);
        // If "Chapter 1 Introduction", seq="Chapter 1", title="Introduction".
        // Bookmark title becomes "Introduction".
        // This might be intended for "EnSeq" style where structure defines Chapter 1.
        // But if user wants to keep "Chapter 1", this logic removes it.
        // I will stick to Java logic for now.
        
        let level: number;
        if (!seq) {
            if (linearBookmarkList.length === 0) {
                level = 1;
            } else {
                level = linearBookmarkList[linearBookmarkList.length - 1].level;
            }
        } else {
            level = getLevelByStandardSeq(seq);
        }

        return createBookmark(title, pageNumStr, level);
    }

    private findPageNumberIndex(line: string): int {
        if (line.length === 0) return -1;
        // Check if ends with digit
        if (!/\d/.test(line[line.length - 1])) {
            return -1;
        }

        for (let i = line.length - 1; i >= 0; i--) {
            if (!/\d/.test(line[i])) {
                return i + 1;
            }
        }
        return -1;
    }

    private divideTitle(title: string): [string, string] {
        for (const keyword of this.keywords) {
            if (title.startsWith(keyword)) {
                const keywordEndIndex = keyword.length;
                const remaining = title.substring(keywordEndIndex).trim();
                
                const firstSpace = remaining.indexOf(' ');
                if (firstSpace !== -1) {
                    const seq = keyword + " " + remaining.substring(0, firstSpace);
                    const titleWithoutSeq = remaining.substring(firstSpace).trim();
                    return [seq, titleWithoutSeq];
                } else {
                    return [keyword, remaining]; // No title, just keyword + remaining (which might be number)
                }
            }
        }

        // Handle implicit cases like "16.4.2 Testing Bayesian Networks"
        const firstSpace = title.indexOf(' ');
        if (firstSpace !== -1) {
            const potentialSeq = title.substring(0, firstSpace);
            // Verify if it looks like a seq (digits and dots)? Java code doesn't verify, just assumes.
            const chapterTitle = title.substring(firstSpace).trim();
            return [potentialSeq, chapterTitle];
        }

        // Cannot divide
        return ["", title]; 
    }
}

// Helper type for int
type int = number;
