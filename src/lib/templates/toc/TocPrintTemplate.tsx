// src/lib/preview-engine/PrintTemplate.tsx

import { createElement, Fragment } from '@/lib/utils/jsx.ts';
import fixDotsRaw from '@/lib/templates/toc/fix-dots.js?raw'; // Corrected import path

interface PrintTemplateProps {
    styles: string;
    pageCss: string;
    headerHtml: string;
    footerHtml: string;
    tocHtml: string;
    dotGap?: number; // Added
}

export function TocPrintTemplate({ styles, pageCss, headerHtml, footerHtml, tocHtml, dotGap = 6 }: PrintTemplateProps) {
    // Transform ESM source to standalone script for injection
    const fixDotsScript = `
        ${fixDotsRaw.replace('export function', 'function')}

        (function() {
            const gap = ${dotGap}; // Dynamically inject dotGap here
            window.addEventListener('load', () => fixDots(gap));
            
            // Paged.js observer
            const observer = new MutationObserver((mutations) => {
                for (const m of mutations) {
                    if (m.attributeName === 'class' && document.body.classList.contains('pagedjs_ready')) {
                        fixDots(gap);
                        observer.disconnect();
                    }
                }
            });
            observer.observe(document.body, { attributes: true });
        })();
    `;

    return (
        <html>
            <head>
                <meta charset="UTF-8" />
                <title>Table of Contents</title>
                <style>{styles}</style>
                <style>{pageCss}</style>
                <script src="libs/paged.polyfill.min.js"></script>
                <script>{fixDotsScript}</script>
            </head>
            <body>
                <div class="print-header">{headerHtml}</div>
                <div class="print-footer">{footerHtml}</div>
                {tocHtml}
            </body>
        </html>
    );
}
