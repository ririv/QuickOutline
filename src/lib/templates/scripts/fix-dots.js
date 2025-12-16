// src/lib/preview-engine/scripts/fix-dots.js

export function fixDots(gap = 6, rootElement = document.body) {
    const radius = 1; // Sync with DOT_DIAMETER / 2
    const gridOffset = radius; // Sync with logic in generator

    const apply = () => {
        const selector = '.toc-leader .dotted-line';
        const elements = rootElement.querySelectorAll(selector); 
        
        // 1. Read phase: Gather all metrics first to avoid layout thrashing
        /** @type {Array<{svg: SVGElement, rect: DOMRect}>} */
        const jobs = [];
        elements.forEach((svg) => {
            if (svg instanceof SVGElement) {
                const rect = svg.getBoundingClientRect();
                jobs.push({ svg, rect });
            }
        });

        // 2. Write phase: Update DOM using a SINGLE <path> element
        jobs.forEach(({ svg, rect }) => {
            // Logic: We only draw dots that are FULLY inside the container.
            // Dot K center = k * gap + gridOffset
            // Left edge of dot = center - radius
            // Right edge of dot = center + radius
            
            // Condition 1: Left edge >= rect.left
            // k * gap + gridOffset - radius >= rect.left
            // Since gridOffset == radius, simplifies to: k * gap >= rect.left
            const startK = Math.ceil(rect.left / gap);

            // Condition 2: Right edge <= rect.right
            // k * gap + gridOffset + radius <= rect.right
            // k * gap + 2*radius <= rect.right
            const endK = Math.floor((rect.right - 2 * radius) / gap);

            let d = '';
            // Optimization: Pre-calculate constants for the arc command
            // Arc command: a rx ry x-axis-rotation large-arc-flag sweep-flag dx dy
            // To draw a full circle of radius R:
            // Move to (cx - R, cy)
            // Arc 1 (top half): a R,R 0 1,0 2R,0
            // Arc 2 (bottom half): a R,R 0 1,0 -2R,0
            const arcStr = `a ${radius},${radius} 0 1,0 ${radius * 2},0 a ${radius},${radius} 0 1,0 -${radius * 2},0`;

            // Align vertical position to bottom
            const cy = rect.height - radius;
            
            for (let k = startK; k <= endK; k++) {
                const globalX = k * gap + gridOffset;
                const localX = globalX - rect.left;
                
                // Move to the left edge of the circle to start drawing
                const startX = (localX - radius).toFixed(2);
                const startY = cy.toFixed(2);
                
                d += `M ${startX},${startY} ${arcStr} `;
            }
            
            // Only update if we have dots to show
            if (d) {
                // Check if we already have a path to avoid full destroy/recreate if not needed (optional optimization)
                // But innerHTML is fast enough for this specific case and cleaner to reset.
                svg.innerHTML = `<path d="${d}" fill="currentColor" />`;
            } else {
                svg.innerHTML = '';
            }
            
            // Ensure width fills container and clean up styles
            svg.style.width = '100%';
            svg.style.clipPath = ''; // Remove any clip-path from previous attempts
            svg.classList.add('dots-ready');
        });
    };

    // Use requestAnimationFrame to ensure layout is settled
    requestAnimationFrame(apply);
}