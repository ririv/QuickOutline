/**
 * Configuration for resolving page links.
 */
export interface LinkResolverConfig {
    labels: string[] | null; // Page labels from the document
    offset: number;          // Global page offset (user defined)
    insertPos: number;       // Where the TOC is inserted
}

/**
 * Resolves a target page string (e.g., "5", "#15", "toc:1") into a target configuration.
 * 
 * @param target The raw target string from the user input.
 * @param config Context configuration for resolution.
 * @returns An object with the 0-based index and a flag indicating if it targets the original document structure.
 */
export function resolveLinkTarget(target: string, config: LinkResolverConfig): { index: number, isOriginal: boolean } | null {
    if (!target) return null;
    const trimmed = target.trim();

    // 1. TOC Internal Link (e.g., toc:1)
    if (trimmed.toLowerCase().startsWith("toc:")) {
        const numStr = trimmed.substring(4);
        const n = parseInt(numStr, 10);
        if (!isNaN(n)) {
            // Physical index = insertPos + (n - 1)
            return { index: config.insertPos + (n - 1), isOriginal: false };
        }
    }

    // 2. Forced Physical Index (e.g., #15)
    if (trimmed.startsWith("#")) {
        const numStr = trimmed.substring(1);
        const n = parseInt(numStr, 10);
        if (!isNaN(n)) {
            // Physical absolute index
            return { index: n - 1, isOriginal: false };
        }
    }

    // 3. Explicit Page Label Matching (e.g., @5 or @iv)
    if (trimmed.startsWith("@")) {
        const labelStr = trimmed.substring(1);
        if (config.labels) {
            const idx = config.labels.indexOf(labelStr);
            if (idx !== -1) {
                return { index: idx, isOriginal: true };
            }
        }
        return null; // Explicit target not found
    }

    // 4. Implicit Page Label Matching
    if (config.labels) {
        const idx = config.labels.indexOf(trimmed);
        if (idx !== -1) {
            // Label matches, return exact index without offset
            return { index: idx, isOriginal: true };
        }
    }

    // 5. Fallback: Pure number + Global Offset
    if (/^\d+$/.test(trimmed)) {
        const n = parseInt(trimmed, 10);
        if (!isNaN(n)) {
            // Logic index with offset correction
            return { index: (n + config.offset) - 1, isOriginal: true };
        }
    }

    return null;
}
