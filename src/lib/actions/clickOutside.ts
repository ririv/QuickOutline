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
    
    if (node.contains(target)) return;
    
    if (exclude.some(el => el && el.contains(target))) return;

    callback(event);
  };

  const addListener = () => document.addEventListener('click', handleClick, capture);
  const removeListener = () => document.removeEventListener('click', handleClick, capture);

  addListener();

  return {
    update(newParams: ClickOutsideParameter) {
      // Check if capture changed, requiring re-binding
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
