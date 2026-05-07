/**
 * Global registry for elements that are portalled to the body.
 * clickOutside actions will check against this set to avoid 
 * closing when clicking inside a portal element.
 */
export const portalElements = new Set<HTMLElement>();

/**
 * Registers an element as a portal element.
 * @returns an unregister function.
 */
export function registerPortal(el: HTMLElement) {
    portalElements.add(el);
    return () => {
        portalElements.delete(el);
    };
}

export interface ClickOutsideOptions {
  callback: (event: MouseEvent) => void;
  enabled?: boolean;
  exclude?: (HTMLElement | undefined)[];
  capture?: boolean;
}

export type ClickOutsideParameter = ((event: MouseEvent) => void) | ClickOutsideOptions;

export function clickOutside(node: HTMLElement, params: ClickOutsideParameter) {
  let callback: (event: MouseEvent) => void;
  let enabled = true;
  let exclude: (HTMLElement | undefined)[] = [];
  let capture = true;

  const updateState = (p: ClickOutsideParameter) => {
    if (typeof p === 'function') {
      callback = p;
      enabled = true;
      exclude = [];
      capture = true;
    } else {
      callback = p.callback;
      enabled = p.enabled ?? true;
      exclude = p.exclude ?? [];
      capture = p.capture ?? true;
    }
  };

  updateState(params);

  const handleClick = (event: MouseEvent) => {
    if (!enabled) return;
    const target = event.target as Node;
    
    // 1. Check if click is inside the node itself
    if (node.contains(target)) return;
    
    // 2. Check explicit exclude list
    if (exclude.some(el => el && el.contains(target))) return;

    // 3. Check global portal registry (Automatic Portal Protection)
    for (const portalEl of portalElements) {
        if (portalEl && portalEl.contains(target)) return;
    }

    callback(event);
  };

  const addListener = () => document.addEventListener('click', handleClick, capture);
  const removeListener = () => document.removeEventListener('click', handleClick, capture);

  addListener();

  return {
    update(newParams: ClickOutsideParameter) {
      const oldCapture = capture;
      const newCapture = typeof newParams === 'object' ? (newParams.capture ?? true) : true;
      
      if (oldCapture !== newCapture) {
          removeListener();
          updateState(newParams);
          addListener();
      } else {
          updateState(newParams);
      }
    },
    destroy() {
      removeListener();
    }
  };
}