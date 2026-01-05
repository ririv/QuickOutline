/**
 * Simple path utility for frontend.
 */

/**
 * Appends a suffix to a file path before the extension.
 * Example: /path/to/doc.pdf -> /path/to/doc_new.pdf
 */
export function addSuffixToPath(path: string, suffix: string = "_new"): string {
    const lastDotIndex = path.lastIndexOf('.');
    if (lastDotIndex === -1) return path + suffix;
    
    const base = path.substring(0, lastDotIndex);
    const ext = path.substring(lastDotIndex);
    return base + suffix + ext;
}

/**
 * Extracts the file name from a path.
 */
export function getFileName(path: string): string {
    const lastSlash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
    return path.substring(lastSlash + 1);
}
