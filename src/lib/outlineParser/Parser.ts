// Ported from parser.java
import type { BookmarkUI } from "../../components/bookmark/types";

export interface Parser {
    /**
     * Parses a single line of text into a Bookmark object.
     * @param line The text line to parse.
     * @param linearBookmarkList The list of bookmarks parsed so far (for context).
     */
    parseLine(line: string, linearBookmarkList: BookmarkUI[]): BookmarkUI;

    /**
     * Parses a list of text lines into a list of Bookmark objects.
     * @param text The list of text lines.
     */
    parse(text: string[]): BookmarkUI[];
}

/**
 * Abstract base class to provide the default `parse` implementation.
 */
export abstract class BaseParser implements Parser {
    abstract parseLine(line: string, linearBookmarkList: BookmarkUI[]): BookmarkUI;

    parse(text: string[]): BookmarkUI[] {
        const linearBookmarkList: BookmarkUI[] = [];
        for (const line of text) {
            try {
                const current = this.parseLine(line, linearBookmarkList);
                linearBookmarkList.push(current);
            } catch (e) {
                // In Java, BookmarkFormatException is thrown.
                // We propagate the error so the UI can handle it or the parser stops.
                throw e;
            }
        }
        return linearBookmarkList;
    }
}
