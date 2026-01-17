import { calculateDragState } from './dragLogic.ts';
import type { BookmarkUI } from '../types/bookmark.ts';

function assertEquals(actual: any, expected: any, message: string) {
    if (JSON.stringify(actual) !== JSON.stringify(expected)) {
        console.error(`❌ FAIL: ${message}`);
        console.error(`   Actual:   ${JSON.stringify(actual)}`);
        console.error(`   Expected: ${JSON.stringify(expected)}`);
        throw new Error(message);
    } else {
        console.log(`✅ PASS: ${message}`);
    }
}

const mockNodes: BookmarkUI[] = [
    { id: '1', title: 'Node 1', level: 1, pageNum: '1', children: [] },
    { id: '2', title: 'Node 2', level: 1, pageNum: '2', children: [] },
    { id: '2-1', title: 'Node 2-1', level: 2, pageNum: '3', children: [] },
    { id: '3', title: 'Node 3', level: 1, pageNum: '4', children: [] }
];

const NODE_HEIGHT = 28;

console.log('--- Starting Drag Logic Tests ---');

// Case 1: Sibling - Below Node 1, Level 1
// Indent for Level 1 is 4px. 
(() => {
    const result = calculateDragState(
        NODE_HEIGHT * 0.8,
        NODE_HEIGHT,
        10, // Close to level 1 (4px)
        '1',
        'drag-id',
        mockNodes
    );
    assertEquals(result?.dropTargetId, '1', 'Case 1: Target ID');
    assertEquals(result?.dropPosition, 'after', 'Case 1: Position');
    assertEquals(result?.dropTargetLevel, 1, 'Case 1: Level');
})();

// Case 2: Child - Below Node 1, Level 2
// Indent for Level 2 is 24px.
(() => {
    const result = calculateDragState(
        NODE_HEIGHT * 0.8,
        NODE_HEIGHT,
        30, // Close to level 2 (24px)
        '1',
        'drag-id',
        mockNodes
    );
    // Target logic: if targetLevel == ref.level + 1 AND nextNode exists -> before nextNode
    assertEquals(result?.dropTargetId, '2', 'Case 2: Target ID (before next)');
    assertEquals(result?.dropPosition, 'before', 'Case 2: Position');
    assertEquals(result?.dropTargetLevel, 2, 'Case 2: Level');
})();

// Case 3: Outdent - Below Node 2-1 (L2), Level 1
(() => {
    const result = calculateDragState(
        NODE_HEIGHT * 0.8,
        NODE_HEIGHT,
        10, // Back to Level 1
        '2-1',
        'drag-id',
        mockNodes
    );
    // Find ancestor with level 1. Before 2-1, node 2 is level 1.
    assertEquals(result?.dropTargetId, '2', 'Case 3: Target ID (ancestor sibling)');
    assertEquals(result?.dropPosition, 'after', 'Case 3: Position');
    assertEquals(result?.dropTargetLevel, 1, 'Case 3: Level');
})();

// Case 4: Top - Top of Node 1
(() => {
    const result = calculateDragState(
        NODE_HEIGHT * 0.1,
        NODE_HEIGHT,
        10,
        '1',
        'drag-id',
        mockNodes
    );
    assertEquals(result?.dropTargetId, '1', 'Case 4: Target ID');
    assertEquals(result?.dropPosition, 'before', 'Case 4: Position');
    assertEquals(result?.dropTargetLevel, 1, 'Case 4: Level');
    assertEquals(result?.gapPosition, 'before', 'Case 4: Visual Gap Pos');
})();

console.log('--- All Tests Passed ---');