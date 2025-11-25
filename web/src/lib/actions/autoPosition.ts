// web/src/lib/actions/autoPosition.ts
export function autoPosition(node: HTMLElement, { triggerEl }: { triggerEl: HTMLElement }) {
  
  // Calculate where the parent is.
  // The popup is inside `div.arrow-popup` which usually has a parent like `.btn-wrapper`.
  // Note: offsetParent might be null if hidden, so we check inside robustAdjust.
  
  function robustAdjust() {
      // Check if elements still exist
      if (!triggerEl || !node) return;

      const offsetParent = node.offsetParent as HTMLElement;
      if (!offsetParent) {
          // If hidden or detached, we can't calculate offset relative to parent.
          // For hover popups, they start hidden. We need to calculate when they become visible.
          // The ResizeObserver on 'node' should trigger this when display changes from none -> block.
          // But visibility:hidden elements DO have offsetParent? No, usually not if display is none.
          // If visibility:hidden, they DO have layout.
          return;
      }
      
      // We need measurements relative to the VIEWPORT to constrain against viewport.
      const triggerRect = triggerEl.getBoundingClientRect();
      const popupRect = node.getBoundingClientRect();
      const parentRect = offsetParent.getBoundingClientRect();
      const viewportWidth = window.innerWidth;
      const margin = 10;

      // Ideal Left (Viewport coords)
      const triggerCenter = triggerRect.left + triggerRect.width / 2;
      let targetViewportLeft = triggerCenter - popupRect.width / 2;

      // Constrain (Viewport coords)
      if (targetViewportLeft < margin) targetViewportLeft = margin;
      else if (targetViewportLeft + popupRect.width > viewportWidth - margin) {
          targetViewportLeft = viewportWidth - margin - popupRect.width;
      }

      // Convert Viewport Left -> Local Left (relative to offsetParent)
      // localLeft = targetViewportLeft - parentRect.left
      const finalLocalLeft = targetViewportLeft - parentRect.left;

      // Arrow Logic (same as before)
      let arrowX = triggerCenter - targetViewportLeft; // relative to popup left edge
      const safeZone = 12; // border-radius + arrow width margin
      if (arrowX < safeZone) arrowX = safeZone;
      if (arrowX > popupRect.width - safeZone) arrowX = popupRect.width - safeZone;

      // Apply
      node.style.left = `${finalLocalLeft}px`;
      node.style.transform = 'none'; // Remove any centering transforms
      node.style.setProperty('--arrow-x', `${arrowX}px`);
  }

  // Run
  // We need to wait for layout. requestAnimationFrame is usually enough.
  requestAnimationFrame(robustAdjust);

  const resizeObserver = new ResizeObserver(robustAdjust);
  resizeObserver.observe(node);
  resizeObserver.observe(triggerEl);
  window.addEventListener('resize', robustAdjust);
  window.addEventListener('scroll', robustAdjust, true); // Capture scroll for position updates

  return {
    update(newParams: { triggerEl: HTMLElement }) {
      if (newParams.triggerEl !== triggerEl) {
        resizeObserver.unobserve(triggerEl);
        triggerEl = newParams.triggerEl;
        resizeObserver.observe(triggerEl);
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