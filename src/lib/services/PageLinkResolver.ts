/**
 * Configuration for resolving page links.
 */
export interface LinkResolverConfig {
    pageLabels: string[] | null; // Page labels from the document
    offset: number;          // Global page offset (user defined)
    insertPos: number;       // Where the TOC is inserted
}

// Cache for page label lookups to avoid O(N) indexOf calls repeatedly
const pageLabelsIndexCache = new WeakMap<string[], Map<string, number>>();

function getPageLabelIndex(labels: string[], label: string): number {
    let map = pageLabelsIndexCache.get(labels);
    if (!map) {
        map = new Map();
        for (let i = 0; i < labels.length; i++) {
            // Map stores the *first* occurrence index, mirroring indexOf behavior
            if (!map.has(labels[i])) {
                map.set(labels[i], i);
            }
        }
        pageLabelsIndexCache.set(labels, map);
    }
    const idx = map.get(label);
    return idx !== undefined ? idx : -1;
}

interface ResolutionContext {
    offset: number;
    pageLabels: string[] | null;
}

/**
 * Core parsing logic for page targets.
 * Supports: # (Physical), p (Logical), " (Label), and Smart/Implicit modes.
 * Returns 0-based index or null if unresolvable.
 */
function resolvePageNumber(target: string, context: ResolutionContext): number | null {
    const trimmed = target.trim();

    // 1. Physical Page Index (#10 -> Index 9)
    if (trimmed.startsWith("#")) {
        const numStr = trimmed.substring(1);
        const n = parseInt(numStr, 10);
        if (!isNaN(n)) return n - 1;
    }

    // 2. Logical Page Number (p10 -> 10 + offset -> Index)
    if (/^p\d+$/i.test(trimmed)) {
        const numStr = trimmed.substring(1);
        const n = parseInt(numStr, 10);
        if (!isNaN(n)) return (n + context.offset) - 1;
    }

    // 3. Explicit Page Label ("10 -> Label "10")
    if (trimmed.startsWith('"') || trimmed.startsWith("'")) {
        let labelStr = trimmed.substring(1);
        if (labelStr.endsWith(trimmed[0])) {
            labelStr = labelStr.slice(0, -1);
        }
        
        if (context.pageLabels) {
            const idx = getPageLabelIndex(context.pageLabels, labelStr);
            if (idx !== -1) return idx;
        }
        return null;
    }

    // 4. Numeric Input (Smart: Label -> Logical)
    if (/^-?\d+$/.test(trimmed)) {
        const n = parseInt(trimmed, 10);
        if (!isNaN(n)) {
            // Priority 1: Label Match
            if (context.pageLabels) {
                const idx = getPageLabelIndex(context.pageLabels, trimmed);
                if (idx !== -1) return idx;
            }
            // Priority 2: Logical Page
            return (n + context.offset) - 1;
        }
    }

    // 5. Implicit Label Matching (Non-numeric)
    if (context.pageLabels) {
        const idx = getPageLabelIndex(context.pageLabels, trimmed);
        if (idx !== -1) return idx;
    }

    return null;
}

/**
 * Resolves a target page string (e.g., "5", "#15", "toc:1") into a target configuration.
 * 
 * @param target The raw target string from the user input.
 * @param config Context configuration for resolution.
 * @returns An object with the 0-based index and a flag indicating if it targets the original document structure.
 */
export function resolveLinkTarget(target: string, config: LinkResolverConfig): { index: number, isOriginalDoc: boolean } | null {
    if (!target) return null;
    const trimmed = target.trim();

    // 1. TOC Internal Link (e.g., toc:1, toc:#1, toc:p1)
    if (trimmed.toLowerCase().startsWith("toc:")) {
        const subTarget = trimmed.substring(4);
        // TOC context: No offset (usually), No labels (yet)
        const relativeIndex = resolvePageNumber(subTarget, { offset: 0, pageLabels: null });
        
        if (relativeIndex !== null && relativeIndex >= 0) {
            return { index: config.insertPos + relativeIndex, isOriginalDoc: false };
        }
    }

    // 2. Page Number Placeholders (e.g. {p}, {p R})
    if (/^\{p( [RrAa])?\}$/.test(trimmed)) {
        return { index: 0, isOriginalDoc: true };
    }

    // 3. Standard Document Link
    const index = resolvePageNumber(trimmed, { 
        offset: config.offset, 
        pageLabels: config.pageLabels 
    });

    if (index !== null) {
        return { index, isOriginalDoc: true };
    }

    return null;
}

/**
 * Validates a target page string against the document context.
 * Returns true if the target resolves to a valid physical page index.
 */
export function validatePageTarget(
    target: string, 
    config: LinkResolverConfig & { totalPage: number }
): boolean {
    const result = resolveLinkTarget(target, config);
    if (!result) {
        // If unresolvable, it could be a label we don't know about yet.
        // If it's pure numeric and failed, it's definitely invalid (shouldn't happen with current resolver).
        // If we have no labels context, we assume it might be a valid label to avoid false positives.
        if (!config.pageLabels && !/^-?\d+$/.test(target.trim())) {
            return true;
        }
        return false;
    }

    // Boundary check
    // If it targets the original doc, it must be within [0, totalPage)
    if (result.isOriginalDoc) {
        return result.index >= 0 && result.index < config.totalPage;
    }

    // For absolute or TOC internal links, we just ensure it's non-negative
    return result.index >= 0;
}
