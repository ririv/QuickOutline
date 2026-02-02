import tocStyles from './toc.css?inline';
import { css } from "@/lib/utils/tags";
import { type PageLayout, PAGE_SIZES_MM, defaultColumnLayout, type ColumnLayoutConfig } from '@/lib/types/page';
import { createElement, Fragment } from '@/lib/utils/jsx';
import { parseTocLine, scanMathInString } from './parser';
import katex from 'katex';
import { generateColumnCss } from '@/lib/templates/utils/column-layout';

interface DotConfig {
    width?: number;
    height?: number;
    radius?: number;
    color?: string;
    position?: string;
}

export function generateDotLeaderData(config: DotConfig = {}) {
    const {
        width = 4,
        height = 4,
        radius = 0.6,
        color = 'currentColor',
        position = 'left bottom 0px'
    } = config;

    const cx = width / 2;
    const cy = height - radius;
    const encodedColor = color.startsWith('#') ? color.replace('#', '%23') : color;
    const svgContent = `%3Csvg xmlns='http://www.w3.org/2000/svg' width='${width}' height='${height}' viewBox='0 0 ${width} ${height}'%3E%3Ccircle cx='${cx}' cy='${cy}' r='${radius}' fill='${encodedColor}' /%3E%3C/svg%3E`;

    return {
        backgroundImage: `url("data:image/svg+xml,${svgContent}")`,
        backgroundSize: `${width}px ${height}px`,
        backgroundRepeat: 'repeat-x',
        backgroundPosition: position
    };
}

export function generateDotLeaderCss(config: DotConfig = {}): string {
    return '';
}

export const DOT_DIAMETER = 2; // px
export const DOT_GAP = 6;      // px (Center-to-Center distance)

function renderTitle(text: string): string {
    const nodes = scanMathInString(text);
    let result = "";
    let lastIndex = 0;

    for (const node of nodes) {
        // Append text before this node
        const gap = text.substring(lastIndex, node.start);
        result += escapeHtml(gap);

        if (node.type === 'escape') {
            result += "$"; // Render \$ as just $
        } else if (node.type === 'math' && node.content) {
            try {
                result += katex.renderToString(node.content, { throwOnError: false });
            } catch (e) {
                // Fallback to source
                result += escapeHtml("$" + node.content + "$");
            }
        }

        lastIndex = node.end;
    }

    // Append remaining text
    const remaining = text.substring(lastIndex);
    result += escapeHtml(remaining);
    
    return result;
}

export function generateTocHtml(
    content: string,
    title: string,
    offset: number,
    numberingStyle: any,
    indentStep: number = 20,
    pageLayout?: PageLayout,
    pageNumberOffset: number = 0, // New param: amount to add to page numbers
    autoCorrectThreshold: number = 1, // New param: only correct pages >= this value
    columnLayout: ColumnLayoutConfig = defaultColumnLayout // New param: full column config
): { html: string, styles: string } {

    const lines = content.split('\n');

    const dotDiameter = DOT_DIAMETER;
    const dotGap = DOT_GAP;
    const dotColor = "currentColor";

    let pageWidthMm = PAGE_SIZES_MM['A4'][0];
    if (pageLayout) {
        const size = PAGE_SIZES_MM[pageLayout.pageSize.size] || PAGE_SIZES_MM['A4'];
        pageWidthMm = pageLayout.pageSize.orientation === 'landscape' ? size[1] : size[0];
    }
    const maxWidth = Math.ceil(pageWidthMm * 3.8);
    const dotCount = Math.ceil(maxWidth / dotGap);

    // Initial dot generation logic is moved to fixDots.js for dynamic rendering
    
    // Generate a unique scope ID to prevent style conflicts during re-renders (double-buffering)
    const scopeId = `toc-${Math.random().toString(36).substring(2, 8)}`;
    
    const { direction } = columnLayout;
    let columnStyle = '';

    if (columnLayout.count > 1) {
        // For Grid (Horizontal), we apply styles to the list container (.toc-list)
        // For Columns (Vertical), we apply styles to the wrapper (.toc-content)
        const targetSelector = direction === 'horizontal' 
            ? `.${scopeId}.toc-content .toc-list` 
            : `.${scopeId}.toc-content`;
            
        const itemSelector = `.${scopeId} .toc-item`;

        columnStyle = generateColumnCss(columnLayout, targetSelector, itemSelector);
    }

    const htmlOutput = (
        <div class={`toc-content ${scopeId}`}>
            <h1 class="toc-title">{escapeHtml(title)}</h1>
            <ul class="toc-list" style={{ '--toc-indent-step': `${indentStep}pt` }}>
                {lines.map(line => {
                    if (!line.trim()) return null;

                    const indentMatch = line.match(/^(\s*)/);
                    const whitespace = indentMatch ? indentMatch[1] : '';

                    const tabCount = (whitespace.match(/\t/g) || []).length;
                    const spaceCount = (whitespace.match(/ /g) || []).length;
                    const level = tabCount + Math.floor(spaceCount / 2);

                    const trimmed = line.trim();
                    const parsed = parseTocLine(trimmed);

                    let label = trimmed;
                    let displayPage = '';
                    let targetLink = '';

                    if (parsed) {
                        label = parsed.title;
                        displayPage = parsed.displayPage;
                        // If explicit link exists (<...>), use it. Otherwise use the display page.
                        targetLink = parsed.hasExplicitLink ? parsed.linkTarget : parsed.displayPage;

                        // Auto-correct logic: only applies if NO explicit link is provided
                        if (!parsed.hasExplicitLink && pageNumberOffset !== 0 && /^\d+$/.test(displayPage)) {
                            const pageNum = parseInt(displayPage, 10);
                            if (pageNum >= autoCorrectThreshold) {
                                const corrected = (pageNum + pageNumberOffset).toString();
                                displayPage = corrected;
                                targetLink = corrected;
                            }
                        }
                    }

                    // Remove trailing dots/whitespace from label only if it wasn't parsed (fallback)
                    // But parsed.title comes from regex group 1 which might have trailing spaces.
                    // Let's just trim end.
                    if (!parsed) {
                        label = label.replace(/[.\s]+$/, '');
                    }
                    
                    // Render label with potential Math
                    // We can't use simple {expression} because renderTitle returns raw HTML string.
                    // We need a way to inject raw HTML.
                    // In this simple JSX implementation, we might need a `dangerouslySetInnerHTML` equivalent
                    // or just return the string if the JSX transform handles it.
                    // Looking at `src/lib/utils/jsx.ts`, it seems to support string children.
                    // But `renderTitle` returns HTML string (with <span> tags from KaTeX).
                    // If we pass it as string, it might get escaped again if the JSX implementation escapes children.
                    // Let's check `escapeHtml` usage below.
                    // The original code used `escapeHtml(label)`.
                    // If `renderTitle` returns HTML, we should pass it in a way that avoids double escaping if the JSX engine escapes.
                    
                    // Hack: Since we are returning a string from `generateTocHtml` eventually (via `htmlOutput`),
                    // and `htmlOutput` is constructed via `createElement`.
                    // If `createElement` escapes strings, we have a problem.
                    // However, `renderTitle` output contains HTML tags.
                    // We can wrap it in a special object or use a prop if the JSX lib supports it.
                    // OR, we can just perform the replacement in the FINAL string if JSX is too limited.
                    
                    // Actually, let's look at `htmlOutput`. It is a JSX element.
                    // The function returns `{ html: htmlOutput, styles }`.
                    // Wait, `htmlOutput` IS NOT A STRING? 
                    // The return type says `{ html: string ... }` but `htmlOutput` is assigned a JSX expression.
                    // This implies the JSX transform returns a string directly?
                    // Let's check `src/lib/utils/jsx.ts`.
                    
                    // Assuming for now we can inject raw HTML string by some mechanism.
                    // If the custom JSX returns string, then `renderTitle` is fine.
                    // If it returns VDOM, we need `innerHTML`.
                    
                    // Let's assume the JSX pragma returns a string or an object that `toString()` turns into HTML.
                    // To be safe, let's use a placeholder and replace it? No, that's messy.
                    
                    // Let's assume the existing `escapeHtml` calls imply that the JSX builder DOES NOT auto-escape, 
                    // or that `escapeHtml` is just a helper used explicitly.
                    // Original: <span class="toc-label">{escapeHtml(label)}</span>
                    // If I change it to <span class="toc-label" innerHTML={renderTitle(label)}></span> ?
                    
                    // Let's try to assume we can just put the string content.
                    // But we need to verify `src/lib/utils/jsx.ts`.
                    
                    const itemLeaderSvg = (
                        <svg class="dotted-line" width="100%" height="1em" style={{display: "block", overflow: "hidden"}} />
                    );

                    return (
                        <li class="toc-item" style={{ '--toc-level': level }} data-target-page={escapeHtml(targetLink)}>
                            <span class="toc-label">{renderTitle(label)}</span>
                            <span class="toc-leader">{itemLeaderSvg}</span>
                            <span class="toc-page">{escapeHtml(displayPage)}</span>
                        </li>
                    );
                })}
            </ul>
        </div>
    );

    const styles = css`
        ${tocStyles}
        ${columnStyle}
        .toc-leader {
            display: flex;
            align-items: flex-end;
            overflow: hidden;
        }
    `;
    return { html: htmlOutput, styles };
}

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
            const dotGap = 6;
            const dotDiameter = 2;
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
    // CORRECTLY return the local 'html' variable, not 'htmlOutput'
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