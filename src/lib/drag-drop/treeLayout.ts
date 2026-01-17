/**
 * Constants and helpers for Tree Node layout calculations.
 * Used for both visual rendering and drag-and-drop level detection.
 */

export const TREE_INDENT = 20;
export const TREE_BASE_PADDING = 4;

/**
 * Calculates the horizontal padding for a node content.
 */
export function getNodePadding(level: number): number {
    return (level - 1) * TREE_INDENT + TREE_BASE_PADDING;
}

/**
 * Calculates the indentation for structural elements like gaps.
 */
export function getGapIndent(level: number): number {
    return (level - 1) * TREE_INDENT;
}
