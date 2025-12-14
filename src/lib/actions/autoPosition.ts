// web/src/lib/actions/autoPosition.ts
export function autoPosition(node: HTMLElement, { triggerEl, fixed = false }: { triggerEl: HTMLElement | undefined, fixed?: boolean }) {
  
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
      const margin = 10;

      // --- Horizontal Positioning (Shared) ---
      
      // Ideal Left (Viewport coords)
      const triggerCenter = triggerRect.left + triggerRect.width / 2;
      let targetViewportLeft = triggerCenter - popupRect.width / 2;

      // Constrain (Viewport coords)
      if (targetViewportLeft < margin) targetViewportLeft = margin;
      else if (targetViewportLeft + popupRect.width > viewportWidth - margin) {
          targetViewportLeft = viewportWidth - margin - popupRect.width;
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
          // Check placement class (top/bottom) to decide
          // Note: Styles like 'bottom: 100%' in CSS won't work nicely with fixed top/left.
          // We override top/bottom here.
          
          if (node.classList.contains('top')) {
              // Popup ABOVE trigger
              // bottom of popup = top of trigger - margin
              const top = triggerRect.top - popupRect.height - 10; 
              node.style.top = `${top}px`;
              node.style.bottom = 'auto';
          } else {
              // Popup BELOW trigger (default)
              // top of popup = bottom of trigger + margin
              const top = triggerRect.bottom + 10;
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
  if (triggerEl) {
      resizeObserver.observe(triggerEl);
  }
  window.addEventListener('resize', robustAdjust);
  window.addEventListener('scroll', robustAdjust, true); // Capture scroll for position updates

  return {
    update(newParams: { triggerEl: HTMLElement, fixed?: boolean }) {
      fixed = newParams.fixed ?? false;
      if (newParams.triggerEl !== triggerEl) {
        if (triggerEl) {
            resizeObserver.unobserve(triggerEl);
        }
        triggerEl = newParams.triggerEl;
        if (triggerEl) {
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
