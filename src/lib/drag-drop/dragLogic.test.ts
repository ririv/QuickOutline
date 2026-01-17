import { calculateDragState } from './dragLogic';
import type { BookmarkUI } from '../types/bookmark';

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

console.log('--- Starting Drag Logic Tests (Requirement-Driven) ---');

// Case 1: Sibling - Below Node 1, Level 1
// Goal: Insert after Node 1, same level.
(() => {
    const result = calculateDragState(
        NODE_HEIGHT * 0.8,
        NODE_HEIGHT,
        10, // Close to level 1
        '1',
        'drag-id',
        mockNodes
    );
    assertEquals(result?.dropTargetId, '1', 'Case 1: Target ID');
    assertEquals(result?.dropPosition, 'after', 'Case 1: Position');
    assertEquals(result?.dropTargetLevel, 1, 'Case 1: Level');
})();

// Case 2: Child (New Parent) - Below Node 1, Level 2
// Goal: Make Node 1 a parent. Node 1 currently has NO visible children.
// Action: Must be 'inside' Node 1.
(() => {
    const result = calculateDragState(
        NODE_HEIGHT * 0.8,
        NODE_HEIGHT,
        30, // Level 2 indent
        '1',
        'drag-id',
        mockNodes
    );
    assertEquals(result?.dropTargetId, '1', 'Case 2: Target ID (parent)');
    assertEquals(result?.dropPosition, 'inside', 'Case 2: Position');
    assertEquals(result?.dropTargetLevel, 2, 'Case 2: Level');
})();

// Case 3: Outdent - Below Node 2-1 (L2), Level 1
// Goal: Insert after the whole Node 2 family (after 2-1), at Level 1.
// Logic: This is effectively inserting after Node 2.
(() => {
    const result = calculateDragState(
        NODE_HEIGHT * 0.8,
        NODE_HEIGHT,
        10, // Back to Level 1
        '2-1',
        'drag-id',
        mockNodes
    );
    assertEquals(result?.dropTargetId, '2', 'Case 3: Target ID (Node 2)');
    assertEquals(result?.dropPosition, 'after', 'Case 3: Position');
    assertEquals(result?.dropTargetLevel, 1, 'Case 3: Level');
})();

// Case 4: Top - Top of Node 1
// Goal: Insert at very top.
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
})();

// Case 5: Child (Existing Parent) - Below Node 2, Level 2
// Goal: Insert between Node 2 and Node 2-1.
// Context: Node 2 is followed immediately by Node 2-1 (its child).
// Action: Insert BEFORE Node 2-1.
(() => {
    const result = calculateDragState(
        NODE_HEIGHT * 0.8, // Bottom of Node 2
        NODE_HEIGHT,
        30, // Level 2
        '2',
        'drag-id',
        mockNodes
    );
    // Gap is between 2 and 2-1.
    // RefNode: 2. NextNode: 2-1.
    // Target Level: 2. Ref Level: 1.
    // Logic: targetLevel == refLevel + 1.
    // Check: NextNode (2-1) is Level 2. So NextNode IS a child.
    // Action: before NextNode.
    assertEquals(result?.dropTargetId, '2-1', 'Case 5: Target ID (2-1)');
    assertEquals(result?.dropPosition, 'before', 'Case 5: Position');
    assertEquals(result?.dropTargetLevel, 2, 'Case 5: Level');
})();

// Case 6: Sibling (Nested) - Below Node 2-1, Level 2
// Goal: Insert after Node 2-1, keeping Level 2.
(() => {
    const result = calculateDragState(
        NODE_HEIGHT * 0.8,
        NODE_HEIGHT,
        30, // Level 2
        '2-1',
        'drag-id',
        mockNodes
    );
    // RefNode: 2-1 (L2). Target Level: 2.
    // Logic: Sibling.
    assertEquals(result?.dropTargetId, '2-1', 'Case 6: Target ID');
    assertEquals(result?.dropPosition, 'after', 'Case 6: Position');
})();

// Case 7: Child (Deepest) - Below Node 2-1, Level 3
// Goal: Make Node 2-1 a parent.
// Action: inside 2-1.
(() => {
    const result = calculateDragState(
        NODE_HEIGHT * 0.8,
        NODE_HEIGHT,
        50, // Level 3 (assuming 20px indent steps: 4, 24, 44, 64...)
        '2-1',
        'drag-id',
        mockNodes
    );
    assertEquals(result?.dropTargetId, '2-1', 'Case 7: Target ID');
    assertEquals(result?.dropPosition, 'inside', 'Case 7: Position');
    assertEquals(result?.dropTargetLevel, 3, 'Case 7: Level');
})();

console.log('--- All Tests Passed ---');