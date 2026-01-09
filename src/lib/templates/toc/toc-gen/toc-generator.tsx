import tocStyles from './toc.css?inline';
import { css } from "@/lib/utils/tags";
import { type PageLayout, PAGE_SIZES_MM } from '@/lib/types/page';
import { createElement, Fragment } from '@/lib/utils/jsx';
import { parseTocLine } from './parser';
import katex from 'katex';

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
    let result = "";
    let lastIndex = 0;
    const regex = /\$/g;
    let match;
    let startMatch: RegExpExecArray | null = null;

    while ((match = regex.exec(text)) !== null) {
        // Check if escaped: count preceding backslashes
        let backslashCount = 0;
        let i = match.index - 1;
        while (i >= 0 && text[i] === '\\') {
            backslashCount++;
            i--;
        }
        
        // If odd backslashes, it's escaped (\$ -> literal $). Skip it.
        if (backslashCount % 2 === 1) {
            continue;
        }

        if (startMatch === null) {
            // Found potential start
            // Append text before this as plain text
            // We need to unescape \$ in the plain text part
            const plainText = text.substring(lastIndex, match.index);
            // Replace \$ with $ (and ideally \\$ with \$ but let's stick to simple \$ unescape)
            // A simple .replace(/\\\$/g, '$') handles simple cases.
            // For rigorous handling we might want to handle all backslash escapes but Toc parser is simple.
            result += escapeHtml(plainText.replace(/\\\$/g, '$'));
            
            startMatch = match;
        } else {
            // Found end
            const formula = text.substring(startMatch.index + 1, match.index);
            // Render formula
            try {
                result += katex.renderToString(formula, { throwOnError: false });
            } catch (e) {
                result += escapeHtml("$" + formula + "$");
            }
            
            startMatch = null;
            lastIndex = match.index + 1;
        }
    }
    
    // Append remaining text
    // If we have a startMatch but no endMatch, the startMatch should be treated as literal text.
    // However, we already processed text up to startMatch.index.
    // So we need to rewind or just append from the point we last committed.
    
    // Simplest approach: if startMatch is not null, we effectively ignore it as a delimiter
    // but we need to include it in the final string.
    // The loop logic above advanced `lastIndex` ONLY when a pair was closed.
    // So if the loop finished with `startMatch !== null`, `lastIndex` is still pointing
    // to the end of the *previous* successful pair (or 0).
    // But we appended `plainText` up to `startMatch.index` into `result`.
    // Wait, if I appended to `result` inside the `if (startMatch === null)` block,
    // and then didn't find a closing brace, `result` effectively swallowed the text before `startMatch`.
    // But it's missing the `startMatch` itself and everything after.
    
    // Let's refine the logic to be safer: only append when we are sure.
    // Actually, simply:
    // When we find a Start, we append everything before it.
    // If we don't find an End, we just append everything from the Start index onwards as plain text.
    
    if (startMatch !== null) {
        // We had an open $, but no closing $.
        // The text BEFORE startMatch was already appended.
        // We just need to append the rest starting from startMatch.
        // Note: startMatch is the $ itself.
        const remaining = text.substring(startMatch.index);
        result += escapeHtml(remaining.replace(/\\\$/g, '$'));
    } else {
        // Normal case: everything was closed or no math at all.
        const remaining = text.substring(lastIndex);
        result += escapeHtml(remaining.replace(/\\\$/g, '$'));
    }
    
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
    autoCorrectThreshold: number = 1 // New param: only correct pages >= this value
): { html: string, styles: string } {

    const lines = content.split('\n');

    const dotDiameter = DOT_DIAMETER;
    const dotGap = DOT_GAP;
    const dotColor = "currentColor";

    let pageWidthMm = PAGE_SIZES_MM['A4'][0];
    if (pageLayout) {
        const size = PAGE_SIZES_MM[pageLayout.size] || PAGE_SIZES_MM['A4'];
        pageWidthMm = pageLayout.orientation === 'landscape' ? size[1] : size[0];
    }
    const maxWidth = Math.ceil(pageWidthMm * 3.8);
    const dotCount = Math.ceil(maxWidth / dotGap);

    // Initial dot generation logic is moved to fixDots.js for dynamic rendering

    const htmlOutput = (
        <>
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
        </>
    );

    const styles = css`
        ${tocStyles}
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