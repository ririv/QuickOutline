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

const parseCache = new Map<string, TocLineParsed | null>();
const CACHE_LIMIT = 10000;

/**
 * Parses a TOC line into its components.
 * @param line The raw text line
 * @returns Parsed object or null if not a valid TOC line
 */
export function parseTocLine(line: string): TocLineParsed | null {
    if (parseCache.has(line)) {
        return parseCache.get(line) || null;
    }

    const match = line.match(TOC_LINE_REGEX);
    if (!match) {
        if (parseCache.size < CACHE_LIMIT) parseCache.set(line, null);
        return null;
    }

    const title = match[1];
    const separator = match[2];
    const pageInfo = match[3].trim();

    // Parse Page Info: "DisplayPage [| LinkTarget]"
    let displayPage = "";
    let linkTarget = "";
    let hasExplicitLink = false;

    if (pageInfo.includes("|")) {
        const parts = pageInfo.split("|");
        displayPage = parts[0].trim();
        linkTarget = parts[1].trim();
        hasExplicitLink = true;
    } else {
        displayPage = pageInfo.trim();
        linkTarget = displayPage;
        hasExplicitLink = false;
    }

    const result: TocLineParsed = {
        title,
        separator,
        pageInfo,
        displayPage,
        linkTarget,
        hasExplicitLink
    };

    if (parseCache.size < CACHE_LIMIT) {
        parseCache.set(line, result);
    }
    
    return result;
}
