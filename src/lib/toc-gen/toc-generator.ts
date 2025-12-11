import tocStyles from './toc.css?inline';

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
 */
export function generateDotLeaderCss(config: DotConfig = {}): string {
    const data = generateDotLeaderData(config);
    return `
        background-image: ${data.backgroundImage};
        background-size: ${data.backgroundSize};
        background-repeat: ${data.backgroundRepeat};
        background-position: ${data.backgroundPosition};
    `;
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
    // Inject indent step as CSS variable
    html += `<ul class="toc-list" style="--toc-indent-step: ${indentStep}pt;">`;

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
                <span class="toc-leader"></span>
                <span class="toc-page">${escapeHtml(page)}</span>
            </li>
        `;
    }
    html += `</ul>`;

    // Combine external styles with dynamic SVG background
    const styles = `
        ${tocStyles}
        .toc-leader {
            ${generateDotLeaderCss()}
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
