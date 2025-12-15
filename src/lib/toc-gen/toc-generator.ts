import tocStyles from './toc.css?inline';
import {css} from "@/lib/utils/tags";

interface DotConfig {
    width?: number;
    height?: number;
    radius?: number;
    color?: string;
    position?: string; // e.g., 'left bottom 0px', 'center'
}

/**
 * Generates the raw properties for a dot leader background.
 * Returns an object suitable for programmatic use (e.g. CSS-in-JS).
 */
export function generateDotLeaderData(config: DotConfig = {}) {
    const {
        width = 4,
        height = 4,
        radius = 0.6,
        color = 'currentColor',
        position = 'left bottom 0px'
    } = config;

    const cx = width / 2;
    // Position dot at the bottom of the SVG canvas
    const cy = height - radius;

    // Encode color to be safe in Data URI (e.g. #aaa -> %23aaa)
    const encodedColor = color.startsWith('#') ? color.replace('#', '%23') : color;

    const svgContent = `%3Csvg xmlns='http://www.w3.org/2000/svg' width='${width}' height='${height}' viewBox='0 0 ${width} ${height}'%3E%3Ccircle cx='${cx}' cy='${cy}' r='${radius}' fill='${encodedColor}' /%3E%3C/svg%3E`;
    
    return {
        backgroundImage: `url("data:image/svg+xml,${svgContent}")`,
        backgroundSize: `${width}px ${height}px`,
        backgroundRepeat: 'repeat-x',
        backgroundPosition: position
    };
}

/**
 * Generates a complete CSS block for a dot leader background using a dynamic SVG.
 * Returns a string containing background-image, size, repeat, and position rules.
 * @deprecated functionality moved to inline SVG in generateTocHtml
 */
export function generateDotLeaderCss(config: DotConfig = {}): string {
    // Return empty string as we use inline SVG now
    return ''; 
}

export function generateTocHtml(
    content: string,
    title: string,
    // These might be used for advanced logic later, but for now we render text as-is
    offset: number, 
    numberingStyle: any,
    indentStep: number = 20 // Default indentation step in pt
): { html: string, styles: string } {
    
    const lines = content.split('\n');
    let html = `<h1 class="toc-title">${escapeHtml(title)}</h1>`;

    // Dot leader configuration
    const dotDiameter = 2; // px
    const dotGap = 6;      // px
    const dotColor = "currentColor";
    const maxWidth = 2000; // px, sufficient for most page widths
    const dotCount = Math.ceil(maxWidth / dotGap);

    // --- SVG IMPLEMENTATION (Active) ---
    // Generate a master row of dots using pure vector circles.
    // This avoids:
    // 1. Safari's stroke-dasharray rendering bugs (connected lines).
    // 2. Pattern rasterization issues (blurriness).
    // 3. File size bloat (by defining once and referencing).
    // Set safetyMargin to 0 to ensure dots are perfectly aligned with the bottom on screen.
    // cy = -1 (radius) puts the bottom at y=0.
    const safetyMargin = 0; 
    
    let dotsHtml = '';
    for (let i = 0; i < dotCount; i++) {
        const cx = i * dotGap + dotDiameter / 2;
        dotsHtml += `<circle cx="${cx}" cy="-${dotDiameter / 2 + safetyMargin}" r="${dotDiameter / 2}" />`;
    }

    const dotsDef = `
    <svg width="0" height="0" style="position: absolute; overflow: hidden;">
        <defs>
            <g id="toc-dots-row" fill="${dotColor}">
                ${dotsHtml}
            </g>
        </defs>
    </svg>`;
    
    // Append dotsDef here, BEFORE the <ul> to ensure definitions are available
    html += dotsDef;

    // Inject indent step as CSS variable
    html += `<ul class="toc-list" style="--toc-indent-step: ${indentStep}pt;">`;

    // Leader SVG simply references the master row.
    // y="100%" moves the reference point to the bottom of the container.
    // overflow: hidden ensures we only see what we need.
    const leaderSvg = `
    <svg class="dotted-line" width="100%" height="1em" style="display: block; overflow: hidden;">
        <use href="#toc-dots-row" y="100%" />
    </svg>`;

    for (const line of lines) {
        if (!line.trim()) continue;

        // Parse indentation
        const indentMatch = line.match(/^(\s*)/);
        const whitespace = indentMatch ? indentMatch[1] : '';
        
        // Calculate level
        const tabCount = (whitespace.match(/\t/g) || []).length;
        const spaceCount = (whitespace.match(/ /g) || []).length;
        const level = tabCount + Math.floor(spaceCount / 2);

        // Parse label and page
        const trimmed = line.trim();
        const pageMatch = trimmed.match(/^(.*?)\s+(\d+|[ivxIVX]+)$/);
        
        let label = trimmed;
        let page = '';

        if (pageMatch) {
            label = pageMatch[1];
            page = pageMatch[2];
        }
        
        // Strip trailing dots/leaders from label manually entered by user
        label = label.replace(/[.\s]+$/, '');

        // Inject level variable for dynamic padding calculation in CSS
        html += `
            <li class="toc-item" style="--toc-level: ${level};" data-target-page="${escapeHtml(page)}">
                <span class="toc-label">${escapeHtml(label)}</span>
                <span class="toc-leader">${leaderSvg}</span>
                <span class="toc-page">${escapeHtml(page)}</span>
            </li>
        `;
    }
    html += `</ul>`;

    // Combine external styles
    const styles = css`
        ${tocStyles}
        /* Ensure the leader container aligns properly */
        .toc-leader {
            display: flex;
            align-items: flex-end; /* Align SVG to bottom */
            overflow: hidden;
        }
    `;
    return { html, styles };
}

/**
 * BACKUP: Canvas implementation of TOC generation.
 * Retained for reference or future fallback.
 */
export function _generateTocHtmlCanvas(
    content: string,
    title: string,
    offset: number, 
    numberingStyle: any,
    indentStep: number = 20
): { html: string, styles: string } {
    const lines = content.split('\n');
    let html = `<h1 class="toc-title">${escapeHtml(title)}</h1>`;
    html += `<ul class="toc-list" style="--toc-indent-step: ${indentStep}pt;">`;

    const dotDiameter = 2; 
    const dotGap = 6;      
    const leaderCanvasHtml = `<canvas class="toc-leader-canvas" style="width: 100%; height: 1em; display: block;"></canvas>`;

    const canvasScript = `
    <script>
    (function() {
        function drawDots() {
            const canvases = document.querySelectorAll('.toc-leader-canvas');
            const dotGap = ${dotGap};
            const dotDiameter = ${dotDiameter};
            const radius = dotDiameter / 2;
            const color = window.getComputedStyle(document.body).color || 'black';

            canvases.forEach(canvas => {
                const rect = canvas.getBoundingClientRect();
                const dpr = window.devicePixelRatio || 1;
                
                if (canvas.width !== rect.width * dpr || canvas.height !== rect.height * dpr) {
                     canvas.width = rect.width * dpr;
                     canvas.height = rect.height * dpr;
                }
                
                const ctx = canvas.getContext('2d');
                ctx.scale(dpr, dpr);
                ctx.fillStyle = color;

                const cy = rect.height - radius;
                const count = Math.ceil(rect.width / dotGap);
                
                for (let i = 0; i < count; i++) {
                    const cx = i * dotGap + radius;
                    if (cx + radius > rect.width) break;
                    
                    ctx.beginPath();
                    ctx.arc(cx, cy, radius, 0, Math.PI * 2);
                    ctx.fill();
                }
            });
        }
        window.addEventListener('load', drawDots);
        drawDots();
        window.addEventListener('resize', drawDots);
        window.matchMedia('print').addListener((m) => { if (m.matches) drawDots(); });
    })();
    </script>
    `;

    for (const line of lines) {
        if (!line.trim()) continue;
        const indentMatch = line.match(/^(\s*)/);
        const whitespace = indentMatch ? indentMatch[1] : '';
        const tabCount = (whitespace.match(/\t/g) || []).length;
        const spaceCount = (whitespace.match(/ /g) || []).length;
        const level = tabCount + Math.floor(spaceCount / 2);
        const trimmed = line.trim();
        const pageMatch = trimmed.match(/^(.*?)\s+(\d+|[ivxIVX]+)$/);
        let label = trimmed;
        let page = '';
        if (pageMatch) {
            label = pageMatch[1];
            page = pageMatch[2];
        }
        label = label.replace(/[.\s]+$/, '');
        html += `
            <li class="toc-item" style="--toc-level: ${level};" data-target-page="${escapeHtml(page)}">
                <span class="toc-label">${escapeHtml(label)}</span>
                <span class="toc-leader">${leaderCanvasHtml}</span>
                <span class="toc-page">${escapeHtml(page)}</span>
            </li>
        `;
    }
    html += `</ul>`;
    html += canvasScript;

    const styles = css`
        ${tocStyles}
        .toc-leader {
            display: flex;
            align-items: flex-end; 
            overflow: hidden;
        }
    `;
    return { html, styles };
}

function escapeHtml(text: string): string {
    return text
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}