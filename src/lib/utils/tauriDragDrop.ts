import { listen } from '@tauri-apps/api/event';

export type DropPosition = 'before' | 'after' | 'inside';

interface DragDropOptions {
    /**
     * Callback to get the ID of the node currently being dragged.
     * Return null if no node is being dragged.
     */
    getDraggedId: () => string | null;
    
    /**
     * Called when a dragged item is over a valid target.
     * @param targetId The ID of the target node, or null if not over a valid target.
     * @param position The position relative to the target, or null.
     */
    onDragOver: (targetId: string | null, position: DropPosition | null) => void;
    
    /**
     * Called when the dragged item is dropped onto a valid target.
     * @param targetId The ID of the target node.
     * @param position The position relative to the target.
     */
    onDrop: (targetId: string, position: DropPosition) => void;
    
    /**
     * Called when the drag operation leaves the window.
     */
    onDragLeave: () => void;
}

// Helper to calculate drop target based on coordinates
function getDropTarget(x: number, y: number, draggedId: string | null): { id: string, position: DropPosition } | null {
    if (!draggedId) return null;

    const element = document.elementFromPoint(x, y);
    const nodeElement = element?.closest('.node-row') as HTMLElement;

    if (nodeElement) {
        const id = nodeElement.dataset.id;
        if (id && id !== draggedId) {
            const rect = nodeElement.getBoundingClientRect();
            const relativeY = y - rect.top;
            const h = rect.height;

            let position: DropPosition;
            if (relativeY < h * 0.25) position = 'before';
            else if (relativeY > h * 0.75) position = 'after';
            else position = 'inside';

            return { id, position };
        }
    }
    return null;
}

/**
 * Sets up Tauri system drag-and-drop event listeners.
 * 
 * @param options Configuration options for drag and drop behavior
 * @returns A cleanup function to remove event listeners
 */
export async function setupTauriDragDrop(options: DragDropOptions): Promise<() => void> {
    const { getDraggedId, onDragOver, onDrop, onDragLeave } = options;
    const unlistenFns: (() => void)[] = [];

    // Check environment
    // @ts-ignore
    const isTauri = !!(window.__TAURI_INTERNALS__ || window.__TAURI__);
    if (!isTauri) return () => {};

    try {
        // 1. Drag Over - Update Visual Feedback
        unlistenFns.push(await listen<{ position: { x: number, y: number } }>('tauri://drag-over', (e) => {
            const draggedId = getDraggedId();
            if (!draggedId) return;

            const { x, y } = e.payload.position;
            // Tauri v2 internal drag coordinates match DOM coordinates directly.
            const target = getDropTarget(x, y, draggedId);

            if (target) {
                onDragOver(target.id, target.position);
            } else {
                onDragOver(null, null);
            }
        }));

        // 2. Drag Drop - Execute Move
        unlistenFns.push(await listen<{ position: { x: number, y: number } }>('tauri://drag-drop', (e) => {
            const draggedId = getDraggedId();
            if (!draggedId) return;

            const { x, y } = e.payload.position;
            const target = getDropTarget(x, y, draggedId);

            if (target) {
                onDrop(target.id, target.position);
            } else {
                // If dropped outside valid target, we might want to clear state?
                // The original code reset state regardless of whether a move happened.
                // But strictly speaking, if we just call onDrop for valid targets,
                // the caller might not clear 'draggedId' if we don't tell them.
                // However, usually 'drop' implies end of drag.
                // Let's call onDragLeave() or similar to ensure cleanup if no target found?
                // Or maybe the caller expects onDrop to handle logic.
                // To match original behavior which resets everything:
                // We will rely on the component to likely treat any drop event (or end of drag) as cleanup time.
                // But here we only fire onDrop if target is valid.
                // If we want to strictly match "Reset" logic from original code:
                onDragLeave(); // Use onDragLeave as a generic "clear drop state" signal if drop missed
            }
        }));

        // 3. Cancel/Leave
        unlistenFns.push(await listen('tauri://drag-leave', () => {
            onDragLeave();
        }));

    } catch (e) {
        console.warn("Tauri event listen failed", e);
    }

    return () => {
        unlistenFns.forEach(fn => fn());
    };
}
