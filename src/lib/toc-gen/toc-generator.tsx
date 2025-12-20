import tocStyles from './toc.css?inline';
import { css } from "@/lib/utils/tags";
import { type PageLayout, PAGE_SIZES_MM } from '@/lib/types/page';
import { createElement, Fragment } from '@/lib/utils/jsx';
import { parseTocLine } from './parser';

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
                            
                            label = label.replace(/[.\s]+$/, '');
                    // The leader SVG for each item will also be populated by fixDots.js
                    const itemLeaderSvg = (
                        <svg class="dotted-line" width="100%" height="1em" style={{display: "block", overflow: "hidden"}} />
                    );

                    return (
                        <li class="toc-item" style={{ '--toc-level': level }} data-target-page={escapeHtml(targetLink)}>
                            <span class="toc-label">{escapeHtml(label)}</span>
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