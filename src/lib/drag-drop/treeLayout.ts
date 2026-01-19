/**
 * Constants and helpers for Tree Node layout calculations.
 * Used for both visual rendering and drag-and-drop level detection.
 */

export const TREE_INDENT = 20;
export const TREE_BASE_PADDING = 4;
export const NODE_CONTENT_OFFSET = 16; // Drag Handle (14+2). Note: without Toggle Button (20)

/**
 * Calculates the horizontal padding for a node content.
 */
export function getNodePadding(level: number): number {
    return (Math.max(1, level) - 1) * TREE_INDENT + TREE_BASE_PADDING;
}

/**
 * Calculates the indentation for structural elements like gaps.
 */
export function getGapIndent(level: number): number {
    return (Math.max(1, level) - 1) * TREE_INDENT + TREE_BASE_PADDING + NODE_CONTENT_OFFSET;
}

/**
 * Inversely calculates the target level from a horizontal mouse offset.
 */
export function calculateLevelFromX(mouseX: number): number {
    return Math.floor((mouseX - TREE_BASE_PADDING - NODE_CONTENT_OFFSET + (TREE_INDENT / 2)) / TREE_INDENT) + 1;
}
