import tocStyles from './toc.css?inline';

export function generateTocHtml(
    content: string,
    title: string,
    // These might be used for advanced logic later, but for now we render text as-is
    offset: number, 
    numberingStyle: any,
    indentStep: number = 20 // Default indentation step in pt
): { html: string, styles: string } {
    
    // Dot configuration
    const dotWidth = 4;       
    const dotHeight = 4;
    const dotRadius = 0.6;    
    // Use currentColor so it inherits from the CSS 'color' property
    const dotColor = 'currentColor'; 

    // Derived values
    const cx = dotWidth / 2;
    // Position dot at the bottom of the SVG canvas
    // cy = height - radius (so the bottom of the circle touches the bottom of the canvas)
    const cy = dotHeight - dotRadius; 

    // Generate SVG Data URI
    const svgContent = `%3Csvg xmlns='http://www.w3.org/2000/svg' width='${dotWidth}' height='${dotHeight}' viewBox='0 0 ${dotWidth} ${dotHeight}'%3E%3Ccircle cx='${cx}' cy='${cy}' r='${dotRadius}' fill='${dotColor}' /%3E%3C/svg%3E`;
    const bgImage = `url("data:image/svg+xml,${svgContent}")`;

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
            <li class="toc-item" style="--toc-level: ${level};">
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
            background-image: ${bgImage};
            background-size: ${dotWidth}px ${dotHeight}px; 
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
