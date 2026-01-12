// web/src/lib/actions/autoPosition.ts
function getScrollParent(node: HTMLElement): HTMLElement | null {
  if (!node) return null;
  
  let parent = node.parentElement;
  while (parent) {
    const style = getComputedStyle(parent);
    // Look for overflow that isn't 'visible'
    if (/(auto|scroll|hidden)/.test(style.overflow + style.overflowY + style.overflowX)) {
      return parent;
    }
    parent = parent.parentElement;
  }
  return document.body; // Default fallback
}

export function autoPosition(node: HTMLElement, { triggerEl, fixed = false, offset = 0, trackTrigger = true }: { triggerEl: HTMLElement | undefined, fixed?: boolean, offset?: number, trackTrigger?: boolean }) {
  
  function robustAdjust() {
      // Check if elements still exist
      if (!triggerEl || !(triggerEl instanceof Element)) {
          // If triggerEl is missing, we can't position.
          return;
      }

      // If not fixed (absolute), we need offsetParent
      let offsetParent: HTMLElement | null = null;
      if (!fixed) {
          offsetParent = node.offsetParent as HTMLElement;
          if (!offsetParent) return; // Hidden or detached
      }
      
      const triggerRect = triggerEl.getBoundingClientRect();
      const popupRect = node.getBoundingClientRect();
      const viewportWidth = window.innerWidth;
      const boundaryMargin = 10;

      // --- Determine Boundaries ---
      // Default: Viewport
      let minX = 0;
      let maxX = viewportWidth;

      // Smart Boundary: Intersection of Viewport and Scroll Parent
      const scrollParent = getScrollParent(node);
      if (scrollParent && scrollParent !== document.body) {
          const parentRect = scrollParent.getBoundingClientRect();
          // We constrain minX to be at least parentRect.left
          minX = Math.max(minX, parentRect.left);
          // We constrain maxX to be at most parentRect.right
          maxX = Math.min(maxX, parentRect.right);
      }

      // --- Horizontal Positioning (Shared) ---
      
      // Ideal Left (Viewport coords)
      const triggerCenter = triggerRect.left + triggerRect.width / 2;
      let targetViewportLeft = triggerCenter - popupRect.width / 2;

      // Constrain (Viewport coords)
      if (targetViewportLeft < minX + boundaryMargin) {
          targetViewportLeft = minX + boundaryMargin;
      } else if (targetViewportLeft + popupRect.width > maxX - boundaryMargin) {
          targetViewportLeft = maxX - boundaryMargin - popupRect.width;
      }

      // Arrow Logic
      let arrowX = triggerCenter - targetViewportLeft; // relative to popup left edge
      const safeZone = 12; // border-radius + arrow width margin
      if (arrowX < safeZone) arrowX = safeZone;
      if (arrowX > popupRect.width - safeZone) arrowX = popupRect.width - safeZone;
      
      node.style.setProperty('--arrow-x', `${arrowX}px`);
      node.style.transform = 'none'; // Remove any centering transforms

      // --- Apply Coordinates ---

      if (fixed) {
          // Fixed Positioning (Portal Mode)
          node.style.position = 'fixed';
          node.style.left = `${targetViewportLeft}px`;
          
          // Vertical Position
          if (node.classList.contains('top')) {
              // Popup ABOVE trigger
              const top = triggerRect.top - popupRect.height - offset; 
              node.style.top = `${top}px`;
              node.style.bottom = 'auto';
          } else {
              // Popup BELOW trigger (default)
              const top = triggerRect.bottom + offset;
              node.style.top = `${top}px`;
              node.style.bottom = 'auto';
          }

      } else {
          // Absolute Positioning (Original Logic)
          if (!offsetParent) return;
          const parentRect = offsetParent.getBoundingClientRect();
          const finalLocalLeft = targetViewportLeft - parentRect.left;

          node.style.left = `${finalLocalLeft}px`;
          // Vertical is handled by CSS (.top { bottom: 100% })
      }
  }

  // Run
  requestAnimationFrame(robustAdjust);

  const resizeObserver = new ResizeObserver(robustAdjust);
  resizeObserver.observe(node);
  if (triggerEl && trackTrigger) {
      resizeObserver.observe(triggerEl);
  }
  window.addEventListener('resize', robustAdjust);
  window.addEventListener('scroll', robustAdjust, true); // Capture scroll for position updates

  return {
    update(newParams: { triggerEl: HTMLElement, fixed?: boolean, offset?: number, trackTrigger?: boolean }) {
      const oldTrackTrigger = trackTrigger;
      fixed = newParams.fixed ?? false;
      offset = newParams.offset ?? 0;
      trackTrigger = newParams.trackTrigger ?? true;

      if (newParams.triggerEl !== triggerEl || trackTrigger !== oldTrackTrigger) {
        if (triggerEl) {
            resizeObserver.unobserve(triggerEl);
        }
        triggerEl = newParams.triggerEl;
        if (triggerEl && trackTrigger) {
            resizeObserver.observe(triggerEl);
        }
      }
      robustAdjust();
    },
    destroy() {
      resizeObserver.disconnect();
      window.removeEventListener('resize', robustAdjust);
      window.removeEventListener('scroll', robustAdjust, true);
    }
  };
}