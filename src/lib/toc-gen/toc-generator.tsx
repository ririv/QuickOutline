import tocStyles from './toc.css?inline';
import { css } from "@/lib/utils/tags";
import { type PageLayout, PAGE_SIZES_MM } from '@/lib/types/page';
import { createElement, Fragment } from '@/lib/utils/jsx';

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

export function generateTocHtml(
    content: string,
    title: string,
    offset: number, 
    numberingStyle: any,
    indentStep: number = 20,
    pageLayout?: PageLayout
): { html: string, styles: string } {
    
    const lines = content.split('\n');
    
    const dotDiameter = 2; 
    const dotGap = 6;      
    const dotColor = "currentColor";
    
    let pageWidthMm = PAGE_SIZES_MM['A4'][0];
    if (pageLayout) {
        const size = PAGE_SIZES_MM[pageLayout.size] || PAGE_SIZES_MM['A4'];
        pageWidthMm = pageLayout.orientation === 'landscape' ? size[1] : size[0];
    }
    const maxWidth = Math.ceil(pageWidthMm * 3.8); 
    const dotCount = Math.ceil(maxWidth / dotGap);

    let dotsHtml = [];
    const safetyMargin = 0; 

    for (let i = 0; i < dotCount; i++) {
        const cx = i * dotGap + dotDiameter / 2;
        // Keep JSX here for consistency
        dotsHtml.push(<circle cx={cx} cy={-(dotDiameter / 2 + safetyMargin)} r={dotDiameter / 2} />);
    }
    
    // Back to JSX!
    const htmlOutput = (
        <>
            <h1 class="toc-title">{escapeHtml(title)}</h1>
            <svg width="0" height="0" style={{position: "absolute", overflow: "hidden"}}>
                <defs>
                    <g id="toc-dots-row" fill={dotColor}>
                        {dotsHtml}
                    </g>
                </defs>
            </svg>
            <ul class="toc-list" style={{ '--toc-indent-step': `${indentStep}pt` }}>
                {lines.map(line => {
                    if (!line.trim()) return null;

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

                    const leaderSvg = (
                        <svg class="dotted-line" width="100%" height="1em" style={{display: "block", overflow: "hidden"}}>
                            <use href="#toc-dots-row" y="100%" />
                        </svg>
                    );

                    return (
                        <li class="toc-item" style={{ '--toc-level': level }} data-target-page={escapeHtml(page)}>
                            <span class="toc-label">{escapeHtml(label)}</span>
                            <span class="toc-leader">{leaderSvg}</span>
                            <span class="toc-page">{escapeHtml(page)}</span>
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