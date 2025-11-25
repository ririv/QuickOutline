<script lang="ts">
  interface Props {
    type: 'header' | 'footer';
    pos: 'left' | 'center' | 'right' | 'inner' | 'outer';
  }

  let { type, pos }: Props = $props();

  // Determine highlight color
  let color = $derived.by(() => {
    if (pos === 'inner') {
      return '#ff4d4f'; // Red for Inner
    } else if (pos === 'outer') {
      return '#1677ff'; // Blue for Outer
    } else {
      return '#faad14'; // Orange for Absolute Positions (Left, Center, Right)
    }
  });
  
  // Determine title
  let title = $derived.by(() => {
    switch (pos) {
      case 'left': return 'Left Aligned';
      case 'center': return 'Center Aligned';
      case 'right': return 'Right Aligned';
      case 'inner': return 'Inner (Binding Side)';
      case 'outer': return 'Outer (Edge Side)';
      default: return '';
    }
  });

  // Highlight Rect Calculations
  // Page dims: Left(10,10,48,60), Right(62,10,48,60)
  
  // Y position: Header at top, Footer at bottom
  const hY = type === 'header' ? 10 : 64; 
  const hHeight = 6; 
  const hWidth = 14;

  // X positions (initially 0)
  let lRect = { x: 0, y: hY, w: hWidth, h: hHeight };
  let rRect = { x: 0, y: hY, w: hWidth, h: hHeight };
  
  // Visibility flags for single/double page rendering
  let isDoublePage = $derived(pos === 'inner' || pos === 'outer');

  // Page constants
  const lPageX = 10;
  const rPageX = 62;
  const pageWidth = 48;
  
  // Single Page Constants (Center 36, 10, 48, 60)
  const sPageX = 36;

  if (pos === 'inner') {
      // Inner: Left Page Right Side, Right Page Left Side
      lRect.x = lPageX + pageWidth - hWidth; 
      rRect.x = rPageX;
  } else if (pos === 'outer') {
      // Outer: Left Page Left Side, Right Page Right Side
      lRect.x = lPageX;
      rRect.x = rPageX + pageWidth - hWidth;
  } else if (pos === 'left') {
      // Left: Single Page Left Side
      lRect.x = sPageX;
  } else if (pos === 'center') {
      // Center: Single Page Center
      lRect.x = sPageX + (pageWidth - hWidth) / 2;
  } else if (pos === 'right') {
      // Right: Single Page Right Side
      lRect.x = sPageX + pageWidth - hWidth;
  }
</script>

<div class="info-tooltip {type}">
  <div class="tooltip-title">{title}</div>
  <svg width="120" height="80" viewBox="0 0 120 80" class="diagram">
      {#if isDoublePage}
          <!-- Double Page (Inner/Outer) -->
          <!-- Left Page (Even) -->
          <rect x="10" y="10" width="48" height="60" fill="white" stroke="#666" stroke-width="1"/>
          <text x="34" y="45" font-size="10" text-anchor="middle" fill="#999">Even</text>
          
          <!-- Right Page (Odd) -->
          <rect x="62" y="10" width="48" height="60" fill="white" stroke="#666" stroke-width="1"/>
          <text x="86" y="45" font-size="10" text-anchor="middle" fill="#999">Odd</text>
          
          <!-- Highlights -->
          <rect x={lRect.x} y={lRect.y} width={lRect.w} height={lRect.h} fill={color} opacity="0.4"/>
          <rect x={rRect.x} y={rRect.y} width={rRect.w} height={rRect.h} fill={color} opacity="0.4"/>
      {:else}
          <!-- Single Page (Left/Center/Right) -->
          <rect x={sPageX} y="10" width="48" height="60" fill="white" stroke="#666" stroke-width="1"/>
          
          <!-- Highlight -->
          <rect x={lRect.x} y={hY} width={hWidth} height={hHeight} fill={color} opacity="0.4"/>
      {/if}
  </svg>
</div>

<style>
  /* Tooltip Base */
  .info-tooltip {
      visibility: hidden;
      opacity: 0;
      position: absolute;
      left: 50%;
      transform: translateX(-50%);
      background: #fff;
      border: 1px solid #e1e4e8;
      box-shadow: 0 4px 12px rgba(0,0,0,0.15);
      border-radius: 6px;
      padding: 8px;
      width: 140px;
      z-index: 100;
      transition: all 0.2s;
      pointer-events: none;
      display: flex;
      flex-direction: column;
      align-items: center;
  }
  
  /* Header: Tooltip appears BELOW */
  .info-tooltip.header {
      top: 120%;
  }
  .info-tooltip.header::after {
      content: "";
      position: absolute;
      bottom: 100%; /* Arrow at top */
      left: 50%;
      margin-left: -5px;
      border-width: 5px;
      border-style: solid;
      border-color: transparent transparent #fff transparent;
  }

  /* Footer: Tooltip appears ABOVE */
  .info-tooltip.footer {
      bottom: 120%;
  }
  .info-tooltip.footer::after {
      content: "";
      position: absolute;
      top: 100%; /* Arrow at bottom */
      left: 50%;
      margin-left: -5px;
      border-width: 5px;
      border-style: solid;
      border-color: #fff transparent transparent transparent;
  }
  
  .tooltip-title {
      font-size: 10px;
      font-weight: 600;
      color: #555;
      margin-bottom: 4px;
      text-align: center;
  }
  
  .diagram {
      display: block;
  }
</style>
