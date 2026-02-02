import type { ColumnLayoutConfig } from '@/lib/types/page';

/**
 * Generates CSS for multi-column layouts (both CSS Columns and CSS Grid).
 * 
 * @param config The column layout configuration
 * @param selector The CSS selector to apply the styles to (e.g. ".toc-content.scope-123")
 * @param itemSelector Optional selector for items within the container (e.g. ".toc-item"), used for grid alignment
 * @returns The generated CSS string
 */
export function generateColumnCss(
    config: ColumnLayoutConfig, 
    selector: string,
    itemSelector: string = '' 
): string {
    const { count, direction, gap, rule } = config;
    
    if (count <= 1) return '';

    let css = '';

    if (direction === 'horizontal') {
        // Grid Layout (Z-flow)
        // Items flow Left -> Right, then Top -> Bottom
        let gridRuleStyle = '';
        if (rule) {
            const color = '#ccc'; // Match default rule color
            const stops: string[] = [];
            for (let i = 1; i < count; i++) {
                const percent = (i / count) * 100;
                stops.push(`transparent calc(${percent}% - 0.5px)`);
                stops.push(`${color} calc(${percent}% - 0.5px)`);
                stops.push(`${color} calc(${percent}% + 0.5px)`);
                stops.push(`transparent calc(${percent}% + 0.5px)`);
            }
            gridRuleStyle = `background-image: linear-gradient(to right, ${stops.join(', ')});`;
        }

        // Note: For Grid, we apply styles to the list container if possible, 
        // but since 'selector' might be the wrapper, we might need a child selector.
        // In TOC context, selector is .toc-content, and we need to style .toc-list inside it.
        // But for generic usage, we might assume 'selector' IS the container.
        // Let's assume 'selector' targets the wrapper, and we look for a direct child or specific class if needed.
        // However, to keep it generic, if itemSelector is provided, we might assume a structure.
        
        // For TOC specifically: .toc-content .toc-list
        // For Generic Markdown: .markdown-body (which is the container itself)
        
        // Strategy: Provide a way to target the inner container if it's different.
        // For now, mirroring the logic in toc-generator:
        // selector = .scope.toc-content
        // rule applies to selector + " .toc-list"
        
        // To make this truly generic, we might need an 'innerContainerSelector' param.
        // Or we assume the caller passes the exact container selector.
        
        // Let's adopt a pattern: 
        // If the layout is Grid, we apply display:grid to the container.
        
        css = `
            ${selector} {
                display: grid;
                grid-template-columns: repeat(${count}, 1fr);
                column-gap: ${gap}pt;
                align-items: end;
                ${gridRuleStyle}
            }
            ${itemSelector ? `
            ${selector} ${itemSelector} {
                break-inside: avoid;
                width: 100%;
            }
            ` : ''}
        `;
    } else {
        // Column Layout (N-flow)
        // Items flow Top -> Bottom, then Left -> Right
        const ruleValue = rule ? '1px solid #ccc' : '1px solid transparent';
        css = `
            ${selector} {
                column-count: ${count};
                column-gap: ${gap}pt;
                column-rule: ${ruleValue};
                /* Force columns to fill sequentially rather than balancing heights */
                column-fill: auto;
                height: 100%;
            }
            
            /* Smart Spanning Rules */
            ${selector} > h1, ${selector} > h2, ${selector} .toc-title {
                column-span: all;
                margin-bottom: 24px;
            }
            
            /* Prevention Rules */
            ${selector} pre, 
            ${selector} blockquote, 
            ${selector} table, 
            ${selector} img, 
            ${selector} figure ${itemSelector ? `, ${selector} ${itemSelector}` : ''} { 
                break-inside: avoid; 
            }
        `;
    }

    return css;
}
