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

        // 2. Write phase: Update DOM
        jobs.forEach(({ svg, rect }) => {
            // Find start and end indices for dots that fully fit inside the rect
            // Condition: dotCenter - radius >= rectLeft  => dotCenter >= rectLeft + radius
            // Condition: dotCenter + radius <= rectRight => dotCenter <= rectRight - radius
            // dotCenter(k) = k * gap + gridOffset
            
            const startK = Math.ceil((rect.left + radius - gridOffset) / gap);
            const endK = Math.floor((rect.right - radius - gridOffset) / gap);
            
            let circles = '';
            for (let k = startK; k <= endK; k++) {
                const globalX = k * gap + gridOffset;
                const localX = globalX - rect.left;
                // Align to bottom like original: cy = height - radius
                // Use a fixed small margin from bottom if needed, e.g. -1px
                const localY = rect.height - radius; 
                
                circles += `<circle cx="${localX}" cy="${localY}" r="${radius}" />`;
            }
            
            svg.innerHTML = circles;
            // Ensure width fills container (though flex usually handles this, we reset any previous fixed width)
            svg.style.width = '100%';
            
            // Trigger opacity transition
            svg.classList.add('dots-ready');
        });
    };

    // Use requestAnimationFrame to ensure layout is settled
    requestAnimationFrame(apply);
}