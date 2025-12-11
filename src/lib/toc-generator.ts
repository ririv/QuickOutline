export function generateTocHtml(
    content: string,
    title: string,
    // These might be used for advanced logic later, but for now we render text as-is
    offset: number, 
    numberingStyle: any
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
    const dotBottomDist = dotHeight - cy; // Should be equal to dotRadius

    // Generate SVG Data URI
    const svgContent = `%3Csvg xmlns='http://www.w3.org/2000/svg' width='${dotWidth}' height='${dotHeight}' viewBox='0 0 ${dotWidth} ${dotHeight}'%3E%3Ccircle cx='${cx}' cy='${cy}' r='${dotRadius}' fill='${dotColor}' /%3E%3C/svg%3E`;
    const bgImage = `url("data:image/svg+xml,${svgContent}")`;

        const lines = content.split('\n');
        let html = `<h1 class="toc-title">${escapeHtml(title)}</h1>`;
        // Inject baselineOffset as a CSS variable
        html += `<ul class="toc-list">`;
    
        for (const line of lines) {
            if (!line.trim()) continue;
    
            // Parse indentation
            // Strategy: 1 Tab = 1 Level. 2 Spaces = 1 Level (conservative) or 4 Spaces?
            // Most editors convert Tab to 4 spaces.
            const indentMatch = line.match(/^(\s*)/);
            const whitespace = indentMatch ? indentMatch[1] : '';
            
            // Calculate level
            // Count tabs
            const tabCount = (whitespace.match(/\t/g) || []).length;
            // Count spaces
            const spaceCount = (whitespace.match(/ /g) || []).length;
            
            // Total level = tabs + floor(spaces / 2). 
            // Using 2 spaces = 1 level is common for quick outlining.
            const level = tabCount + Math.floor(spaceCount / 2);
    
            // Parse label and page
            const trimmed = line.trim();
            // Regex to find page number at the end
            // Matches: whitespace + (digits OR roman numerals) + end of string
            // We capture the label and the page
            const pageMatch = trimmed.match(/^(.*?)\s+(\d+|[ivxIVX]+)$/);
            
            let label = trimmed;
            let page = '';

            if (pageMatch) {
                label = pageMatch[1];
                page = pageMatch[2];
            }
            
            // Strip trailing dots/leaders from label manually entered by user
            label = label.replace(/[.\s]+$/, '');

            html += `
                <li class="toc-item toc-level-${level}">
                    <span class="toc-label">${escapeHtml(label)}</span>
                    <span class="toc-leader"></span>
                    <span class="toc-page">${escapeHtml(page)}</span>
                </li>
            `;
        }
        html += `</ul>`;
    
        const styles = `
            .toc-title {
                text-align: center;
                font-size: 20pt;
                font-weight: normal; /* Java backend uses regular font weight usually */
                margin-top: 0;
                margin-bottom: 20pt;
                font-family: Helvetica, Arial, sans-serif; 
            }
            .toc-list {
                list-style: none;
                padding: 0;
                margin: 0;
                font-family: Helvetica, Arial, sans-serif;
                font-size: 12pt; /* Standard body size */
                line-height: 1.6; /* Match TocEditor line height */
            }
            .toc-item {
                display: flex;
                align-items: baseline; /* Align text baselines */
                margin-bottom: 4px;
                break-inside: avoid;
            }
            .toc-label {
                flex: 0 0 auto;
            }
        .toc-leader {
            flex: 1 1 auto;
            min-height: 1em;
            color: #aaa; /* Default dot color */
            
            /* Generated SVG */
            background-image: ${bgImage};
            background-repeat: repeat-x;
            background-size: ${dotWidth}px ${dotHeight}px; 
            
            /* ALIGNMENT LOGIC:
               1. The .toc-item uses 'align-items: baseline', so the bottom edge of this empty 
                  .toc-leader element is aligned to the text baseline.
               2. We position the background image (the dot) at the very bottom of this element (0px).
               3. The SVG is generated such that the dot touches the bottom of the SVG canvas.
               Result: The dot sits exactly on the text baseline.
            */
            background-position: left bottom 0px; 
            
            opacity: 1; 
        }

        .toc-leader::before {
            content: none;
        }            .toc-page {
                flex: 0 0 auto;
                text-align: right;
            }
            
            /* Indentation levels - Java uses 20pt padding per level */
            /* level 0: 0pt */
            /* level 1: 20pt ... */
            /* In CSS, we can just use padding-left on the item or the label? */
            /* Flexbox handles the item, so padding-left on the list item works best. */
            
            .toc-level-0 { padding-left: 0pt; }
            .toc-level-1 { padding-left: 20pt; }
            .toc-level-2 { padding-left: 40pt; }
            .toc-level-3 { padding-left: 60pt; }
            .toc-level-4 { padding-left: 80pt; }
            .toc-level-5 { padding-left: 100pt; }
            .toc-level-6 { padding-left: 120pt; }
            .toc-level-7 { padding-left: 140pt; }
            .toc-level-8 { padding-left: 160pt; }
            
            /* Optional: Hide leader if no page */
            .toc-item:not(:has(.toc-page:not(:empty))) .toc-leader {
                display: none;
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
