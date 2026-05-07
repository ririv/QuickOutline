export function draggable(node: HTMLElement, params: { handle?: string | HTMLElement } = {}) {
    let handle: HTMLElement;
    let isDragging = false;
    let startX: number;
    let startY: number;
    let initialLeft: number;
    let initialTop: number;

    function onMouseDown(e: MouseEvent) {
        isDragging = true;
        startX = e.clientX;
        startY = e.clientY;

        const rect = node.getBoundingClientRect();
        initialLeft = rect.left;
        initialTop = rect.top;
        
        // Set fixed positioning if not already
        const style = window.getComputedStyle(node);
        if (style.position !== 'fixed' && style.position !== 'absolute') {
            node.style.position = 'fixed';
            node.style.left = `${initialLeft}px`;
            node.style.top = `${initialTop}px`;
        }

        window.addEventListener('mousemove', onMouseMove);
        window.addEventListener('mouseup', onMouseUp);
        
        // Prevent text selection during drag initiation
        e.preventDefault();
        
        node.classList.add('dragging');
    }

    function onMouseMove(e: MouseEvent) {
        if (!isDragging) return;

        const dx = e.clientX - startX;
        const dy = e.clientY - startY;

        node.style.left = `${initialLeft + dx}px`;
        node.style.top = `${initialTop + dy}px`;
    }

    function onMouseUp() {
        isDragging = false;
        window.removeEventListener('mousemove', onMouseMove);
        window.removeEventListener('mouseup', onMouseUp);
        node.classList.remove('dragging');
    }

    function init() {
        if (params && Object.prototype.hasOwnProperty.call(params, 'handle')) {
            if (!params.handle) return; // Wait for update if bind:this is not ready
            
            if (typeof params.handle === 'string') {
                handle = node.querySelector(params.handle) as HTMLElement;
            } else {
                handle = params.handle;
            }
            if (!handle) return;
        } else {
            handle = node;
        }

        handle.style.cursor = 'move';
        handle.addEventListener('mousedown', onMouseDown);
    }

    function destroy() {
        if (handle) {
            handle.removeEventListener('mousedown', onMouseDown);
        }
        window.removeEventListener('mousemove', onMouseMove);
        window.removeEventListener('mouseup', onMouseUp);
    }

    init();

    return {
        destroy,
        update(newParams: { handle?: string | HTMLElement }) {
            destroy();
            params = newParams;
            init();
        }
    };
}
