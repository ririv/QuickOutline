export function portal(node: HTMLElement, target: HTMLElement | string = 'body') {
    let targetEl: HTMLElement | null;

    async function update(newTarget: HTMLElement | string) {
        targetEl = typeof newTarget === 'string' ? document.querySelector(newTarget) : newTarget;
        if (targetEl) {
            targetEl.appendChild(node);
            node.hidden = false;
        } else {
            node.hidden = true; // Hide if target not found
        }
    }

    function destroy() {
        if (node.parentNode) {
            node.parentNode.removeChild(node);
        }
    }

    update(target);

    return {
        update,
        destroy,
    };
}
