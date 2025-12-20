/**
 * Common Regular Expression and Parser for TOC Line Format.
 * Format: [Title] ... [PageInfo]
 * PageInfo: [DisplayPage] [<LinkTarget>]?
 */

// Regex Breakdown:
// ^(.*?)           -> Group 1: Title (non-greedy)
// (\s*\.{3,}\s*)   -> Group 2: Separator (at least 3 dots, surrounded by optional whitespace)
// (.*)$            -> Group 3: Page Info (rest of the line)
// Note: We use the LAST occurrence of "..." conceptually, but JS regex runs left-to-right.
// To ensure we split at the right place if there are multiple "...", usually the last one is preferred,
// but for simplicity and performance in live editor, the first valid match is often used.
// However, the previous logic used `lastIndexOf` in generator. Let's try to standardize.
// A regex that matches the *last* "..." is harder.
// Let's stick to the regex we used in plugins which works well: /^(.*?)(\s*\.{3,}\s*)(.*)$/
// This matches the *first* "..." sequence. If a title contains "...", user should escape it or we accept this limitation.
export const TOC_LINE_REGEX = /^(.*?)(\s*\.{3,}\s*)(.*)$/;

export interface TocLineParsed {
    title: string;
    separator: string; // The " ... " part
    pageInfo: string;  // The raw page info string
    displayPage: string; // The part to show (e.g. "5")
    linkTarget: string; // The explicit target (e.g. "#15") or same as displayPage
    hasExplicitLink: boolean;
}

/**
 * Parses a TOC line into its components.
 * @param line The raw text line
 * @returns Parsed object or null if not a valid TOC line
 */
export function parseTocLine(line: string): TocLineParsed | null {
    const match = line.match(TOC_LINE_REGEX);
    if (!match) return null;

    const title = match[1];
    const separator = match[2];
    const pageInfo = match[3].trim();

    // Parse Page Info: "DisplayPage [<LinkTarget>]"
    // Regex: Start with non-whitespace/non-< chars, optionally followed by <...>
    // e.g. "5" -> match[1]="5", match[2]=undefined
    // e.g. "5 <#15>" -> match[1]="5", match[2]="#15"
    // e.g. "<toc:1>" -> match[1]=undefined (if strict) or need handling
    
    let displayPage = pageInfo;
    let linkTarget = pageInfo;
    let hasExplicitLink = false;

    // Try to extract display page and bracketed link
    const infoMatch = pageInfo.match(/^([^\s<]+)(?:\s*<([^>]+)>)?/);
    
    if (infoMatch) {
        displayPage = infoMatch[1];
        if (infoMatch[2]) {
            linkTarget = infoMatch[2].trim();
            hasExplicitLink = true;
        } else {
            linkTarget = displayPage;
        }
    } else if (pageInfo.startsWith("<") && pageInfo.endsWith(">")) {
        // Case: Only link provided, e.g. "<toc:1>"
        // Display page is empty or maybe we treat the whole thing as link?
        // Let's say display page is empty string (visual gap)
        displayPage = ""; 
        linkTarget = pageInfo.slice(1, -1).trim();
        hasExplicitLink = true;
    }

    return {
        title,
        separator,
        pageInfo,
        displayPage,
        linkTarget,
        hasExplicitLink
    };
}
